# API Specifications
## Carbon Credit Marketplace for EV Owners

**Version**: 1.0  
**Last Updated**: October 25, 2025  
**Base URL**: `https://api.carbonmarketplace.vn/v1`  
**Protocol**: HTTPS only  
**Format**: JSON

---

## Table of Contents

1. [Authentication](#authentication)
2. [API Conventions](#api-conventions)
3. [User Management](#user-management)
4. [Vehicle Management](#vehicle-management)
5. [Trip Data & Carbon Credits](#trip-data--carbon-credits)
6. [Marketplace & Listings](#marketplace--listings)
7. [Transactions & Payments](#transactions--payments)
8. [Verification](#verification)
9. [Certificates](#certificates)
10. [Admin APIs](#admin-apis)
11. [Webhooks](#webhooks)
12. [Error Codes](#error-codes)

---

## Authentication

### Overview
The API uses **JWT (JSON Web Token)** based authentication with Bearer tokens.

### Authentication Flow

#### 1. Register User
```http
POST /auth/register
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "phone": "+84901234567",
  "password": "SecureP@ss123",
  "full_name": "Nguyen Van A",
  "role": "evowner",  // evowner, buyer, verifier
  "referral_code": "ABC123" // optional
}

Response: 201 Created
{
  "success": true,
  "data": {
    "user_id": "uuid-here",
    "email": "user@example.com",
    "phone": "+84901234567",
    "role": "evowner",
    "kyc_level": 0,
    "created_at": "2025-10-25T10:30:00Z"
  },
  "message": "Registration successful. Please verify your email/phone."
}
```

#### 2. Verify OTP
```http
POST /auth/verify-otp
Content-Type: application/json

Request Body:
{
  "identifier": "user@example.com",  // email or phone
  "otp": "123456",
  "type": "email"  // email or sms
}

Response: 200 OK
{
  "success": true,
  "message": "Verification successful"
}
```

#### 3. Login
```http
POST /auth/login
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "password": "SecureP@ss123",
  "remember_me": true
}

Response: 200 OK
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "refresh-token-here",
    "token_type": "Bearer",
    "expires_in": 3600,  // seconds
    "user": {
      "user_id": "uuid-here",
      "email": "user@example.com",
      "full_name": "Nguyen Van A",
      "role": "evowner",
      "kyc_level": 1
    }
  }
}
```

#### 4. Refresh Token
```http
POST /auth/refresh
Content-Type: application/json

Request Body:
{
  "refresh_token": "refresh-token-here"
}

Response: 200 OK
{
  "success": true,
  "data": {
    "access_token": "new-access-token",
    "expires_in": 3600
  }
}
```

#### 5. Logout
```http
POST /auth/logout
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "message": "Logged out successfully"
}
```

#### 6. Social Login (OAuth)
```http
GET /auth/oauth/{provider}
  ?redirect_uri=https://app.carbonmarketplace.vn/callback

Providers: google, facebook, apple

Response: 302 Redirect to OAuth provider
```

---

## API Conventions

### Request Headers
```http
Authorization: Bearer {access_token}
Content-Type: application/json
Accept: application/json
X-Request-ID: {unique-request-id}  // optional, for tracing
X-API-Version: v1  // optional
```

### Response Format
```json
{
  "success": true,
  "data": { },
  "meta": {
    "request_id": "req-123",
    "timestamp": "2025-10-25T10:30:00Z"
  },
  "pagination": {  // if applicable
    "page": 1,
    "per_page": 20,
    "total_pages": 5,
    "total_items": 100
  }
}
```

### HTTP Status Codes
- `200 OK` - Successful GET, PATCH, DELETE
- `201 Created` - Successful POST (resource created)
- `204 No Content` - Successful DELETE
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate)
- `422 Unprocessable Entity` - Validation errors
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error
- `503 Service Unavailable` - Maintenance mode

### Pagination
```http
GET /resource?page=2&per_page=50&sort=created_at&order=desc

Query Parameters:
- page: Page number (default: 1)
- per_page: Items per page (default: 20, max: 100)
- sort: Field to sort by
- order: asc or desc
```

### Rate Limiting
```http
Response Headers:
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1635177600  // Unix timestamp
```

**Limits:**
- Public API: 100 requests/minute per IP
- Authenticated: 1000 requests/minute per user
- Admin: 5000 requests/minute

---

## User Management

### Get Current User
```http
GET /users/me
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "user_id": "uuid",
    "email": "user@example.com",
    "phone": "+84901234567",
    "full_name": "Nguyen Van A",
    "role": "evowner",
    "kyc_level": 1,
    "kyc_status": "approved",
    "account_status": "active",
    "avatar_url": "https://cdn.example.com/avatars/user.jpg",
    "created_at": "2025-01-01T00:00:00Z",
    "last_login_at": "2025-10-25T10:30:00Z"
  }
}
```

### Update Profile
```http
PATCH /users/me
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "full_name": "Nguyen Van B",
  "phone": "+84909999999",
  "avatar_url": "https://cdn.example.com/avatars/new.jpg"
}

Response: 200 OK
{
  "success": true,
  "data": { /* updated user object */ },
  "message": "Profile updated successfully"
}
```

### Submit KYC Documents
```http
POST /users/me/kyc
Authorization: Bearer {access_token}
Content-Type: multipart/form-data

Form Data:
- kyc_level: 1 or 2
- id_type: "national_id", "passport", "driver_license"
- id_number: "001234567890"
- id_front: <file>  // image file
- id_back: <file>   // image file
- selfie: <file>    // image file
- address_proof: <file>  // optional, required for Level 2

Response: 201 Created
{
  "success": true,
  "data": {
    "kyc_submission_id": "uuid",
    "kyc_level": 1,
    "status": "pending",
    "submitted_at": "2025-10-25T10:30:00Z"
  },
  "message": "KYC documents submitted successfully. Review in progress."
}
```

### Change Password
```http
POST /users/me/change-password
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "current_password": "OldP@ss123",
  "new_password": "NewP@ss456",
  "confirm_password": "NewP@ss456"
}

Response: 200 OK
{
  "success": true,
  "message": "Password changed successfully. Please log in again."
}
```

### Enable 2FA
```http
POST /users/me/2fa/enable
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "qr_code": "data:image/png;base64,...",
    "secret": "BASE32SECRET",
    "backup_codes": ["12345678", "23456789", ...]
  },
  "message": "Scan QR code with authenticator app"
}
```

### Verify 2FA Setup
```http
POST /users/me/2fa/verify
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "code": "123456"
}

Response: 200 OK
{
  "success": true,
  "message": "2FA enabled successfully"
}
```

---

## Vehicle Management

### Register Vehicle
```http
POST /vehicles
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "make": "VinFast",
  "model": "VF8",
  "year": 2024,
  "vin": "VIN1234567890",
  "registration_number": "30A-12345",
  "data_source": "api",  // api, obd, manual
  "data_source_config": {
    "api_type": "vinfast",
    "api_credentials": { /* encrypted */ }
  }
}

Response: 201 Created
{
  "success": true,
  "data": {
    "vehicle_id": "uuid",
    "make": "VinFast",
    "model": "VF8",
    "year": 2024,
    "vin": "VIN1234567890",
    "registration_number": "30A-12345",
    "verification_status": "pending",
    "created_at": "2025-10-25T10:30:00Z"
  }
}
```

### List User Vehicles
```http
GET /vehicles
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "vehicle_id": "uuid",
      "make": "VinFast",
      "model": "VF8",
      "year": 2024,
      "vin": "VIN1234567890",
      "registration_number": "30A-12345",
      "data_source": "api",
      "verification_status": "verified",
      "last_sync_at": "2025-10-25T02:00:00Z",
      "total_distance_km": 5420.5,
      "total_co2_saved_kg": 542.05
    }
  ]
}
```

### Get Vehicle Details
```http
GET /vehicles/{vehicle_id}
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "vehicle_id": "uuid",
    "make": "VinFast",
    "model": "VF8",
    "year": 2024,
    "vin": "VIN1234567890",
    "registration_number": "30A-12345",
    "data_source": "api",
    "verification_status": "verified",
    "statistics": {
      "total_trips": 245,
      "total_distance_km": 5420.5,
      "total_co2_saved_kg": 542.05,
      "avg_trip_distance_km": 22.1,
      "last_trip_at": "2025-10-25T08:30:00Z"
    },
    "sync_status": {
      "last_sync_at": "2025-10-25T02:00:00Z",
      "sync_frequency": "daily",
      "next_sync_at": "2025-10-26T02:00:00Z",
      "sync_status": "success"
    }
  }
}
```

### Trigger Manual Sync
```http
POST /vehicles/{vehicle_id}/sync
Authorization: Bearer {access_token}

Response: 202 Accepted
{
  "success": true,
  "data": {
    "sync_job_id": "uuid",
    "status": "queued",
    "estimated_completion": "2025-10-25T10:35:00Z"
  },
  "message": "Sync initiated. You'll be notified when complete."
}
```

### Get Trip History
```http
GET /vehicles/{vehicle_id}/trips
  ?start_date=2025-10-01
  &end_date=2025-10-31
  &page=1
  &per_page=50
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "trip_id": "uuid",
      "start_time": "2025-10-25T08:00:00Z",
      "end_time": "2025-10-25T08:45:00Z",
      "distance_km": 35.2,
      "start_location": {
        "lat": 10.762622,
        "lng": 106.660172,
        "address": "District 1, HCMC"
      },
      "end_location": {
        "lat": 10.854886,
        "lng": 106.629639,
        "address": "Binh Thanh, HCMC"
      },
      "avg_speed": 47.5,
      "energy_consumed_kwh": 6.5,
      "co2_saved_kg": 3.52,
      "data_quality_score": 0.98
    }
  ],
  "pagination": { /* ... */ }
}
```

---

## Trip Data & Carbon Credits

### Upload Manual Trip Data
```http
POST /trips/upload
Authorization: Bearer {access_token}
Content-Type: multipart/form-data

Form Data:
- vehicle_id: uuid
- file: <csv/json file>
- file_format: "csv" or "json"

CSV Format:
start_time,end_time,distance_km,start_lat,start_lng,end_lat,end_lng,energy_kwh
2025-10-01T08:00:00Z,2025-10-01T08:30:00Z,25.5,10.762622,106.660172,10.775832,106.700806,5.2

Response: 201 Created
{
  "success": true,
  "data": {
    "upload_id": "uuid",
    "vehicle_id": "uuid",
    "total_rows": 45,
    "valid_rows": 42,
    "invalid_rows": 3,
    "total_distance_km": 890.5,
    "total_co2_saved_kg": 89.05,
    "errors": [
      {
        "row": 5,
        "error": "Invalid distance: exceeds 500km"
      }
    ]
  },
  "message": "42 trips imported successfully, 3 failed"
}
```

### Get Carbon Wallet
```http
GET /carbon-credits/wallet
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "total_balance_tons": 12.5,
    "available_tons": 5.2,      // can list for sale
    "pending_tons": 3.1,         // awaiting verification
    "listed_tons": 2.8,          // on marketplace
    "sold_pending_tons": 1.4,    // sold, pending settlement
    "lifetime_earned_tons": 25.8,
    "lifetime_revenue_vnd": 65000000,
    "credits_by_status": {
      "issued": 5.2,
      "listed": 2.8,
      "sold": 17.8
    }
  }
}
```

### Get Carbon Credits List
```http
GET /carbon-credits
  ?status=issued
  &page=1
  &per_page=20
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "credit_id": "uuid",
      "serial_number": "VN-EV-2025-TUV-000012345",
      "amount_tons": 1.5,
      "vintage_year": 2025,
      "methodology": "CDM ACM0018",
      "verification_id": "uuid",
      "verification_status": "approved",
      "status": "issued",
      "issued_at": "2025-10-15T10:00:00Z",
      "cva_organization": "TÜV SÜD"
    }
  ],
  "pagination": { /* ... */ }
}
```

### Request Verification
```http
POST /carbon-credits/verification-request
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "vehicle_id": "uuid",
  "trip_date_start": "2025-10-01",
  "trip_date_end": "2025-10-31",
  "notes": "October 2025 trips"
}

Response: 201 Created
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "vehicle_id": "uuid",
    "trip_date_start": "2025-10-01",
    "trip_date_end": "2025-10-31",
    "total_km": 1250.5,
    "co2_saved_kg": 125.05,
    "credit_amount_tons": 0.125,
    "status": "pending",
    "created_at": "2025-10-25T10:30:00Z",
    "estimated_completion": "2025-10-27T10:30:00Z"
  },
  "message": "Verification request submitted successfully"
}
```

### Get Verification Status
```http
GET /carbon-credits/verifications/{verification_id}
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "status": "approved",
    "assigned_auditor": "Auditor Name",
    "cva_organization": "TÜV SÜD",
    "created_at": "2025-10-25T10:30:00Z",
    "approved_at": "2025-10-26T14:20:00Z",
    "auditor_notes": "All checks passed. Data quality: Excellent",
    "credits_issued": {
      "amount_tons": 0.125,
      "serial_numbers": ["VN-EV-2025-TUV-000012345"]
    }
  }
}
```

---

## Marketplace & Listings

### Search Listings
```http
GET /marketplace/listings
  ?region=HCMC
  &min_price=2000000
  &max_price=3000000
  &min_amount=0.5
  &listing_type=fixed
  &verification_status=verified
  &sort=price
  &order=asc
  &page=1
  &per_page=20

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "listing_id": "uuid",
      "seller_id": "uuid",
      "seller_name": "Nguyen Van A",
      "seller_rating": 4.8,
      "seller_transactions": 15,
      "credit_amount_tons": 1.5,
      "price_per_ton_vnd": 2500000,
      "total_price_vnd": 3750000,
      "listing_type": "fixed",
      "region": "HCMC",
      "vintage_year": 2025,
      "verification_status": "verified",
      "cva_organization": "TÜV SÜD",
      "status": "active",
      "created_at": "2025-10-20T10:00:00Z",
      "views_count": 45
    }
  ],
  "meta": {
    "total_listings": 150,
    "avg_price_per_ton": 2350000,
    "price_range": {
      "min": 2000000,
      "max": 3500000
    }
  },
  "pagination": { /* ... */ }
}
```

### Get Listing Details
```http
GET /marketplace/listings/{listing_id}

Response: 200 OK
{
  "success": true,
  "data": {
    "listing_id": "uuid",
    "seller": {
      "user_id": "uuid",
      "name": "Nguyen Van A",
      "rating": 4.8,
      "total_transactions": 15,
      "member_since": "2024-01-01"
    },
    "credit": {
      "amount_tons": 1.5,
      "serial_numbers": ["VN-EV-2025-TUV-000012345"],
      "vintage_year": 2025,
      "methodology": "CDM ACM0018",
      "region": "HCMC",
      "verification": {
        "status": "verified",
        "cva_organization": "TÜV SÜD",
        "verified_at": "2025-10-15T10:00:00Z",
        "certificate_url": "https://cdn.../cert.pdf"
      }
    },
    "pricing": {
      "listing_type": "fixed",
      "price_per_ton_vnd": 2500000,
      "total_price_vnd": 3750000,
      "platform_fee_vnd": 187500,
      "buyer_total_vnd": 3937500
    },
    "description": "High-quality carbon credits from VinFast VF8...",
    "status": "active",
    "created_at": "2025-10-20T10:00:00Z",
    "views_count": 45,
    "watchers_count": 3
  }
}
```

### Create Listing
```http
POST /marketplace/listings
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body (Fixed Price):
{
  "credit_id": "uuid",
  "listing_type": "fixed",
  "credit_amount_tons": 1.5,
  "price_per_ton_vnd": 2500000,
  "region": "HCMC",
  "description": "High-quality carbon credits..."
}

Request Body (Auction):
{
  "credit_id": "uuid",
  "listing_type": "auction",
  "credit_amount_tons": 2.0,
  "starting_price_per_ton_vnd": 2000000,
  "reserve_price_per_ton_vnd": 2300000,  // optional
  "auction_duration_days": 7,
  "region": "HCMC",
  "description": "Auction for premium credits..."
}

Response: 201 Created
{
  "success": true,
  "data": {
    "listing_id": "uuid",
    "listing_type": "fixed",
    "credit_amount_tons": 1.5,
    "price_per_ton_vnd": 2500000,
    "status": "active",
    "created_at": "2025-10-25T10:30:00Z",
    "expires_at": "2026-01-23T10:30:00Z"  // 90 days
  },
  "message": "Listing created successfully. It will appear in marketplace within 5 minutes."
}
```

### Update Listing
```http
PATCH /marketplace/listings/{listing_id}
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "price_per_ton_vnd": 2400000,
  "description": "Updated description..."
}

Response: 200 OK
{
  "success": true,
  "data": { /* updated listing */ },
  "message": "Listing updated successfully"
}
```

### Cancel Listing
```http
DELETE /marketplace/listings/{listing_id}
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "message": "Listing cancelled. Credits returned to your wallet."
}
```

### Get Price Recommendation
```http
GET /marketplace/listings/price-recommendation
  ?credit_amount=1.5
  &region=HCMC
  &vintage_year=2025
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "recommended_price_per_ton_vnd": 2450000,
    "confidence_percentage": 85,
    "price_range": {
      "min": 2000000,
      "avg": 2350000,
      "max": 2800000,
      "period": "last_30_days"
    },
    "demand_indicator": "medium",
    "expected_sale_days": 7,
    "reasoning": "Based on 23 similar transactions in HCMC in the past 30 days. Demand is steady."
  }
}
```

### Place Auction Bid
```http
POST /marketplace/listings/{listing_id}/bids
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "bid_amount_per_ton_vnd": 2300000,
  "max_auto_bid_vnd": 2500000  // optional
}

Response: 201 Created
{
  "success": true,
  "data": {
    "bid_id": "uuid",
    "listing_id": "uuid",
    "bid_amount_per_ton_vnd": 2300000,
    "total_bid_vnd": 4600000,  // 2.0 tons * 2300000
    "is_winning_bid": true,
    "created_at": "2025-10-25T10:30:00Z"
  },
  "message": "Bid placed successfully. You are currently the highest bidder."
}
```

### Get Auction Bids
```http
GET /marketplace/listings/{listing_id}/bids
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "listing_id": "uuid",
    "current_bid_vnd": 2300000,
    "minimum_next_bid_vnd": 2350000,
    "bid_increment_vnd": 50000,
    "total_bids": 5,
    "auction_end_time": "2025-10-27T10:30:00Z",
    "time_remaining_seconds": 172800,
    "bids": [
      {
        "bid_id": "uuid",
        "bidder": "User****",  // anonymized
        "bid_amount_vnd": 2300000,
        "bid_time": "2025-10-25T10:30:00Z",
        "is_winning": true
      }
    ]
  }
}
```

---

## Transactions & Payments

### Create Purchase Transaction
```http
POST /transactions
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "listing_id": "uuid",
  "credit_amount_tons": 1.5,  // can be partial if seller allows
  "payment_method": "momo",  // momo, vnpay, zalopay, bank_transfer, stripe
  "billing_info": {
    "company_name": "ABC Corp",  // for corporate buyers
    "tax_code": "0123456789",
    "address": "123 Street, HCMC"
  }
}

Response: 201 Created
{
  "success": true,
  "data": {
    "transaction_id": "uuid",
    "listing_id": "uuid",
    "credit_amount_tons": 1.5,
    "unit_price_vnd": 2500000,
    "subtotal_vnd": 3750000,
    "platform_fee_vnd": 187500,
    "total_amount_vnd": 3937500,
    "payment_method": "momo",
    "payment_status": "pending",
    "payment_url": "https://payment.momo.vn/...",
    "expires_at": "2025-10-25T11:00:00Z"  // 30 min
  },
  "message": "Transaction created. Please complete payment within 30 minutes."
}
```

### Get Transaction Details
```http
GET /transactions/{transaction_id}
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "transaction_id": "uuid",
    "buyer": {
      "user_id": "uuid",
      "name": "ABC Corp"
    },
    "seller": {
      "user_id": "uuid",
      "name": "Nguyen Van A"
    },
    "credit_amount_tons": 1.5,
    "unit_price_vnd": 2500000,
    "subtotal_vnd": 3750000,
    "platform_fee_vnd": 187500,
    "total_amount_vnd": 3937500,
    "payment_method": "momo",
    "payment_status": "completed",
    "escrow_status": "released",
    "certificate_id": "uuid",
    "timeline": [
      {
        "event": "transaction_created",
        "timestamp": "2025-10-25T10:30:00Z"
      },
      {
        "event": "payment_completed",
        "timestamp": "2025-10-25T10:35:00Z"
      },
      {
        "event": "credits_transferred",
        "timestamp": "2025-10-25T10:35:30Z"
      },
      {
        "event": "certificate_issued",
        "timestamp": "2025-10-25T10:36:00Z"
      },
      {
        "event": "payment_released_to_seller",
        "timestamp": "2025-10-27T10:36:00Z"
      }
    ],
    "created_at": "2025-10-25T10:30:00Z",
    "completed_at": "2025-10-27T10:36:00Z"
  }
}
```

### List My Transactions
```http
GET /transactions
  ?role=buyer  // buyer or seller
  &status=completed
  &start_date=2025-10-01
  &end_date=2025-10-31
  &page=1
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "transaction_id": "uuid",
      "role": "buyer",
      "counterparty_name": "Nguyen Van A",
      "credit_amount_tons": 1.5,
      "total_amount_vnd": 3937500,
      "payment_status": "completed",
      "certificate_id": "uuid",
      "created_at": "2025-10-25T10:30:00Z"
    }
  ],
  "pagination": { /* ... */ },
  "summary": {
    "total_transactions": 15,
    "total_spent_vnd": 45000000,
    "total_credits_tons": 18.5
  }
}
```

### Request Refund
```http
POST /transactions/{transaction_id}/refund
Authorization: Bearer {access_token}
Content-Type: application/json

Request Body:
{
  "reason": "quality_issue",  // quality_issue, duplicate, other
  "description": "Credits were already retired elsewhere",
  "evidence_urls": ["https://..."]  // optional
}

Response: 201 Created
{
  "success": true,
  "data": {
    "refund_request_id": "uuid",
    "transaction_id": "uuid",
    "status": "pending_review",
    "created_at": "2025-10-25T10:30:00Z"
  },
  "message": "Refund request submitted. Admin will review within 5 business days."
}
```

---

## Verification

### CVA: Get Verification Queue
```http
GET /verifications/queue
  ?status=pending
  &assigned_to=me
  &page=1
Authorization: Bearer {access_token}
X-Role: verifier

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "verification_id": "uuid",
      "owner": {
        "user_id": "uuid",
        "name": "Nguyen Van A",
        "kyc_level": 1
      },
      "vehicle": {
        "vehicle_id": "uuid",
        "make": "VinFast",
        "model": "VF8"
      },
      "trip_date_range": {
        "start": "2025-10-01",
        "end": "2025-10-31"
      },
      "total_km": 1250.5,
      "co2_saved_kg": 125.05,
      "credit_amount_tons": 0.125,
      "data_source": "api",
      "status": "pending",
      "priority": "normal",
      "created_at": "2025-10-25T10:30:00Z",
      "sla_deadline": "2025-10-27T10:30:00Z"
    }
  ],
  "pagination": { /* ... */ }
}
```

### CVA: Assign Verification
```http
POST /verifications/{verification_id}/assign
Authorization: Bearer {access_token}
X-Role: verifier
Content-Type: application/json

Request Body:
{
  "auditor_id": "uuid"  // optional, assigns to self if not provided
}

Response: 200 OK
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "assigned_to": "uuid",
    "assigned_at": "2025-10-25T10:30:00Z"
  }
}
```

### CVA: Get Verification Details
```http
GET /verifications/{verification_id}
Authorization: Bearer {access_token}
X-Role: verifier

Response: 200 OK
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "owner": { /* detailed owner info */ },
    "vehicle": { /* detailed vehicle info */ },
    "trip_data": {
      "total_trips": 45,
      "total_km": 1250.5,
      "data_source": "api",
      "data_quality_score": 0.95,
      "anomalies": [
        {
          "trip_id": "uuid",
          "issue": "High speed detected",
          "severity": "warning"
        }
      ],
      "download_url": "https://cdn.../trip-data.csv"
    },
    "calculation": {
      "total_km": 1250.5,
      "ice_emission_factor_kg_per_km": 0.15,
      "ev_emission_factor_kg_per_km": 0.05,
      "net_reduction_factor": 0.10,
      "co2_saved_kg": 125.05,
      "credit_amount_tons": 0.125,
      "methodology": "CDM ACM0018",
      "calculation_breakdown_url": "https://cdn.../calc.pdf"
    },
    "status": "pending",
    "created_at": "2025-10-25T10:30:00Z"
  }
}
```

### CVA: Approve Verification
```http
POST /verifications/{verification_id}/approve
Authorization: Bearer {access_token}
X-Role: verifier
Content-Type: application/json

Request Body:
{
  "auditor_notes": "All checks passed. Data quality: Excellent",
  "adjusted_amount_tons": 0.123  // optional, if recalculated
}

Response: 200 OK
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "status": "approved",
    "approved_at": "2025-10-26T14:20:00Z",
    "credits_issued": {
      "amount_tons": 0.125,
      "serial_numbers": ["VN-EV-2025-TUV-000012345"]
    }
  },
  "message": "Verification approved. Credits issued to owner's wallet."
}
```

### CVA: Reject Verification
```http
POST /verifications/{verification_id}/reject
Authorization: Bearer {access_token}
X-Role: verifier
Content-Type: application/json

Request Body:
{
  "reason": "insufficient_data_quality",
  "auditor_notes": "GPS accuracy below threshold for 30% of trips"
}

Response: 200 OK
{
  "success": true,
  "data": {
    "verification_id": "uuid",
    "status": "rejected",
    "rejected_at": "2025-10-26T14:20:00Z"
  },
  "message": "Verification rejected. Owner has been notified."
}
```

---

## Certificates

### Get Certificate
```http
GET /certificates/{certificate_id}
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": {
    "certificate_id": "uuid",
    "certificate_number": "CERT-2025-123456",
    "transaction_id": "uuid",
    "buyer": {
      "company_name": "ABC Corp",
      "tax_code": "0123456789"
    },
    "seller": {
      "name": "Nguyen Van A"
    },
    "credit_details": {
      "amount_tons": 1.5,
      "serial_numbers": ["VN-EV-2025-TUV-000012345"],
      "vintage_year": 2025,
      "methodology": "CDM ACM0018",
      "region": "HCMC"
    },
    "verification": {
      "cva_organization": "TÜV SÜD",
      "auditor_name": "John Doe",
      "verification_date": "2025-10-15T10:00:00Z"
    },
    "issue_date": "2025-10-25T10:36:00Z",
    "validity_status": "valid",
    "pdf_url": "https://cdn.../certificates/cert.pdf",
    "qr_code": "data:image/png;base64,...",
    "blockchain_hash": "0x123abc..."  // optional
  }
}
```

### List My Certificates
```http
GET /certificates
  ?start_date=2025-01-01
  &end_date=2025-12-31
  &page=1
Authorization: Bearer {access_token}

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "certificate_id": "uuid",
      "certificate_number": "CERT-2025-123456",
      "credit_amount_tons": 1.5,
      "issue_date": "2025-10-25T10:36:00Z",
      "pdf_url": "https://cdn.../cert.pdf"
    }
  ],
  "pagination": { /* ... */ },
  "summary": {
    "total_certificates": 12,
    "total_credits_tons": 18.5
  }
}
```

### Download Certificate PDF
```http
GET /certificates/{certificate_id}/download
Authorization: Bearer {access_token}

Response: 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="certificate-123456.pdf"

[Binary PDF data]
```

### Verify Certificate (Public)
```http
GET /public/certificates/verify/{certificate_number}

OR

POST /public/certificates/verify
Content-Type: application/json

Request Body:
{
  "certificate_number": "CERT-2025-123456"
}

Response: 200 OK
{
  "success": true,
  "data": {
    "certificate_number": "CERT-2025-123456",
    "validity_status": "valid",  // valid, revoked, expired, not_found
    "issue_date": "2025-10-25",
    "credit_amount_tons": 1.5,
    "cva_organization": "TÜV SÜD",
    "vintage_year": 2025,
    "is_retired": false
  }
}
```

---

## Admin APIs

### Admin: Get Platform Statistics
```http
GET /admin/statistics
  ?period=month  // today, week, month, year, custom
  &start_date=2025-10-01
  &end_date=2025-10-31
Authorization: Bearer {access_token}
X-Role: admin

Response: 200 OK
{
  "success": true,
  "data": {
    "users": {
      "total": 15000,
      "new_this_period": 1200,
      "by_role": {
        "evowner": 12000,
        "buyer": 2800,
        "verifier": 200
      },
      "active_users_mau": 8500
    },
    "marketplace": {
      "total_listings": 450,
      "active_listings": 320,
      "total_transactions": 3500,
      "gmv_vnd": 8500000000,
      "avg_transaction_value_vnd": 2428571
    },
    "carbon_credits": {
      "total_issued_tons": 8500,
      "total_traded_tons": 6200,
      "total_co2_offset_tons": 6200
    },
    "revenue": {
      "total_revenue_vnd": 425000000,
      "transaction_fees_vnd": 340000000,
      "listing_fees_vnd": 0,
      "other_fees_vnd": 85000000
    },
    "verification": {
      "total_requests": 10500,
      "approved": 8900,
      "rejected": 1200,
      "pending": 400,
      "avg_processing_hours": 28
    }
  }
}
```

### Admin: List Users
```http
GET /admin/users
  ?role=evowner
  &kyc_level=1
  &account_status=active
  &search=nguyen
  &page=1
Authorization: Bearer {access_token}
X-Role: admin

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "user_id": "uuid",
      "email": "user@example.com",
      "full_name": "Nguyen Van A",
      "role": "evowner",
      "kyc_level": 1,
      "account_status": "active",
      "total_transactions": 5,
      "lifetime_value_vnd": 12500000,
      "created_at": "2025-01-01T00:00:00Z",
      "last_login_at": "2025-10-25T10:00:00Z"
    }
  ],
  "pagination": { /* ... */ }
}
```

### Admin: Update User Status
```http
PATCH /admin/users/{user_id}
Authorization: Bearer {access_token}
X-Role: admin
Content-Type: application/json

Request Body:
{
  "account_status": "suspended",
  "reason": "Fraudulent activity detected"
}

Response: 200 OK
{
  "success": true,
  "data": { /* updated user */ },
  "message": "User account suspended successfully"
}
```

### Admin: Process Settlements
```http
POST /admin/settlements/process
Authorization: Bearer {access_token}
X-Role: admin
Content-Type: application/json

Request Body:
{
  "settlement_date": "2025-10-25",
  "dry_run": false  // if true, only simulate
}

Response: 202 Accepted
{
  "success": true,
  "data": {
    "job_id": "uuid",
    "total_payouts": 45,
    "total_amount_vnd": 125000000,
    "status": "processing",
    "estimated_completion": "2025-10-25T12:00:00Z"
  }
}
```

---

## Webhooks

### Webhook Event Structure
```json
{
  "event_id": "uuid",
  "event_type": "transaction.completed",
  "timestamp": "2025-10-25T10:36:00Z",
  "data": {
    "transaction_id": "uuid",
    "buyer_id": "uuid",
    "seller_id": "uuid",
    "amount_vnd": 3937500
  },
  "signature": "sha256-hash-here"
}
```

### Supported Webhook Events
- `user.registered`
- `user.kyc_approved`
- `user.kyc_rejected`
- `vehicle.registered`
- `trip.synced`
- `verification.submitted`
- `verification.approved`
- `verification.rejected`
- `credits.issued`
- `listing.created`
- `listing.sold`
- `transaction.created`
- `transaction.payment_completed`
- `transaction.completed`
- `transaction.failed`
- `certificate.issued`
- `payout.completed`
- `payout.failed`

### Webhook Verification
```javascript
const crypto = require('crypto');

function verifyWebhookSignature(payload, signature, secret) {
  const hash = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');
  
  return signature === `sha256=${hash}`;
}
```

---

## Error Codes

### Error Response Format
```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT",
    "message": "The provided email address is invalid",
    "field": "email",  // optional
    "details": { }  // optional
  },
  "meta": {
    "request_id": "req-123",
    "timestamp": "2025-10-25T10:30:00Z"
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_INPUT` | 400 | Invalid request parameters |
| `VALIDATION_ERROR` | 422 | Validation failed |
| `UNAUTHORIZED` | 401 | Missing or invalid authentication |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `ALREADY_EXISTS` | 409 | Resource already exists |
| `INSUFFICIENT_BALANCE` | 400 | Not enough credits/funds |
| `LISTING_UNAVAILABLE` | 410 | Listing sold or expired |
| `PAYMENT_FAILED` | 402 | Payment processing failed |
| `VERIFICATION_PENDING` | 400 | Verification not yet complete |
| `KYC_REQUIRED` | 403 | KYC verification required |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Internal server error |
| `SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable |

---

## Postman Collection

A complete Postman collection is available at:
```
https://api.carbonmarketplace.vn/public/postman-collection.json
```

Import this into Postman for easy API testing.

---

## API Versioning

**Current Version**: v1

### Version Strategy
- Versions are specified in the URL: `/v1/`, `/v2/`
- Major version changes for breaking changes
- Minor changes backwards-compatible
- Version support: Current + 1 previous version (12-month deprecation notice)

### Deprecated Endpoints
- Marked with `Deprecated` header in response
- Documentation includes migration guide
- 12-month sunset period before removal

---

## SDK & Client Libraries

Official SDKs available:
- **JavaScript/TypeScript**: `@carbonmarketplace/api-client`
- **Python**: `carbonmarketplace-python`
- **Java**: `carbonmarketplace-java-sdk`

```javascript
// JavaScript Example
import { CarbonMarketplace } from '@carbonmarketplace/api-client';

const client = new CarbonMarketplace({
  apiKey: 'your-api-key',
  environment: 'production'
});

const listings = await client.marketplace.searchListings({
  region: 'HCMC',
  minPrice: 2000000,
  maxPrice: 3000000
});
```

---

**Document Version**: 1.0  
**Last Updated**: October 25, 2025  
**API Status**: https://status.carbonmarketplace.vn  
**Support**: api-support@carbonmarketplace.vn

