# Test Scenarios
## End-to-End Test Cases with Sample Data

**Version**: 1.0  
**Last Updated**: October 26, 2025  
**Test Environment**: Staging/UAT

---

## 🎯 Test Coverage Overview

| Scenario | User Type | Services Tested | Priority |
|----------|-----------|-----------------|----------|
| 1. New EV Owner Journey | EV Owner | User, Vehicle, Carbon Credit, Verification | CRITICAL |
| 2. Fixed Price Purchase | Corporate Buyer | Marketplace, Transaction, Payment, Certificate | CRITICAL |
| 3. Auction Participation | Corporate Buyer | Marketplace, Transaction, Notification | HIGH |
| 4. Withdrawal Process | EV Owner | Payment, Transaction, Notification | HIGH |
| 5. Fraud Detection | Admin | Admin, Analytics, Notification | MEDIUM |

---

## Scenario 1: New EV Owner Journey 🚗
**From Registration to First Carbon Credit Sale**

### Test Data

#### User Registration Data
```json
{
  "email": "nguyen.van.test@gmail.com",
  "phone": "+84909123456",
  "password": "SecureP@ss2025",
  "full_name": "Nguyễn Văn Test",
  "role": "evowner",
  "referral_code": "EARLY2025"
}
```

#### Vehicle Registration Data
```json
{
  "make": "VinFast",
  "model": "VF8",
  "year": 2024,
  "vin": "VF8VN2024TEST001",
  "registration_number": "30A-12345",
  "data_source": "api",
  "data_source_config": {
    "api_type": "vinfast",
    "api_credentials": {
      "client_id": "test_client_123",
      "client_secret": "encrypted_secret"
    }
  }
}
```

#### Sample Trip Data (CSV Upload)
```csv
start_time,end_time,distance_km,start_lat,start_lng,end_lat,end_lng,energy_kwh,avg_speed
2025-10-01T08:00:00Z,2025-10-01T08:45:00Z,35.5,10.762622,106.660172,10.854886,106.629639,7.2,47.3
2025-10-01T18:00:00Z,2025-10-01T18:30:00Z,22.3,10.854886,106.629639,10.762622,106.660172,4.5,44.6
2025-10-02T07:30:00Z,2025-10-02T08:15:00Z,42.1,10.762622,106.660172,10.801827,106.814077,8.5,56.1
2025-10-02T17:45:00Z,2025-10-02T18:25:00Z,38.7,10.801827,106.814077,10.762622,106.660172,7.8,58.1
2025-10-03T08:15:00Z,2025-10-03T09:30:00Z,67.2,10.762622,106.660172,10.933099,106.582813,13.5,53.8
```

### Test Steps

1. **User Registration**
   - POST `/api/v1/auth/register` with user data
   - Verify OTP sent to email/phone
   - POST `/api/v1/auth/verify-otp` with OTP: "123456"
   - Expected: User created with `kyc_level: 0`

2. **KYC Submission**
   - POST `/api/v1/users/me/kyc` with documents:
     ```json
     {
       "kyc_level": 1,
       "id_type": "national_id",
       "id_number": "001234567890",
       "id_front": "base64_image_data",
       "id_back": "base64_image_data",
       "selfie": "base64_image_data"
     }
     ```
   - Admin approves KYC
   - Expected: `kyc_level: 1`, `kyc_status: "approved"`

3. **Vehicle Registration**
   - POST `/api/v1/vehicles` with vehicle data
   - System validates VIN format
   - Expected: Vehicle registered with `verification_status: "pending"`

4. **Trip Data Sync**
   - POST `/api/v1/trips/upload` with CSV file
   - System processes 5 trips
   - Calculation:
     - Total distance: 206.8 km
     - CO₂ saved: 206.8 × 0.10 = 20.68 kg
     - Credits: 0.02068 tons
   - Expected: All trips validated, CO₂ calculated

