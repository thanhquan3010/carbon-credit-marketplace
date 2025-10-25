# Requirements Specification
## Carbon Credit Marketplace for EV Owners

**Version**: 1.0  
**Last Updated**: October 25, 2025

---

## Table of Contents

1. [Business Requirements](#1-business-requirements)
2. [Functional Requirements](#2-functional-requirements)
   - 2.1 [EV Owner Functions](#21-ev-owner-functions)
   - 2.2 [Corporate Buyer Functions](#22-corporate-buyer-functions)
   - 2.3 [CVA Functions](#23-carbon-verification--audit-functions)
   - 2.4 [Admin Functions](#24-admin-functions)
3. [Non-Functional Requirements](#3-non-functional-requirements)
   - 3.1 [Performance](#31-performance-requirements)
   - 3.2 [Security](#32-security-requirements)
   - 3.3 [Availability & Reliability](#33-availability--reliability)
   - 3.4 [Usability](#34-usability-requirements)
   - 3.5 [Maintainability](#35-maintainability-requirements)

---

## 1. BUSINESS REQUIREMENTS

### BR-001: Carbon Credit Calculation & Issuance
**Priority**: MUST HAVE  
**Description**: The system must automatically calculate CO₂ emission reductions based on EV trip data and convert them into carbon credits according to international standards.

**Acceptance Criteria**:
- Use standard methodology (e.g., CDM ACM0018)
- Baseline emission factor: 0.15 kg CO₂/km (average gasoline vehicle)
- EV emission factor: 0.05 kg CO₂/km (electricity grid emission)
- Net reduction: 0.10 kg CO₂/km
- Calculation accuracy: ±2% compared to manual calculation
- Processing time: <5 seconds for 10,000 km of data

**Calculation Formula**:
```
CO₂ Saved (kg) = Distance (km) × (ICE Emission Factor - EV Emission Factor)
Carbon Credit (tons) = CO₂ Saved (kg) / 1000
```

---

### BR-002: Marketplace Transaction
**Priority**: MUST HAVE  
**Description**: The platform must support secure and transparent carbon credit trading between sellers and buyers.

**Acceptance Criteria**:
- Support 2 transaction types: Fixed Price and Auction
- Implement escrow mechanism for payment security
- Ensure atomic transactions (all-or-nothing)
- Configurable transaction fee: 5-8%
- Settlement time: T+2 working days
- 100% transaction traceability

---

### BR-003: Verification & Compliance
**Priority**: MUST HAVE  
**Description**: All carbon credits must be audited and verified by CVA organizations before trading.

**Acceptance Criteria**:
- Complete verification workflow with approval gates
- Document storage with version control
- Certificate generation using standard templates
- Full audit trail logging
- SLA: 48 hours for standard verification
- Compliance with Gold Standard, Verra VCS standards

---

### BR-004: Payment Integration
**Priority**: MUST HAVE  
**Description**: Integrate diverse payment methods suitable for the Vietnamese market.

**Acceptance Criteria**:
- **E-wallets**: MoMo, ZaloPay, VNPay, ShopeePay
- **Bank transfer**: Napas, local banks with virtual account numbers
- **International cards**: Visa, Mastercard (via Stripe for foreign buyers)
- **Invoice payment**: NET 30/60 for verified corporate buyers
- **Currency support**: VND (primary), USD (optional)
- **Refund mechanism**: Complete within 7 days

---

### BR-005: Data Security & Privacy
**Priority**: MUST HAVE  
**Description**: Protect personal data and transaction information according to PDPA and GDPR standards.

**Acceptance Criteria**:
- Encryption: AES-256 at rest, TLS 1.3 in transit
- PII data anonymization for analytics
- GDPR right-to-be-forgotten support (30-day deletion)
- Access control: RBAC with least privilege principle
- Security audits: Quarterly penetration testing
- Data retention compliant with regulations

---

### BR-006: AI-Powered Price Recommendation
**Priority**: SHOULD HAVE  
**Description**: Provide optimal pricing recommendations using ML models analyzing market data.

**Acceptance Criteria**:
- Input features: historical prices, demand/supply ratio, region, seasonality
- Model accuracy: MAPE (Mean Absolute Percentage Error) <15%
- Confidence interval: 90%
- Update frequency: Daily model refresh
- Explainability: Display reasoning behind recommendations

---

### BR-007: Mobile-First Experience
**Priority**: SHOULD HAVE  
**Description**: Deliver responsive design and progressive web app capabilities for mobile users.

**Acceptance Criteria**:
- Mobile-responsive design (viewport <768px)
- PWA features: offline mode, push notifications, install prompt
- Page load time: <3 seconds on 4G connection
- Touch-optimized UI components
- Works on iOS Safari and Android Chrome

---

### BR-008: Multi-Language Support
**Priority**: COULD HAVE  
**Description**: Support multiple languages to expand to international markets.

**Acceptance Criteria**:
- Languages: Vietnamese (primary), English, Korean
- i18n framework implementation (react-i18next or similar)
- Real-time currency conversion
- Localized date/time formats
- Complete translation coverage (>95%)

---

## 2. FUNCTIONAL REQUIREMENTS

## 2.1 EV Owner Functions

### FR-EVO-001: User Registration & Onboarding
**Priority**: MUST HAVE

**Description**: Allow EV owners to register accounts and connect vehicles to the platform.

**Detailed Requirements**:

1. **Registration Options**
   - Email/phone registration with OTP verification
   - Social login: Google, Facebook, Apple ID
   - Password requirements: min 8 chars, mixed case, numbers, special chars

2. **KYC Verification**
   - **Level 1**: Name, ID number, selfie photo
   - **Level 2**: ID document verification, address proof (for withdrawals >10M VND)
   - Processing time: Level 1 <24h, Level 2 <5 days

3. **Vehicle Registration**
   - Input vehicle details: make, model, year, VIN
   - Upload registration documents
   - Optional: API verification against GTVT database
   - Support for multiple vehicles per account

4. **Data Source Connection**
   - **Option A - OEM API**: OAuth2 flow with VinFast/Tesla
   - **Option B - OBD-II Device**: Bluetooth pairing
   - **Option C - Manual Upload**: CSV/JSON file upload

**Acceptance Criteria**:
- Registration completion rate >80%
- Vehicle verification success rate >95%
- Onboarding time: <10 minutes average

---

### FR-EVO-002: Trip Data Sync & CO₂ Calculation
**Priority**: MUST HAVE

**Description**: Automatically sync driving data and calculate CO₂ savings.

**Detailed Requirements**:

1. **Automatic Synchronization**
   - Sync frequency: Daily at 2:00 AM
   - Incremental sync (only new trips)
   - Conflict resolution: Last-write-wins
   - Sync status indicator in UI

2. **Trip Data Structure**
   ```json
   {
     "trip_id": "uuid",
     "vehicle_id": "VIN",
     "start_time": "ISO8601",
     "end_time": "ISO8601",
     "distance_km": 45.2,
     "start_location": {"lat": 10.762622, "lng": 106.660172},
     "end_location": {"lat": 10.762622, "lng": 106.660172},
     "avg_speed": 35.5,
     "energy_consumed_kwh": 7.8
   }
   ```

3. **Validation Rules**
   - Distance: 0.1 km to 500 km per trip
   - Average speed: <180 km/h
   - Energy efficiency: 10-25 kWh/100km
   - GPS accuracy threshold: <50m
   - Trip duration: >2 minutes

4. **Calculation Engine**
   - Batch processing: 1000 trips in <30 seconds
   - Outlier detection and flagging
   - Data quality scoring

**Acceptance Criteria**:
- Sync success rate: >98%
- Calculation accuracy: ±2% vs manual
- Processing latency: <1 minute for 100 trips

---

### FR-EVO-003: Carbon Wallet Management
**Priority**: MUST HAVE

**Description**: View and manage carbon credit balances and transaction history.

**Detailed Requirements**:

1. **Wallet Dashboard Display**
   - Available credits (can list for sale)
   - Pending credits (awaiting verification)
   - Listed credits (on marketplace)
   - Sold credits (pending settlement)

2. **Transaction History**
   - Filters: date range, type (credit/debit/sale), status
   - Table columns: date, type, amount (tons), value (VND), status, certificate
   - Export to CSV/PDF

3. **Balance Operations**
   - Reserve credits for listing (lock mechanism)
   - Prevent double-spending
   - Auto-unlock if listing expires/cancelled

4. **Wallet Security**
   - 2FA required for withdrawal >5M VND
   - Email notification for all transactions
   - SMS alert for suspicious activities
   - Daily withdrawal limit: 50M VND

**Acceptance Criteria**:
- Real-time balance updates (WebSocket)
- Transaction history loads in <2 seconds
- 100% transaction traceability

---

### FR-EVO-004: Carbon Credit Listing
**Priority**: MUST HAVE

**Description**: List carbon credits for sale with flexible pricing options.

**Listing Types**:

**A. Fixed Price Listing**
- Seller sets fixed price per ton
- First-come, first-served
- Auto-delisting after 90 days if unsold
- Editable price before sale

**B. Auction Listing**
- Starting price with optional reserve price
- Duration: 1-30 days
- Minimum bid increment: 50,000 VND
- Auto-extend: +10 minutes if bid in last 5 minutes
- Winner has 24 hours to complete payment

**Detailed Requirements**:

1. **Create Listing Form**
   - Credit amount: 0.1 to available balance
   - Listing type selection: Fixed / Auction
   - Price per ton (minimum: 1,000,000 VND)
   - Duration (for auctions)
   - Region/location tag
   - Description (max 500 characters)

2. **AI Price Recommendation**
   - Recommended price with confidence percentage
   - Price range: Min, Avg, Max (last 30 days)
   - Demand indicator: Low / Medium / High
   - Expected sale time estimation

3. **Listing Management Actions**
   - Edit (price, description) - only if no bids
   - Pause listing temporarily
   - Cancel listing (credits return to wallet)
   - Promote listing (paid feature: +10% visibility)

4. **Fees & Commissions**
   - Listing fee: Free (promotional period)
   - Success fee: 5% of transaction value
   - Transparent fee breakdown before confirmation

**Acceptance Criteria**:
- Listing creation: <30 seconds
- AI recommendation load: <3 seconds
- Listing visibility: <5 minutes after creation
- Maximum 10 active listings per user

---

### FR-EVO-005: Earnings & Withdrawal
**Priority**: MUST HAVE

**Description**: Withdraw earnings to bank accounts.

**Detailed Requirements**:

1. **Earnings Dashboard**
   - Total earnings (lifetime)
   - Current month earnings
   - Pending payments
   - Available for withdrawal
   - Monthly earnings trend chart (12 months)

2. **Withdrawal Process**
   - **Step 1**: 2FA authentication + SMS OTP
   - **Step 2**: Bank account selection/addition
     - Bank name, account number, account holder name (must match KYC)
     - Verification with 1 VND test transaction
   - **Step 3**: Amount selection
     - Minimum: 500,000 VND
     - Maximum: 50,000,000 VND/day
     - Fee: 2% or 50,000 VND (whichever is lower)
   - **Step 4**: Confirmation and submission

3. **Withdrawal Status Tracking**
   - States: Requested → Under Review → Approved → Processing → Completed/Failed
   - Timeline: Review <4 hours, Processing 1-2 business days
   - Failed cases: Auto-refund to wallet within 1 hour

4. **Tax & Compliance**
   - Auto-generate VAT-compliant invoice
   - Annual tax report export (PDF)
   - TIN registration for earners >100M VND/year

**Acceptance Criteria**:
- Withdrawal success rate: >99%
- Processing time: <2 business days
- Tax report generation: <5 seconds

---

### FR-EVO-006: Personal Reporting & Analytics
**Priority**: SHOULD HAVE

**Description**: Detailed reports on environmental impact and earnings.

**Report Types**:

**A. Environmental Impact Report**
- Total CO₂ reduced (kg, tons)
- Equivalents: trees planted, ICE km offset, gasoline saved
- Monthly/yearly trends
- Comparison with average EV owner
- Social media sharing feature

**B. Financial Performance Report**
- Total revenue from carbon credits
- Average price per ton achieved
- Number of transactions
- ROI on EV purchase (carbon credit perspective)
- Projected earnings (next 12 months)

**C. Driving Behavior Report**
- Total distance driven
- Average trip distance
- Most frequent routes
- Energy efficiency score
- Eco-driving tips

**Detailed Requirements**:

1. **Report Generation**
   - Filters: date range (last 7/30/90 days, YTD, custom)
   - Vehicle selection (if multiple)
   - Export formats: PDF, Excel, Image (social sharing)

2. **Visualizations**
   - Interactive charts with hover details
   - Color-coded performance indicators
   - Progress bars for goals
   - Heat maps for geographic distribution

3. **Gamification Elements**
   - Badges: Milestones (1 ton, 10 tons, 100 tons CO₂)
   - Leaderboard: Top earners by city/country
   - Achievements: Streaks, referrals, premium features
   - Monthly challenges

**Acceptance Criteria**:
- Report generation: <10 seconds
- Export success rate: >99%
- Mobile chart rendering: <3 seconds

---

## 2.2 Corporate Buyer Functions

### FR-CCB-001: Buyer Registration & Company Verification
**Priority**: MUST HAVE

**Description**: Register company accounts and verify credentials.

**Detailed Requirements**:

1. **Company Registration**
   - Company legal name
   - Business registration number
   - Tax code (MST)
   - Industry/sector
   - Company size (employees, revenue bracket)
   - Address, contact details, website
   - CSR/Sustainability report link (optional)

2. **Document Upload**
   - Business license (GPKD)
   - Tax registration certificate
   - Letter of authorization for representative
   - Company stamp sample

3. **Verification Levels**
   - **Level 1 (Basic)**: <24 hours
     - Auto-check business registration via government API
     - Email domain verification
     - Phone OTP verification
     - Purchase limit: 500M VND/transaction
   
   - **Level 2 (Enhanced)**: 2-5 business days
     - Video call with company representative
     - Office address verification
     - Financial stability check (D&B, credit bureau)
     - Benefit: Unlimited purchases, invoice payment terms

4. **User Roles within Company**
   - Admin: Full access, user management
   - Buyer: Purchase, view reports
   - Finance: Payment approval, invoice management
   - Compliance: Download certificates, reports only

**Acceptance Criteria**:
- Registration completion rate: >70%
- Level 1 verification: <24 hours
- Level 2 verification: <5 business days
- Document rejection rate: <10%

---

### FR-CCB-002: Carbon Credit Search & Discovery
**Priority**: MUST HAVE

**Description**: Search and filter available carbon credits in the marketplace.

**Detailed Requirements**:

1. **Search Interface**
   - Keyword search with auto-complete
   - Voice search (mobile)
   - Quick filters: amount, price, region, listing type, verification status

2. **Advanced Filters**
   - Price range (slider or manual input)
   - Seller reputation (star rating, transaction count)
   - Credit characteristics: methodology, vintage year, certification body
   - Availability: available now, auction ending soon, newly listed

3. **Search Results Display**
   - **List View**: Card layout with key info, quick actions
   - **Map View**: Geographic distribution with cluster markers
   - **Table View**: Sortable columns, bulk selection, CSV export

4. **Saved Searches & Alerts**
   - Save search criteria
   - Email notifications for new matching listings
   - Price drop alerts
   - Auction ending reminders

**Acceptance Criteria**:
- Search results load: <2 seconds for 1000 listings
- Real-time filter application (no page refresh)
- Mobile-responsive UI
- Alert delivery rate: >95%

---

### FR-CCB-003: Purchase Process
**Priority**: MUST HAVE

**Description**: Purchase carbon credits via fixed price or auction.

**Fixed Price Purchase Flow**:
1. View listing details and seller profile
2. Review verification documents
3. Add to cart (partial purchases allowed)
4. Checkout with billing information
5. Select payment method
6. Complete payment via gateway
7. Escrow activated
8. Credits transferred + certificate generated
9. Seller paid (minus platform fee)

**Auction Purchase Flow**:
1. View auction details with countdown timer
2. Place bid (minimum increment enforced)
3. Receive outbid notifications
4. Win auction (24-hour payment deadline)
5. Complete checkout process

**Payment Methods**:
- E-wallets: MoMo, ZaloPay, VNPay, ShopeePay
- Bank transfer: Napas with unique reference code
- Credit/Debit cards: Visa, Mastercard, AMEX (via Stripe)
- Invoice payment: NET 30/60 for verified companies

**Payment Security**:
- PCI-DSS compliant payment gateway
- 3D Secure for card payments
- Escrow: funds held until credit transfer confirmed
- Automatic refund if transaction fails

**Acceptance Criteria**:
- Checkout completion rate: >85%
- Payment success rate: >98%
- Average checkout time: <5 minutes
- Escrow settlement: T+2 days

---

### FR-CCB-004: Certificate Management
**Priority**: MUST HAVE

**Description**: Receive and manage carbon credit certificates for ESG reporting.

**Certificate Components**:

1. **Standard Information**
   - Unique certificate ID (QR code + alphanumeric)
   - Issue date and validity period
   - Issuing authority (CVA organization)

2. **Transaction Details**
   - Buyer and seller information
   - Credit amount (tons CO₂)
   - Transaction value and date

3. **Credit Details**
   - Methodology used (e.g., CDM ACM0018)
   - Verification body & auditor
   - Vintage year
   - Project description
   - Geographic origin
   - Serial numbers of credits

4. **Verification & Authenticity**
   - Digital signature from CVA
   - Blockchain hash (optional)
   - QR code for public verification
   - Watermark & security features

**Detailed Requirements**:

1. **Certificate Generation**
   - Auto-generate upon transaction completion
   - PDF format (A4, print-ready, 300 DPI, <2MB)
   - Multi-language: Vietnamese, English
   - Customizable logo area (buyer's company)
   - Template compliance: Gold Standard, Verra VCS

2. **Certificate Repository**
   - List all certificates with filters and search
   - Actions: Download, Share, Revoke (admin approval), Re-issue
   - Tag certificates for internal organization

3. **Public Verification Portal**
   - Input certificate ID or scan QR code
   - Display validity status
   - Show basic transaction info (anonymized)
   - Flag revoked/expired certificates

4. **Integration with Reporting Tools**
   - Export: CSV for CDP/GRI, XML for ERP integration
   - Templates: GHG Protocol, ISO 14064, custom formats

**Acceptance Criteria**:
- Certificate generation: <10 seconds
- QR code scan success: >99%
- Public verification portal uptime: >99.9%

---

### FR-CCB-005: Portfolio Management
**Priority**: MUST HAVE

**Description**: Track purchase history and manage carbon credit portfolio.

**Detailed Requirements**:

1. **Purchase History Dashboard**
   - Summary: total credits, total spend, average price, transaction count
   - Filters: date range, amount range, status
   - Transaction details: ID, date, seller, amount, price, certificate, invoice

2. **Portfolio Overview**
   - Total CO₂ offset (tons)
   - Equivalents: trees, cars off road, flights offset
   - Breakdown by: vintage year, region, verification body, seller
   - Charts: monthly trend, cumulative offset, cost per ton over time

3. **Reporting & Export**
   - Standard reports: Annual offset, quarterly ESG, tax documentation
   - Custom report builder
   - Save report templates
   - Schedule automated reports (monthly/quarterly)
   - Export: PDF, Excel, CSV, JSON

4. **Forecast & Goal Setting**
   - Set annual offsetting targets
   - Budget allocation
   - Progress tracking
   - Alerts when falling behind goal

**Acceptance Criteria**:
- Dashboard load: <3 seconds
- Report generation: <15 seconds
- Data retention: 10 years minimum

---

## 2.3 Carbon Verification & Audit Functions

### FR-CVA-001: Verification Request Management
**Priority**: MUST HAVE

**Description**: Manage and process verification requests efficiently.

**Detailed Requirements**:

1. **Verification Queue**
   - Dashboard views: pending, unassigned, in-progress, completed
   - Filters: status, priority, date range, credit amount, assigned auditor
   - Auto-assignment or manual assignment

2. **Request Details**
   - EV owner information and KYC status
   - Vehicle details and historical record
   - Trip data: date range, distance, trip count, data source
   - Raw data file download (CSV/JSON)
   - Calculated credits with methodology breakdown

3. **Verification Process Steps**
   - **Step 1**: Initial review (data completeness, source authenticity, anomaly flagging)
   - **Step 2**: Data validation (pattern checking, GPS validation, distance vs energy ratio)
   - **Step 3**: Calculation audit (recalculate, compare, acceptable variance ±2%)
   - **Step 4**: Decision (approve, reject with reason, or request more info)

4. **Auditor Tools**
   - Data analyzer: statistical summary, outlier detection, map view
   - Reference library: emission factors, methodology guidelines, precedent cases
   - Communication: in-app messaging, document requests, video call scheduling

**Acceptance Criteria**:
- Verification SLA: 80% completed within 48 hours
- Approval rate: 85-95%
- Average verification time: <3 hours per request
- 100% rejection with reason provided

---

### FR-CVA-002: Credit Issuance & Registry
**Priority**: MUST HAVE

**Description**: Issue verified carbon credits to EV owner wallets.

**Detailed Requirements**:

1. **Credit Issuance Workflow**
   - Automatic issuance upon auditor approval
   - Credits minted in system registry
   - Unique serial number assignment: `VN-EV-[YEAR]-[CVA_ID]-[SEQUENCE]`
   - Example: `VN-EV-2025-TUV-000012345`

2. **Credit Metadata**
   - Serial number, vintage year, project ID
   - Methodology, verification date
   - CVA organization name, auditor ID

3. **Master Registry**
   - All issued credits (lifetime)
   - Status tracking: Issued → Listed → Sold → Retired
   - Ownership trail (blockchain-like)
   - Transfer history
   - Search & filter by: serial number, owner, vintage, CVA, status

4. **Wallet Integration**
   - Atomic credit transfer to EV owner
   - Update wallet balance
   - Send notifications (email + in-app)
   - Generate issuance certificate (PDF)

5. **Quality Assurance**
   - Random spot checks: 5% of approved requests
   - Auditor performance metrics: completion count, processing time, approval rate, quality score

**Acceptance Criteria**:
- Credit issuance: <5 minutes after approval
- Serial number uniqueness: 100%
- Wallet update success: >99.9%
- Registry uptime: >99.9%

---

### FR-CVA-003: Audit Trail & Reporting
**Priority**: MUST HAVE

**Description**: Comprehensive audit trails and compliance reporting.

**Detailed Requirements**:

1. **Audit Trail Logging**
   - Logged events: request created, auditor assigned, data uploaded/downloaded, queries sent, decision made, credits issued
   - Log format: JSON with timestamp, event type, auditor ID, request ID, details, IP, user agent
   - Immutability: append-only logs, cryptographic hash chain

2. **Verification Reports**
   - Individual report: request summary, data quality assessment, calculation verification, decision rationale
   - Batch report: date range aggregation, statistics, approval/rejection breakdown

3. **CVA Organization Dashboard**
   - KPIs: total verifications, credits issued, processing time, utilization rate, satisfaction score
   - Charts: verifications over time, credits by vintage, auditor performance, request volume by region

4. **Regulatory Reporting**
   - Compliance formats: ISO 14064-2, Gold Standard, Verra VCS
   - Export for regulators: Bộ TN&MT, UNFCCC
   - Accreditation tracking and renewal reminders

**Acceptance Criteria**:
- Log completeness: 100% of actions logged
- Report generation: <30 seconds
- Log retention: 10 years
- Audit trail query: <5 seconds

---

## 2.4 Admin Functions

### FR-ADM-001: User Management
**Priority**: MUST HAVE

**Description**: Manage all platform users and their accounts.

**Detailed Requirements**:

1. **User Directory**
   - List all users with search and filters
   - Filters: role, status, KYC level, join date
   - Bulk actions: export, notifications, status updates

2. **User Profile Management**
   - View: personal info, KYC documents, verification level, account status, linked vehicles, transaction history
   - Actions: edit info, reset password, force 2FA, suspend, unsuspend, delete (GDPR), upgrade/downgrade KYC

3. **KYC Review & Approval**
   - Pending KYC queue with priority flagging
   - Document verification with zoom/rotate
   - OCR and watermark detection
   - Approve or reject with reason
   - Video call scheduling and notes

4. **User Analytics**
   - Demographics: geographic, role, age, vehicle type distribution
   - Engagement: DAU, MAU, retention, churn by cohort
   - Cohort analysis: onboarding completion, time to first transaction, LTV

5. **Role & Permission Management**
   - Roles: EV Owner, Buyer, CVA Auditor, Admin, Support Agent
   - Granular permissions: Read, Write, Delete, Execute
   - IP whitelist for sensitive operations
   - Admin hierarchy: Super, Operations, Finance, Support

**Acceptance Criteria**:
- User search: <2 seconds
- KYC review completion: <24 hours
- Bulk operations: <1 minute for 1000 users

---

### FR-ADM-002: Transaction Monitoring
**Priority**: MUST HAVE

**Description**: Real-time monitoring of all platform transactions.

**Detailed Requirements**:

1. **Real-time Transaction Feed**
   - Live updates every 30 seconds
   - Display: transaction ID, buyer, seller, amount, value, status, payment method, timestamp
   - Filters: date range, status, amount range, payment method, user type

2. **Transaction Details**
   - Complete information: participants, timeline, credit details, payment details, escrow status, certificate
   - Actions: hold transaction, cancel, force complete, refund, escalate to support

3. **Fraud Detection**
   - Real-time alerts: unusual patterns, velocity checks, high-value transactions, failed payment attempts
   - ML-based risk scoring
   - Auto-suspend on high-risk score
   - Manual review queue

4. **Settlement Management**
   - Pending settlements dashboard
   - Batch processing: daily settlement run
   - Failed settlement handling with retry logic
   - Reconciliation reports

**Acceptance Criteria**:
- Transaction feed latency: <30 seconds
- Fraud detection accuracy: >95%
- Settlement processing: T+2 days
- Manual intervention: <5% of transactions

---

### FR-ADM-003: Financial Operations
**Priority**: MUST HAVE

**Description**: Manage platform finances, fees, and payouts.

**Detailed Requirements**:

1. **Revenue Dashboard**
   - KPIs: total GMV, platform revenue, average transaction value, revenue growth rate
   - Charts: daily/weekly/monthly revenue trend, revenue by category, top revenue sources

2. **Fee Management**
   - Configure fees: transaction fee (%), listing fee, withdrawal fee, verification fee
   - Fee rules by user segment or transaction size
   - Fee waiver/discount campaigns
   - Track fee collection and receivables

3. **Payout Management**
   - Pending payouts to sellers
   - Batch payout processing
   - Payment method routing
   - Failed payout handling
   - Payout history and reconciliation

4. **Financial Reporting**
   - Daily/weekly/monthly financial summary
   - Tax reports (VAT, withholding)
   - Audit-ready transaction logs
   - Bank reconciliation

**Acceptance Criteria**:
- Real-time revenue tracking
- Payout processing: <2 business days
- Report generation: <30 seconds
- 100% financial reconciliation

---

### FR-ADM-004: Platform Analytics
**Priority**: MUST HAVE

**Description**: Comprehensive analytics and business intelligence.

**Detailed Requirements**:

1. **Executive Dashboard**
   - KPIs: total users, active users, GMV, transactions, CO₂ offset, revenue
   - Growth metrics: user growth, transaction growth, MoM/YoY comparison
   - Visual cards with trend indicators

2. **User Analytics**
   - User acquisition: sources, channels, conversion rates
   - User engagement: session duration, feature usage, retention cohorts
   - User segments: demographics, behavior, value tiers

3. **Marketplace Analytics**
   - Supply/demand balance
   - Listing performance: views, conversion, time to sale
   - Price trends: average price, price by region, price elasticity
   - Auction vs fixed price performance

4. **Operational Metrics**
   - System health: uptime, response time, error rates
   - Support metrics: ticket volume, resolution time, CSAT
   - Verification metrics: queue length, processing time, approval rate

5. **Custom Reports**
   - Report builder with drag-drop
   - Save and schedule reports
   - Export formats: PDF, Excel, CSV
   - Share with stakeholders

**Acceptance Criteria**:
- Dashboard load: <5 seconds
- Data freshness: <15 minutes
- Custom report generation: <30 seconds
- Export limits: 100,000 rows

---

### FR-ADM-005: Content & Configuration Management
**Priority**: SHOULD HAVE

**Description**: Manage platform content, settings, and configurations.

**Detailed Requirements**:

1. **System Configuration**
   - Platform settings: fees, limits, thresholds
   - Feature flags: enable/disable features
   - Maintenance mode
   - Rate limiting rules

2. **Content Management**
   - Static pages: About, Terms, Privacy, FAQ
   - WYSIWYG editor
   - Multi-language content
   - Version control and publish scheduling

3. **Notification Templates**
   - Email templates with variables
   - SMS templates
   - Push notification templates
   - A/B testing support

4. **Verification Methodology Management**
   - Emission factor configuration
   - Calculation formula updates
   - Methodology documentation
   - Version history

**Acceptance Criteria**:
- Configuration changes: immediate effect or scheduled
- Content publishing: <1 minute delay
- Template editor: WYSIWYG with preview

---

### FR-ADM-006: Support Tools
**Priority**: SHOULD HAVE

**Description**: Customer service and support tools.

**Detailed Requirements**:

1. **Support Ticket System**
   - Ticket creation: web form, email auto-create, admin-created
   - Ticket workflow: New → Assigned → In Progress → Resolved → Closed
   - SLA tracking and escalation
   - Auto-assignment by category

2. **Agent Dashboard**
   - My tickets: assigned, awaiting response, resolved
   - Team queue: unassigned, by priority, overdue
   - Quick actions: assign, change status, reply, add note

3. **User Context Panel**
   - User info and verification level
   - Recent activity and transactions
   - Recent support tickets
   - Quick admin actions: view profile, reset password, refund

4. **Knowledge Base**
   - Article management: create, edit, delete, categories
   - User-facing FAQ with search
   - Internal wiki for agents
   - View analytics

5. **Communication Tools**
   - In-app messaging (real-time if online)
   - Email with templates and merge tags
   - Canned responses
   - Track email opens/clicks

6. **Support Analytics**
   - Performance: tickets per agent, response time, resolution time, CSAT
   - Trends: ticket volume, common issues, peak hours

**Acceptance Criteria**:
- Response SLA: <2 hours (business hours)
- Resolution SLA: <24 hours (non-technical), <3 days (technical)
- CSAT target: >4.5/5
- Knowledge base coverage: >80% common issues

---

## 3. NON-FUNCTIONAL REQUIREMENTS

## 3.1 Performance Requirements

### NFR-P-001: Response Time
**Priority**: MUST HAVE

**Requirements**:
- **Web pages**: 95th percentile load time <3 seconds on 4G
- **API endpoints**:
  - Read operations: <500ms (95th percentile)
  - Write operations: <1 second (95th percentile)
  - Search: <2 seconds for 10,000 results
  - Report generation: <30 seconds
- **Real-time updates**: WebSocket latency <100ms

---

### NFR-P-002: Throughput
**Priority**: MUST HAVE

**Requirements**:
- Support 5,000 concurrent users
- Handle 100 transactions/minute during peak
- Process 10,000 API requests/minute across all endpoints
- Database: 5,000 queries/second

---

### NFR-P-003: Scalability
**Priority**: MUST HAVE

**Requirements**:
- Horizontal auto-scaling up to 20 instances
- Database read replicas for scaling reads
- Support 10x user growth (150,000 users) within 3 years
- Accommodate 10TB data storage within 3 years

---

## 3.2 Security Requirements

### NFR-S-001: Authentication & Authorization
**Priority**: MUST HAVE

**Password Policy**:
- Minimum 8 characters
- Mix of uppercase, lowercase, numbers, special characters
- Cannot reuse last 5 passwords
- Expiry: 180 days (admin accounts)

**Multi-Factor Authentication (2FA)**:
- TOTP-based (Google Authenticator compatible)
- Mandatory for admin, optional for users
- SMS OTP as backup

**Session Management**:
- Timeout: 30 minutes inactive, 8 hours absolute
- Force logout on password change
- Concurrent session limit: 3 devices

---

### NFR-S-002: Data Protection
**Priority**: MUST HAVE

**Encryption**:
- Data at rest: AES-256
- Data in transit: TLS 1.3
- Database: Encrypted storage
- Backups: Encrypted

**PII Protection**:
- Mask sensitive data in logs
- Tokenize payment information
- Anonymize data for analytics

**GDPR Compliance**:
- Right to access data
- Right to delete data (within 30 days)
- Data portability (export JSON/CSV)
- Consent management

---

### NFR-S-003: Application Security
**Priority**: MUST HAVE

**OWASP Top 10 Protection**:
- SQL Injection: Parameterized queries, ORM usage
- XSS: Input sanitization, Content Security Policy
- CSRF: CSRF tokens for state-changing operations
- Broken Authentication: Secure session management
- Security Misconfiguration: Regular security audits

**API Security**:
- API key rotation every 90 days
- Rate limiting (per IP and per user)
- Input validation on all endpoints
- Signed API requests (HMAC) for sensitive operations

**Vulnerability Management**:
- Dependency scanning (Snyk, Dependabot)
- Penetration testing: Quarterly
- Bug bounty program (future phase)

---

### NFR-S-004: Compliance
**Priority**: MUST HAVE

**Requirements**:
- **Vietnam PDPA**: Personal data protection compliance
- **PCI-DSS**: Level 1 compliance (use tokenization to avoid storing card data)
- **ISO 27001**: Information security management (target certification)
- **SOC 2**: Security, availability, confidentiality (target certification)

---

## 3.3 Availability & Reliability

### NFR-A-001: Uptime
**Priority**: MUST HAVE

**Requirements**:
- Target availability: 99.5% (downtime <3.6 hours/month)
- Critical services: 99.9% (payment, authentication)
- Scheduled maintenance: 2-4 AM with 7-day advance notice

---

### NFR-A-002: Disaster Recovery
**Priority**: MUST HAVE

**Requirements**:
- **RTO** (Recovery Time Objective): <4 hours
- **RPO** (Recovery Point Objective): <1 hour (max data loss)
- **Backup Strategy**:
  - Database: Continuous replication + daily snapshots
  - Files: Real-time replication to secondary region
  - Retention: Daily (7 days), Weekly (4 weeks), Monthly (12 months)
- **DR Plan**:
  - Multi-region deployment (primary: Singapore, DR: US West)
  - Automated failover for critical services
  - Quarterly DR drills

---

### NFR-A-003: Monitoring & Alerting
**Priority**: MUST HAVE

**Requirements**:
- Health checks: Every 30 seconds for critical services
- **Alert triggers**:
  - CPU/Memory >80% for 5 minutes
  - Error rate >1% of requests
  - API latency >2 seconds (95th percentile)
  - Payment gateway downtime
  - Database connection failures
- **On-call**: 24/7 on-call engineer with <15 minute response time

---

## 3.4 Usability Requirements

### NFR-U-001: User Interface
**Priority**: MUST HAVE

**Requirements**:
- Consistent design system across platform
- **Accessibility**: WCAG 2.1 Level AA compliance
  - Screen reader compatible
  - Keyboard navigation
  - Color contrast ratio >4.5:1
- **Responsive design**: Desktop (1920x1080 to 1366x768), Tablet (768x1024), Mobile (375x667 to 414x896)
- **Browser support**: Chrome, Firefox, Safari, Edge (last 2 versions)

---

### NFR-U-002: Localization
**Priority**: SHOULD HAVE

**Requirements**:
- Languages: Vietnamese (primary), English
- Currency: VND (primary), USD (optional for international buyers)
- Date/Time: Vietnam timezone (GMT+7), customizable
- Number formats:
  - Vietnamese: 1.234.567,89
  - English: 1,234,567.89

---

### NFR-U-003: User Experience
**Priority**: MUST HAVE

**Requirements**:
- Onboarding: <10 minutes to complete registration and first vehicle connection
- **Task completion times**:
  - Create listing: <5 minutes
  - Purchase credits: <3 minutes
  - Withdraw earnings: <2 minutes
- **Help & Documentation**:
  - Contextual help tooltips
  - Comprehensive FAQ (>50 articles)
  - Video tutorials for key features
  - Live chat support (business hours)

---

## 3.5 Maintainability Requirements

### NFR-M-001: Code Quality
**Priority**: MUST HAVE

**Requirements**:
- Unit test coverage: >80%
- All code peer-reviewed before merge
- Follow language-specific style guides (PEP 8 for Python, Airbnb for JavaScript)
- API documentation: OpenAPI/Swagger
- Architecture diagrams: C4 model

---

### NFR-M-002: DevOps
**Priority**: MUST HAVE

**Requirements**:
- CI/CD: Automated build, test, deploy pipeline
- Deployment frequency: Daily to staging, weekly to production
- Rollback: <15 minutes for failed deployments
- Infrastructure as Code: Terraform/CloudFormation

---

### NFR-M-003: Logging & Debugging
**Priority**: MUST HAVE

**Requirements**:
- Structured logging: JSON format, centralized (ELK stack or equivalent)
- Log levels: DEBUG, INFO, WARN, ERROR, CRITICAL
- Correlation IDs: Track requests across microservices
- Log retention: 30 days hot, 90 days warm, 1 year cold storage

---

## Requirement Traceability Matrix

| Requirement ID | Priority | Phase | Dependencies |
|----------------|----------|-------|--------------|
| BR-001 | MUST HAVE | 1 | FR-EVO-002 |
| BR-002 | MUST HAVE | 1 | FR-EVO-004, FR-CCB-003 |
| BR-003 | MUST HAVE | 1 | FR-CVA-001, FR-CVA-002 |
| BR-004 | MUST HAVE | 1 | - |
| BR-005 | MUST HAVE | 1 | NFR-S-001, NFR-S-002 |
| BR-006 | SHOULD HAVE | 2 | FR-EVO-004 |
| BR-007 | SHOULD HAVE | 2 | NFR-U-001 |
| BR-008 | COULD HAVE | 3 | NFR-U-002 |

---

## Glossary

- **CVA**: Carbon Verification & Audit organization
- **EV**: Electric Vehicle
- **ESG**: Environmental, Social, and Governance
- **GMV**: Gross Merchandise Value
- **ICE**: Internal Combustion Engine
- **KYC**: Know Your Customer
- **MST**: Mã Số Thuế (Tax Code)
- **OBD**: On-Board Diagnostics
- **OEM**: Original Equipment Manufacturer
- **PDPA**: Personal Data Protection Act
- **PII**: Personally Identifiable Information
- **PWA**: Progressive Web App
- **RBAC**: Role-Based Access Control
- **SLA**: Service Level Agreement
- **VIN**: Vehicle Identification Number

---

**Document Version**: 1.0  
**Last Updated**: October 25, 2025  
**Status**: Approved

