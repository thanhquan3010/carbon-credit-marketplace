# BUSINESS ANALYSIS DOCUMENT
## Carbon Credit Marketplace for EV Owners

**Document Version:** 1.0  
**Date:** October 25, 2025  
**Prepared By:** Business Analysis Team  
**Project Sponsor:** [Tên tổ chức]

---

## TABLE OF CONTENTS

1. Executive Summary
2. Business Context & Problem Statement
3. Stakeholder Analysis
4. Business Requirements
5. Functional Requirements (Chi tiết theo Role)
6. User Stories & Use Cases
7. Data Requirements & Information Architecture
8. System Integration Requirements
9. Non-Functional Requirements
10. Risk Analysis & Mitigation
11. Implementation Roadmap
12. Success Metrics & KPIs
13. Assumptions & Constraints

---

## 1. EXECUTIVE SUMMARY

### 1.1 Project Overview
Nền tảng Carbon Credit Marketplace for EV Owners là một giải pháp số hóa toàn diện nhằm tạo ra thị trường giao dịch tín chỉ carbon cho chủ sở hữu xe điện tại Việt Nam. Nền tảng kết nối 4 nhóm stakeholder chính: Chủ xe điện (EV Owners), Người mua tín chỉ (Carbon Credit Buyers), Tổ chức kiểm toán carbon (Carbon Verification & Audit), và Quản trị viên hệ thống (Admin).

### 1.2 Business Objectives
- **Môi trường:** Khuyến khích sử dụng xe điện, giảm 50,000 tấn CO₂/năm trong 3 năm đầu
- **Kinh tế:** Tạo nguồn thu nhập thụ động cho chủ xe điện, dự kiến 5-7 triệu VND/xe/năm
- **Thị trường:** Xây dựng marketplace với 10,000+ EV owners và 500+ corporate buyers trong năm đầu
- **Xã hội:** Nâng cao nhận thức về ESG và phát triển bền vững

### 1.3 Success Criteria
- Platform uptime: 99.5%
- Transaction success rate: >98%
- User acquisition: 5,000 EV owners trong 6 tháng đầu
- GMV (Gross Merchandise Value): 50 tỷ VND trong năm đầu
- Verification turnaround time: <48 giờ

---

## 2. BUSINESS CONTEXT & PROBLEM STATEMENT

### 2.1 Current Market Landscape
- **Xu hướng toàn cầu:** Thị trường carbon credit đạt $2 tỷ USD năm 2023, tăng trưởng 20%/năm
- **Việt Nam:** 
  - Số lượng xe điện tăng 150% năm 2024
  - Cam kết Net Zero 2050 tại COP26
  - Thiếu cơ chế monetize giá trị môi trường cho EV owners
  - Doanh nghiệp khó tiếp cận carbon credit chất lượng cao

### 2.2 Problem Statement

**For** chủ sở hữu xe điện (EV Owners)  
**Who** đang giảm phát thải CO₂ thông qua việc sử dụng xe điện  
**The problem is** không có cách thức để monetize đóng góp môi trường của họ  
**Which impacts** việc thiếu động lực kinh tế để chuyển đổi sang xe điện  
**A successful solution would be** một nền tảng cho phép họ chuyển đổi CO₂ giảm phát thải thành tín chỉ carbon có thể bán được

**For** doanh nghiệp cần mua carbon credit (Corporate Buyers)  
**Who** cần đáp ứng mục tiêu ESG và báo cáo phát thải  
**The problem is** khó tiếp cận nguồn carbon credit minh bạch, có thể kiểm chứng  
**Which impacts** uy tín ESG và compliance với các tiêu chuẩn quốc tế  
**A successful solution would be** marketplace với carbon credit đã được verify, có certificate chuẩn quốc tế

### 2.3 Business Opportunity
- **TAM (Total Addressable Market):** 200,000 xe điện dự kiến tại VN năm 2025
- **SAM (Serviceable Available Market):** 50,000 xe trong các thành phố lớn
- **SOM (Serviceable Obtainable Market):** 10,000 xe (20% SAM) trong 18 tháng đầu
- **Revenue Model:**
  - Transaction fee: 5-8% mỗi giao dịch
  - Verification fee: 500,000-1,000,000 VND/request
  - Premium features: Subscription 99,000 VND/tháng

---

## 3. STAKEHOLDER ANALYSIS

### 3.1 Primary Stakeholders

#### 3.1.1 EV Owners (Chủ sở hữu xe điện)
**Profile:**
- **Demographics:** 25-45 tuổi, thu nhập 20-50 triệu/tháng
- **Geography:** TP.HCM, Hà Nội, Đà Nẵng, Cần Thơ
- **Motivations:** 
  - Thu nhập thụ động từ việc lái xe
  - Đóng góp cho môi trường
  - Early adopter mindset
- **Pain Points:**
  - Không biết cách monetize green behavior
  - Thiếu thông tin về carbon credit
  - Lo ngại về tính minh bạch

**Key Needs:**
1. Quy trình đơn giản để sync dữ liệu xe
2. Tính toán tự động và minh bạch
3. Thanh toán nhanh chóng, an toàn
4. Tư vấn giá bán tối ưu

#### 3.1.2 Carbon Credit Buyers (Người mua tín chỉ)
**Profile:**
- **Organization Types:**
  - Doanh nghiệp FDI có yêu cầu ESG
  - Công ty niêm yết (VN100, VNX Allshare)
  - SMEs có cam kết phát triển bền vững
- **Decision Makers:** CSO, Sustainability Manager, CFO
- **Budget:** 100 triệu - 5 tỷ VND/năm cho carbon offset

**Key Needs:**
1. Carbon credit có certificate hợp lệ
2. Pricing minh bạch, competitive
3. Due diligence process rõ ràng
4. Báo cáo chi tiết cho compliance

#### 3.1.3 Carbon Verification & Audit Organizations (CVA)
**Profile:**
- Tổ chức kiểm toán carbon quốc tế (SGS, TÜV, Bureau Veritas)
- Tổ chức trong nước được cấp phép
- Technical experts về carbon accounting

**Key Needs:**
1. Workflow management hiệu quả
2. Truy xuất nguồn gốc dữ liệu đầy đủ
3. Template và công cụ tính toán chuẩn
4. Integration với standard quốc tế (Gold Standard, Verra)

#### 3.1.4 System Administrators
**Profile:**
- Platform operations team
- Customer support team
- Finance & compliance team

**Key Needs:**
1. Comprehensive dashboard và reporting
2. User management tools
3. Fraud detection và risk management
4. Automated workflows

### 3.2 Secondary Stakeholders
- **Regulators:** Bộ TN&MT, Bộ GTVT
- **Financial Partners:** Payment gateway, banks, e-wallets
- **Vehicle Manufacturers:** Tesla, VinFast, BYD (data integration)
- **Insurance Companies:** Potential partnerships
- **Environmental NGOs:** Endorsement và promotion

### 3.3 Stakeholder Influence-Interest Matrix

| Stakeholder | Interest | Influence | Engagement Strategy |
|-------------|----------|-----------|---------------------|
| EV Owners | High | High | Collaborate, co-design |
| Corporate Buyers | High | High | Collaborate, regular feedback |
| CVA Organizations | Medium | High | Consult, partnership agreement |
| Admins | High | Medium | Keep informed, training |
| Regulators | Medium | High | Consult early, ensure compliance |
| Payment Partners | Low | Medium | Keep informed |
| Vehicle OEMs | Medium | Medium | Consult for API integration |

---

## 4. BUSINESS REQUIREMENTS

### BR-001: Carbon Credit Calculation & Issuance
**Priority:** MUST HAVE  
**Description:** Hệ thống phải tự động tính toán CO₂ giảm phát thải dựa trên dữ liệu hành trình xe điện và quy đổi sang tín chỉ carbon theo tiêu chuẩn quốc tế.

**Acceptance Criteria:**
- Sử dụng methodology chuẩn (ví dụ: CDM ACM0018)
- Baseline emission factor: 0.15 kg CO₂/km (xe xăng trung bình)
- EV emission factor: 0 kg CO₂/km (direct emission)
- Accuracy: ±2% so với manual calculation
- Calculation time: <5 giây cho 10,000 km data

### BR-002: Marketplace Transaction
**Priority:** MUST HAVE  
**Description:** Nền tảng phải hỗ trợ giao dịch tín chỉ carbon an toàn, minh bạch giữa sellers và buyers.

**Acceptance Criteria:**
- Hỗ trợ 2 loại giao dịch: Fixed price và Auction
- Escrow mechanism cho payment security
- Atomic transaction (all-or-nothing)
- Transaction fee: 5-8% (configurable)
- Settlement time: T+2 working days

### BR-003: Verification & Compliance
**Priority:** MUST HAVE  
**Description:** Mọi tín chỉ carbon phải được tổ chức CVA kiểm toán và xác nhận trước khi giao dịch.

**Acceptance Criteria:**
- Verification workflow với approval gates
- Document storage và version control
- Certificate generation theo template chuẩn
- Audit trail đầy đủ
- SLA: 48 giờ cho standard verification

### BR-004: Payment Integration
**Priority:** MUST HAVE  
**Description:** Tích hợp đa dạng phương thức thanh toán phù hợp thị trường Việt Nam.

**Acceptance Criteria:**
- E-wallets: MoMo, ZaloPay, VNPay
- Bank transfer: Napas, local banks
- International: Visa, Mastercard (cho foreign buyers)
- Currency: VND (primary), USD (optional)
- Refund mechanism: <7 ngày

### BR-005: Data Security & Privacy
**Priority:** MUST HAVE  
**Description:** Bảo vệ dữ liệu cá nhân và thông tin giao dịch theo PDPA và GDPR.

**Acceptance Criteria:**
- Encryption: AES-256 at rest, TLS 1.3 in transit
- PII data anonymization
- GDPR right-to-be-forgotten support
- Access control: RBAC với least privilege
- Penetration test: Quarterly

### BR-006: AI-Powered Price Recommendation
**Priority:** SHOULD HAVE  
**Description:** Cung cấp gợi ý giá bán tối ưu dựa trên ML model phân tích thị trường.

**Acceptance Criteria:**
- Input features: historical prices, demand, supply, region, time
- Model accuracy: MAPE <15%
- Confidence interval: 90%
- Update frequency: Daily
- Explainability: Show reasoning behind recommendation

### BR-007: Mobile-First Experience
**Priority:** SHOULD HAVE  
**Description:** Giao diện responsive và progressive web app cho mobile users.

**Acceptance Criteria:**
- Mobile-responsive design (viewport <768px)
- PWA capabilities: offline mode, push notifications
- Page load time: <3 seconds on 4G
- Touch-optimized UI
- Native app features via PWA

### BR-008: Multi-Language Support
**Priority:** COULD HAVE  
**Description:** Hỗ trợ đa ngôn ngữ để mở rộng thị trường quốc tế.

**Acceptance Criteria:**
- Languages: Vietnamese (primary), English, Korean
- i18n framework implementation
- Currency conversion real-time
- Localized date/time formats
- RTL support (future)

---

## 5. FUNCTIONAL REQUIREMENTS (Chi tiết theo Role)

## 5.1 EV OWNER FUNCTIONS

### FR-EVO-001: User Registration & Onboarding
**Priority:** MUST HAVE

**User Story:**  
*As an EV owner, I want to register my account and connect my vehicle, so that I can start earning carbon credits.*

**Detailed Requirements:**
1. **Registration Flow**
   - Email/phone registration với OTP verification
   - Social login: Google, Facebook, Apple
   - KYC Level 1: Name, ID number, photo
   - KYC Level 2 (for withdrawal >10M VND): ID verification, address proof

2. **Vehicle Registration**
   - Input: Vehicle make, model, year, VIN
   - Upload registration documents
   - Verification against GTVT database (API integration)
   - Support multiple vehicles per account

3. **Data Source Connection**
   - **Option A - Direct OEM API:**
     - OAuth2 flow với VinFast, Tesla API
     - Request permissions: location, mileage, charging history
   - **Option B - OBD-II Device:**
     - Pair Bluetooth OBD-II dongle
     - Real-time data streaming via mobile app
   - **Option C - Manual Upload:**
     - CSV/JSON file format template
     - Batch upload for historical data
     - Validation: date range, distance reasonability

**Acceptance Criteria:**
- Registration completion rate >80%
- Vehicle verification success rate >95%
- API connection success rate >90% (when available)
- Onboarding time: <10 minutes

**Technical Notes:**
- Store vehicle data encrypted
- Rate limiting: 100 API calls/day per user
- Fallback to manual upload if API fails

---

### FR-EVO-002: Trip Data Sync & CO₂ Calculation
**Priority:** MUST HAVE

**User Story:**  
*As an EV owner, I want my driving data automatically synced and CO₂ savings calculated, so I don't need to manually track.*

**Calculation Methodology:**
```
CO₂ Saved (kg) = Distance (km) × (ICE Emission Factor - EV Emission Factor)

Where:
- ICE Emission Factor = 0.15 kg CO₂/km (Vietnam average)
- EV Emission Factor = 0.05 kg CO₂/km (electricity grid emission)
- Net Reduction = 0.10 kg CO₂/km

Carbon Credit (ton) = CO₂ Saved (kg) / 1000
```

**Detailed Requirements:**