5. **Verification Request**
   - POST `/api/v1/carbon-credits/verification-request`
     ```json
     {
       "vehicle_id": "uuid-vehicle-001",
       "trip_date_start": "2025-10-01",
       "trip_date_end": "2025-10-03",
       "notes": "First verification request"
     }
     ```
   - CVA reviews and approves
   - Expected: Credits issued with serial number "VN-EV-2025-TUV-000001"

6. **Create Listing**
   - POST `/api/v1/marketplace/listings`
     ```json
     {
       "credit_id": "uuid-credit-001",
       "listing_type": "fixed",
       "credit_amount_tons": 0.02068,
       "price_per_ton_vnd": 2500000,
       "region": "TP.HCM",
       "description": "First carbon credits from VinFast VF8"
     }
     ```
   - Expected: Listing active in marketplace

### Expected Results
- ✅ User fully onboarded with KYC Level 1
- ✅ Vehicle connected and syncing trips
- ✅ 0.02068 tons of verified carbon credits
- ✅ Listing visible in marketplace
- ✅ Total time: ~48 hours (including verification)

### Validation Points
- [ ] JWT tokens working (15-min access, 7-day refresh)
- [ ] Email/SMS notifications received at each step
- [ ] Trip validation catches any anomalies
- [ ] CO₂ calculation matches formula exactly
- [ ] Credits locked when listed

---

## Scenario 2: Fixed Price Purchase 💳
**Corporate Buyer Purchases Carbon Credits**

### Test Data

#### Buyer Registration
```json
{
  "email": "procurement@greencorp.vn",
  "phone": "+84282223333",
  "password": "CorpSecure#2025",
  "full_name": "Trần Thị Mua",
  "role": "buyer",
  "company_name": "Green Corporation Vietnam",
  "tax_code": "0312345678",
  "business_registration_number": "0312345678-001"
}
```

#### Search Parameters
```json
{
  "region": "TP.HCM",
  "min_price": 2000000,
  "max_price": 3000000,
  "min_amount": 0.5,
  "verification_status": "verified",
  "sort": "price",
  "order": "asc"
}
```

#### Purchase Transaction
```json
{
  "listing_id": "uuid-listing-001",
  "credit_amount_tons": 1.5,
  "payment_method": "momo",
  "billing_info": {
    "company_name": "Green Corporation Vietnam",
    "tax_code": "0312345678",
    "address": "123 Nguyen Hue, District 1, HCMC",
    "contact_person": "Trần Thị Mua",
    "contact_phone": "+84282223333"
  }
}
```

### Test Steps

1. **Buyer Registration & KYC**
   - Register company account
   - Submit business documents
   - Admin verifies within 24 hours
   - Expected: Company verified with purchase limit 500M VND

2. **Search Marketplace**
   - GET `/api/v1/marketplace/listings` with search params
   - Results include test listing from Scenario 1
   - Expected: <2 second response, relevant listings

3. **View Listing Details**
   - GET `/api/v1/marketplace/listings/{listing_id}`
   - Download verification documents
   - Expected: Complete details with CVA certificate

4. **Create Purchase**
   - POST `/api/v1/transactions`
   - Calculation:
     - Subtotal: 1.5 × 2,500,000 = 3,750,000 VND
     - Platform fee (5%): 187,500 VND
     - **Total: 3,937,500 VND**

5. **Payment Flow**
   - Redirect to MoMo gateway
   - Complete payment with test credentials:
     - Phone: "0909123456"
     - OTP: "123456"
   - Webhook received: 
     ```json
     {
       "partnerCode": "MOMO",
       "orderId": "CARBON-uuid-transaction-001",
       "requestId": "momo-request-123",
       "amount": 3937500,
       "orderInfo": "Carbon Credit Purchase",
       "orderType": "momo_wallet",
       "transId": "momo-trans-456",
       "resultCode": 0,
       "message": "Successful",
       "payType": "qr",
       "signature": "signature_hash_here"
     }
     ```

6. **Transaction Completion**
   - Credits transferred to buyer wallet
   - Certificate generated
   - Payment released to seller (T+2)
   - Notifications sent

