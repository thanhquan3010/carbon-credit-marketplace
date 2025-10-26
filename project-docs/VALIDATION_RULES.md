# Validation Rules
## Comprehensive Field Validation for Carbon Credit Marketplace

**Version**: 1.0  
**Last Updated**: October 26, 2025  
**Source**: Extracted from REQUIREMENTS.md and DATABASE_SCHEMA.md

---

## 📋 Validation Categories

1. [User & Authentication](#1-user--authentication)
2. [Vehicle & Trip Data](#2-vehicle--trip-data)
3. [Carbon Credits & Verification](#3-carbon-credits--verification)
4. [Marketplace & Listings](#4-marketplace--listings)
5. [Transactions & Payments](#5-transactions--payments)
6. [System-wide Rules](#6-system-wide-rules)

---

## 1. User & Authentication

### 1.1 Registration Fields

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **email** | Valid email format | "Please enter a valid email address" | `@Email` |
| | Lowercase only | "Email must be lowercase" | `.toLowerCase()` |
| | Unique in database | "This email is already registered" | `@Unique` |
| | Max 255 characters | "Email is too long" | `@Size(max=255)` |
| **phone** | Vietnam format (+84XXXXXXXXX) | "Phone must be Vietnamese format starting with +84" | `@Pattern("^\\+84[0-9]{9,10}$")` |
| | Unique in database | "This phone number is already registered" | `@Unique` |
| **password** | Min 8 characters | "Password must be at least 8 characters" | `@Size(min=8)` |
| | Contains uppercase | "Password must contain at least one uppercase letter" | Custom validator |
| | Contains lowercase | "Password must contain at least one lowercase letter" | Custom validator |
| | Contains number | "Password must contain at least one number" | Custom validator |
| | Contains special char | "Password must contain at least one special character (!@#$%^&*)" | Custom validator |
| | Not in common passwords list | "This password is too common" | Check against list |
| | Cannot reuse last 5 | "You cannot reuse your last 5 passwords" | Check history |
| **full_name** | Required | "Full name is required" | `@NotBlank` |
| | Min 2 characters | "Name must be at least 2 characters" | `@Size(min=2)` |
| | Max 255 characters | "Name is too long" | `@Size(max=255)` |
| | No special characters except space, dash, apostrophe | "Name contains invalid characters" | `@Pattern` |
| **role** | Must be valid role | "Invalid role selected" | `@Enum(evowner, buyer, verifier, admin)` |
| **referral_code** | Optional | - | - |
| | Max 20 characters | "Referral code is too long" | `@Size(max=20)` |
| | Alphanumeric only | "Referral code must be alphanumeric" | `@Pattern("^[A-Z0-9]+$")` |

### 1.2 KYC Documents

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **kyc_level** | Must be 1 or 2 | "Invalid KYC level" | `@Min(1) @Max(2)` |
| **id_type** | Valid types only | "Invalid ID type" | `@Enum(national_id, passport, driver_license)` |
| **id_number** | Required for KYC | "ID number is required" | `@NotBlank` |
| | Alphanumeric | "ID number contains invalid characters" | `@Pattern` |
| | Max 100 characters | "ID number is too long" | `@Size(max=100)` |
| | Format validation per type | "Invalid {id_type} format" | Custom validator |
| **id_front** | Required file | "Front ID image is required" | `@NotNull` |
| | Image format (JPG, PNG) | "File must be JPG or PNG" | MIME type check |
| | Max 5MB | "File size must not exceed 5MB" | Size validation |
| | Min resolution 300x300 | "Image resolution too low" | Image validation |
| **selfie** | Required file | "Selfie photo is required" | `@NotNull` |
| | Face detection | "No face detected in selfie" | AI validation |
| | Liveness check | "Please take a live photo" | Liveness detection |

### 1.3 Company Registration (Corporate Buyers)

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **company_name** | Required for buyers | "Company name is required" | `@NotBlank` |
| | Max 255 characters | "Company name is too long" | `@Size(max=255)` |
| **tax_code** | Required for buyers | "Tax code is required" | `@NotBlank` |
| | 10-13 digits | "Tax code must be 10-13 digits" | `@Pattern("^[0-9]{10,13}$")` |
| | Valid checksum | "Invalid tax code" | Custom validator |
| **business_registration_number** | Format validation | "Invalid business registration format" | `@Pattern` |
| | Unique | "This business is already registered" | Database check |

### 1.4 Authentication

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **OTP** | 6 digits | "OTP must be 6 digits" | `@Pattern("^[0-9]{6}$")` |
| | Valid for 5 minutes | "OTP has expired" | Time check |
| | Max 3 attempts | "Too many attempts. Please request new OTP" | Counter check |
| **2FA Code** | 6 digits | "2FA code must be 6 digits" | `@Pattern("^[0-9]{6}$")` |
| | Valid time window (±30s) | "Invalid or expired 2FA code" | TOTP validation |
| **JWT Token** | Valid signature | "Invalid token signature" | JWT validation |
| | Not expired | "Token has expired" | Expiry check |
| | Valid issuer | "Invalid token issuer" | Issuer validation |

---

## 2. Vehicle & Trip Data

### 2.1 Vehicle Registration

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **make** | Required | "Vehicle make is required" | `@NotBlank` |
| | Max 100 characters | "Make name is too long" | `@Size(max=100)` |
| | From approved list | "Vehicle make not supported" | Enum validation |
| **model** | Required | "Vehicle model is required" | `@NotBlank` |
| | Max 100 characters | "Model name is too long" | `@Size(max=100)` |
| | Valid for make | "Invalid model for selected make" | Relationship check |
| **year** | Required | "Manufacturing year is required" | `@NotNull` |
| | Between 2010-current+1 | "Year must be between 2010 and {current_year+1}" | `@Min(2010) @Max(current+1)` |
| **VIN** | Required | "VIN is required" | `@NotBlank` |
| | Exactly 17 characters | "VIN must be exactly 17 characters" | `@Size(min=17, max=17)` |
| | Alphanumeric (no I, O, Q) | "VIN contains invalid characters" | `@Pattern` |
| | Valid checksum (9th digit) | "Invalid VIN checksum" | VIN algorithm |
| | Unique in system | "This vehicle is already registered" | `@Unique` |
| **registration_number** | Optional | - | - |
| | Max 50 characters | "Registration number is too long" | `@Size(max=50)` |
| | Valid format (e.g., 30A-12345) | "Invalid registration number format" | `@Pattern` |
| **data_source** | Required | "Data source is required" | `@NotNull` |
| | Valid types only | "Invalid data source" | `@Enum(api, obd, manual)` |

### 2.2 Trip Data

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **start_time** | Required | "Trip start time is required" | `@NotNull` |
| | Not in future | "Start time cannot be in the future" | Date validation |
| | ISO 8601 format | "Invalid datetime format" | DateTime parser |
| **end_time** | Required | "Trip end time is required" | `@NotNull` |
| | After start_time | "End time must be after start time" | `@AssertTrue` |
| | Duration > 2 minutes | "Trip duration must be at least 2 minutes" | Duration check |
| **distance_km** | Required | "Distance is required" | `@NotNull` |
| | Min 0.1 km | "Distance must be at least 0.1 km" | `@DecimalMin("0.1")` |
| | Max 500 km | "Distance cannot exceed 500 km per trip" | `@DecimalMax("500")` |
| | Positive value | "Distance must be positive" | `@Positive` |
| **avg_speed** | Optional but validated | - | - |
| | Max 180 km/h | "Average speed cannot exceed 180 km/h" | `@Max(180)` |
| | Non-negative | "Speed cannot be negative" | `@Min(0)` |
| | Consistent with distance/time | "Speed doesn't match distance and duration" | Calculation check |
| **energy_consumed_kwh** | Optional | - | - |
| | Positive value | "Energy consumption must be positive" | `@Positive` |
| | Efficiency 10-25 kWh/100km | "Unusual energy efficiency detected" | Range validation |
| **start_lat/start_lng** | Valid coordinates | "Invalid GPS coordinates" | Coordinate validation |
| | Lat: -90 to 90 | "Latitude must be between -90 and 90" | `@Min(-90) @Max(90)` |
| | Lng: -180 to 180 | "Longitude must be between -180 and 180" | `@Min(-180) @Max(180)` |
| | Within Vietnam bounds | "Location outside Vietnam" | Geo-fence check |

### 2.3 CSV Upload Validation

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **File format** | CSV or JSON | "File must be CSV or JSON format" | MIME type check |
| **File size** | Max 10MB | "File size cannot exceed 10MB" | Size validation |
| **Row count** | Max 10,000 rows | "Maximum 10,000 trips per upload" | Row counter |
| **Required columns** | Must have all required | "Missing required column: {column}" | Header validation |
| **Date format** | ISO 8601 | "Invalid date format in row {row}" | DateTime parser |
| **Duplicate detection** | No duplicate trips | "Duplicate trip found at row {row}" | Hash comparison |

---

## 3. Carbon Credits & Verification

### 3.1 Verification Request

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **vehicle_id** | Required | "Vehicle ID is required" | `@NotNull` |
| | Must exist | "Vehicle not found" | Foreign key check |
| | Must be verified | "Vehicle must be verified first" | Status check |
| **trip_date_start** | Required | "Start date is required" | `@NotNull` |
| | Not in future | "Start date cannot be in the future" | Date validation |
| | Max 1 year ago | "Cannot verify trips older than 1 year" | Date range |
| **trip_date_end** | Required | "End date is required" | `@NotNull` |
| | After start date | "End date must be after start date" | Date comparison |
| | Max range 3 months | "Maximum 3 months per verification" | Range validation |
| **credit_amount_tons** | Min 0.001 tons | "Minimum credit amount is 0.001 tons" | `@DecimalMin("0.001")` |
| | Max 100 tons per request | "Maximum 100 tons per verification" | `@DecimalMax("100")` |
| | Calculation accuracy ±2% | "Calculation variance exceeds 2%" | Tolerance check |

### 3.2 Credit Issuance

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **serial_number** | Format: VN-EV-YYYY-CVA-NNNNNN | "Invalid serial number format" | Regex validation |
| | Unique | "Serial number already exists" | `@Unique` |
| | Sequential | "Serial number out of sequence" | Sequence check |
| **amount_tons** | Positive | "Credit amount must be positive" | `@Positive` |
| | Max 4 decimal places | "Too many decimal places" | `@Digits(integer=10, fraction=4)` |
| **vintage_year** | Current year or previous | "Invalid vintage year" | Year validation |
| | Not future | "Vintage year cannot be in the future" | `@Max(current_year)` |
| **methodology** | From approved list | "Invalid methodology" | Enum validation |
| | Currently CDM ACM0018 | "Only CDM ACM0018 supported" | Specific check |

---

## 4. Marketplace & Listings

### 4.1 Listing Creation

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **listing_type** | Required | "Listing type is required" | `@NotNull` |
| | Valid types | "Invalid listing type" | `@Enum(fixed, auction)` |
| **credit_amount_tons** | Required | "Credit amount is required" | `@NotNull` |
| | Min 0.1 tons | "Minimum listing is 0.1 tons" | `@DecimalMin("0.1")` |
| | Max available balance | "Insufficient credit balance" | Balance check |
| | For auction: Min 1 ton | "Auction minimum is 1 ton" | Conditional validation |
| **price_per_ton_vnd** | Required for fixed | "Price is required for fixed listings" | Conditional |
| | Min 1,000,000 VND | "Minimum price is 1,000,000 VND/ton" | `@Min(1000000)` |
| | Max 100,000,000 VND | "Maximum price is 100,000,000 VND/ton" | `@Max(100000000)` |
| **starting_price** | Required for auction | "Starting price required for auction" | Conditional |
| | Min 1,000,000 VND | "Minimum starting price is 1,000,000 VND" | `@Min(1000000)` |
| **reserve_price** | Optional for auction | - | - |
| | Above starting price | "Reserve must be above starting price" | Comparison |
| | Max 10x starting | "Reserve price too high" | Ratio check |
| **auction_duration_days** | Required for auction | "Duration required for auction" | Conditional |
| | Min 1 day | "Minimum auction duration is 1 day" | `@Min(1)` |
| | Max 30 days | "Maximum auction duration is 30 days" | `@Max(30)` |
| **description** | Optional | - | - |
| | Max 500 characters | "Description too long (max 500 chars)" | `@Size(max=500)` |
| | No HTML/scripts | "Description contains invalid content" | XSS prevention |
| **region** | Required | "Region is required" | `@NotBlank` |
| | From province list | "Invalid region" | Enum validation |

### 4.2 Bidding

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **bid_amount** | Required | "Bid amount is required" | `@NotNull` |
| | Above current bid | "Bid must be higher than current bid" | Comparison |
| | Min increment 50,000 VND | "Minimum bid increment is 50,000 VND" | Increment check |
| | Within user budget | "Bid exceeds your available budget" | Budget check |
| **max_auto_bid** | Optional | - | - |
| | Above current bid | "Auto-bid max must be above current bid" | Comparison |
| | Max 1 billion VND | "Maximum auto-bid is 1 billion VND" | `@Max(1000000000)` |

### 4.3 Search Filters

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **min_price** | Non-negative | "Minimum price cannot be negative" | `@Min(0)` |
| | Less than max_price | "Min price must be less than max price" | Cross-field |
| **max_price** | Positive | "Maximum price must be positive" | `@Positive` |
| **min_amount** | Positive | "Minimum amount must be positive" | `@Positive` |
| | Max 1000 tons | "Search limited to 1000 tons" | `@Max(1000)` |
| **page** | Min 1 | "Page must be at least 1" | `@Min(1)` |
| | Max 1000 | "Maximum page is 1000" | `@Max(1000)` |
| **per_page** | Min 1 | "Items per page must be at least 1" | `@Min(1)` |
| | Max 100 | "Maximum 100 items per page" | `@Max(100)` |

---

## 5. Transactions & Payments

### 5.1 Purchase Transaction

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **listing_id** | Required | "Listing ID is required" | `@NotNull` |
| | Must exist | "Listing not found" | Foreign key |
| | Must be active | "Listing is not available" | Status check |
| **credit_amount** | Required | "Amount is required" | `@NotNull` |
| | Positive | "Amount must be positive" | `@Positive` |
| | Max available in listing | "Amount exceeds available credits" | Availability check |
| **payment_method** | Required | "Payment method is required" | `@NotNull` |
| | Valid methods | "Invalid payment method" | `@Enum(momo, vnpay, zalopay, bank_transfer, stripe, invoice)` |
| **total_amount_vnd** | Min 100,000 VND | "Minimum transaction is 100,000 VND" | `@Min(100000)` |
| | Max 10 billion VND | "Maximum transaction is 10 billion VND" | `@Max(10000000000)` |

### 5.2 Withdrawal

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **amount_vnd** | Required | "Withdrawal amount is required" | `@NotNull` |
| | Min 500,000 VND | "Minimum withdrawal is 500,000 VND" | `@Min(500000)` |
| | Max 50,000,000 VND/day | "Daily limit is 50,000,000 VND" | Daily limit check |
| | Max available balance | "Insufficient balance" | Balance check |
| **bank_account_id** | Required | "Bank account is required" | `@NotNull` |
| | Must be verified | "Bank account not verified" | Verification check |
| | Must belong to user | "Invalid bank account" | Ownership check |
| **two_fa_code** | Required for >5M VND | "2FA required for large withdrawals" | Conditional |
| | Valid TOTP | "Invalid 2FA code" | TOTP validation |

### 5.3 Bank Account

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **bank_name** | Required | "Bank name is required" | `@NotBlank` |
| | From bank list | "Invalid bank" | Enum validation |
| **account_number** | Required | "Account number is required" | `@NotBlank` |
| | Numeric only | "Account number must be numeric" | `@Pattern("^[0-9]+$")` |
| | Length 9-20 digits | "Invalid account number length" | `@Size(min=9, max=20)` |
| | Valid for bank | "Invalid account format for {bank}" | Bank-specific validation |
| **account_holder** | Required | "Account holder name is required" | `@NotBlank` |
| | Uppercase only | "Name must be in UPPERCASE" | Text transformation |
| | Match KYC name | "Account name doesn't match KYC" | Name matching |

### 5.4 Payment Processing

| Field | Rule | Error Message | Implementation |
|-------|------|---------------|----------------|
| **payment_timeout** | 30 minutes | "Payment session expired" | Timeout check |
| **idempotency_key** | Required for retries | "Idempotency key required" | `@NotNull` for retry |
| | UUID format | "Invalid idempotency key format" | UUID validation |
| | Unique per 24h | "Duplicate request" | Deduplication |
| **webhook_signature** | Required | "Missing webhook signature" | `@NotNull` |
| | Valid HMAC | "Invalid webhook signature" | HMAC validation |
| **refund_reason** | Required for refund | "Refund reason is required" | `@NotBlank` |
| | From reason list | "Invalid refund reason" | Enum validation |
| | Max 500 characters | "Reason too long" | `@Size(max=500)` |

---

## 6. System-wide Rules

### 6.1 General Input Validation

| Field Type | Rule | Error Message | Implementation |
|------------|------|---------------|----------------|
| **All text inputs** | XSS prevention | "Input contains invalid characters" | HTML encoding |
| | SQL injection prevention | "Invalid input detected" | Parameterized queries |
| | Max length enforcement | "Input exceeds maximum length" | `@Size` annotation |
| **All file uploads** | Virus scanning | "File failed security scan" | AV integration |
| | MIME type validation | "Invalid file type" | Content-type check |
| | Size limits | "File too large" | Size validation |
| **All API requests** | Rate limiting | "Too many requests" | Rate limiter |
| | Request size limit | "Request too large" | Max body size |
| | Timeout | "Request timeout" | 30s timeout |

### 6.2 Business Rules

| Rule | Validation | Error Message | Implementation |
|------|------------|---------------|----------------|
| **Platform fees** | 5-8% configurable | "Invalid fee configuration" | Config validation |
| **Daily limits** | User: 10 listings/day | "Daily listing limit reached" | Counter check |
| | User: 50 transactions/day | "Daily transaction limit reached" | Counter check |
| | System: 10,000 tx/day | "System capacity reached" | Capacity check |
| **Time windows** | Cart reservation: 30 min | "Cart reservation expired" | Timer check |
| | Payment: 30 min | "Payment session expired" | Timer check |
| | Auction extension: 10 min | "Invalid auction extension" | Rule engine |

### 6.3 Data Consistency

| Rule | Validation | Error Message | Implementation |
|------|------------|---------------|----------------|
| **Double-entry** | Credits + money balanced | "Transaction imbalance detected" | Ledger check |
| **Idempotency** | No duplicate transactions | "Duplicate transaction" | Idempotency key |
| **Atomic operations** | All or nothing | "Transaction failed" | Database transaction |
| **Eventual consistency** | Sync within 5 minutes | "Sync delayed" | Queue monitoring |

---

## Implementation Examples

### Java Spring Boot Validation

```java
@RestController
@Validated
public class UserController {
    
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Validation happens automatically
        return ResponseEntity.ok(userService.register(request));
    }
}

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "Email is too long")
    private String email;
    
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+84[0-9]{9,10}$", 
             message = "Phone must be Vietnamese format starting with +84")
    private String phone;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @ValidPassword // Custom annotation
    private String password;
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Name must be 2-255 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s'-]+$", 
             message = "Name contains invalid characters")
    private String fullName;
    
    @NotNull(message = "Role is required")
    @ValidRole // Custom enum validator
    private String role;
}
```

### Custom Validator Example

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {
    String message() default "Invalid password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;
        
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*].*");
        
        if (!hasUpper) {
            setMessage(context, "Password must contain at least one uppercase letter");
            return false;
        }
        if (!hasLower) {
            setMessage(context, "Password must contain at least one lowercase letter");
            return false;
        }
        if (!hasDigit) {
            setMessage(context, "Password must contain at least one number");
            return false;
        }
        if (!hasSpecial) {
            setMessage(context, "Password must contain at least one special character");
            return false;
        }
        
        return true;
    }
    
    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
```

### Frontend Validation (React + Yup)

```typescript
import * as Yup from 'yup';

const registrationSchema = Yup.object({
  email: Yup.string()
    .email('Please enter a valid email address')
    .max(255, 'Email is too long')
    .required('Email is required'),
    
  phone: Yup.string()
    .matches(/^\+84[0-9]{9,10}$/, 'Phone must be Vietnamese format starting with +84')
    .required('Phone is required'),
    
  password: Yup.string()
    .min(8, 'Password must be at least 8 characters')
    .matches(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .matches(/[a-z]/, 'Password must contain at least one lowercase letter')
    .matches(/[0-9]/, 'Password must contain at least one number')
    .matches(/[!@#$%^&*]/, 'Password must contain at least one special character')
    .required('Password is required'),
    
  confirmPassword: Yup.string()
    .oneOf([Yup.ref('password')], 'Passwords must match')
    .required('Please confirm your password'),
    
  fullName: Yup.string()
    .min(2, 'Name must be at least 2 characters')
    .max(255, 'Name is too long')
    .matches(/^[a-zA-ZÀ-ỹ\s'-]+$/, 'Name contains invalid characters')
    .required('Full name is required'),
    
  role: Yup.string()
    .oneOf(['evowner', 'buyer'], 'Invalid role selected')
    .required('Role is required'),
    
  agreeToTerms: Yup.boolean()
    .oneOf([true], 'You must accept the terms and conditions')
});
```

---

## Error Response Format

### Standard Error Response

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "timestamp": "2025-10-26T10:30:00Z",
    "path": "/api/v1/auth/register",
    "details": [
      {
        "field": "email",
        "value": "invalid-email",
        "message": "Please enter a valid email address"
      },
      {
        "field": "password",
        "value": null,
        "message": "Password must contain at least one uppercase letter"
      },
      {
        "field": "phone",
        "value": "123456",
        "message": "Phone must be Vietnamese format starting with +84"
      }
    ]
  }
}
```

### Localized Error Messages

```javascript
const errorMessages = {
  en: {
    'email.required': 'Email is required',
    'email.invalid': 'Please enter a valid email address',
    'password.weak': 'Password is too weak'
  },
  vi: {
    'email.required': 'Email là bắt buộc',
    'email.invalid': 'Vui lòng nhập địa chỉ email hợp lệ',
    'password.weak': 'Mật khẩu quá yếu'
  }
};
```

---

## Testing Validation Rules

### Unit Test Example

```java
@Test
public void testEmailValidation() {
    RegisterRequest request = new RegisterRequest();
    
    // Test missing email
    request.setEmail(null);
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().equals("Email is required")));
    
    // Test invalid format
    request.setEmail("invalid-email");
    violations = validator.validate(request);
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().equals("Please enter a valid email address")));
    
    // Test too long
    request.setEmail("a".repeat(256) + "@example.com");
    violations = validator.validate(request);
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().equals("Email is too long")));
    
    // Test valid email
    request.setEmail("user@example.com");
    violations = validator.validateProperty(request, "email");
    assertTrue(violations.isEmpty());
}
```

---

**Document Version**: 1.0  
**Last Updated**: October 26, 2025  
**Total Validation Rules**: 200+  
**For Cursor Implementation**: Use these rules when generating validation code