1. **Automatic Sync**
   - Frequency: Daily at 2:00 AM
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
     "energy_consumed_kwh": 7.8,
     "charging_emissions_kg": 2.26
   }
   ```

3. **Calculation Engine**
   - Batch processing: Calculate for 1000 trips in <30 seconds
   - Validation rules:
     - Distance: 0.1 km to 500 km per trip
     - Speed: <180 km/h
     - Energy efficiency: 10-25 kWh/100km
   - Outlier detection & flagging for review

4. **Data Quality Checks**
   - GPS accuracy threshold: <50m
   - Trip duration reasonability: >2 minutes
   - Duplicate trip detection
   - Anomaly detection using ML (future phase)

**Acceptance Criteria:**
- Sync success rate: >98%
- Calculation accuracy: ±2% vs. manual
- Processing latency: <1 minute for 100 trips
- Data retention: 5 years minimum

**Edge Cases:**
- Network interruption during sync → Retry with exponential backoff
- Incomplete trip data → Estimate using avg efficiency
- Mixed driving (highway/city) → Use weighted average emission factor

---

### FR-EVO-003: Carbon Wallet Management
**Priority:** MUST HAVE

**User Story:**  
*As an EV owner, I want to view my carbon credit balance and transaction history, so I can manage my earnings.*

**Detailed Requirements:**

1. **Wallet Dashboard**
   - **Total Balance Display:**
     - Available credits (can list for sale)
     - Pending credits (awaiting verification)
     - Listed credits (on marketplace)
     - Sold credits (pending settlement)
   
   - **Visual Breakdown:**
     - Pie chart: Balance by status
     - Line chart: Earnings over time
     - Bar chart: Monthly CO₂ reduction

2. **Transaction History**
   - **Filters:**
     - Date range picker
     - Transaction type: Credit, Debit, Sale, Purchase
     - Status: Completed, Pending, Failed, Refunded
   
   - **Table Columns:**
     - Date & time
     - Type & description
     - Amount (tons CO₂)
     - Value (VND)
     - Status badge
     - Receipt/certificate link

3. **Balance Operations**
   - **Reserve for Sale:**
     - Lock specified amount for listing
     - Prevent double-spending
     - Auto-unlock if listing expires/cancelled
   
   - **Withdrawal:**
     - Minimum: 0.5 ton
     - Requires verified bank account
     - Processing fee: 2% or 50,000 VND (min)
     - Timeline: T+2 days

4. **Wallet Security**
   - 2FA required for withdrawal >5M VND
   - Email notification for all transactions
   - SMS alert for suspicious activities
   - Daily withdrawal limit: 50M VND

**Acceptance Criteria:**
- Balance updates real-time (WebSocket)
- Transaction history loads <2 seconds
- Export to CSV/PDF functional
- 100% transaction traceability

---

### FR-EVO-004: Carbon Credit Listing (Marketplace)
**Priority:** MUST HAVE

**User Story:**  
*As an EV owner, I want to list my carbon credits for sale with flexible pricing, so I can maximize my earnings.*

**Listing Types:**

**A. Fixed Price Listing**
- Seller sets fixed price per ton
- First-come, first-served
- Auto-delisting after 90 days if unsold
- Can edit price anytime (before sale)

**B. Auction Listing**
- Starting price (reserve price optional)
- Auction duration: 1-30 days
- Bid increment: Minimum 50,000 VND
- Auto-extend: +10 minutes if bid in last 5 minutes
- Winner notification via email/SMS

**Detailed Requirements:**

1. **Create Listing Form**
   - **Fields:**
     - Credit amount (0.1 - available balance)
     - Listing type: Fixed / Auction
     - Price per ton (VND)
     - (Auction) Reserve price (optional)
     - (Auction) Duration
     - Region/location tag
     - Description (500 chars max)
   
   - **Validations:**
     - Amount ≤ available balance
     - Price ≥ 1,000,000 VND/ton (min threshold)
     - Duration: 1-30 days for auction

2. **AI Price Recommendation**
   - **Display:**
     - Recommended price with confidence %
     - Price range: Min, Avg, Max (last 30 days)
     - Demand indicator: Low / Medium / High
     - Expected sale time estimation
   
   - **ML Model Inputs:**
     - Historical transaction prices
     - Current supply/demand ratio
     - Seasonal trends
     - Regional factors
     - Credit verification status

3. **Listing Management**
   - **Actions:**
     - Edit (price, description) - only if no bids
     - Pause listing (temporarily)
     - Cancel listing (release credits back to wallet)
     - Promote listing (paid feature: +10% visibility)
   
   - **Status Tracking:**
     - Draft → Active → Sold / Expired / Cancelled
     - View count analytics
     - Favorite/watchlist count

4. **Fees & Commissions**
   - Listing fee: Free (promotional period)
   - Success fee: 5% of transaction value
   - Payment at settlement time
   - Transparent fee breakdown before confirmation

**Acceptance Criteria:**
- Listing creation time: <30 seconds
- AI recommendation load time: <3 seconds
- Listing visibility delay: <5 minutes
- Edit/cancel success rate: >99%

**Business Rules:**
- Maximum 10 active listings per user
- Credits must be verified before listing
- Auto-cancel if user account suspended
- Auction winner has 24h to complete payment

---

### FR-EVO-005: Transaction Management
**Priority:** MUST HAVE

**User Story:**  
*As an EV owner, I want to track all my sales and manage transactions, so I know my payment status.*

**Detailed Requirements:**

1. **Transaction Dashboard**
   - **Active Transactions:**
     - Awaiting buyer payment
     - In escrow (payment received, pending transfer)
     - Pending verification (if applicable)
   
   - **Completed Transactions:**
     - Credits transferred
     - Payment released to seller
     - Certificate issued
   
   - **Failed/Disputed:**
     - Payment failed
     - Dispute raised
     - Cancelled by buyer/seller

2. **Transaction Details View**
   - **Information:**
     - Transaction ID (unique reference)
     - Buyer information (company name, hidden contact)
     - Credit amount & total value
     - Fees breakdown
     - Net earnings
     - Payment method
     - Escrow status
     - Certificate download link
   
   - **Timeline:**
     - Order placed
     - Payment confirmed
     - In verification (if needed)
     - Credits transferred
     - Payment released
     - Transaction completed

3. **Seller Actions**
   - **Cancel Order:**
     - Allowed before buyer pays
     - Requires reason selection
     - Credits auto-return to wallet
   
   - **Request Refund:**
     - If buyer disputes
     - Requires admin approval
     - Full/partial refund options
   
   - **Rate Buyer:**
     - 1-5 star rating
     - Optional comment
     - Visible to other sellers (buyer reputation)

4. **Notifications**
   - Real-time: New order, payment received
   - Email: Payment released (with invoice PDF)
   - SMS: High-value transaction (>10M VND)
   - Push: In-app notifications

**Acceptance Criteria:**
- Transaction status update real-time
- Payment release within 2 business days
- Dispute resolution time <5 days
- Notification delivery rate >98%

---

### FR-EVO-006: Earnings & Withdrawal
**Priority:** MUST HAVE

**User Story:**  
*As an EV owner, I want to withdraw my earnings to my bank account, so I can access my money.*

**Detailed Requirements:**

1. **Earnings Summary**
   - **Dashboard Widgets:**
     - Total earnings (lifetime)
     - This month earnings
     - Pending payments
     - Available for withdrawal
   
   - **Charts:**
     - Monthly earnings trend (12 months)
     - Earnings by credit amount
     - Comparison: Actual vs. Projected

2. **Withdrawal Process**
   - **Step 1: Verify Identity**
     - 2FA authentication
     - SMS OTP confirmation
   
   - **Step 2: Bank Account**
     - Select saved bank account or add new
     - Required fields:
       - Bank name (dropdown)
       - Account number
       - Account holder name (must match KYC)
       - Branch (optional)
     - Verify with 1 VND test transaction
   
   - **Step 3: Amount**
     - Minimum: 500,000 VND
     - Maximum: 50,000,000 VND/day
     - Fee: 2% or 50,000 VND (whichever is lower)
     - Net amount display
   
   - **Step 4: Confirmation**
     - Review all details
     - Agree to terms
     - Submit withdrawal request

3. **Withdrawal Status**
   - **States:**
     - Requested → Under Review → Approved → Processing → Completed / Failed
   
   - **Timeline:**
     - Review: <4 hours (business hours)
     - Processing: 1-2 business days
     - Failed cases: Refund to wallet within 1 hour

4. **Tax & Compliance**
   - Auto-generate invoice (VAT compliant)
   - Annual tax report (export PDF)
   - TIN (Tax ID) registration for earners >100M VND/year
   - Withholding tax calculation (if applicable)

**Acceptance Criteria:**
- Withdrawal success rate: >99%
- Processing time: <2 business days
- Failed withdrawal auto-refund <1 hour
- Tax report generation <5 seconds

**Security Measures:**
- Daily withdrawal limit per user
- Cooling period: 24h after bank account change
- Suspicious activity detection (ML-based)
- Manual review for withdrawals >20M VND

---

### FR-EVO-007: Personal Reporting & Analytics
**Priority:** SHOULD HAVE

**User Story:**  
*As an EV owner, I want detailed reports on my environmental impact and earnings, so I can track my contribution.*

**Report Types:**

**A. Environmental Impact Report**
- Total CO₂ reduced (kg, tons)
- Equivalent to:
  - Trees planted
  - Km traveled by ICE vehicle offset
  - Liters of gasoline saved
- Monthly/yearly trends
- Comparison with avg EV owner
- Share on social media feature

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

**Detailed Requirements:**

1. **Report Generation**
   - **Filters:**
     - Date range: Last 7/30/90 days, YTD, Custom
     - Vehicle selection (if multiple vehicles)
     - Report type selection
   
   - **Export Formats:**
     - PDF (with charts & graphs)
     - Excel (raw data)
     - Image (for social sharing)

2. **Visualizations**
   - Interactive charts (hover for details)
   - Color-coded performance indicators
   - Progress bars for goals
   - Heat maps for geographic distribution

3. **Gamification Elements**
   - Badges: Milestones (1 ton, 10 tons, 100 tons CO₂ saved)
   - Leaderboard: Top earners in city/country
   - Achievements: Streaks, referrals, premium user
   - Challenges: Monthly CO₂ reduction goals

4. **Social Sharing**
   - Generate shareable infographic
   - Pre-filled social media posts
   - QR code for quick sharing
   - Privacy controls (hide earnings)

**Acceptance Criteria:**
- Report generation time: <10 seconds
- Export success rate: >99%
- Chart rendering on mobile: <3 seconds
- Social share click-through rate: >5%

---

## 5.2 CARBON CREDIT BUYER FUNCTIONS

### FR-CCB-001: Buyer Registration & Company Verification
**Priority:** MUST HAVE

**User Story:**  
*As a corporate buyer, I want to register my company and verify credentials, so I can purchase carbon credits.*

**Detailed Requirements:**

1. **Company Registration**
   - **Required Information:**
     - Company legal name
     - Business registration number
     - Tax code (MST)
     - Industry/sector
     - Company size (employees, revenue bracket)
     - Address & contact details
     - Website (optional)
     - CSR/Sustainability report link (optional)
   
   - **Document Upload:**
     - Business license (GPKD)
     - Tax registration certificate
     - Letter of authorization (for rep)
     - Company stamp sample

2. **Verification Process**
   - **Level 1 (Basic):**
     - Auto-check business registration number against govt database API
     - Email domain verification (@company-domain.com)
     - Phone verification via OTP
     - Timeline: <24 hours
   
   - **Level 2 (Enhanced - for large purchases):**
     - Video call with company representative
     - Office address verification
     - Financial stability check (D&B, credit bureau)
     - Timeline: 2-5 business days
   
   - **Benefits by Level:**
     - Level 1: Purchase limit 500M VND/transaction
     - Level 2: Unlimited purchases, invoice payment terms

3. **User Roles within Company**
   - Admin: Full access, user management
   - Buyer: Can purchase, view reports
   - Finance: Payment approval, invoice management
   - Compliance: Download certificates, reports only

**Acceptance Criteria:**
- Registration completion rate: >70%
- Level 1 verification: <24 hours
- Level 2 verification: <5 business days
- Document rejection rate: <10%

---

### FR-CCB-002: Carbon Credit Search & Discovery
**Priority:** MUST HAVE

**User Story:**  
*As a buyer, I want to search and filter available carbon credits, so I can find the best options for my needs.*

**Detailed Requirements:**

1. **Search Interface**
   - **Search Bar:**
     - Keyword search (seller name, location, description)
     - Auto-complete suggestions
     - Voice search (mobile)
   
   - **Quick Filters (Top Bar):**
     - Amount needed (slider: 0.1 - 1000 tons)
     - Max price per ton
     - Region/city
     - Listing type: Fixed / Auction / All
     - Verification status: Verified / Pending / All

2. **Advanced Filters (Sidebar)**
   - **Price Range:**
     - Slider or manual input
     - Sort: Low to High, High to Low
   
   - **Seller Reputation:**
     - Star rating (1-5 stars)
     - Number of successful transactions
     - Response time
   
   - **Credit Characteristics:**
     - CO₂ reduction methodology
     - Vintage (year of reduction)
     - Certification body (Gold Standard, Verra, etc.)
     - Project type (EV transportation)
   
   - **Availability:**
     - Available now
     - Auction ending soon (<24h)
     - Newly listed (<7 days)

3. **Search Results Display**
   - **List View:**
     - Card layout with key info
     - Thumbnail image (optional)
     - Amount, price, region
     - Seller rating
     - Quick actions: View details, Add to cart, Watchlist
   
   - **Map View:**
     - Geographic distribution of listings
     - Cluster markers for high-density areas
     - Click marker → Show listing details popup
   
   - **Table View:**
     - Sortable columns
     - Bulk selection for comparison
     - Export results to CSV

4. **Saved Searches & Alerts**
   - Save search criteria
   - Email notifications when new matching listings appear
   - Price drop alerts
   - Auction ending reminders

**Acceptance Criteria:**
- Search results load time: <2 seconds for 1000 listings
- Filter application: Real-time (no page refresh)
- Mobile-responsive search UI
- Alert delivery rate: >95%

---

### FR-CCB-003: Purchase Process (Fixed Price & Auction)
**Priority:** MUST HAVE

**User Story:**  
*As a buyer, I want to purchase carbon credits easily and securely, so I can offset my emissions.*

**Fixed Price Purchase Flow:**

1. **Step 1: Listing Details**
   - View full listing information
   - See seller profile & rating
   - Review certificate samples
   - Calculate total cost (amount × price + fees)

2. **Step 2: Due Diligence**
   - Download verification documents
   - Review calculation methodology
   - Check certificate validity
   - Contact seller (in-app messaging)

3. **Step 3: Add to Cart**
   - Select quantity (partial purchase allowed if listed >1 ton)
   - Apply coupon/promo code (if any)
   - View cart summary

4. **Step 4: Checkout**
   - Billing information
   - Payment method selection
   - Terms & conditions acceptance
   - Purchase order number generation

5. **Step 5: Payment**
   - Redirect to payment gateway
   - Escrow mechanism activated
   - Payment confirmation

6. **Step 6: Settlement**
   - Credits transferred to buyer wallet
   - Certificate generated
   - Seller paid (minus platform fee)
   - Invoice emailed

**Auction Purchase Flow:**

1. **View Auction Details**
   - Current bid
   - Bid history (anonymized)
   - Time remaining (countdown timer)
   - Reserve price met indicator

2. **Place Bid**
   - Minimum bid amount displayed
   - Bid increment rules explained
   - Confirm bid amount
   - Auto-bid feature (max bid, system bids up to this amount)

3. **Outbid Notification**
   - Real-time notification if outbid
   - Quick re-bid button

4. **Winning Auction**
   - Notification: You won!
   - Payment deadline: 24 hours
   - If no payment → Next highest bidder wins

5. **Post-Auction Checkout**
   - Same as fixed price steps 4-6

**Detailed Requirements:**

1. **Payment Methods**
   - E-wallets: MoMo, ZaloPay, VNPay, ShopeePay
   - Bank transfer: Napas, local banks (with unique reference code)
   - Credit/Debit card: Visa, Mastercard, AMEX (via payment gateway)
   - Invoice payment (for verified companies, NET 30/60 days)

2. **Payment Security**
   - PCI-DSS compliant payment gateway
   - 3D Secure for card payments
   - Escrow mechanism:
     - Funds held until credit transfer confirmed
     - Automatic refund if transaction fails
     - Dispute resolution process

3. **Bulk Purchase**
   - Purchase multiple listings in one transaction
   - Volume discount (configurable by admin)
   - Single invoice for accounting

**Acceptance Criteria:**
- Checkout completion rate: >85%
- Payment success rate: >98%
- Average checkout time: <5 minutes
- Escrow settlement: T+2 days

---

### FR-CCB-004: Certificate Management
**Priority:** MUST HAVE

**User Story:**  
*As a buyer, I want to receive and manage carbon credit certificates, so I can use them for ESG reporting.*

**Certificate Components:**

1. **Standard Information**
   - Unique certificate ID (QR code + alphanumeric)
   - Issue date
   - Validity period (typically perpetual for retired credits)
   - Issuing authority (CVA organization)

2. **Transaction Details**
   - Buyer company name & details
   - Seller information
   - Credit amount (tons CO₂)
   - Transaction value
   - Transaction date

3. **Credit Details**
   - Methodology used (e.g., CDM ACM0018)
   - Verification body & auditor
   - Vintage (year of CO₂ reduction)
   - Project description (EV transportation)
   - Geographic origin
   - Serial numbers of carbon credits

4. **Verification & Authenticity**
   - Digital signature from CVA
   - Blockchain hash (optional, for immutability)
   - QR code for public verification
   - Watermark & security features

**Detailed Requirements:**

1. **Certificate Generation**
   - Auto-generate upon transaction completion
   - PDF format (A4, print-ready)
   - Multi-language: Vietnamese, English
   - Customizable logo area (buyer's company logo)
   - Template compliance: Gold Standard, Verra VCS

2. **Certificate Repository**
   - **My Certificates Dashboard:**
     - List all certificates
     - Filters: Date range, amount, status
     - Search by certificate ID
     - Tag certificates (for internal org)
   
   - **Actions:**
     - Download PDF
     - Share via email
     - Revoke (if error, admin approval required)
     - Re-issue (if lost, with unique ID)

3. **Public Verification**
   - **Verification Portal:**
     - Input certificate ID or scan QR code
     - Display certificate validity status
     - Show basic transaction info (anonymized)
     - Flag revoked/expired certificates
   
   - **Blockchain Integration (Phase 2):**
     - Store certificate hash on blockchain
     - Immutable audit trail
     - Cross-platform verification

4. **Integration with Reporting Tools**
   - **Export Options:**
     - CSV for import to CDP, GRI tools
     - XML for automated reporting systems
     - API endpoint for ERP integration
   
   - **Reporting Templates:**
     - GHG Protocol format
     - ISO 14064 format
     - Custom formats

**Acceptance Criteria:**
- Certificate generation time: <10 seconds
- PDF quality: 300 DPI, <2MB file size
- QR code scan success rate: >99%
- Public verification portal uptime: >99.9%

---

### FR-CCB-005: Purchase History & Portfolio Management
**Priority:** MUST HAVE

**User Story:**  
*As a buyer, I want to track my purchase history and manage my carbon credit portfolio, so I can monitor my offsetting progress.*

**Detailed Requirements:**

1. **Purchase History Dashboard**
   - **Summary Cards:**
     - Total credits purchased (all-time)
     - Total spend
     - Average price per ton
     - Number of transactions
   
   - **Filters & Sort:**
     - Date range
     - Amount range
     - Status: Completed, Pending, Cancelled
     - Sort by: Date, Amount, Price

2. **Transaction Details**
   - **For Each Transaction:**
     - Transaction ID
     - Date & time
     - Seller information
     - Credit amount
     - Price per ton & total
     - Payment method
     - Certificate ID (clickable link)
     - Invoice (download PDF)
   
   - **Actions:**
     - Download invoice
     - Download certificate
     - Contact seller (if issue)
     - Leave review/rating

3. **Portfolio Overview**
   - **Visual Dashboard:**
     - Total CO₂ offset (tons)
     - Equivalent visualizations:
       - Trees planted equivalent
       - Cars off the road for 1 year
       - Flights offset
     - Breakdown by:
       - Vintage year
       - Region
       - Verification body
       - Seller
   
   - **Charts:**
     - Monthly offsetting trend
     - Cumulative offset over time
     - Cost per ton over time

4. **Reporting & Export**
   - **Standard Reports:**
     - Annual offset report (PDF)
     - Quarter ESG report (customizable)
     - Tax documentation (VAT invoices compilation)
   
   - **Custom Reports:**
     - Report builder tool
     - Select date range, metrics, visualizations
     - Save report templates
     - Schedule automated reports (monthly/quarterly)
   
   - **Export Formats:**
     - PDF (formatted, with charts)
     - Excel (raw data)
     - CSV (for data analysis)
     - JSON (API export)

5. **Forecast & Goal Setting**
   - **Set Offsetting Goals:**
     - Annual target (tons CO₂)
     - Budget allocation
     - Progress tracking
   
   - **Forecasting:**
     - Based on historical purchase rate
     - Projected offset by year-end
     - Budget utilization forecast
     - Alert when falling behind goal

**Acceptance Criteria:**
- Dashboard load time: <3 seconds
- Report generation: <15 seconds
- Export file size: <5MB for 1000 transactions
- Data retention: 10 years minimum

---

## 5.3 CARBON VERIFICATION & AUDIT (CVA) FUNCTIONS

### FR-CVA-001: Verification Request Management
**Priority:** MUST HAVE

**User Story:**  
*As a CVA auditor, I want to manage verification requests efficiently, so I can process applications quickly and accurately.*

**Detailed Requirements:**

1. **Verification Queue**
   - **Dashboard View:**
     - Pending requests (assigned to me)
     - Unassigned requests (available pool)
     - In-progress (current work)
     - Completed (last 30 days)
   
   - **Filters:**
     - Status: New, In Review, Awaiting Info, Approved, Rejected
     - Priority: High, Medium, Low
     - Request date range
     - Credit amount range
     - Assigned auditor

2. **Request Details**
   - **EV Owner Information:**
     - Name, contact, KYC status
     - Vehicle details (make, model, VIN)
     - Historical verification record
     - Reputation score
   
   - **Trip Data:**
     - Date range of trips
     - Total distance (km)
     - Number of trips
     - Data source (OEM API, OBD, manual)
     - Raw data file (download CSV/JSON)
   
   - **Calculated Credits:**
     - CO₂ saved (kg)
     - Carbon credits (tons)
     - Methodology used
     - Calculation breakdown (step-by-step)

3. **Verification Process**
   - **Step 1: Initial Review**
     - Check data completeness
     - Verify data source authenticity
     - Flag anomalies (outliers, duplicates)
   
   - **Step 2: Data Validation**
     - Cross-check with known patterns
     - Validate GPS coordinates (not impossible locations)
     - Check distance vs. energy consumed ratio
     - Spot-check sample trips (10% of data)
   
   - **Step 3: Calculation Audit**
     - Recalculate CO₂ using standard formula
     - Compare with submitted calculation
     - Acceptable variance: ±2%
   
   - **Step 4: Decision**
     - Approve: Issue credits
     - Reject: Provide reason
     - Request more info: Send query to EV owner

4. **Auditor Tools**
   - **Data Analyzer:**
     - Statistical summary (mean, median, std dev)
     - Outlier detection algorithm
     - Map view of trips (visualize routes)
     - Energy efficiency calculator
   
   - **Reference Library:**
     - Emission factor database (by country, fuel type)
     - Methodology guidelines (CDM, Gold Standard)
     - Precedent cases (similar verification examples)
     - FAQ & troubleshooting guide
   
   - **Communication:**
     - In-app messaging with EV owner
     - Request additional documents
     - Schedule video verification call (if needed)

**Acceptance Criteria:**
- Verification SLA: 80% completed within 48 hours
- Approval rate: 85-95% (not too strict, not too lenient)
- Rejection with reason: 100%
- Average verification time: <3 hours per request

---

### FR-CVA-002: Credit Issuance & Registry
**Priority:** MUST HAVE

**User Story:**  
*As a CVA auditor, I want to issue verified carbon credits to EV owners' wallets, so they can start selling.*

**Detailed Requirements:**

1. **Credit Issuance Workflow**
   - **Approval Triggers Issuance:**
     - Automatic after auditor approves
     - Credits minted in system registry
     - Unique serial number assigned to each credit unit
   
   - **Serial Number Format:**
     ```
     VN-EV-[YEAR]-[CVA_ID]-[SEQUENCE_NUMBER]
     Example: VN-EV-2025-TUV-000012345
     ```
   
   - **Credit Metadata:**
     - Serial number
     - Vintage (year of CO₂ reduction)
     - Project ID (linked to EV owner)
     - Methodology
     - Verification date
     - CVA organization name
     - Auditor ID

2. **Registry Management**
   - **Master Registry:**
     - All issued credits (lifetime)
     - Status: Issued → Listed → Sold → Retired
     - Ownership trail (blockchain-like)
     - Transfer history
   
   - **Search & Filter:**
     - By serial number
     - By EV owner
     - By vintage year
     - By CVA organization
     - By status

3. **Wallet Integration**
   - **Credit Transfer to EV Owner:**
     - Atomic transaction (all-or-nothing)
     - Update owner's wallet balance
     - Send notification (email + in-app)
     - Generate issuance certificate
   
   - **Issuance Certificate:**
     - Unique certificate per issuance batch
     - Lists all serial numbers issued
     - Signed by CVA auditor
     - PDF download

4. **Quality Assurance**
   - **Random Spot Checks:**
     - Supervisor reviews 5% of approved requests
     - Flag discrepancies
     - Feedback to auditor for improvement
   
   - **Auditor Performance Metrics:**
     - Number of verifications completed
     - Average processing time
     - Approval/rejection rate
     - Quality score (based on spot checks)
     - Owner satisfaction rating

**Acceptance Criteria:**
- Credit issuance time: <5 minutes after approval
- Serial number uniqueness: 100%
- Wallet update success rate: >99.9%
- Registry uptime: >99.9%

---

### FR-CVA-003: Audit Trail & Reporting
**Priority:** MUST HAVE

**User Story:**  
*As a CVA organization, I want comprehensive audit trails and reports, so I can maintain compliance and transparency.*

**Detailed Requirements:**

1. **Audit Trail Logging**
   - **Logged Events:**
     - Verification request created
     - Auditor assigned
     - Data uploaded/downloaded
     - Queries sent to EV owner
     - Approval/rejection decision
     - Credits issued
     - Certificate generated
   
   - **Log Entry Format:**
     ```json
     {
       "timestamp": "ISO8601",
       "event_type": "approval",
       "auditor_id": "AUD-12345",
       "request_id": "REQ-67890",
       "details": {...},
       "ip_address": "x.x.x.x",
       "user_agent": "..."
     }
     ```
   
   - **Immutability:**
     - Logs append-only (no deletion/editing)
     - Cryptographic hash chain
     - Tamper-evident

2. **Verification Reports**
   - **Individual Verification Report:**
     - Request summary
     - Data quality assessment
     - Calculation verification
     - Decision rationale
     - Auditor notes
     - Signature & timestamp
   
   - **Batch Report:**
     - All verifications in a date range
     - Aggregate statistics
     - Approval/rejection breakdown
     - Average processing time
     - Quality metrics

3. **CVA Organization Dashboard**
   - **KPIs:**
     - Total verifications (YTD, all-time)
     - Total credits issued (tons CO₂)
     - Average processing time
     - Auditor utilization rate
     - Customer satisfaction score
   
   - **Charts:**
     - Verifications over time (trend)
     - Credits issued by vintage year
     - Auditor performance comparison
     - Request volume by region

4. **Regulatory Reporting**
   - **Compliance Reports:**
     - ISO 14064-2 format
     - Gold Standard annual report
     - Verra VCS report
   
   - **Export for Regulators:**
     - Bộ TN&MT format (if required)
     - UNFCCC reporting
   
   - **Accreditation Maintenance:**
     - Track CVA accreditation status
     - Renewal reminders
     - Upload accreditation documents

**Acceptance Criteria:**
- Log completeness: 100% of actions logged
- Report generation time: <30 seconds
- Log retention: 10 years minimum
- Audit trail queryable within <5 seconds

---

## 5.4 ADMIN FUNCTIONS

### FR-ADM-001: User Management
**Priority:** MUST HAVE

**User Story:**  
*As an admin, I want to manage all users on the platform, so I can ensure smooth operations and handle issues.*

**Detailed Requirements:**

1. **User Directory**
   - **List View:**
     - All users across all roles
     - Key info: Name, email, role, status, join date
     - Search by name, email, phone
     - Filter by role, status, KYC level
   
   - **Bulk Actions:**
     - Export to CSV
     - Send bulk notifications
     - Bulk status update (suspend, activate)

2. **User Profile Management**
   - **View User Details:**
     - Personal information
     - KYC documents & status
     - Verification level
     - Account status
     - Linked vehicles (for EV owners)
     - Company details (for buyers)
     - Transaction history
     - Support tickets
   
   - **Admin Actions:**
     - Edit user information
     - Reset password
     - Force 2FA setup
     - Suspend account (with reason)
     - Unsuspend account
     - Delete account (GDPR right-to-be-forgotten)
     - Upgrade/downgrade verification level

3. **KYC Review & Approval**
   - **Pending KYC Queue:**
     - List of users awaiting verification
     - Priority flagging (high-value users)
   
   - **Document Verification:**
     - View uploaded documents (ID, passport, business license)
     - Zoom & rotate images
     - Check document authenticity (OCR, watermark detection)
     - Approve or reject with reason
   
   - **Manual Verification:**
     - Schedule video call
     - Log verification notes
     - Upload additional documents (if provided)

4. **User Segmentation & Analytics**
   - **User Demographics:**
     - Geographic distribution
     - User role breakdown
     - Age distribution
     - Vehicle type distribution (for EV owners)
   
   - **Engagement Metrics:**
     - Active users (DAU, MAU)
     - User retention rate
     - Churn rate by cohort
     - Feature adoption rate
     - Support ticket frequency
   
   - **Cohort Analysis:**
     - New user onboarding completion
     - Time to first transaction
     - Lifetime value (LTV) by cohort

5. **Role & Permission Management**
   - **Role Definition:**
     - EV Owner, Buyer, CVA Auditor, Admin, Support Agent
     - Custom roles (future)
   
   - **Permission Matrix:**
     - Granular permissions per feature
     - Read, Write, Delete, Execute
     - IP whitelist for sensitive operations
   
   - **Admin Hierarchy:**
     - Super Admin: Full access
     - Operations Admin: User & transaction mgmt
     - Finance Admin: Payment & settlement
     - Support Admin: Customer service only

**Acceptance Criteria:**
- User search: <2 seconds for 100K users
- KYC review time: <1 hour during business hours
- Account suspension takes effect immediately
- GDPR deletion: Complete within 30 days

---

### FR-ADM-002: Transaction Monitoring & Management
**Priority:** MUST HAVE

**User Story:**  
*As an admin, I want to monitor all transactions in real-time, so I can detect and resolve issues quickly.*

**Detailed Requirements:**

1. **Transaction Dashboard**
   - **Real-Time Metrics:**
     - Active transactions (in-progress)
     - Completed transactions (today, this week)
     - Total GMV (Gross Merchandise Value)
     - Average transaction size
     - Transaction success rate
     - Failed transactions count
   
   - **Live Feed:**
     - Recent transactions (last 100)
     - Auto-refresh every 30 seconds
     - Color-coded by status (green=success, yellow=pending, red=failed)
     - Quick action buttons (view details, intervene)

2. **Transaction Search & Filter**
   - **Search Options:**
     - Transaction ID
     - User name/email
     - Date range
     - Amount range
     - Status
     - Payment method
   
   - **Advanced Filters:**
     - Flagged transactions (suspicious)
     - High-value (>10M VND)
     - Failed payments
     - Disputed transactions
     - Refund requests

3. **Transaction Details View**
   - **Comprehensive Information:**
     - Buyer & seller details
     - Credit amount & pricing
     - Payment breakdown (subtotal, fees, total)
     - Payment method & status
     - Escrow status
     - Certificate status
     - Timeline with timestamps
   
   - **Admin Actions:**
     - **Cancel Transaction:**
       - Requires reason and approval (2-level)
       - Trigger refund process
       - Notify both parties
     
     - **Force Complete:**
       - Override pending status
       - Manual settlement
       - Requires documentation
     
     - **Hold/Investigate:**
       - Freeze transaction temporarily
       - Request additional verification
       - Anti-fraud check
     
     - **Adjust Amount:**
       - In case of calculation error
       - Requires approval
       - Issue credit note/debit note

4. **Dispute Management**
   - **Dispute Queue:**
     - New disputes (unassigned)
     - In-progress (assigned to admin)
     - Resolved
   
   - **Dispute Details:**
     - Transaction information
     - Dispute reason (category)
     - Evidence from buyer
     - Evidence from seller
     - Communication log
   
   - **Resolution Workflow:**
     - Assign to admin
     - Review evidence
     - Request additional info
     - Make decision (favor buyer, favor seller, split)
     - Execute resolution (refund, credit adjustment, etc.)
     - Close dispute with resolution notes

5. **Fraud Detection**
   - **Automated Flags:**
     - Unusual transaction patterns
     - Velocity checks (too many transactions in short time)
     - Mismatched information
     - Suspicious login locations
     - Chargeback history
   
   - **Manual Review:**
     - Flagged transactions queue
     - Investigation tools (IP lookup, device fingerprint)
     - User behavior analysis
     - Decision: Approve, Decline, Request verification

**Acceptance Criteria:**
- Transaction search: <3 seconds
- Real-time dashboard lag: <30 seconds
- Dispute resolution SLA: <5 business days
- Fraud detection false positive rate: <5%

---

### FR-ADM-003: Financial Management & Settlement
**Priority:** MUST HAVE

**User Story:**  
*As an admin, I want to manage platform finances and settlements, so I can ensure accurate accounting and timely payouts.*

**Detailed Requirements:**

1. **Payment Gateway Management**
   - **Connected Gateways:**
     - List of active payment providers
     - Status (active, inactive, maintenance)
     - Transaction limits
     - Fee structure
     - Success rate by gateway
   
   - **Configuration:**
     - Enable/disable gateway
     - Update API credentials
     - Set routing rules (amount-based, user-based)
     - Test mode toggle

2. **Settlement Management**
   - **Pending Settlements:**
     - Sellers awaiting payment
     - Amount due
     - Payment method (bank transfer, e-wallet)
     - Scheduled date
     - Status (queued, processing, completed, failed)
   
   - **Batch Processing:**
     - Process all due settlements
     - Schedule automatic daily settlement (at 9 AM)
     - Retry failed settlements
     - Generate settlement report
   
   - **Settlement Actions:**
     - Approve batch
     - Hold individual settlement (investigation)
     - Adjust amount (fee correction)
     - Mark as paid manually (for offline transfers)

3. **Platform Wallet**
   - **Balance Overview:**
     - Available balance
     - Reserved in escrow
     - Pending clearance
     - Total revenue (fees collected)
   
   - **Ledger:**
     - All money movements
     - Credits (deposits, fees earned)
     - Debits (settlements, refunds, chargebacks)
     - Running balance
     - Reconciliation status

4. **Fee Management**
   - **Fee Configuration:**
     - Transaction fee (% or fixed)
     - Listing fee
     - Verification fee
     - Withdrawal fee
     - Premium subscription fee
     - Currency conversion fee (for international)
   
   - **Dynamic Pricing:**
     - Volume-based discounts
     - Promotional periods (free listing)
     - Referral bonuses
     - Loyalty program discounts

5. **Financial Reporting**
   - **Revenue Reports:**
     - Daily/weekly/monthly revenue
     - Revenue by fee type
     - Revenue by user segment
     - Projected revenue (based on pipeline)
   
   - **Accounting Reports:**
     - P&L statement
     - Balance sheet
     - Cash flow statement
     - Tax reports (VAT, withholding)
   
   - **Export for Accounting Software:**
     - QuickBooks format
     - Xero format
     - MISA format (Vietnam)
     - CSV for custom systems

6. **Reconciliation**
   - **Daily Reconciliation:**
     - Compare platform ledger vs. payment gateway reports
     - Flag discrepancies
     - Investigate and resolve
   
   - **Month-End Close:**
     - Ensure all transactions settled
     - Generate month-end reports
     - Archive data
     - Prepare for audit

**Acceptance Criteria:**
- Settlement processing time: <4 hours for batch
- Settlement success rate: >98%
- Reconciliation discrepancy: <0.1%
- Financial report generation: <1 minute

---

### FR-ADM-004: Platform Configuration & Settings
**Priority:** MUST HAVE

**User Story:**  
*As an admin, I want to configure platform settings, so I can adapt to changing business needs.*

**Detailed Requirements:**

1. **General Settings**
   - **Platform Information:**
     - Platform name & logo
     - Contact email & phone
     - Support hours
     - Terms of service URL
     - Privacy policy URL
     - About us content
   
   - **Regional Settings:**
     - Default language
     - Default currency
     - Default timezone
     - Date/time format
     - Number format (decimal separator)

2. **Business Rules Configuration**
   - **Carbon Credit Rules:**
     - Emission factor (kg CO₂/km) - configurable by vehicle type
     - Minimum credit amount for issuance (e.g., 0.1 ton)
     - Vintage period (e.g., credits valid for 1 year from reduction date)
     - Maximum listing duration (e.g., 90 days)
   
   - **Transaction Rules:**
     - Minimum transaction amount
     - Maximum transaction amount per user/day
     - Transaction timeout (e.g., buyer has 24h to pay auction win)
     - Escrow hold period (e.g., T+2 days)
   
   - **User Limits:**
     - Maximum active listings per user
     - Maximum withdrawal amount per day
     - KYC level limits (purchase/sell amounts)
     - Rate limits (API calls, searches)

3. **Notification Settings**
   - **Email Templates:**
     - Manage email templates (subject, body, variables)
     - Supported languages
     - A/B testing variants
     - Preview before send
   
   - **SMS Templates:**
     - Similar to email
     - Character count optimization
   
   - **Push Notification Templates:**
     - Title, body, action
     - Deep link configuration
   
   - **Notification Triggers:**
     - Enable/disable specific triggers
     - Set delay (send after X minutes)
     - Set frequency limit (max 1 per day)

4. **Integration Management**
   - **API Keys:**
     - Generate/revoke API keys for partners
     - Set permissions per API key
     - Monitor API usage
   
   - **Webhooks:**
     - Configure webhook URLs for events
     - Test webhooks
     - View webhook delivery logs
     - Retry failed webhooks
   
   - **Third-Party Services:**
     - Payment gateways (credentials, config)
     - SMS provider (API key, sender ID)
     - Email service (SMTP, API)
     - Cloud storage (S3, GCS)
     - Analytics (Google Analytics, Mixpanel)

5. **Content Management**
   - **Static Pages:**
     - Edit FAQ
     - Edit Help Center articles
     - Edit About Us
     - Edit Terms & Conditions
     - Edit Privacy Policy
   
   - **Rich Text Editor:**
     - WYSIWYG editor
     - Image upload
     - Embed videos
     - HTML source view
   
   - **Multi-language Content:**
     - Manage translations
     - Flag missing translations
     - Translation workflow (draft, review, publish)

6. **Feature Flags**
   - **Enable/Disable Features:**
     - Auction listings
     - AI price recommendation
     - Social sharing
     - Referral program
     - Premium subscriptions
   
   - **Gradual Rollout:**
     - Enable for % of users (A/B testing)
     - Enable for specific user segments
     - Schedule feature launch

**Acceptance Criteria:**
- Settings update takes effect within 5 minutes
- Configuration changes logged in audit trail
- Critical settings require 2-level approval
- Configuration backup daily

---

### FR-ADM-005: Analytics & Reporting
**Priority:** MUST HAVE

**User Story:**  
*As an admin, I want comprehensive analytics and reports, so I can make data-driven decisions.*

**Detailed Requirements:**

1. **Executive Dashboard**
   - **Key Metrics (Big Numbers):**
     - Total users (by role)
     - Total transactions (count & GMV)
     - Total carbon credits traded (tons CO₂)
     - Total CO₂ offset (environmental impact)
     - Platform revenue
     - Active users (DAU, WAU, MAU)
   
   - **Trend Charts:**
     - User growth over time
     - Transaction volume over time
     - GMV over time
     - Average transaction size trend
   
   - **Comparison Periods:**
     - Today vs. Yesterday
     - This week vs. Last week
     - This month vs. Last month
     - This year vs. Last year

2. **User Analytics**
   - **Acquisition:**
     - New user signups over time
     - Acquisition channels (organic, referral, paid ads)
     - Cost per acquisition (CPA)
     - Signup funnel conversion
   
   - **Engagement:**
     - User activity heatmap (by day/hour)
     - Feature usage statistics
     - Session duration
     - Pages per session
     - Bounce rate
   
   - **Retention:**
     - User retention cohorts
     - Churn rate
     - Time to churn
     - Reactivation rate
   
   - **Segmentation:**
     - Users by city
     - Users by vehicle type (EV owners)
     - Users by company size (buyers)
     - High-value users (top 10% by transaction volume)

3. **Transaction Analytics**
   - **Volume Metrics:**
     - Transactions per day/week/month
     - GMV breakdown
     - Average order value (AOV)
     - Transaction success rate
   
   - **Marketplace Health:**
     - Supply (active listings)
     - Demand (searches, views)
     - Liquidity (avg time to sell)
     - Price trends (avg price per ton over time)
   
   - **Payment Analytics:**
     - Payment method distribution
     - Payment success rate by method
     - Failed payment reasons
     - Refund rate

4. **Financial Analytics**
   - **Revenue Breakdown:**
     - By fee type (transaction, verification, subscription)
     - By user segment (EV owner, buyer)
     - By region
   
   - **Cost Analysis:**
     - Payment gateway fees
     - SMS/email costs
     - Cloud hosting costs
     - Customer acquisition cost (CAC)
   
   - **Profitability:**
     - Gross margin
     - Net margin
     - LTV:CAC ratio
     - Break-even analysis

5. **Environmental Impact Dashboard**
   - **CO₂ Impact:**
     - Total CO₂ offset (tons)
     - CO₂ offset rate (tons/day)
     - Equivalent metrics:
       - Trees planted
       - Cars off road
       - Homes powered
       - Flights offset
   
   - **Geographic Impact:**
     - CO₂ reduction map (by city/region)
     - Contribution by EV type
   
   - **Trend Analysis:**
     - Monthly CO₂ offset trend
     - Seasonality patterns
     - Forecast future impact

6. **Custom Reports**
   - **Report Builder:**
     - Drag-and-drop interface
     - Select metrics & dimensions
     - Add filters
     - Choose visualization type
     - Save report template
   
   - **Scheduled Reports:**
     - Email delivery (daily, weekly, monthly)
     - Recipient list
     - PDF or Excel format
   
   - **Ad-hoc Queries:**
     - SQL query interface (for advanced users)
     - Query library (saved queries)
     - Export results

7. **Alerting System**
   - **Threshold Alerts:**
     - Metric falls below/above threshold
     - Example: Transaction success rate <95%
   
   - **Anomaly Detection:**
     - ML-based anomaly detection
     - Alert on unusual patterns
   
   - **Alert Channels:**
     - Email
     - SMS
     - Slack/Teams integration
     - PagerDuty (for critical alerts)

**Acceptance Criteria:**
- Dashboard load time: <5 seconds
- Real-time metrics lag: <5 minutes
- Custom report generation: <30 seconds
- Alert delivery: <1 minute from trigger

---

### FR-ADM-006: Support & Customer Service Tools
**Priority:** SHOULD HAVE

**User Story:**  
*As an admin/support agent, I want tools to provide excellent customer service, so I can resolve user issues quickly.*

**Detailed Requirements:**

1. **Support Ticket System**
   - **Ticket Creation:**
     - Users submit tickets via web form
     - Auto-create from email (support@platform.com)
     - Admin can create on behalf of user
   
   - **Ticket Information:**
     - Subject & description
     - Category (technical, billing, general, etc.)
     - Priority (low, medium, high, urgent)
     - Status (new, open, waiting on user, resolved, closed)
     - Assigned agent
     - Attachments (screenshots, documents)
   
   - **Ticket Workflow:**
     - New → Assigned → In Progress → Resolved → Closed
     - Auto-assign based on category and agent availability
     - SLA tracking (response time, resolution time)
     - Escalation if SLA breached

2. **Agent Dashboard**
   - **My Tickets:**
     - Assigned to me
     - Awaiting my response
     - Resolved by me (today, this week)
   
   - **Team Queue:**
     - Unassigned tickets
     - Tickets by priority
     - Overdue tickets (SLA breach)
   
   - **Quick Actions:**
     - Assign to self
     - Change priority
     - Change status
     - Add internal note
     - Reply to user

3. **User Context Panel**
   - **Quick User Info:**
     - Name, email, phone
     - User role & verification level
     - Account status
     - Lifetime value
   
   - **Recent Activity:**
     - Last login
     - Recent transactions
     - Recent support tickets
     - Recent notifications sent
   
   - **Quick Admin Actions:**
     - View full profile
     - Reset password
     - Suspend account
     - Refund transaction
     - Issue credit

4. **Knowledge Base Management**
   - **Article Management:**
     - Create/edit/delete articles
     - Categories & tags
     - Search functionality
     - View count analytics
   
   - **Self-Service:**
     - User-facing FAQ
     - Search before ticket submission
     - Suggested articles based on ticket description
   
   - **Internal Wiki:**
     - Agent-only documentation
     - Troubleshooting guides
     - Escalation procedures

5. **Communication Tools**
   - **In-App Messaging:**
     - Real-time chat (if user online)
     - Offline messaging
     - File sharing
     - Canned responses (templates)
   
   - **Email:**
     - Send from ticket interface
     - Email templates
     - Merge tags for personalization
     - Track opens & clicks
   
   - **Phone:**
     - Click-to-call integration (if available)
     - Call logging
     - Call recording (with consent)

6. **Support Analytics**
   - **Performance Metrics:**
     - Tickets resolved per agent
     - Average response time
     - Average resolution time
     - Customer satisfaction (CSAT) score
     - First contact resolution rate
   
   - **Trend Analysis:**
     - Ticket volume over time
     - Common issues (by category)
     - Peak support hours
     - Seasonal patterns

**Acceptance Criteria:**
- Ticket response SLA: <2 hours (business hours)
- Ticket resolution SLA: <24 hours (non-technical), <3 days (technical)
- CSAT score target: >4.5/5
- Knowledge base article coverage: >80% of common issues

---

## 6. USER STORIES & USE CASES

### 6.1 Epic: Carbon Credit Generation

#### User Story: US-001
**As an** EV owner  
**I want to** connect my vehicle and sync driving data automatically  
**So that** I don't have to manually track my trips

**Acceptance Criteria:**
- Given I have registered an account and added my vehicle
- When I authorize data access via VinFast/Tesla API
- Then my trips are synced daily without manual intervention
- And I can see sync status and last sync time
- And I receive a notification if sync fails

**Priority:** MUST HAVE  
**Story Points:** 8

---

#### User Story: US-002
**As an** EV owner  
**I want to** see how much CO₂ I've saved in real-time  
**So that** I feel motivated to continue driving electric

**Acceptance Criteria:**
- Given I have driven my EV for at least 1 km
- When I open my dashboard
- Then I see total CO₂ saved (kg) with visual indicators
- And I see equivalents (trees planted, flights offset)
- And I see a trend chart of my impact over time

**Priority:** MUST HAVE  
**Story Points:** 5

---

#### User Story: US-003
**As an** EV owner  
**I want to** submit my carbon credits for verification  
**So that** I can list them for sale

**Acceptance Criteria:**
- Given I have accumulated at least 0.1 ton of carbon credits
- When I click "Request Verification"
- Then my trip data and calculation are sent to CVA
- And I can track verification status
- And I receive notification when approved or if more info needed

**Priority:** MUST HAVE  
**Story Points:** 5

---

### 6.2 Epic: Marketplace Trading

#### User Story: US-010
**As an** EV owner  
**I want to** get AI-powered price recommendations  
**So that** I can maximize my earnings

**Acceptance Criteria:**
- Given I am creating a listing
- When I view the pricing section
- Then I see a recommended price with confidence level
- And I see price range (min, avg, max) from recent transactions
- And I see demand indicator for my credit type
- And I can accept recommendation or set custom price

**Priority:** SHOULD HAVE  
**Story Points:** 8

---

#### User Story: US-011
**As a** corporate buyer  
**I want to** search for carbon credits by location and price  
**So that** I can find credits that match my needs

**Acceptance Criteria:**
- Given I am on the marketplace page
- When I apply filters (region, price range, amount)
- Then I see matching listings in <2 seconds
- And I can sort by price, date, seller rating
- And I can save my search for future notifications

**Priority:** MUST HAVE  
**Story Points:** 5

---

#### User Story: US-012
**As a** corporate buyer  
**I want to** participate in carbon credit auctions  
**So that** I can potentially get better prices

**Acceptance Criteria:**
- Given there is an active auction listing
- When I place a bid meeting the minimum increment
- Then my bid is recorded and displayed
- And I receive notifications if outbid
- And I can set a maximum auto-bid amount
- And if I win, I have 24 hours to complete payment

**Priority:** SHOULD HAVE  
**Story Points:** 13

---

### 6.3 Epic: Verification & Compliance

#### User Story: US-020
**As a** CVA auditor  
**I want to** review verification requests efficiently  
**So that** I can process more requests per day

**Acceptance Criteria:**
- Given I have pending verification requests
- When I open a request
- Then I see all trip data, calculations, and data source info
- And I have tools to validate outliers and anomalies
- And I can approve, reject, or request more information
- And the EV owner is notified of my decision

**Priority:** MUST HAVE  
**Story Points:** 8

---

#### User Story: US-021
**As a** corporate buyer  
**I want to** receive verified carbon credit certificates  
**So that** I can use them for ESG reporting

**Acceptance Criteria:**
- Given I have completed a purchase
- When the transaction is settled
- Then I receive a certificate with unique ID, QR code, and verifier signature
- And the certificate includes all transaction details
- And I can download it as PDF
- And I can verify its authenticity on a public portal

**Priority:** MUST HAVE  
**Story Points:** 8

---

### 6.4 Epic: Platform Administration

#### User Story: US-030
**As an** admin  
**I want to** monitor transactions in real-time  
**So that** I can detect and resolve issues immediately

**Acceptance Criteria:**
- Given I am on the admin dashboard
- When I view the transaction monitor
- Then I see live transaction feed updating every 30 seconds
- And I can filter by status, amount, payment method
- And I can drill into any transaction for details
- And I can take actions (hold, cancel, force complete)

**Priority:** MUST HAVE  
**Story Points:** 8

---

### 6.5 Use Case: Complete Transaction Flow

**Use Case ID:** UC-001  
**Use Case Name:** Purchase Carbon Credits (Fixed Price)  
**Actor:** Corporate Buyer  
**Preconditions:**  
- Buyer has registered and verified account
- Seller has listed carbon credits (fixed price)
- Credits are verified

**Main Success Scenario:**
1. Buyer searches for carbon credits by region "TP.HCM"
2. System displays matching listings
3. Buyer selects a listing (1.5 tons @ 2,500,000 VND/ton)
4. Buyer clicks "Buy Now"
5. System calculates total (3,750,000 VND) + platform fee (187,500 VND) = 3,937,500 VND
6. Buyer proceeds to checkout
7. Buyer selects payment method (MoMo e-wallet)
8. System redirects to MoMo payment gateway
9. Buyer completes payment successfully
10. System moves funds to escrow
11. System transfers credits from seller wallet to buyer wallet
12. System generates certificate
13. System releases payment to seller (minus platform fee)
14. System sends confirmation emails to both parties
15. Transaction complete

**Alternative Flows:**
- **3a.** Listing no longer available → System shows "Sold Out" message
- **9a.** Payment fails → System returns to checkout, credits remain with seller
- **11a.** Credit transfer fails → System refunds buyer, notifies admin

**Postconditions:**
- Credits transferred to buyer wallet
- Payment released to seller
- Certificate issued
- Transaction recorded in both parties' history

---

## 7. DATA REQUIREMENTS & INFORMATION ARCHITECTURE

### 7.1 Core Data Entities

#### Entity: User
```
user_id (PK, UUID)
role (ENUM: evowner, buyer, verifier, admin)
email (UNIQUE, INDEXED)
phone (UNIQUE)
full_name
kyc_level (0, 1, 2)
kyc_status (pending, approved, rejected)
account_status (active, suspended, closed)
created_at
updated_at
last_login_at
```

#### Entity: Vehicle
```
vehicle_id (PK, UUID)
owner_id (FK → user_id)
make (e.g., VinFast, Tesla)
model
year
vin (UNIQUE)
registration_number
data_source (api, obd, manual)
data_source_config (JSON)
verification_status
created_at
```

#### Entity: Trip
```
trip_id (PK, UUID)
vehicle_id (FK)
start_time
end_time
distance_km
start_lat, start_lng
end_lat, end_lng
avg_speed
energy_consumed_kwh
co2_saved_kg
data_quality_score
sync_batch_id
created_at
```

#### Entity: CarbonCredit
```
credit_id (PK, UUID)
serial_number (UNIQUE, INDEXED)
owner_id (FK → user_id)
amount_tons
vintage_year
methodology
verification_id (FK)
status (pending, issued, listed, sold, retired)
created_at (issuance date)
```

#### Entity: Listing
```
listing_id (PK, UUID)
seller_id (FK → user_id)
credit_id (FK)
listing_type (fixed, auction)
price_per_ton
reserve_price (for auction)
auction_end_date
status (active, sold, expired, cancelled)
views_count
created_at
updated_at
```

#### Entity: Transaction
```
transaction_id (PK, UUID)
listing_id (FK)
buyer_id (FK → user_id)
seller_id (FK → user_id)
credit_amount_tons
unit_price
platform_fee
total_amount
payment_method
payment_status
escrow_status
certificate_id (FK)
created_at
completed_at
```

#### Entity: VerificationRequest
```
verification_id (PK, UUID)
owner_id (FK → user_id)
vehicle_id (FK)
trip_date_start
trip_date_end
total_km
co2_saved_kg
credit_amount_tons
status (pending, approved, rejected)
assigned_auditor_id (FK → user_id)
auditor_notes
approved_at
created_at
```

#### Entity: Certificate
```
certificate_id (PK, UUID)
transaction_id (FK)
buyer_id (FK → user_id)
credit_serial_numbers (ARRAY)
issue_date
issuer_organization
auditor_signature
certificate_pdf_url
qr_code_data
verification_url
```

### 7.2 Data Relationships

**One-to-Many Relationships:**
- User → Vehicles (1:N)
- Vehicle → Trips (1:N)
- User → CarbonCredits (1:N) [as owner]
- User → Listings (1:N) [as seller]
- User → Transactions (1:N) [as buyer/seller]
- User → VerificationRequests (1:N)

**Many-to-One Relationships:**
- CarbonCredit → VerificationRequest (N:1)
- Listing → CarbonCredit (N:1) [can split credits]
- Transaction → Listing (N:1) [multiple buyers for auction]

**One-to-One Relationships:**
- Transaction → Certificate (1:1)

### 7.3 Data Volume Estimates (Year 1)

| Entity | Estimated Records | Growth Rate |
|--------|------------------|-------------|
| Users | 15,000 | +1,000/month |
| Vehicles | 12,000 | +800/month |
| Trips | 3,600,000 | +300,000/month |
| Carbon Credits | 8,000 tons | +667 tons/month |
| Listings | 5,000 | +400/month |
| Transactions | 3,000 | +250/month |
| Verification Requests | 10,000 | +833/month |
| Certificates | 3,000 | +250/month |

### 7.4 Data Retention Policy

| Data Type | Retention Period | Archive Strategy |
|-----------|------------------|------------------|
| User account data | Lifetime + 7 years after closure | Cold storage after 2 years inactive |
| Trip data | 5 years | Archive to S3 Glacier after 1 year |
| Transaction data | 10 years (regulatory) | Archive after 2 years |
| Certificates | Perpetual | Never delete |
| Audit logs | 10 years | Archive after 1 year |
| Support tickets | 3 years | Archive after closure + 6 months |
| Analytics data | 2 years raw, perpetual aggregated | Aggregate monthly after 6 months |

---

## 8. SYSTEM INTEGRATION REQUIREMENTS

### 8.1 External API Integrations

#### INT-001: Vehicle OEM APIs
**Purpose:** Sync trip data from EV manufacturers  
**Providers:** VinFast API, Tesla API, BYD API (future)

**Integration Details:**
- **Authentication:** OAuth 2.0
- **Data Sync Frequency:** Daily (configurable)
- **Data Retrieved:**
  - Trip history (start/end time, location, distance)
  - Charging history
  - Energy consumption
  - Vehicle health data (optional)
- **Rate Limits:** 100 calls/day per vehicle
- **Error Handling:** Retry with exponential backoff, fallback to manual upload

**Priority:** HIGH

---

#### INT-002: Payment Gateways
**Purpose:** Process payments from buyers

**A. MoMo E-Wallet**
- API Version: v2.1
- Endpoints: Create payment, Check status, Refund
- Webhook for real-time status updates
- Fee: 1.5% per transaction

**B. VNPay**
- Similar to MoMo
- Fee: 1.8% per transaction

**C. Bank Transfer (Napas)**
- Virtual account number generation
- Auto-reconciliation via bank API
- Fee: 5,000 VND flat per transaction

**D. International Cards (Stripe)**
- For foreign buyers
- Fee: 2.9% + 5,000 VND per transaction

**Priority:** CRITICAL

---

#### INT-003: Government Databases
**Purpose:** Verify business registration and vehicle ownership

**A. Business Registration Portal (Bộ Kế Hoạch & Đầu Tư)**
- API to check business registration number validity
- Verify company name, tax code, legal representative
- Endpoint: GET /api/v1/business/verify
- Response time: <2 seconds
- Availability: 99% (government SLA)

**B. Vehicle Registration Database (Cục Đăng Kiểm)**
- Verify VIN and vehicle ownership
- Check vehicle specifications
- Requires MOU with agency
- Alternative: Manual document verification

**Priority:** MEDIUM (nice-to-have, manual verification fallback)

---

#### INT-004: SMS & Email Services
**Purpose:** Send notifications to users

**A. SMS Provider (Twilio/local provider)**
- OTP messages
- Transaction notifications
- Alert messages
- Cost: 500-700 VND per SMS

**B. Email Service (SendGrid/AWS SES)**
- Transactional emails
- Marketing emails
- Cost: ~$1 per 1000 emails

**Priority:** HIGH

---

#### INT-005: Cloud Storage (AWS S3 / GCS)
**Purpose:** Store documents, certificates, trip data files

**Requirements:**
- Document uploads (KYC, verification docs): <5MB per file
- Certificate PDFs: <1MB per file
- Trip data exports: <10MB per file
- Retention: As per data retention policy
- Encryption: AES-256 at rest
- Backup: Daily to separate region

**Priority:** CRITICAL

---

#### INT-006: Maps & Geocoding (Google Maps API)
**Purpose:** Display trip routes, verify locations

**Usage:**
- Geocoding API: Convert lat/lng to addresses
- Maps JavaScript API: Display trip maps
- Directions API: Validate route distances (anti-fraud)
- Cost: $5-7 per 1000 requests (varies by API)
- Optimization: Cache frequent locations

**Priority:** MEDIUM

---

#### INT-007: Analytics & Monitoring
**Purpose:** Track user behavior and system performance

**A. Google Analytics / Mixpanel**
- User behavior tracking
- Funnel analysis
- Cohort analysis

**B. Application Performance Monitoring (New Relic / Datadog)**
- Server performance
- API latency
- Error tracking
- Uptime monitoring

**C. Error Tracking (Sentry)**
- Frontend & backend errors
- Stack traces
- User context

**Priority:** HIGH

---

### 8.2 Internal System Integrations

#### INT-101: Authentication Service ↔ All Services
- Single Sign-On (SSO) architecture
- JWT token-based authentication
- Token refresh mechanism
- Session management

#### INT-102: Notification Service ↔ All Services
- Central notification dispatcher
- Queue-based (RabbitMQ/AWS SQS)
- Email, SMS, Push, In-app notifications
- Retry logic for failed deliveries

#### INT-103: Payment Service ↔ Transaction Service
- Escrow management
- Settlement processing
- Refund handling
- Synchronous API for critical operations

#### INT-104: Analytics Service ↔ All Services
- Event streaming (Kafka/AWS Kinesis)
- Real-time dashboards
- Data warehouse (BigQuery/Redshift)
- Batch jobs for aggregations

---

### 8.3 API Design Standards

**RESTful API Conventions:**
- Base URL: https://api.carbonmarketplace.vn/v1
- Authentication: Bearer token in Authorization header
- Request format: JSON
- Response format: JSON
- HTTP Status codes:
  - 200: Success
  - 201: Created
  - 400: Bad Request
  - 401: Unauthorized
  - 403: Forbidden
  - 404: Not Found
  - 429: Rate Limit Exceeded
  - 500: Internal Server Error

**Sample Endpoints:**
```
# User Management
POST   /users/register
POST   /users/login
GET    /users/me
PATCH  /users/me
POST   /users/me/kyc