### Expected Results
- ✅ Payment processed successfully
- ✅ Credits in buyer's wallet: 1.5 tons
- ✅ Certificate PDF generated with QR code
- ✅ Seller receives: 3,750,000 - 187,500 = 3,562,500 VND
- ✅ Transaction completed in <3 minutes

### Certificate Sample Data
```json
{
  "certificate_number": "CERT-2025-000001",
  "issue_date": "2025-10-26",
  "buyer": {
    "company_name": "Green Corporation Vietnam",
    "tax_code": "0312345678"
  },
  "seller": {
    "name": "Nguyễn Văn Test"
  },
  "credit_details": {
    "amount_tons": 1.5,
    "serial_numbers": ["VN-EV-2025-TUV-000001", "VN-EV-2025-TUV-000002"],
    "vintage_year": 2025,
    "methodology": "CDM ACM0018",
    "region": "TP.HCM"
  },
  "verification": {
    "cva_organization": "TÜV SÜD Vietnam",
    "auditor_name": "John Doe",
    "verification_date": "2025-10-25"
  },
  "qr_code_data": "https://verify.carbonmarketplace.vn/CERT-2025-000001"
}
```

---

## Scenario 3: Auction Participation 🎯
**Competitive Bidding for Carbon Credits**

### Test Data

#### Auction Listing
```json
{
  "seller_id": "uuid-user-002",
  "credit_id": "uuid-credit-002",
  "listing_type": "auction",
  "credit_amount_tons": 5.0,
  "starting_price_per_ton_vnd": 2000000,
  "reserve_price_per_ton_vnd": 2300000,
  "auction_duration_days": 3,
  "region": "Ha Noi",
  "description": "Premium carbon credits from Tesla Model 3 fleet"
}
```

#### Bidders
```json
[
  {
    "bidder_id": "uuid-buyer-001",
    "company": "Tech Corp",
    "max_budget": 15000000
  },
  {
    "bidder_id": "uuid-buyer-002",
    "company": "Bank ABC",
    "max_budget": 12000000
  },
  {
    "bidder_id": "uuid-buyer-003",
    "company": "Green Fund",
    "max_budget": 13000000
  }
]
```

### Bidding Sequence

| Time | Bidder | Bid/ton (VND) | Total (VND) | Action |
|------|--------|---------------|-------------|---------|
| 0:00 | - | 2,000,000 | 10,000,000 | Auction starts |
| 0:15 | Tech Corp | 2,050,000 | 10,250,000 | Manual bid |
| 0:30 | Bank ABC | 2,100,000 | 10,500,000 | Manual bid |
| 0:45 | Green Fund | 2,150,000 | 10,750,000 | Manual bid |
| 1:00 | Tech Corp | 2,200,000 | 11,000,000 | Auto-bid triggered |
| 1:15 | Bank ABC | 2,250,000 | 11,250,000 | Manual bid |
| 71:55 | Green Fund | 2,300,000 | 11,500,000 | Last-minute bid |
| 72:05 | Tech Corp | 2,350,000 | 11,750,000 | During extension |
| 72:14 | Green Fund | 2,400,000 | 12,000,000 | Final bid |
| 72:24 | - | - | - | Auction ends |

### WebSocket Messages

```javascript
// Real-time bid update
{
  "type": "BID_UPDATE",
  "listingId": "uuid-listing-002",
  "currentBid": 2400000,
  "totalBids": 8,
  "highestBidder": "uuid-buyer-003",
  "timeRemaining": 600,
  "extended": true
}

// Outbid notification
{
  "type": "OUTBID",
  "listingId": "uuid-listing-002",
  "yourBid": 2350000,
  "currentBid": 2400000,
  "message": "You have been outbid"
}

// Auction ending soon
{
  "type": "ENDING_SOON",
  "listingId": "uuid-listing-002",
  "minutesRemaining": 5,
  "currentBid": 2400000
}
```

### Expected Results
- ✅ Real-time updates via WebSocket
- ✅ Auto-bid functionality working
- ✅ 10-minute extension triggered
- ✅ Winner: Green Fund @ 2,400,000/ton
- ✅ 24-hour payment deadline set
- ✅ All bidders notified of results

