# Database Schema Design
## Carbon Credit Marketplace for EV Owners

**Version**: 1.0  
**Last Updated**: October 25, 2025  
**Database Type**: PostgreSQL 14+  
**ORM**: Sequelize / TypeORM / Prisma

---

## Table of Contents

1. [Database Overview](#database-overview)
2. [Core Tables](#core-tables)
3. [Entity Relationship Diagram](#entity-relationship-diagram)
4. [Table Definitions](#table-definitions)
5. [Indexes & Constraints](#indexes--constraints)
6. [Data Volume & Growth](#data-volume--growth)
7. [Backup & Recovery](#backup--recovery)
8. [Migration Strategy](#migration-strategy)

---

## Database Overview

### Database Architecture
- **Primary Database**: PostgreSQL 14+ (ACID compliance, JSON support, advanced indexing)
- **Read Replicas**: 2+ replicas for read scaling
- **Cache Layer**: Redis for session management and frequently accessed data
- **Search Engine**: Elasticsearch for marketplace search
- **Time-Series Data**: TimescaleDB extension for analytics
- **File Storage**: AWS S3 / Google Cloud Storage (external)

### Design Principles
- **Normalization**: 3NF (Third Normal Form) for transactional data
- **Denormalization**: Selected fields for performance (e.g., cached totals)
- **Soft Deletes**: Use `deleted_at` timestamp instead of hard deletes
- **Audit Trail**: Track all changes with `created_at`, `updated_at`, `created_by`, `updated_by`
- **UUID Primary Keys**: For distributed systems and security
- **Partitioning**: For large tables (trips, audit_logs)

---

## Core Tables

### Table Categories

1. **User Management** (6 tables)
   - users
   - user_kyc_documents
   - user_sessions
   - user_settings
   - user_bank_accounts
   - user_2fa_secrets

2. **Vehicle & Trip Data** (3 tables)
   - vehicles
   - trips
   - trip_sync_jobs

3. **Carbon Credits** (3 tables)
   - carbon_credits
   - verification_requests
   - credit_transactions

4. **Marketplace** (4 tables)
   - listings
   - auction_bids
   - saved_searches
   - watchlist

5. **Transactions & Payments** (4 tables)
   - transactions
   - payments
   - escrow_accounts
   - payouts

6. **Certificates** (2 tables)
   - certificates
   - certificate_verifications

7. **Admin & Support** (4 tables)
   - admin_actions
   - support_tickets
   - notifications
   - audit_logs

8. **Configuration** (2 tables)
   - platform_settings
   - emission_factors

---

## Entity Relationship Diagram

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   users     │────1:N──│  vehicles    │────1:N──│   trips     │
└─────────────┘         └──────────────┘         └─────────────┘
       │                                                 │
       │ 1:N                                            │
       ▼                                                │
┌─────────────────────┐                                │
│ verification_       │◄───────────────────────────────┘
│ requests            │
└─────────────────────┘
       │
       │ 1:N
       ▼
┌─────────────────────┐         ┌──────────────┐
│ carbon_credits      │────1:N──│  listings    │
└─────────────────────┘         └──────────────┘
       │                               │
       │ 1:N                           │ 1:N
       ▼                               ▼
┌─────────────────────┐         ┌──────────────┐
│ credit_             │         │ auction_bids │
│ transactions        │         └──────────────┘
└─────────────────────┘                │
       │                               │
       │                               │ N:1
       ▼                               ▼
┌─────────────────────┐         ┌──────────────┐
│ transactions        │────1:1──│ payments     │
└─────────────────────┘         └──────────────┘
       │
       │ 1:1
       ▼
┌─────────────────────┐
│ certificates        │
└─────────────────────┘
```

---

## Table Definitions

### 1. users

**Description**: Core user accounts for all platform roles

```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('evowner', 'buyer', 'verifier', 'admin', 'support')),
    
    -- KYC Information
    kyc_level INTEGER DEFAULT 0 CHECK (kyc_level IN (0, 1, 2)),
    kyc_status VARCHAR(50) DEFAULT 'pending' CHECK (kyc_status IN ('pending', 'approved', 'rejected')),
    kyc_submitted_at TIMESTAMP,
    kyc_approved_at TIMESTAMP,
    
    -- Account Status
    account_status VARCHAR(50) DEFAULT 'active' CHECK (account_status IN ('active', 'suspended', 'closed')),
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    
    -- Profile
    avatar_url TEXT,
    bio TEXT,
    company_name VARCHAR(255),  -- For corporate buyers
    tax_code VARCHAR(50),        -- For corporate buyers
    business_registration_number VARCHAR(50),
    
    -- Security
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    last_password_change TIMESTAMP,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    deleted_at TIMESTAMP,
    
    -- Indexes
    CONSTRAINT email_lowercase CHECK (email = LOWER(email))
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_kyc_status ON users(kyc_status, kyc_level);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
```

---

### 2. user_kyc_documents

**Description**: KYC document submissions and verification history

```sql
CREATE TABLE user_kyc_documents (
    kyc_doc_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Document Information
    kyc_level INTEGER NOT NULL CHECK (kyc_level IN (1, 2)),
    id_type VARCHAR(50) NOT NULL CHECK (id_type IN ('national_id', 'passport', 'driver_license')),
    id_number VARCHAR(100) NOT NULL,
    
    -- Document Files
    id_front_url TEXT NOT NULL,
    id_back_url TEXT,
    selfie_url TEXT NOT NULL,
    address_proof_url TEXT,
    business_license_url TEXT,  -- For corporate buyers
    
    -- Verification
    status VARCHAR(50) DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    reviewed_by UUID REFERENCES users(user_id),
    reviewed_at TIMESTAMP,
    rejection_reason TEXT,
    
    -- Metadata
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kyc_user ON user_kyc_documents(user_id);
CREATE INDEX idx_kyc_status ON user_kyc_documents(status);
```

---

### 3. vehicles

**Description**: Electric vehicles registered by EV owners

```sql
CREATE TABLE vehicles (
    vehicle_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Vehicle Information
    make VARCHAR(100) NOT NULL,  -- VinFast, Tesla, BYD
    model VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL CHECK (year >= 2010 AND year <= 2100),
    vin VARCHAR(17) UNIQUE NOT NULL,
    registration_number VARCHAR(50),
    color VARCHAR(50),
    
    -- Data Source Configuration
    data_source VARCHAR(50) NOT NULL CHECK (data_source IN ('api', 'obd', 'manual')),
    data_source_config JSONB,  -- Store API credentials, OBD settings, etc.
    
    -- Verification
    verification_status VARCHAR(50) DEFAULT 'pending' CHECK (verification_status IN ('pending', 'verified', 'rejected')),
    verified_at TIMESTAMP,
    verified_by UUID REFERENCES users(user_id),
    
    -- Sync Status
    last_sync_at TIMESTAMP,
    sync_frequency VARCHAR(50) DEFAULT 'daily',
    next_sync_at TIMESTAMP,
    sync_status VARCHAR(50) DEFAULT 'success' CHECK (sync_status IN ('success', 'failed', 'in_progress')),
    sync_error_message TEXT,
    
    -- Statistics (Denormalized for performance)
    total_trips INTEGER DEFAULT 0,
    total_distance_km DECIMAL(10, 2) DEFAULT 0,
    total_co2_saved_kg DECIMAL(10, 2) DEFAULT 0,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_vehicles_owner ON vehicles(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_vehicles_vin ON vehicles(vin);
CREATE INDEX idx_vehicles_sync_at ON vehicles(next_sync_at) WHERE deleted_at IS NULL;
```

---

### 4. trips

**Description**: Individual EV trips for carbon credit calculation

**Note**: This table will grow large. Consider partitioning by date.

```sql
CREATE TABLE trips (
    trip_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    
    -- Trip Details
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_seconds INTEGER GENERATED ALWAYS AS (EXTRACT(EPOCH FROM (end_time - start_time))) STORED,
    
    -- Location
    start_lat DECIMAL(10, 7),
    start_lng DECIMAL(10, 7),
    end_lat DECIMAL(10, 7),
    end_lng DECIMAL(10, 7),
    start_address TEXT,
    end_address TEXT,
    
    -- Distance & Energy
    distance_km DECIMAL(10, 2) NOT NULL CHECK (distance_km > 0 AND distance_km <= 500),
    avg_speed DECIMAL(10, 2) CHECK (avg_speed >= 0 AND avg_speed <= 180),
    energy_consumed_kwh DECIMAL(10, 2),
    
    -- Carbon Calculation
    co2_saved_kg DECIMAL(10, 4) NOT NULL,
    
    -- Data Quality
    data_source VARCHAR(50) NOT NULL CHECK (data_source IN ('api', 'obd', 'manual')),
    data_quality_score DECIMAL(3, 2) DEFAULT 1.0 CHECK (data_quality_score >= 0 AND data_quality_score <= 1),
    has_anomaly BOOLEAN DEFAULT FALSE,
    anomaly_description TEXT,
    
    -- Sync Information
    sync_batch_id UUID,
    raw_data JSONB,  -- Store original API response for audit
    
    -- Verification Link
    verification_id UUID REFERENCES verification_requests(verification_id),
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_trip_time CHECK (end_time > start_time)
);

-- Partition by month for scalability
CREATE TABLE trips_2025_10 PARTITION OF trips
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE INDEX idx_trips_vehicle ON trips(vehicle_id, start_time DESC);
CREATE INDEX idx_trips_time ON trips(start_time DESC);
CREATE INDEX idx_trips_verification ON trips(verification_id);
CREATE INDEX idx_trips_anomaly ON trips(has_anomaly) WHERE has_anomaly = TRUE;
```

---

### 5. verification_requests

**Description**: Carbon credit verification requests submitted to CVA

```sql
CREATE TABLE verification_requests (
    verification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(user_id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(vehicle_id),
    
    -- Request Scope
    trip_date_start DATE NOT NULL,
    trip_date_end DATE NOT NULL,
    total_km DECIMAL(10, 2) NOT NULL,
    total_trips INTEGER NOT NULL,
    
    -- Carbon Calculation
    co2_saved_kg DECIMAL(10, 2) NOT NULL,
    credit_amount_tons DECIMAL(10, 4) NOT NULL,
    methodology VARCHAR(100) DEFAULT 'CDM ACM0018',
    calculation_details JSONB,
    
    -- Verification Status
    status VARCHAR(50) DEFAULT 'pending' CHECK (status IN ('pending', 'in_review', 'approved', 'rejected', 'more_info_required')),
    priority VARCHAR(50) DEFAULT 'normal' CHECK (priority IN ('low', 'normal', 'high')),
    
    -- CVA Assignment
    assigned_auditor_id UUID REFERENCES users(user_id),
    assigned_at TIMESTAMP,
    cva_organization VARCHAR(255),
    
    -- Auditor Review
    auditor_notes TEXT,
    rejection_reason TEXT,
    adjusted_amount_tons DECIMAL(10, 4),  -- If auditor recalculates
    
    -- Supporting Documents
    data_file_url TEXT,
    calculation_file_url TEXT,
    
    -- SLA Tracking
    sla_deadline TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_verif_owner ON verification_requests(owner_id);
CREATE INDEX idx_verif_status ON verification_requests(status, priority);
CREATE INDEX idx_verif_auditor ON verification_requests(assigned_auditor_id) WHERE status != 'approved' AND status != 'rejected';
CREATE INDEX idx_verif_sla ON verification_requests(sla_deadline) WHERE status IN ('pending', 'in_review');
CREATE INDEX idx_verif_created ON verification_requests(created_at DESC);
```

---

### 6. carbon_credits

**Description**: Issued carbon credits available for trading

```sql
CREATE TABLE carbon_credits (
    credit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    serial_number VARCHAR(100) UNIQUE NOT NULL,  -- VN-EV-2025-TUV-000012345
    
    -- Ownership
    owner_id UUID NOT NULL REFERENCES users(user_id),
    original_owner_id UUID NOT NULL REFERENCES users(user_id),
    
    -- Credit Details
    amount_tons DECIMAL(10, 4) NOT NULL CHECK (amount_tons > 0),
    vintage_year INTEGER NOT NULL,
    methodology VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    
    -- Verification
    verification_id UUID NOT NULL REFERENCES verification_requests(verification_id),
    cva_organization VARCHAR(255) NOT NULL,
    verified_at TIMESTAMP NOT NULL,
    
    -- Status Tracking
    status VARCHAR(50) DEFAULT 'issued' CHECK (status IN ('pending', 'issued', 'listed', 'sold', 'retired')),
    
    -- Listing Link (if listed)
    listing_id UUID REFERENCES listings(listing_id),
    
    -- Transaction History
    transaction_id UUID REFERENCES transactions(transaction_id),
    
    -- Retirement
    retired_at TIMESTAMP,
    retirement_reason TEXT,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_credits_owner ON carbon_credits(owner_id) WHERE status != 'retired';
CREATE INDEX idx_credits_serial ON carbon_credits(serial_number);
CREATE INDEX idx_credits_status ON carbon_credits(status);
CREATE INDEX idx_credits_vintage ON carbon_credits(vintage_year);
CREATE INDEX idx_credits_verification ON carbon_credits(verification_id);
```

---

### 7. listings

**Description**: Marketplace listings for carbon credits

```sql
CREATE TABLE listings (
    listing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL REFERENCES users(user_id),
    credit_id UUID NOT NULL REFERENCES carbon_credits(credit_id),
    
    -- Listing Details
    listing_type VARCHAR(50) NOT NULL CHECK (listing_type IN ('fixed', 'auction')),
    credit_amount_tons DECIMAL(10, 4) NOT NULL,
    
    -- Pricing (Fixed Price)
    price_per_ton_vnd DECIMAL(15, 2) CHECK (price_per_ton_vnd >= 1000000),
    
    -- Pricing (Auction)
    starting_price_per_ton_vnd DECIMAL(15, 2),
    reserve_price_per_ton_vnd DECIMAL(15, 2),
    current_bid_vnd DECIMAL(15, 2),
    current_bidder_id UUID REFERENCES users(user_id),
    total_bids INTEGER DEFAULT 0,
    
    -- Auction Timing
    auction_start_time TIMESTAMP,
    auction_end_time TIMESTAMP,
    
    -- Listing Information
    title VARCHAR(255),
    description TEXT,
    region VARCHAR(100),
    
    -- Status
    status VARCHAR(50) DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'sold', 'expired', 'cancelled')),
    
    -- Analytics
    views_count INTEGER DEFAULT 0,
    watchers_count INTEGER DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    sold_at TIMESTAMP,
    expires_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_listings_seller ON listings(seller_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_listings_status ON listings(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_listings_type ON listings(listing_type, status);
CREATE INDEX idx_listings_price ON listings(price_per_ton_vnd) WHERE status = 'active';
CREATE INDEX idx_listings_region ON listings(region) WHERE status = 'active';
CREATE INDEX idx_listings_auction_end ON listings(auction_end_time) WHERE listing_type = 'auction' AND status = 'active';
CREATE INDEX idx_listings_created ON listings(created_at DESC);
```

---

### 8. auction_bids

**Description**: Bids placed on auction listings

```sql
CREATE TABLE auction_bids (
    bid_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(listing_id),
    bidder_id UUID NOT NULL REFERENCES users(user_id),
    
    -- Bid Details
    bid_amount_per_ton_vnd DECIMAL(15, 2) NOT NULL,
    total_bid_vnd DECIMAL(15, 2) NOT NULL,
    
    -- Auto-bid
    max_auto_bid_vnd DECIMAL(15, 2),
    is_auto_bid BOOLEAN DEFAULT FALSE,
    
    -- Status
    is_winning BOOLEAN DEFAULT FALSE,
    is_outbid BOOLEAN DEFAULT FALSE,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX idx_bids_listing ON auction_bids(listing_id, created_at DESC);
CREATE INDEX idx_bids_bidder ON auction_bids(bidder_id);
CREATE INDEX idx_bids_winning ON auction_bids(listing_id, is_winning) WHERE is_winning = TRUE;
```

---

### 9. transactions

**Description**: Purchase transactions for carbon credits

```sql
CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(listing_id),
    
    -- Parties
    buyer_id UUID NOT NULL REFERENCES users(user_id),
    seller_id UUID NOT NULL REFERENCES users(user_id),
    
    -- Transaction Details
    credit_amount_tons DECIMAL(10, 4) NOT NULL,
    unit_price_vnd DECIMAL(15, 2) NOT NULL,
    subtotal_vnd DECIMAL(15, 2) NOT NULL,
    platform_fee_vnd DECIMAL(15, 2) NOT NULL,
    total_amount_vnd DECIMAL(15, 2) NOT NULL,
    
    -- Payment
    payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN ('momo', 'vnpay', 'zalopay', 'bank_transfer', 'stripe', 'invoice')),
    payment_id UUID REFERENCES payments(payment_id),
    payment_status VARCHAR(50) DEFAULT 'pending' CHECK (payment_status IN ('pending', 'processing', 'completed', 'failed', 'refunded')),
    
    -- Escrow
    escrow_status VARCHAR(50) DEFAULT 'pending' CHECK (escrow_status IN ('pending', 'held', 'released', 'refunded')),
    escrow_account_id UUID REFERENCES escrow_accounts(escrow_account_id),
    
    -- Certificate
    certificate_id UUID REFERENCES certificates(certificate_id),
    
    -- Billing Information
    billing_info JSONB,  -- Company name, tax code, address, etc.
    
    -- Status
    status VARCHAR(50) DEFAULT 'pending' CHECK (status IN ('pending', 'processing', 'completed', 'failed', 'cancelled', 'refunded')),
    
    -- Timeline
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_completed_at TIMESTAMP,
    credits_transferred_at TIMESTAMP,
    certificate_issued_at TIMESTAMP,
    settlement_completed_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    
    -- Refund
    refund_requested_at TIMESTAMP,
    refund_reason TEXT,
    refunded_at TIMESTAMP
);

CREATE INDEX idx_trans_buyer ON transactions(buyer_id);
CREATE INDEX idx_trans_seller ON transactions(seller_id);
CREATE INDEX idx_trans_listing ON transactions(listing_id);
CREATE INDEX idx_trans_status ON transactions(status);
CREATE INDEX idx_trans_created ON transactions(created_at DESC);
CREATE INDEX idx_trans_payment_status ON transactions(payment_status) WHERE payment_status NOT IN ('completed', 'refunded');
```

---

### 10. payments

**Description**: Payment records for transactions

```sql
CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(transaction_id),
    
    -- Payment Details
    payment_method VARCHAR(50) NOT NULL,
    amount_vnd DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    
    -- Gateway Information
    gateway_provider VARCHAR(50),  -- momo, vnpay, stripe
    gateway_transaction_id VARCHAR(255),
    gateway_reference VARCHAR(255),
    
    -- Payment URLs
    payment_url TEXT,
    redirect_url TEXT,
    callback_url TEXT,
    
    -- Status
    status VARCHAR(50) DEFAULT 'pending' CHECK (status IN ('pending', 'processing', 'completed', 'failed', 'expired', 'refunded')),
    
    -- Gateway Response
    gateway_response JSONB,
    error_code VARCHAR(50),
    error_message TEXT,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    authorized_at TIMESTAMP,
    captured_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    refunded_at TIMESTAMP,
    expires_at TIMESTAMP,
    
    -- Metadata
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX idx_payments_transaction ON payments(transaction_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_gateway_txn ON payments(gateway_transaction_id);
```

---

### 11. certificates

**Description**: Carbon credit certificates issued to buyers

```sql
CREATE TABLE certificates (
    certificate_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificate_number VARCHAR(100) UNIQUE NOT NULL,  -- CERT-2025-123456
    transaction_id UUID NOT NULL REFERENCES transactions(transaction_id),
    
    -- Parties
    buyer_id UUID NOT NULL REFERENCES users(user_id),
    seller_id UUID NOT NULL REFERENCES users(user_id),
    
    -- Credit Details
    credit_amount_tons DECIMAL(10, 4) NOT NULL,
    credit_serial_numbers TEXT[] NOT NULL,
    vintage_year INTEGER NOT NULL,
    methodology VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    
    -- Verification Details
    cva_organization VARCHAR(255) NOT NULL,
    auditor_name VARCHAR(255),
    verification_date DATE NOT NULL,
    
    -- Certificate Files
    pdf_url TEXT NOT NULL,
    qr_code_data TEXT NOT NULL,
    blockchain_hash VARCHAR(255),  -- Optional blockchain verification
    
    -- Digital Signature
    issuer_signature TEXT,
    signature_algorithm VARCHAR(50),
    
    -- Status
    status VARCHAR(50) DEFAULT 'valid' CHECK (status IN ('valid', 'revoked', 'expired')),
    validity_period_years INTEGER DEFAULT 0,  -- 0 = perpetual
    
    -- Retirement
    is_retired BOOLEAN DEFAULT FALSE,
    retired_at TIMESTAMP,
    retirement_reason TEXT,
    
    -- Timestamps
    issue_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    revocation_reason TEXT
);

CREATE INDEX idx_cert_buyer ON certificates(buyer_id);
CREATE INDEX idx_cert_transaction ON certificates(transaction_id);
CREATE INDEX idx_cert_number ON certificates(certificate_number);
CREATE INDEX idx_cert_status ON certificates(status);
CREATE INDEX idx_cert_issue_date ON certificates(issue_date DESC);
```

---

### 12. notifications

**Description**: System notifications to users

```sql
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Notification Content
    type VARCHAR(100) NOT NULL,  -- trip_synced, verification_approved, listing_sold, etc.
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    
    -- Action
    action_url TEXT,
    action_label VARCHAR(100),
    
    -- Channels
    channel VARCHAR(50) NOT NULL CHECK (channel IN ('in_app', 'email', 'sms', 'push')),
    
    -- Status
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    
    -- Delivery
    sent_at TIMESTAMP,
    delivery_status VARCHAR(50) CHECK (delivery_status IN ('pending', 'sent', 'failed')),
    delivery_error TEXT,
    
    -- Metadata
    data JSONB,  -- Additional contextual data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notif_user ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notif_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notif_type ON notifications(type, created_at DESC);
```

---

### 13. audit_logs

**Description**: Comprehensive audit trail for compliance

**Note**: This table will grow very large. Partition by month.

```sql
CREATE TABLE audit_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Actor
    user_id UUID REFERENCES users(user_id),
    actor_role VARCHAR(50),
    
    -- Action
    action VARCHAR(100) NOT NULL,  -- create, update, delete, approve, reject, etc.
    resource_type VARCHAR(100) NOT NULL,  -- user, vehicle, transaction, etc.
    resource_id UUID,
    
    -- Changes
    old_values JSONB,
    new_values JSONB,
    changes JSONB,  -- Computed diff
    
    -- Context
    description TEXT,
    reason TEXT,
    ip_address INET,
    user_agent TEXT,
    request_id VARCHAR(100),
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Hash Chain (for tamper detection)
    previous_hash VARCHAR(64),
    log_hash VARCHAR(64)
);

-- Partition by month
CREATE TABLE audit_logs_2025_10 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE INDEX idx_audit_user ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_action ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
```

---

### 14. platform_settings

**Description**: Configurable platform settings

```sql
CREATE TABLE platform_settings (
    setting_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Setting Identification
    category VARCHAR(100) NOT NULL,  -- fees, limits, features, etc.
    key VARCHAR(100) NOT NULL,
    
    -- Value
    value JSONB NOT NULL,
    value_type VARCHAR(50) NOT NULL CHECK (value_type IN ('string', 'number', 'boolean', 'json')),
    
    -- Metadata
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    is_editable BOOLEAN DEFAULT TRUE,
    
    -- Versioning
    version INTEGER DEFAULT 1,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(user_id),
    
    UNIQUE(category, key)
);

-- Example settings
INSERT INTO platform_settings (category, key, value, value_type, description) VALUES
('fees', 'transaction_fee_percentage', '5', 'number', 'Platform fee as percentage of transaction'),
('fees', 'withdrawal_fee_percentage', '2', 'number', 'Withdrawal fee percentage'),
('fees', 'withdrawal_fee_minimum_vnd', '50000', 'number', 'Minimum withdrawal fee'),
('limits', 'daily_withdrawal_limit_vnd', '50000000', 'number', 'Maximum daily withdrawal amount'),
('limits', 'minimum_withdrawal_vnd', '500000', 'number', 'Minimum withdrawal amount'),
('limits', 'max_active_listings_per_user', '10', 'number', 'Maximum active listings per user'),
('features', 'auction_enabled', 'true', 'boolean', 'Enable auction functionality'),
('features', 'blockchain_verification', 'false', 'boolean', 'Enable blockchain certificate verification');
```

---

## Indexes & Constraints

### Primary Indexes (Already defined above)
- All tables have UUID primary keys
- Foreign key indexes automatically created
- Custom indexes on frequently queried columns

### Additional Performance Indexes

```sql
-- Composite indexes for common query patterns

-- Users: Login queries
CREATE INDEX idx_users_login ON users(email, password_hash) WHERE deleted_at IS NULL AND account_status = 'active';

-- Marketplace search
CREATE INDEX idx_listings_search ON listings(status, listing_type, region, price_per_ton_vnd) 
    WHERE deleted_at IS NULL;

-- Transaction processing
CREATE INDEX idx_trans_processing ON transactions(status, payment_status, created_at)
    WHERE status IN ('pending', 'processing');

-- Wallet balance queries
CREATE INDEX idx_credits_wallet ON carbon_credits(owner_id, status, amount_tons)
    WHERE deleted_at IS NULL;
```

### Constraints Summary

1. **Check Constraints**: Data validation at database level
2. **Unique Constraints**: Prevent duplicates (email, VIN, serial numbers)
3. **Foreign Keys**: Referential integrity with CASCADE deletes where appropriate
4. **Not Null**: Critical fields must have values
5. **Default Values**: Sensible defaults for all status fields

---

## Data Volume & Growth

### Year 1 Projections

| Table | Initial | Monthly Growth | Year 1 Total | Storage (Approx) |
|-------|---------|----------------|--------------|------------------|
| users | 1,000 | 1,000 | 13,000 | 10 MB |
| vehicles | 800 | 800 | 10,400 | 5 MB |
| trips | 100,000 | 300,000 | 3,700,000 | 15 GB |
| verification_requests | 500 | 833 | 10,500 | 50 MB |
| carbon_credits | 500 | 667 | 8,500 | 5 MB |
| listings | 200 | 400 | 5,000 | 10 MB |
| transactions | 150 | 250 | 3,150 | 20 MB |
| certificates | 150 | 250 | 3,150 | 5 MB |
| notifications | 5,000 | 50,000 | 605,000 | 200 MB |
| audit_logs | 10,000 | 100,000 | 1,210,000 | 500 MB |
| **TOTAL** | | | | **~16 GB** |

### Partitioning Strategy

**Large Tables Requiring Partitioning:**

1. **trips** - Partition by month (start_time)
   - Retention: 5 years online, then archive to cold storage
   - Query pattern: Usually recent data (last 3-6 months)

2. **audit_logs** - Partition by month (created_at)
   - Retention: 10 years (regulatory requirement)
   - Archive old partitions to compressed storage

3. **notifications** - Partition by month (created_at)
   - Retention: 3 months online, then delete
   - No archiving needed

```sql
-- Example: Create partitions for next 12 months
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    table_name TEXT;
BEGIN
    FOR i IN 0..11 LOOP
        start_date := DATE_TRUNC('month', CURRENT_DATE + (i || ' months')::INTERVAL);
        end_date := start_date + INTERVAL '1 month';
        table_name := 'trips_' || TO_CHAR(start_date, 'YYYY_MM');
        
        EXECUTE FORMAT('CREATE TABLE IF NOT EXISTS %I PARTITION OF trips FOR VALUES FROM (%L) TO (%L)',
                      table_name, start_date, end_date);
    END LOOP;
END $$;
```

---

## Backup & Recovery

### Backup Strategy

**1. Continuous Replication**
- Primary → Read Replica (async replication)
- Cross-region replica for disaster recovery
- WAL (Write-Ahead Logging) archived to S3

**2. Automated Snapshots**
- Full database backup: Daily at 2:00 AM
- Incremental backups: Every 6 hours
- Retention: Daily (7 days), Weekly (4 weeks), Monthly (12 months)

**3. Point-in-Time Recovery (PITR)**
- WAL archiving enables PITR up to 35 days
- RPO (Recovery Point Objective): <1 hour
- RTO (Recovery Time Objective): <4 hours

**4. Backup Testing**
- Monthly restoration drills
- Quarterly disaster recovery simulation

### Backup Commands

```bash
# Full database backup
pg_dump -h localhost -U dbuser -Fc carbon_marketplace > backup_$(date +%Y%m%d).dump

# Restore from backup
pg_restore -h localhost -U dbuser -d carbon_marketplace -c backup_20251025.dump

# Continuous archiving (WAL)
# In postgresql.conf:
# archive_mode = on
# archive_command = 'aws s3 cp %p s3://backups/wal/%f'
```

---

## Migration Strategy

### Schema Versioning

Use migration tools like:
- **Java/Spring Boot**: Flyway (recommended), Liquibase
- **Database-agnostic**: Flyway, Liquibase

### Migration Best Practices

1. **Never destructive migrations in production**
   - Add new columns as nullable first
   - Backfill data
   - Add NOT NULL constraint later

2. **Version control all migrations**
   - Sequential naming: `001_initial_schema.sql`, `002_add_blockchain.sql`
   - Each migration reversible (up/down)

3. **Test migrations on staging**
   - Use production data snapshot
   - Measure migration time for large tables
   - Plan maintenance window

### Example Migration (Flyway)

```sql
-- V003__add_blockchain_hash_to_certificates.sql

-- Add column as nullable
ALTER TABLE certificates
ADD COLUMN blockchain_hash VARCHAR(255);

-- Create index
CREATE INDEX idx_cert_blockchain ON certificates(blockchain_hash)
WHERE blockchain_hash IS NOT NULL;

-- Flyway automatically tracks version and execution time
```

**Rollback Script** (if needed):
```sql
-- U003__add_blockchain_hash_to_certificates.sql

DROP INDEX IF EXISTS idx_cert_blockchain;
ALTER TABLE certificates DROP COLUMN IF EXISTS blockchain_hash;
```

---

## Database Performance Optimization

### Query Optimization

1. **Use EXPLAIN ANALYZE** for slow queries
2. **Proper indexing** on WHERE, JOIN, ORDER BY columns
3. **Avoid N+1 queries** - use JOINs or batch loading
4. **Pagination** for large result sets
5. **Materialized views** for complex aggregations

### Connection Pooling

```yaml
# application.yml - Spring Boot configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/carbon_marketplace
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

### Caching Strategy

1. **Application-level cache** (Redis)
   - User sessions
   - Frequently accessed settings
   - Marketplace listing cache (5 min TTL)

2. **Database query cache**
   - Materialized views for dashboards
   - Refresh every 15 minutes

---

**Document Version**: 1.0  
**Last Updated**: October 25, 2025  
**Database Version**: PostgreSQL 14.x  
**Total Tables**: 14 core tables + partitions