# Vehicle Management
POST   /vehicles
GET    /vehicles
GET    /vehicles/{id}
POST   /vehicles/{id}/sync
GET    /vehicles/{id}/trips

# Carbon Credits
GET    /carbon-credits
POST   /carbon-credits/verification-request
GET    /carbon-credits/wallet

# Marketplace
GET    /marketplace/listings
POST   /marketplace/listings
GET    /marketplace/listings/{id}
PATCH  /marketplace/listings/{id}
DELETE /marketplace/listings/{id}
POST   /marketplace/listings/{id}/purchase

# Transactions
GET    /transactions
GET    /transactions/{id}
POST   /transactions/{id}/cancel

# Admin
GET    /admin/users
GET    /admin/transactions
GET    /admin/analytics
POST   /admin/settlements/process
```

**Rate Limiting:**
- Public API: 100 requests/minute per IP
- Authenticated API: 1000 requests/minute per user
- Admin API: 5000 requests/minute per admin

---

## 9. NON-FUNCTIONAL REQUIREMENTS

### 9.1 Performance Requirements

#### NFR-P-001: Response Time
- **Web Pages:** 95th percentile load time <3 seconds on 4G
- **API Endpoints:** 
  - Read operations: <500ms (95th percentile)
  - Write operations: <1 second (95th percentile)
  - Search: <2 seconds for 10,000 results
  - Report generation: <30 seconds
- **Real-time Updates:** WebSocket latency <100ms

#### NFR-P-002: Throughput
- **Concurrent Users:** Support 5,000 simultaneous users
- **Transactions:** 100 transactions/minute during peak
- **API Requests:** 10,000 requests/minute across all endpoints
- **Database:** 5,000 queries/second

#### NFR-P-003: Scalability
- **Horizontal Scaling:** Support auto-scaling up to 20 instances
- **Database:** Read replicas for scaling reads
- **User Growth:** Support 10x user growth (150,000 users) within 3 years
- **Storage:** Accommodate 10TB data within 3 years

---

### 9.2 Security Requirements

#### NFR-S-001: Authentication & Authorization
- **Password Policy:** 
  - Minimum 8 characters
  - Mix of uppercase, lowercase, numbers, special chars
  - Password history: Cannot reuse last 5 passwords
  - Expiry: 180 days (for admin accounts)
- **Multi-Factor Authentication (2FA):**
  - TOTP-based (Google Authenticator)
  - Mandatory for admin, optional for users
  - SMS OTP as backup
- **Session Management:**
  - Session timeout: 30 minutes inactive, 8 hours absolute
  - Force logout on password change
  - Concurrent session limit: 3 devices

#### NFR-S-002: Data Protection
- **Encryption:**
  - Data at rest: AES-256
  - Data in transit: TLS 1.3
  - Database: Encrypted storage (AWS RDS encryption)
  - Backups: Encrypted
- **PII Protection:**
  - Mask sensitive data in logs
  - Tokenize payment information
  - Anonymize data for analytics
- **GDPR Compliance:**
  - Right to access data
  - Right to delete data (within 30 days)
  - Data portability (export in JSON/CSV)
  - Consent management

#### NFR-S-003: Application Security
- **OWASP Top 10 Protection:**
  - SQL Injection: Parameterized queries, ORM
  - XSS: Input sanitization, Content Security Policy
  - CSRF: CSRF tokens
  - Broken Authentication: Secure session management
  - Security Misconfiguration: Regular security audits
- **API Security:**
  - API key rotation every 90 days
  - Rate limiting
  - Input validation on all endpoints
  - Signed API requests (HMAC)
- **Vulnerability Management:**
  - Dependency scanning (Snyk, Dependabot)
  - Penetration testing: Quarterly
  - Bug bounty program (future)

#### NFR-S-004: Compliance
- **Vietnam PDPA:** Personal data protection compliance
- **PCI-DSS:** Level 1 compliance (if storing card data, otherwise use tokenization)
- **ISO 27001:** Information security management (target certification)
- **SOC 2:** Security, availability, confidentiality (target certification)

---

### 9.3 Availability & Reliability

#### NFR-A-001: Uptime
- **Target Availability:** 99.5% (downtime <3.6 hours/month)
- **Critical Services:** 99.9% (payment, authentication)
- **Maintenance Windows:** Scheduled during low-traffic (2-4 AM, announced 7 days prior)

#### NFR-A-002: Disaster Recovery
- **Recovery Time Objective (RTO):** <4 hours
- **Recovery Point Objective (RPO):** <1 hour (max data loss)
- **Backup Strategy:**
  - Database: Continuous replication + daily snapshots
  - Files: Real-time replication to secondary region
  - Retention: Daily for 7 days, weekly for 4 weeks, monthly for 12 months
- **Disaster Recovery Plan:**
  - Multi-region deployment (primary: Singapore, DR: US West)
  - Automated failover for critical services
  - Regular DR drills: Quarterly

#### NFR-A-003: Monitoring & Alerting
- **Health Checks:** Every 30 seconds for critical services
- **Alerts:**
  - CPU/Memory >80% for 5 minutes
  - Error rate >1% of requests
  - API latency >2 seconds (95th percentile)
  - Payment gateway downtime
  - Database connection failures
- **On-Call Rotation:** 24/7 on-call engineer, <15 minute response time

---

### 9.4 Usability Requirements

#### NFR-U-001: User Interface
- **Design System:** Consistent UI components across platform
- **Accessibility:** 
  - WCAG 2.1 Level AA compliance
  - Screen reader compatible
  - Keyboard navigation
  - Color contrast ratio >4.5:1
- **Responsive Design:** Support desktop (1920x1080 to 1366x768), tablet (768x1024), mobile (375x667 to 414x896)
- **Browser Support:** 
  - Chrome (last 2 versions)
  - Firefox (last 2 versions)
  - Safari (last 2 versions)
  - Edge (last 2 versions)

#### NFR-U-002: Localization
- **Languages:** Vietnamese (primary), English
- **Currency:** VND (primary), USD (for international buyers)
- **Date/Time:** Vietnam timezone (GMT+7), customizable
- **Number Format:** 
  - Vietnamese: 1.234.567,89
  - English: 1,234,567.89

#### NFR-U-003: User Experience
- **Onboarding:** <10 minutes to complete registration and first vehicle connection
- **Task Completion:** 
  - Create listing: <5 minutes
  - Purchase credits: <3 minutes
  - Withdraw earnings: <2 minutes
- **Help & Documentation:**
  - Contextual help tooltips
  - Comprehensive FAQ (>50 articles)
  - Video tutorials for key features
  - Live chat support (business hours)

---

### 9.5 Maintainability Requirements

#### NFR-M-001: Code Quality
- **Code Coverage:** >80% unit test coverage
- **Code Review:** All code peer-reviewed before merge
- **Coding Standards:** Follow language-specific style guides (e.g., PEP 8 for Python, Airbnb for JavaScript)
- **Documentation:** API documentation (OpenAPI/Swagger), architecture diagrams (C4 model)

#### NFR-M-002: DevOps
- **CI/CD:** Automated build, test, deploy pipeline
- **Deployment Frequency:** Daily deployments to staging, weekly to production
- **Rollback:** <15 minutes to rollback failed deployment
- **Infrastructure as Code:** Terraform/CloudFormation for all infrastructure

#### NFR-M-003: Logging & Debugging
- **Structured Logging:** JSON format, centralized (ELK stack or equivalent)
- **Log Levels:** DEBUG, INFO, WARN, ERROR, CRITICAL
- **Correlation IDs:** Track requests across services
- **Log Retention:** 30 days hot, 90 days warm, 1 year cold storage

---

### 9.6 Operational Requirements

#### NFR-O-001: Support
- **Support Channels:**
  - In-app chat (business hours: 8 AM - 8 PM)
  - Email: 24/7, response <4 hours
  - Phone: Business hours, Vietnamese & English
- **Support SLA:**
  - Critical (payment issues): <1 hour response, <4 hours resolution
  - High (account access): <4 hours response, <24 hours resolution
  - Medium: <24 hours response, <3 days resolution
  - Low: <48 hours response

#### NFR-O-002: Reporting
- **System Reports:** Daily/weekly/monthly automated reports
- **Custom Reports:** On-demand report generation <30 seconds
- **Export Formats:** PDF, Excel, CSV
- **Report Scheduling:** Email delivery at specified intervals

#### NFR-O-003: Data Management
- **Data Export:** Users can export their data (JSON, CSV) on demand
- **Data Deletion:** Complete data deletion within 30 days of request (GDPR)
- **Data Archival:** Automated archival of old data per retention policy
- **Data Migration:** Support for importing historical data during onboarding

---

## 10. RISK ANALYSIS & MITIGATION

### 10.1 Technical Risks

#### RISK-T-001: Vehicle API Integration Failures
**Likelihood:** HIGH  
**Impact:** HIGH  
**Description:** Vehicle OEM APIs may be unreliable, have downtime, or change without notice, preventing automatic trip data sync.

**Mitigation Strategies:**
1. **Multi-source support:** Implement 3 options: OEM API, OBD-II device, manual upload
2. **Fallback mechanism:** Auto-switch to manual upload if API fails >3 times
3. **API monitoring:** Track API health and alert on degradation
4. **Data caching:** Cache trip data locally on mobile app, sync when available
5. **Partnership:** Negotiate SLA with VinFast and other OEMs

**Contingency Plan:** If major API failure, temporarily waive data source verification requirement, rely on manual upload with enhanced verification

---

#### RISK-T-002: Scalability Bottlenecks
**Likelihood:** MEDIUM  
**Impact:** HIGH  
**Description:** System may not scale to handle rapid user growth, leading to slow performance or downtime.

**Mitigation Strategies:**
1. **Performance testing:** Load test for 10x expected traffic before launch
2. **Auto-scaling:** Configure auto-scaling for web and API servers
3. **Database optimization:** Implement read replicas, query optimization, caching (Redis)
4. **CDN:** Use CDN for static assets
5. **Microservices:** Design for horizontal scalability from day 1

**Contingency Plan:** Temporary user registration cap during scaling bottleneck, priority access for verified users

---

#### RISK-T-003: Data Accuracy & Fraud
**Likelihood:** MEDIUM  
**Impact:** CRITICAL  
**Description:** Users may attempt to manipulate trip data to generate more carbon credits fraudulently.

**Mitigation Strategies:**
1. **Data validation:** Multi-layer validation (GPS accuracy, distance vs. energy consumed, speed reasonability)
2. **Anomaly detection:** ML model to detect suspicious patterns
3. **Random audits:** CVA spot-checks 5-10% of verifications
4. **Reputation system:** Track user history, flag repeat offenders
5. **Penalties:** Suspend accounts with confirmed fraud, blacklist devices

**Contingency Plan:** If large-scale fraud detected, temporarily pause all verifications, manual review all pending requests

---

### 10.2 Business Risks

#### RISK-B-001: Low Buyer Demand
**Likelihood:** MEDIUM  
**Impact:** HIGH  
**Description:** Corporate buyers may not adopt the platform, leaving EV owners unable to sell credits.

**Mitigation Strategies:**
1. **Pre-launch partnerships:** Secure 10+ corporate buyers before launch (MOU signed)
2. **Marketing campaigns:** Target ESG-focused companies, showcase case studies
3. **Pricing incentives:** Offer discounted credits for first 6 months
4. **Guaranteed buyback:** Platform acts as buyer of last resort at floor price (2M VND/ton)
5. **International expansion:** Open to foreign buyers if domestic demand low

**Contingency Plan:** Platform aggregates credits and sells in bulk to international carbon markets (Gold Standard, Verra)

---

#### RISK-B-002: Regulatory Changes
**Likelihood:** MEDIUM  
**Impact:** HIGH  
**Description:** Government may introduce regulations that restrict or complicate carbon credit trading.

**Mitigation Strategies:**
1. **Regulatory monitoring:** Engage legal team to track policy changes
2. **Industry association:** Join carbon trading associations for advocacy
3. **Government relations:** Build relationships with Bộ TN&MT
4. **Flexible architecture:** Design platform to adapt to regulatory requirements
5. **International standards:** Align with UN frameworks to increase legitimacy

**Contingency Plan:** If Vietnam regulation unfavorable, pivot to international markets, register credits with global standards

---

#### RISK-B-003: Competition from Established Players
**Likelihood:** MEDIUM  
**Impact:** MEDIUM  
**Description:** Large carbon trading platforms or vehicle OEMs may launch competing services.

**Mitigation Strategies:**
1. **First-mover advantage:** Launch quickly, capture market share early
2. **Differentiation:** Focus on EV-specific features, user experience
3. **Network effects:** Build liquidity (buyers & sellers) to make platform sticky
4. **Partnerships:** Strategic partnerships with VinFast, charging station networks
5. **Community building:** Create engaged user community, loyalty programs

**Contingency Plan:** Position as specialized niche player, focus on premium user experience, potential acquisition target

---

### 10.3 Operational Risks

#### RISK-O-001: Verification Bottleneck
**Likelihood:** HIGH  
**Impact:** MEDIUM  
**Description:** CVA organizations may not have capacity to verify all requests timely, creating backlog.

**Mitigation Strategies:**
1. **Multiple CVA partners:** Onboard 3-5 CVA organizations
2. **Automated pre-verification:** Use ML to pre-screen requests, only flag anomalies for manual review
3. **Verification toolkit:** Provide CVA with efficient tools to speed up process
4. **Scalable pricing:** Pay CVA per verification to incentivize capacity increase
5. **Training program:** Train and certify more auditors

**Contingency Plan:** Temporarily lower verification requirements for small amounts (<0.5 ton), implement batch verification

---

#### RISK-O-002: Payment Processing Issues
**Likelihood:** MEDIUM  
**Impact:** CRITICAL  
**Description:** Payment gateway downtime or failures could prevent transactions, losing buyer trust.

**Mitigation Strategies:**
1. **Multiple gateways:** Integrate 3+ payment providers
2. **Smart routing:** Automatically route to healthy gateway
3. **Health monitoring:** Real-time monitoring of gateway status
4. **Retry logic:** Automatic retry with exponential backoff
5. **Manual processing:** Admin can manually process payments if all gateways down

**Contingency Plan:** Enable bank transfer as fallback (slower but reliable), provide 48-hour grace period for payment

---

#### RISK-O-003: Customer Support Overload
**Likelihood:** HIGH  
**Impact:** MEDIUM  
**Description:** High volume of support requests may overwhelm support team, degrading service quality.

**Mitigation Strategies:**
1. **Self-service:** Comprehensive FAQ, video tutorials, chatbot for common queries
2. **Tiered support:** L1 (chatbot), L2 (support agents), L3 (technical team)
3. **Ticket prioritization:** Auto-categorize by severity
4. **Knowledge base:** Continuously update based on common issues
5. **Outsourced support:** Contract with BPO for overflow (after training)

**Contingency Plan:** Temporarily extend response SLA, hire temporary support staff during peak periods

---

### 10.4 Financial Risks

#### RISK-F-001: High Payment Gateway Fees
**Likelihood:** HIGH  
**Impact:** MEDIUM  
**Description:** Payment processing fees (1.5-3%) may significantly reduce profitability.

**Mitigation Strategies:**
1. **Negotiate rates:** Volume-based discounts with providers
2. **Optimize routing:** Route to cheapest available gateway
3. **Pass on fees:** Charge buyers for payment processing (transparent)
4. **Bank transfer incentive:** Offer discount for bank transfer (lower fees)
5. **Alternative:** Explore crypto payments (lower fees, but higher volatility)

**Contingency Plan:** Increase platform commission from 5% to 6-7% to compensate for gateway fees

---

#### RISK-F-002: Currency Fluctuation (International Buyers)
**Likelihood:** MEDIUM  
**Impact:** LOW  
**Description:** If accepting USD, VND/USD fluctuation may create pricing issues.

**Mitigation Strategies:**
1. **Real-time exchange rates:** Update prices dynamically
2. **Hedge:** Use forward contracts if large USD exposure
3. **VND primary:** Price everything in VND, USD as display only
4. **Buffer:** Include 2-3% buffer in exchange rate

**Contingency Plan:** Temporarily disable USD purchases during high volatility

---

## 11. IMPLEMENTATION ROADMAP

### Phase 0: Foundation (Months 1-2)
**Objective:** Setup core infrastructure and team

**Deliverables:**
- Technical architecture finalized
- Development environment setup
- CI/CD pipeline configured
- Cloud infrastructure provisioned (AWS/GCS)
- Team onboarding complete (developers, designers, BA)

**Key Activities:**
- Vendor selection (payment gateway, SMS, etc.)
- Security audit preparation
- Database schema design
- API specification (OpenAPI)

**Risks:** Delayed vendor negotiations, team hiring delays

---

### Phase 1: MVP - Core Features (Months 3-5)
**Objective:** Launch minimum viable product for beta testing

**Features:**
- ✓ User registration & KYC (Level 1)
- ✓ Vehicle registration & manual trip upload
- ✓ CO₂ calculation engine
- ✓ Carbon wallet
- ✓ Basic marketplace (fixed price listings only)
- ✓ Simple purchase flow (bank transfer payment)
- ✓ Manual verification by CVA
- ✓ Basic certificate generation
- ✓ Admin dashboard (user & transaction management)

**Exclusions (Phase 2):**
- OEM API integration
- Auction listings
- AI price recommendation
- Mobile app
- Advanced analytics

**Launch Criteria:**
- 100 beta users onboarded
- 50 transactions completed successfully
- 99% uptime for 2 consecutive weeks
- Security audit passed

**Timeline:** Month 5 - Beta launch to 500 invited users

---

### Phase 2: Enhanced Marketplace (Months 6-8)
**Objective:** Add advanced trading features and scale user base

**Features:**
- ✓ Auction listings
- ✓ VinFast API integration (trip auto-sync)
- ✓ Advanced search & filters
- ✓ Buyer company verification (Level 2 KYC)
- ✓ Multiple payment gateways (MoMo, VNPay, cards)
- ✓ AI price recommendation (basic ML model)
- ✓ Automated verification (for low-risk cases)
- ✓ Enhanced analytics & reporting
- ✓ Mobile-responsive web app (PWA)

**Performance Targets:**
- 2,000 active EV owners
- 100 corporate buyers
- 500 transactions/month
- $500K GMV/month

**Timeline:** Month 8 - Public launch with marketing campaign

---

### Phase 3: Scale & Optimize (Months 9-12)
**Objective:** Scale operations and optimize user experience

**Features:**
- ✓ Tesla/BYD API integration
- ✓ OBD-II device support
- ✓ Advanced AI pricing (deep learning model)
- ✓ Gamification (badges, leaderboard)
- ✓ Social sharing features
- ✓ Referral program
- ✓ Bulk purchase for enterprise buyers
- ✓ Premium subscription tier
- ✓ API for third-party integrations
- ✓ Enhanced fraud detection (ML-based)

**Optimizations:**
- Performance improvements (caching, CDN)
- UX refinements based on user feedback
- Automated customer support (chatbot)
- Verification process optimization

**Performance Targets:**
- 10,000 active EV owners
- 500 corporate buyers
- 2,000 transactions/month
- $3M GMV/month

**Timeline:** Month 12 - Stable, scaled platform

---

### Phase 4: Expansion (Year 2)
**Objective:** Geographic and product expansion

**Features:**
- ✓ International expansion (Thailand, Indonesia, Philippines)
- ✓ Native mobile apps (iOS & Android)
- ✓ Corporate dashboards for buyers
- ✓ ESG reporting integrations (CDP, GRI)
- ✓ Blockchain-based certificate registry
- ✓ Carbon offsetting for non-EV activities (solar, reforestation)
- ✓ B2B partnerships (ride-hailing fleets, logistics companies)
- ✓ White-label solution for OEMs

**Performance Targets:**
- 50,000 users across 4 countries
- $20M GMV/year
- Profitability achieved

---

### Agile Methodology

**Sprint Structure:**
- Sprint duration: 2 weeks
- Sprint planning: Monday 9 AM
- Daily standup: 9:30 AM (15 minutes)
- Sprint review: Bi-weekly Friday 2 PM
- Sprint retrospective: Bi-weekly Friday 4 PM

**Team Structure:**
- Product Owner: 1 (stakeholder proxy, backlog prioritization)
- Scrum Master: 1 (process facilitator)
- Backend Developers: 3
- Frontend Developers: 2
- Mobile Developers: 1 (Phase 2+)
- UX/UI Designer: 1
- QA Engineers: 2
- DevOps Engineer: 1
- Data Scientist: 1 (Phase 2+ for AI features)

**Release Cadence:**
- Production deployment: Weekly (every Friday 6 PM)
- Hotfixes: As needed (within 2 hours of critical bug detection)
- Feature flags: Enable gradual rollout of new features

---

## 12. SUCCESS METRICS & KPIs

### 12.1 User Acquisition Metrics

| Metric | Month 3 (Beta) | Month 8 (Launch) | Month 12 | Year 2 |
|--------|---------------|------------------|----------|--------|
| **Total Users** | 500 | 5,000 | 15,000 | 50,000 |
| - EV Owners | 400 | 4,000 | 12,000 | 40,000 |
| - Corporate Buyers | 50 | 500 | 1,500 | 5,000 |
| - CVA Organizations | 3 | 5 | 10 | 20 |
| **Monthly Active Users (MAU)** | 300 | 3,500 | 10,000 | 35,000 |
| **Daily Active Users (DAU)** | 100 | 800 | 2,500 | 10,000 |
| **DAU/MAU Ratio** | 33% | 23% | 25% | 28% |

### 12.2 Engagement Metrics

| Metric | Target (Month 12) |
|--------|-------------------|
| **Avg sessions per user/month** | 8-12 |
| **Avg session duration** | 5-7 minutes |
| **Onboarding completion rate** | >80% |
| **Vehicle connection rate** | >70% (of EV owners) |
| **Trip sync frequency** | 90% users sync weekly |
| **Feature adoption rate** ||
| - AI price recommendation usage | >60% of sellers |
| - Auction participation | >20% of transactions |
| - Social sharing | >15% of users |

### 12.3 Transaction Metrics

| Metric | Month 8 | Month 12 | Year 2 |
|--------|---------|----------|--------|
| **Total Transactions** | 500 | 2,000 | 25,000 |
| **GMV (Gross Merch Value)** | 10B VND | 50B VND | 500B VND |
| **Avg Transaction Size** | 20M VND | 25M VND | 20M VND |
| **Transaction Success Rate** | >98% | >98% | >99% |
| **Time to First Transaction** | <7 days | <5 days | <3 days |
| **Repeat Transaction Rate** | >40% | >60% | >70% |

### 12.4 Marketplace Health

| Metric | Target |
|--------|--------|
| **Supply (Active Listings)** | 300-500 at any time |
| **Demand (Monthly Searches)** | 10K-20K searches/month |
| **Liquidity** ||
| - Avg time to sell (fixed price) | <7 days |
| - Listing sell-through rate | >60% |
| **Pricing** ||
| - Avg price per ton CO₂ | 2.5M VND (±20%) |
| - Price volatility (std dev) | <15% |

### 12.5 Environmental Impact

| Metric | Year 1 Target | Year 2 Target |
|--------|---------------|---------------|
| **Total CO₂ Offset** | 8,000 tons | 50,000 tons |
| **Equivalent Trees Planted** | 400,000 | 2,500,000 |
| **Equivalent Cars Off Road** | 1,700 | 11,000 |
| **Avg CO₂/EV Owner/Year** | 0.67 tons | 1.25 tons |

### 12.6 Financial Metrics

| Metric | Year 1 | Year 2 | Year 3 |
|--------|--------|--------|--------|
| **Revenue** | 3B VND | 25B VND | 80B VND |
| - Transaction fees (5-8%) | 2.5B VND | 20B VND | 65B VND |
| - Verification fees | 300M VND | 3B VND | 8B VND |
| - Premium subscriptions | 100M VND | 1.5B VND | 5B VND |
| - Other (API, data, partnerships) | 100M VND | 500M VND | 2B VND |
| **Gross Margin** | 70% | 75% | 78% |
| **Net Margin** | -50% (investment) | 10% | 25% |
| **CAC (Customer Acq Cost)** | 500K VND | 300K VND | 200K VND |
| **LTV (Lifetime Value)** | 2M VND | 3M VND | 4M VND |
| **LTV:CAC Ratio** | 4:1 | 10:1 | 20:1 |
| **Burn Rate** | 500M VND/month | Break-even | +100M VND/month |

### 12.7 Operational Efficiency

| Metric | Target |
|--------|--------|
| **Platform Uptime** | >99.5% |
| **API Response Time (p95)** | <500ms |
| **Verification Turnaround** | <48 hours |
| **Settlement Time** | T+2 days |
| **Customer Support** ||
| - First response time | <2 hours |
| - Resolution time | <24 hours |
| - CSAT score | >4.5/5 |
| **Payment Success Rate** | >98% |
| **Fraud Rate** | <0.5% of transactions |

### 12.8 Quality Metrics

| Metric | Target |
|--------|--------|
| **Bug Escape Rate** | <2% to production |
| **Critical Bugs in Production** | 0 |
| **Mean Time to Recovery (MTTR)** | <1 hour |
| **Code Coverage** | >80% |
| **Security Vulnerabilities** | 0 critical, <5 medium |
| **Data Accuracy** | >99.5% |

---

## 13. ASSUMPTIONS & CONSTRAINTS

### 13.1 Assumptions

#### Business Assumptions
1. **Market Demand:** Vietnam will have 200,000+ EVs by end of 2025
2. **Carbon Price:** Market price for carbon credits will remain between 2-3M VND/ton
3. **Corporate Buyers:** Sufficient demand from 500+ companies needing carbon offsets for ESG
4. **Regulatory Environment:** Government will not ban or heavily restrict carbon credit trading
5. **Competition:** No dominant player will emerge in first 12 months
6. **User Behavior:** EV owners willing to share driving data for financial incentive
7. **Payment Habits:** Users comfortable with e-wallet payments (80%+ adoption)

#### Technical Assumptions
1. **OEM APIs:** VinFast and Tesla will provide stable APIs for trip data
2. **Third-Party Services:** Payment gateways, SMS providers maintain 99%+ uptime
3. **Internet Access:** 95%+ of users have reliable 4G/5G connection
4. **Device Capability:** Users have smartphones capable of running modern web apps
5. **Cloud Infrastructure:** AWS/GCS provides scalable, reliable infrastructure
6. **Development Team:** Can recruit and retain skilled developers in Vietnam
7. **AI/ML:** Sufficient transaction data available within 6 months to train pricing model

#### Financial Assumptions
1. **Funding:** Adequate funding secured for 18-month runway
2. **Unit Economics:** Platform commission of 5-8% sufficient for profitability at scale
3. **Payment Processing Costs:** Average 2% of transaction value
4. **Customer Acquisition:** CAC can be maintained at <500K VND through organic growth
5. **Churn Rate:** Annual churn rate <30%
6. **Revenue Growth:** Can achieve 10x revenue growth from Year 1 to Year 2

#### Regulatory Assumptions
1. **Carbon Standards:** International standards (Gold Standard, Verra) accepted in Vietnam
2. **Tax Treatment:** Carbon credit sales taxed as regular income (not special category)
3. **KYC Requirements:** Current KYC regulations sufficient, no major changes
4. **Data Privacy:** Vietnam PDPA regulations stable, no GDPR-equivalent introduced suddenly
5. **Payment Regulations:** No restrictions on peer-to-peer carbon credit transactions

---

### 13.2 Constraints

#### Time Constraints
1. **Launch Deadline:** Beta launch target Month 5, public launch Month 8 (firm dates for market window)
2. **Partnership Timelines:** OEM API integrations dependent on partner timelines (3-6 months negotiation)
3. **CVA Onboarding:** Verification organizations require 2-3 months onboarding and training
4. **Regulatory Approval:** If required, may take 3-6 months (potential delay risk)

#### Budget Constraints
1. **Development Budget:** $500K for Year 1 (fixed)
2. **Marketing Budget:** $200K for Year 1 (fixed)
3. **Payment Processing:** 2% of GMV (variable cost, cannot exceed)
4. **Infrastructure:** $50K/year cloud costs (target), scalable based on usage
5. **Team Size:** Maximum 15 FTE in Year 1 (salary constraints)

#### Technology Constraints
1. **OEM API Limitations:**
   - VinFast API: Rate limit 100 calls/day/vehicle, no real-time streaming
   - Tesla API: May be discontinued or changed without notice
   - No API access for legacy vehicles (pre-2022)
2. **Payment Gateway:** 
   - Transaction limits: Max 100M VND per transaction (most gateways)
   - Settlement time: Minimum T+2 days (banking regulations)
3. **Mobile Limitations:**
   - No native app in Phase 1 (PWA only)
   - Limited offline functionality due to complexity
4. **Data Storage:**
   - GDPR/PDPA compliance requires Vietnam data center (higher cost)
   - Data residency requirements may limit cloud provider options

#### Resource Constraints
1. **Development Team:**
   - Limited senior developer availability in Vietnam market
   - Knowledge gap in carbon accounting domain (requires training)
2. **CVA Partners:**
   - Limited number of certified carbon verification organizations in Vietnam (5-8 total)
   - Verification capacity: ~500 requests/month per organization
3. **Customer Support:**
   - Vietnamese language support required (limits outsourcing options)
   - Business hours support only initially (8 AM - 8 PM)

#### Legal & Regulatory Constraints
1. **Carbon Standards:**
   - Must align with international methodologies (CDM, Gold Standard, Verra)
   - Certificate format must be acceptable for corporate ESG reporting
2. **Financial Regulations:**
   - Not licensed as financial institution (cannot hold customer funds long-term)
   - Must use licensed payment intermediaries
   - Cannot offer investment advice (price recommendations must be clearly labeled as "informational")
3. **Data Privacy:**
   - Cannot share user location data without explicit consent
   - Must honor data deletion requests within 30 days
   - Parental consent required for users under 18

#### Operational Constraints
1. **Business Hours:**
   - CVA verification only during business hours (delays for overnight submissions)
   - Customer support limited to business hours initially
   - Payment settlement delays on weekends/holidays
2. **Language Support:**
   - Vietnamese required for all user-facing content
   - English support for international buyers (but limited staff)
3. **Geographic Limitations:**
   - Phase 1 limited to Vietnam only
   - International expansion requires separate legal entities

---

## 14. DEPENDENCIES

### 14.1 External Dependencies

#### Critical Dependencies (Blocking)
1. **Payment Gateway Partnerships**
   - Required for: Transaction processing
   - Timeline: Month 2-3
   - Risk: High - No alternative without payment processing
   - Mitigation: Begin negotiations immediately, have backup providers

2. **CVA Partnership Agreements**
   - Required for: Credit verification and issuance
   - Timeline: Month 2-4
   - Risk: High - Cannot issue credits without CVA
   - Mitigation: Sign MOUs with 3 organizations minimum

3. **Cloud Infrastructure Setup**
   - Required for: Hosting platform
   - Timeline: Month 1
   - Risk: Medium - Can switch providers but causes delay
   - Mitigation: Start with AWS (most reliable), design for portability

4. **Business License & Legal Entity**
   - Required for: Operating marketplace
   - Timeline: Month 1-2
   - Risk: Medium - Regulatory approval uncertainty
   - Mitigation: Engage legal counsel early, prepare all documents

#### High Priority Dependencies
5. **VinFast API Access**
   - Required for: Automatic trip sync for VinFast vehicles
   - Timeline: Month 4-6
   - Risk: Medium - Can operate with manual upload, but poor UX
   - Mitigation: Start negotiations early, develop manual upload as fallback

6. **SMS Provider**
   - Required for: OTP and notifications
   - Timeline: Month 2
   - Risk: Low - Many provider options
   - Mitigation: Use Twilio or local provider (FPT, Viettel)

7. **Email Service**
   - Required for: User communications
   - Timeline: Month 2
   - Risk: Low - Many options available
   - Mitigation: AWS SES or SendGrid

8. **SSL Certificate & Domain**
   - Required for: Secure HTTPS access
   - Timeline: Month 1
   - Risk: Low - Easy to obtain
   - Mitigation: Purchase from registrar, use Let's Encrypt for dev

#### Medium Priority Dependencies
9. **Maps API (Google Maps)**
   - Required for: Trip visualization, location verification
   - Timeline: Month 3
   - Risk: Low - Platform can function without it
   - Mitigation: Use free tier initially, consider alternatives (Mapbox)

10. **Analytics Tools**
    - Required for: User behavior tracking, business intelligence
    - Timeline: Month 3-4
    - Risk: Low - Nice-to-have initially
    - Mitigation: Use free tiers (Google Analytics, Mixpanel)

---

### 14.2 Internal Dependencies

#### Team Dependencies
1. **Product Requirements Finalization**
   - Dependent on: Stakeholder interviews, user research
   - Timeline: Month 1
   - Blocks: Design, development
   - Owner: Product Manager/BA

2. **UI/UX Design Completion**
   - Dependent on: Requirements
   - Timeline: Month 2
   - Blocks: Frontend development
   - Owner: UX Designer

3. **API Specification**
   - Dependent on: Architecture design
   - Timeline: Month 1
   - Blocks: Frontend and backend development (can parallel with draft spec)
   - Owner: Tech Lead

4. **Database Schema Design**
   - Dependent on: Data requirements
   - Timeline: Month 1
   - Blocks: Backend development
   - Owner: Backend Lead

#### Technical Dependencies
5. **Authentication Service**
   - Dependent on: Database setup
   - Timeline: Month 2
   - Blocks: All user-facing features
   - Owner: Backend Team

6. **Payment Service Integration**
   - Dependent on: Payment gateway partnerships
   - Timeline: Month 3
   - Blocks: Transaction processing
   - Owner: Backend Team

7. **CO₂ Calculation Engine**
   - Dependent on: Methodology research, database
   - Timeline: Month 2
   - Blocks: Credit issuance, wallet features
   - Owner: Backend Team

8. **Admin Dashboard**
   - Dependent on: Core transaction features
   - Timeline: Month 4
   - Blocks: Platform operations (but can launch beta without)
   - Owner: Frontend Team

#### Data Dependencies
9. **Emission Factor Database**
   - Dependent on: Research, data collection
   - Timeline: Month 2
   - Blocks: CO₂ calculation accuracy
   - Owner: Product Manager/Data Team

10. **Test Data Generation**
    - Dependent on: Database schema
    - Timeline: Month 2
    - Blocks: QA testing, demo environments
    - Owner: QA Team

---

### 14.3 Dependency Management Strategy

**Tracking:**
- Maintain dependency matrix in project management tool (Jira, Asana)
- Weekly dependency review in team meeting
- Red/Yellow/Green status indicators
- Owner assigned to each dependency

**Communication:**
- Weekly updates to stakeholders on critical dependencies
- Escalation path: Team Lead → Product Manager → Executive Sponsor
- Slack channel #dependencies for real-time updates

**Risk Mitigation:**
- Identify alternative solutions for each critical dependency
- Parallel track where possible (e.g., design can start with draft requirements)
- Buffer time in schedule for dependency delays (20% contingency)
- Early warning system: Flag any dependency at risk 2 weeks before needed

---

## 15. APPENDICES

### Appendix A: Glossary of Terms

| Term | Definition |
|------|------------|
| **Carbon Credit** | A tradable certificate representing the reduction of 1 ton of CO₂ or equivalent greenhouse gas |
| **CDM (Clean Development Mechanism)** | UN framework for carbon offset projects in developing countries |
| **CO₂e** | Carbon Dioxide Equivalent - standard unit for measuring carbon footprint |
| **CVA** | Carbon Verification & Audit organization - certifies carbon credits |
| **Emission Factor** | Amount of CO₂ emitted per km driven (varies by vehicle type) |
| **ESG** | Environmental, Social, Governance - corporate sustainability framework |
| **EV** | Electric Vehicle |
| **GMV** | Gross Merchandise Value - total value of transactions on platform |
| **Gold Standard** | Leading carbon offset certification standard |
| **ICE** | Internal Combustion Engine (gasoline/diesel vehicles) |
| **KYC** | Know Your Customer - identity verification process |
| **Marketplace** | Platform where carbon credits are bought and sold |
| **Methodology** | Standardized approach to calculating carbon reductions |
| **OBD-II** | On-Board Diagnostics - vehicle data port |
| **Offset** | Compensating for emissions by funding equivalent reductions elsewhere |
| **PDPA** | Personal Data Protection Act (Vietnam) |
| **Retirement** | Permanent removal of carbon credit from circulation after use |
| **Serial Number** | Unique identifier for each carbon credit unit |
| **Settlement** | Final transfer of funds and credits after transaction |
| **Verra VCS** | Verified Carbon Standard - carbon offset certification program |
| **Vintage** | Year in which CO₂ reduction occurred |
| **Wallet** | Digital account holding user's carbon credits |

---

### Appendix B: Carbon Calculation Methodology

#### Baseline Approach
We use the **Baseline and Monitoring Methodology** following CDM ACM0018 principles.

**Formula:**
```
Emission Reduction (kg CO₂) = Baseline Emissions - Project Emissions