---

## Scenario 4: Withdrawal Process 💰
**EV Owner Withdraws Earnings**

### Test Data

#### Seller Account State
```json
{
  "user_id": "uuid-user-001",
  "wallet_balance": {
    "total_vnd": 8750000,
    "available_vnd": 8750000,
    "pending_vnd": 0,
    "lifetime_earned_vnd": 25000000,
    "lifetime_withdrawn_vnd": 16250000
  },
  "bank_accounts": [
    {
      "account_id": "uuid-bank-001",
      "bank_name": "Vietcombank",
      "account_number": "0071000123456",
      "account_holder": "NGUYEN VAN TEST",
      "verified": true,
      "is_primary": true
    }
  ]
}
```

#### Withdrawal Request
```json
{
  "amount_vnd": 5000000,
  "bank_account_id": "uuid-bank-001",
  "two_fa_code": "123456",
  "note": "Monthly withdrawal October 2025"
}
```

### Test Steps

1. **Initiate Withdrawal**
   - POST `/api/v1/withdrawals`
   - 2FA verification required
   - Fee calculation:
     - Amount: 5,000,000 VND
     - Fee (2%): 100,000 VND
     - Net: 4,900,000 VND

2. **Admin Review**
   - Withdrawal appears in admin queue
   - Anti-fraud checks:
     - [ ] Account age > 30 days ✓
     - [ ] KYC Level 2 verified ✓
     - [ ] No suspicious activity ✓
   - Admin approves

3. **Bank Transfer**
   - System initiates Napas transfer
   - Reference: `WD202510260001`
   - Bank response:
     ```json
     {
       "transaction_id": "NAPAS-2025102600123",
       "status": "success",
       "amount": 4900000,
       "fee": 11000,
       "completed_at": "2025-10-27T10:30:00Z"
     }
     ```

4. **Confirmation**
   - Wallet updated: 8,750,000 - 5,000,000 = 3,750,000 VND
   - Email confirmation sent
   - Transaction recorded

### Expected Results
- ✅ Withdrawal processed in T+2 days
- ✅ Correct fee deduction
- ✅ Bank transfer successful
- ✅ Wallet balance updated
- ✅ Email notifications sent

### Email Template
```html
Subject: Withdrawal Completed - 4,900,000 VND

Dear Nguyễn Văn Test,

Your withdrawal has been processed successfully.

Details:
- Withdrawal ID: WD202510260001
- Amount Requested: 5,000,000 VND
- Platform Fee: 100,000 VND
- Net Amount: 4,900,000 VND
- Bank: Vietcombank (****3456)
- Status: Completed
- Date: October 27, 2025

New Wallet Balance: 3,750,000 VND

Thank you for using Carbon Credit Marketplace.
```

---

## Scenario 5: Fraud Detection & Prevention 🚨
**Suspicious Activity Detection**

### Test Data - Suspicious Patterns

#### Pattern 1: Impossible Trip Data
```json
{
  "vehicle_id": "uuid-vehicle-suspect-001",
  "trips": [
    {
      "distance_km": 500,
      "duration_minutes": 60,
      "avg_speed": 500,
      "alert": "IMPOSSIBLE_SPEED"
    },
    {
      "distance_km": 100,
      "energy_kwh": 2,
      "efficiency": 2,
      "alert": "IMPOSSIBLE_EFFICIENCY"
    }
  ]
}
```

#### Pattern 2: Duplicate Transactions
```json
{
  "user_id": "uuid-user-suspect-002",
  "actions": [
    {
      "time": "10:00:00",
      "action": "create_listing",
      "listing_id": "uuid-listing-001"
    },
    {
      "time": "10:00:01",
      "action": "create_listing",
      "listing_id": "uuid-listing-002"
    },
    {
      "time": "10:00:02",
      "action": "create_listing",
      "listing_id": "uuid-listing-003"
    }
  ],
  "alert": "VELOCITY_CHECK_FAILED"
}
```

#### Pattern 3: Unusual Withdrawal
```json
{
  "user_id": "uuid-user-suspect-003",
  "account_age_days": 2,
  "total_earned": 100000,
  "withdrawal_request": 50000000,
  "alert": "WITHDRAWAL_EXCEEDS_EARNINGS"
}
```

### Fraud Detection Rules

```javascript
const fraudRules = {
  trips: {
    maxSpeed: 180, // km/h
    maxDistance: 500, // km per trip
    minEfficiency: 10, // kWh/100km
    maxEfficiency: 25, // kWh/100km
  },
  transactions: {
    maxPerDay: 10,
    maxAmountFirstTransaction: 10000000, // VND
    velocityCheckWindow: 60, // seconds
    maxActionsPerWindow: 3
  },
  withdrawals: {
    minAccountAge: 7, // days
    maxFirstWithdrawal: 5000000, // VND
    maxDailyWithdrawal: 50000000, // VND
    requireManualReview: 20000000 // VND
  }
};
```

### Alert Dashboard

```json
{
  "alerts": [
    {
      "alert_id": "ALERT-001",
      "type": "IMPOSSIBLE_TRIP",
      "severity": "HIGH",
      "user_id": "uuid-user-suspect-001",
      "details": "Trip shows 500 km/h average speed",
      "recommended_action": "Suspend vehicle sync, request verification",
      "timestamp": "2025-10-26T10:00:00Z"
    },
    {
      "alert_id": "ALERT-002",
      "type": "VELOCITY_CHECK",
      "severity": "MEDIUM",
      "user_id": "uuid-user-suspect-002",
      "details": "3 listings created in 2 seconds",
      "recommended_action": "Rate limit user, manual review",
      "timestamp": "2025-10-26T10:15:00Z"
    },
    {
      "alert_id": "ALERT-003",
      "type": "SUSPICIOUS_WITHDRAWAL",
      "severity": "CRITICAL",
      "user_id": "uuid-user-suspect-003",
      "details": "Withdrawal 500x greater than earnings",
      "recommended_action": "Block withdrawal, freeze account",
      "timestamp": "2025-10-26T10:30:00Z"
    }
  ]
}
```

### Admin Actions

1. **Review Alert**
   - GET `/api/v1/admin/alerts/{alert_id}`
   - View user history and patterns
   - Check related transactions

2. **Take Action**
   - POST `/api/v1/admin/users/{user_id}/suspend`
   - POST `/api/v1/admin/transactions/{transaction_id}/hold`
   - POST `/api/v1/admin/alerts/{alert_id}/resolve`

3. **Generate Report**
   - GET `/api/v1/admin/reports/fraud?start_date=2025-10-01&end_date=2025-10-31`

### Expected Results
- ✅ All suspicious patterns detected
- ✅ Alerts generated within 30 seconds
- ✅ Admin notified immediately for HIGH/CRITICAL
- ✅ Automated actions taken (rate limiting, holds)
- ✅ Complete audit trail maintained

---

## Test Environment Setup 🛠️

### Database Seeding Script