Where:
Baseline Emissions = Distance (km) × Baseline Emission Factor (kg CO₂/km)
Project Emissions = Distance (km) × Project Emission Factor (kg CO₂/km)

For Vietnam:
Baseline Emission Factor (ICE vehicle average) = 0.15 kg CO₂/km
Project Emission Factor (EV, including grid electricity) = 0.05 kg CO₂/km

Net Reduction = 0.10 kg CO₂/km
```

**Example Calculation:**
- EV Owner drives 1,000 km in a month
- Baseline Emissions = 1,000 km × 0.15 = 150 kg CO₂
- Project Emissions = 1,000 km × 0.05 = 50 kg CO₂
- Emission Reduction = 150 - 50 = 100 kg CO₂
- Carbon Credits = 100 / 1,000 = 0.1 ton CO₂

**Data Quality Requirements:**
- GPS accuracy: <50m
- Distance measurement accuracy: ±2%
- Trip completeness: >95% of trips captured
- Data source verification: OEM API (high quality), OBD-II (medium), Manual (requires additional checks)

**Adjustments:**
- **Grid Emission Factor:** Updated annually based on Vietnam electricity grid (EVN data)
- **Vehicle Efficiency:** Adjusted for vehicle model (Tesla Model 3 vs. VinFast VF e34)
- **Charging Source:** Solar charging results in 0 project emissions (requires proof)

---

### Appendix C: Compliance & Certification Standards

#### International Standards Supported

**1. Gold Standard**
- Focus: High-quality carbon credits with sustainable development benefits
- Process: Register project → Validate methodology → Monitor and verify → Issue credits
- Timeline: 3-6 months for project approval
- Cost: $5,000-$15,000 per project
- Platform Approach: Aggregate all EV emissions as one program

**2. Verra VCS (Verified Carbon Standard)**
- Focus: Most widely used voluntary carbon standard
- Process: Similar to Gold Standard
- Timeline: 2-4 months
- Cost: Lower than Gold Standard ($3,000-$10,000)
- Platform Approach: Use VCS for broader market acceptance

**3. ISO 14064**
- Focus: International standard for GHG accounting and verification
- Part 2: Project-level quantification
- Process: Verification by accredited auditor (CVA)
- Platform Approach: CVAs must be ISO 14065 accredited

#### Vietnam-Specific Compliance

**Ministry of Natural Resources & Environment (Bộ TN&MT)**
- Carbon pricing mechanism under development
- Circular 25/2019/TT-BTNMT on GHG inventory
- Platform must align with national methodology if/when released

**Vietnam GHG Inventory**
- Emission factors from national inventory
- Report to UNFCCC every 2 years
- Platform uses official Vietnam emission factors

---

### Appendix D: Sample User Flows

#### Flow 1: EV Owner - First Credit Sale

1. **Registration**
   - User signs up with email/phone
   - Completes KYC Level 1 (ID upload)
   - Receives welcome email

2. **Vehicle Connection**
   - Clicks "Add Vehicle"
   - Enters VIN, make, model
   - Uploads registration document
   - Chooses data source: VinFast API
   - Authorizes API access via OAuth
   - System verifies vehicle ownership

3. **Trip Sync**
   - System automatically syncs trip history (last 30 days)
   - User sees trips on map
   - System calculates CO₂ saved: 50 kg (0.05 tons)

4. **Verification Request**
   - User clicks "Request Verification"
   - System submits to CVA partner
   - User receives notification: "Under review"
   - 24 hours later: "Approved! 0.05 tons added to your wallet"

5. **Create Listing**
   - User navigates to "Sell Credits"
   - Enters amount: 0.05 tons
   - Views AI recommendation: 2.65M VND/ton
   - Sets price: 2.5M VND/ton (total: 125,000 VND)
   - Publishes listing

6. **Sale**
   - 3 days later: Buyer purchases
   - User receives notification: "Your credit sold!"
   - Payment pending (T+2)
   - 2 days later: 118,750 VND credited to wallet (after 5% fee)

7. **Withdrawal**
   - User clicks "Withdraw"
   - Enters bank account details
   - Requests withdrawal: 118,750 VND
   - Fee: 2,375 VND (2%)
   - Net: 116,375 VND
   - Receives bank transfer in 1-2 days

**Total time:** 7-10 days from signup to money in bank

---

#### Flow 2: Corporate Buyer - Purchasing for ESG Report

1. **Company Registration**
   - Sustainability Manager signs up
   - Enters company details, business license
   - Uploads documents
   - Waits for verification (24 hours)

2. **Browse Marketplace**
   - Searches for "TP.HCM" (location filter)
   - Needs 5 tons to offset annual emissions
   - Sorts by price: Low to High
   - Finds listing: 2 tons @ 2.4M VND/ton

3. **Purchase**
   - Clicks "Buy Now"
   - Reviews details
   - Total: 4.8M VND + 240K fee = 5.04M VND
   - Proceeds to checkout

4. **Payment**
   - Selects payment method: Bank Transfer
   - System generates unique reference code
   - User transfers via internet banking
   - Upload transfer receipt
   - System confirms payment (auto or manual)

5. **Certificate**
   - Credits transferred to buyer wallet
   - System generates certificate
   - User downloads PDF
   - Certificate includes:
     - 2 tons CO₂ offset
     - Serial numbers: VN-EV-2025-TUV-000012345, 000012346
     - QR code for verification

6. **ESG Reporting**
   - User includes certificate in annual ESG report
   - Stakeholders can verify certificate authenticity via QR code
   - Company achieves carbon neutral status for Scope 3 emissions (business travel)

**Total time:** 2-3 days from search to certificate

---

### Appendix E: Technical Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         USER LAYER                           │
├──────────────┬──────────────┬──────────────┬────────────────┤
│  Web App     │  Mobile PWA  │  Admin Panel │  Partner APIs  │
│  (React)     │  (React)     │  (React)     │  (REST/GQL)    │
└──────┬───────┴──────┬───────┴──────┬───────┴────────┬───────┘
       │              │              │                │
       └──────────────┴──────────────┴────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     API GATEWAY LAYER                        │
│          (NGINX, Rate Limiting, Load Balancer)               │
└──────────────────────────┬──────────────────────────────────┘
                           │
       ┌───────────────────┼───────────────────┐
       │                   │                   │
┌──────▼──────┐  ┌────────▼────────┐  ┌──────▼──────┐
│   Auth      │  │   Application   │  │   Payment   │
│  Service    │  │     Services    │  │   Service   │
│  (JWT)      │  │                 │  │  (Escrow)   │
└──────┬──────┘  └────────┬────────┘  └──────┬──────┘
       │                  │                   │
       │         ┌────────┴────────┐          │
       │         │                 │          │
┌──────▼─────────▼──────┐  ┌──────▼──────────▼─────┐
│   User Service        │  │  Transaction Service   │
│  - Registration       │  │  - Listings            │
│  - KYC                │  │  - Orders              │
│  - Profile            │  │  - Settlement          │
└───────────┬───────────┘  └─────────┬──────────────┘
            │                        │
┌───────────▼────────────┐  ┌────────▼─────────────┐
│  Vehicle Service       │  │  Verification Service│
│  - Vehicle mgmt        │  │  - CVA workflow      │
│  - Trip sync           │  │  - Credit issuance   │
│  - CO₂ calculation     │  │  - Certificate gen   │
└───────────┬────────────┘  └────────┬─────────────┘
            │                        │
            └────────────┬───────────┘
                         │
┌────────────────────────▼───────────────────────────┐
│                  DATA LAYER                        │
├────────────┬────────────┬────────────┬─────────────┤
│ PostgreSQL │   Redis    │  AWS S3    │  ElasticS   │
│ (Primary)  │  (Cache)   │  (Files)   │  (Search)   │
└────────────┴────────────┴────────────┴─────────────┘

┌──────────────────────────────────────────────────────┐
│              EXTERNAL INTEGRATIONS                   │
├────────┬────────┬──────────┬──────────┬──────────────┤
│VinFast │ Tesla  │  MoMo    │  VNPay   │  Twilio SMS  │
│  API   │  API   │ Gateway  │ Gateway  │              │
└────────┴────────┴──────────┴──────────┴──────────────┘

┌──────────────────────────────────────────────────────┐
│            INFRASTRUCTURE & OPS                      │
├────────┬────────┬──────────┬──────────┬──────────────┤
│  AWS   │ Docker │    K8s   │  Github  │  Datadog     │
│  Cloud │Containers│ Orchestration│ CI/CD │  Monitor   │
└────────┴────────┴──────────┴──────────┴──────────────┘
```