```sql
-- Create test users
INSERT INTO users (user_id, email, phone, full_name, role, kyc_level) VALUES
('11111111-0001-0001-0001-000000000001', 'test.seller@gmail.com', '+84909111111', 'Test Seller', 'evowner', 2),
('11111111-0002-0002-0002-000000000002', 'test.buyer@corp.vn', '+84909222222', 'Test Buyer', 'buyer', 1),
('11111111-0003-0003-0003-000000000003', 'test.auditor@tuv.com', '+84909333333', 'Test Auditor', 'verifier', 2),
('11111111-0004-0004-0004-000000000004', 'test.admin@platform.vn', '+84909444444', 'Test Admin', 'admin', 2);

-- Create test vehicles
INSERT INTO vehicles (vehicle_id, owner_id, make, model, vin, verification_status) VALUES
('22222222-0001-0001-0001-000000000001', '11111111-0001-0001-0001-000000000001', 'VinFast', 'VF8', 'TEST123456789VF8', 'verified'),
('22222222-0002-0002-0002-000000000002', '11111111-0001-0001-0001-000000000001', 'Tesla', 'Model 3', 'TEST123456789TM3', 'verified');

-- Create test carbon credits
INSERT INTO carbon_credits (credit_id, owner_id, amount_tons, status, serial_number) VALUES
('33333333-0001-0001-0001-000000000001', '11111111-0001-0001-0001-000000000001', 1.5, 'issued', 'VN-EV-2025-TUV-TEST001'),
('33333333-0002-0002-0002-000000000002', '11111111-0001-0001-0001-000000000001', 2.0, 'issued', 'VN-EV-2025-TUV-TEST002');

-- Create test listings
INSERT INTO listings (listing_id, seller_id, credit_id, listing_type, price_per_ton_vnd, status) VALUES
('44444444-0001-0001-0001-000000000001', '11111111-0001-0001-0001-000000000001', '33333333-0001-0001-0001-000000000001', 'fixed', 2500000, 'active'),
('44444444-0002-0002-0002-000000000002', '11111111-0001-0001-0001-000000000001', '33333333-0002-0002-0002-000000000002', 'auction', 2000000, 'active');
```

### API Test Collection

```javascript
// Postman Environment Variables
{
  "base_url": "http://localhost:8080/api/v1",
  "test_user_email": "test.seller@gmail.com",
  "test_user_password": "TestP@ss2025",
  "access_token": "{{generated_after_login}}",
  "refresh_token": "{{generated_after_login}}",
  "vehicle_id": "{{generated_after_vehicle_registration}}",
  "listing_id": "{{generated_after_listing_creation}}",
  "transaction_id": "{{generated_after_purchase}}"
}
```

### Performance Benchmarks

| Operation | Target | Acceptable | Critical |
|-----------|--------|------------|----------|
| User Registration | <2s | <5s | >10s |
| Login | <500ms | <1s | >2s |
| Search Listings | <2s | <3s | >5s |
| Create Transaction | <3s | <5s | >10s |
| Generate Certificate | <10s | <15s | >30s |
| Trip Sync (100 trips) | <30s | <60s | >120s |

### Load Testing Script

```javascript
// k6 load test script
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 }, // Ramp up
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 200 }, // Ramp up
    { duration: '5m', target: 200 }, // Stay at 200 users
    { duration: '2m', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
    http_req_failed: ['rate<0.05'],    // Error rate under 5%
  },
};

export default function() {
  // Test marketplace search
  let searchResponse = http.get(`${__ENV.BASE_URL}/marketplace/listings`);
  check(searchResponse, {
    'search status is 200': (r) => r.status === 200,
    'search response time < 2s': (r) => r.timings.duration < 2000,
  });
  
  sleep(1);
  
  // Test user dashboard
  let dashboardResponse = http.get(`${__ENV.BASE_URL}/users/me`, {
    headers: { 'Authorization': `Bearer ${__ENV.TOKEN}` },
  });
  check(dashboardResponse, {
    'dashboard status is 200': (r) => r.status === 200,
    'dashboard response time < 1s': (r) => r.timings.duration < 1000,
  });
  
  sleep(1);
}
```

---

## Validation Checklist ✅

### For Each Scenario

- [ ] **Functional Testing**
  - [ ] All happy path steps complete successfully
  - [ ] Error cases handled gracefully
  - [ ] Data consistency maintained

- [ ] **Performance Testing**
  - [ ] Response times within SLA
  - [ ] No memory leaks
  - [ ] Database queries optimized

- [ ] **Security Testing**
  - [ ] Authentication required for protected endpoints
  - [ ] Authorization checks working
  - [ ] Input validation preventing injection

- [ ] **Integration Testing**
  - [ ] External APIs responding correctly
  - [ ] Payment gateways processing
  - [ ] Notifications delivered

- [ ] **User Experience**
  - [ ] Error messages helpful
  - [ ] Loading states shown
  - [ ] Mobile responsive

---

**Document Version**: 1.0  
**Last Updated**: October 26, 2025  
**Test Data Valid Until**: December 31, 2025