---

### Appendix F: Sample API Specifications

#### POST /api/v1/trips/sync
Sync trip data from vehicle

**Request:**
```json
{
  "vehicle_id": "550e8400-e29b-41d4-a716-446655440000",
  "data_source": "vinfast_api",
  "date_range": {
    "start": "2025-10-01",
    "end": "2025-10-25"
  }
}
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "trips_synced": 47,
    "total_distance_km": 892.5,
    "co2_saved_kg": 89.25,
    "carbon_credits_tons": 0.089,
    "sync_id": "sync_12345"
  }
}
```

---

#### GET /api/v1/marketplace/listings
Search for carbon credit listings

**Query Parameters:**
- `region`: string (optional)
- `min_amount`: number (optional)
- `max_amount`: number (optional)
- `min_price`: number (optional)
- `max_price`: number (optional)
- `listing_type`: enum[fixed, auction] (optional)
- `page`: number (default: 1)
- `limit`: number (default: 20)

**Response:**
```json
{
  "status": "success",
  "data": {
    "listings": [
      {
        "id": "listing_001",
        "seller": {
          "id": "user_123",
          "name": "Nguyen Van A",
          "rating": 4.8
        },
        "amount_tons": 1.5,
        "price_per_ton": 2500000,
        "total_price": 3750000,
        "listing_type": "fixed",
        "region": "TP.HCM",
        "created_at": "2025-10-20T10:30:00Z",
        "status": "active"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 157,
      "total_pages": 8
    }
  }
}
```

---

## DOCUMENT APPROVAL

| Role | Name | Signature | Date |
|------|------|-----------|------|
| **Product Owner** | [Name] | ____________ | ________ |
| **Tech Lead** | [Name] | ____________ | ________ |
| **Business Sponsor** | [Name] | ____________ | ________ |
| **Compliance Officer** | [Name] | ____________ | ________ |

---

## DOCUMENT REVISION HISTORY

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 0.1 | 2025-10-15 | BA Team | Initial draft |
| 0.5 | 2025-10-20 | BA Team | Stakeholder feedback incorporated |
| 1.0 | 2025-10-25 | BA Team | Final version for approval |

---

**END OF DOCUMENT**

*This Business Analysis Document is confidential and proprietary. Distribution outside the project team requires approval from the Product Owner.*