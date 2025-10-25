# User Stories & Use Cases
## Carbon Credit Marketplace for EV Owners

**Version**: 1.0  
**Last Updated**: October 25, 2025

---

## Table of Contents

1. [Epic 1: Carbon Credit Generation](#epic-1-carbon-credit-generation)
2. [Epic 2: Marketplace Trading](#epic-2-marketplace-trading)
3. [Epic 3: Verification & Compliance](#epic-3-verification--compliance)
4. [Epic 4: Platform Administration](#epic-4-platform-administration)
5. [Epic 5: User Account Management](#epic-5-user-account-management)
6. [Epic 6: Payments & Withdrawals](#epic-6-payments--withdrawals)
7. [Epic 7: Analytics & Reporting](#epic-7-analytics--reporting)
8. [Use Cases](#use-cases)

---

## Story Point Legend

| Points | Complexity | Effort |
|--------|-----------|--------|
| 1 | Trivial | < 2 hours |
| 3 | Simple | 1-2 days |
| 5 | Medium | 3-5 days |
| 8 | Complex | 1-2 weeks |
| 13 | Very Complex | 2-3 weeks |
| 21 | Epic | Break down further |

---

## Epic 1: Carbon Credit Generation

### US-001: Connect Vehicle with OEM API
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
**Dependencies:** Vehicle registration, OAuth integration  
**Technical Notes:** OAuth 2.0 flow, handle token refresh, implement retry logic

---

### US-002: View Real-time CO₂ Savings
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
**Dependencies:** Trip data sync, CO₂ calculation engine  
**UI/UX Notes:** Use engaging visualizations, green color scheme

---

### US-003: Submit Credits for Verification
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
**Dependencies:** CO₂ calculation, CVA workflow  

---

### US-004: Manual Trip Data Upload
**As an** EV owner without API access  
**I want to** manually upload my trip data  
**So that** I can still participate in the marketplace

**Acceptance Criteria:**
- Given I don't have API access to my vehicle
- When I upload a CSV file with trip data
- Then the system validates the data format and content
- And I see a summary of trips imported
- And invalid rows are flagged with error messages
- And I can correct errors and re-upload

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** File upload, data validation  
**Technical Notes:** Support CSV and JSON formats, provide template download

---

### US-005: Connect OBD-II Device
**As an** EV owner with an OBD-II capable vehicle  
**I want to** connect an OBD-II device for automatic data collection  
**So that** I have a reliable backup to OEM APIs

**Acceptance Criteria:**
- Given I have an OBD-II device
- When I pair it via Bluetooth using the mobile app
- Then trip data is automatically collected and synced
- And I can see device connection status
- And I receive alerts if device disconnects

**Priority:** SHOULD HAVE  
**Story Points:** 8  
**Dependencies:** Mobile app, Bluetooth integration  

---

### US-006: View Trip History
**As an** EV owner  
**I want to** view my complete trip history  
**So that** I can verify the data used for carbon credit calculation

**Acceptance Criteria:**
- Given I have synced trip data
- When I navigate to Trip History page
- Then I see a list of all trips with date, distance, duration
- And I can filter by date range
- And I can view individual trip details on a map
- And I can export trip data to CSV

**Priority:** SHOULD HAVE  
**Story Points:** 5  
**Dependencies:** Trip data sync, maps integration  

---

## Epic 2: Marketplace Trading

### US-010: Get AI-Powered Price Recommendation
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
**Dependencies:** ML model, historical pricing data  
**Technical Notes:** Train model on historical transaction data, refresh daily

---

### US-011: Search for Carbon Credits
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
**Dependencies:** Search engine (Elasticsearch recommended)  

---

### US-012: Participate in Auctions
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
**Dependencies:** Real-time bidding system, notification service  
**Technical Notes:** Use WebSocket for real-time bid updates

---

### US-013: Create Fixed Price Listing
**As an** EV owner  
**I want to** list my carbon credits at a fixed price  
**So that** buyers can purchase them immediately

**Acceptance Criteria:**
- Given I have verified carbon credits
- When I create a fixed-price listing
- Then I specify amount, price per ton, and description
- And the listing appears in marketplace within 5 minutes
- And I can edit price before any purchase
- And unsold listings auto-expire after 90 days

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** Verification status, wallet balance  

---

### US-014: Create Auction Listing
**As an** EV owner  
**I want to** create an auction for my carbon credits  
**So that** I can potentially get higher prices through competitive bidding

**Acceptance Criteria:**
- Given I have verified carbon credits ≥1 ton
- When I create an auction listing
- Then I set starting price, optional reserve, and duration (1-30 days)
- And bidders can place bids with minimum 50,000 VND increment
- And auction auto-extends 10 min if bid in last 5 min
- And highest bidder wins and has 24h to pay

**Priority:** SHOULD HAVE  
**Story Points:** 8  
**Dependencies:** Auction engine, real-time notifications  

---

### US-015: Add Credits to Cart
**As a** corporate buyer  
**I want to** add multiple listings to my cart  
**So that** I can purchase from multiple sellers in one transaction

**Acceptance Criteria:**
- Given I am browsing marketplace listings
- When I click "Add to Cart" on multiple listings
- Then items are added to my cart
- And I see total amount and price
- And I can remove items or adjust quantities
- And items are reserved for 30 minutes

**Priority:** SHOULD HAVE  
**Story Points:** 5  
**Dependencies:** Session management, inventory reservation  

---

### US-016: View Listing Details
**As a** corporate buyer  
**I want to** view detailed information about a listing  
**So that** I can make informed purchase decisions

**Acceptance Criteria:**
- Given I am viewing a listing
- When I open the listing details page
- Then I see credit amount, price, seller rating, region
- And I see verification status and CVA organization
- And I see calculation methodology and vintage year
- And I can contact seller via in-app messaging
- And I can download verification documents

**Priority:** MUST HAVE  
**Story Points:** 3  
**Dependencies:** Listing data, document storage  

---

### US-017: Save Search & Get Alerts
**As a** corporate buyer  
**I want to** save my search criteria and receive alerts  
**So that** I'm notified when matching listings appear

**Acceptance Criteria:**
- Given I have performed a search with specific filters
- When I click "Save Search"
- Then my search criteria are saved
- And I receive email notifications for new matching listings
- And I can manage saved searches in my account
- And I can set notification frequency (instant, daily, weekly)

**Priority:** COULD HAVE  
**Story Points:** 5  
**Dependencies:** Search service, notification service  

---

## Epic 3: Verification & Compliance

### US-020: Review Verification Requests
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
**Dependencies:** Verification queue, data analysis tools  

---

### US-021: Receive Carbon Credit Certificate
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
**Dependencies:** Certificate generation service, PDF rendering  

---

### US-022: Assign Verification Request
**As a** CVA supervisor  
**I want to** assign verification requests to auditors  
**So that** workload is balanced and SLAs are met

**Acceptance Criteria:**
- Given there are unassigned verification requests
- When I view the verification queue
- Then I can manually assign requests to auditors
- And I can set up auto-assignment rules
- And I can see each auditor's current workload
- And high-priority requests are flagged

**Priority:** SHOULD HAVE  
**Story Points:** 5  
**Dependencies:** User management, workflow engine  

---

### US-023: Issue Carbon Credits
**As a** CVA auditor  
**I want to** issue verified carbon credits to EV owner wallets  
**So that** they can start selling

**Acceptance Criteria:**
- Given I have approved a verification request
- When I click "Issue Credits"
- Then credits are minted with unique serial numbers
- And credits are transferred to owner's wallet
- And owner receives notification with issuance certificate
- And credits appear in the master registry

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** Credit minting system, wallet service  

---

### US-024: Generate Audit Report
**As a** CVA organization  
**I want to** generate compliance and audit reports  
**So that** I can maintain our accreditation and demonstrate transparency

**Acceptance Criteria:**
- Given I need to submit a regulatory report
- When I generate an audit report for a date range
- Then I receive a comprehensive report with all verifications
- And the report includes statistics and quality metrics
- And I can export in ISO 14064, Gold Standard, or Verra formats
- And all logs are tamper-evident

**Priority:** MUST HAVE  
**Story Points:** 8  
**Dependencies:** Audit trail, report generation service  

---

### US-025: Verify Certificate Authenticity
**As a** third party (e.g., investor, regulator)  
**I want to** verify the authenticity of a carbon credit certificate  
**So that** I can confirm its legitimacy

**Acceptance Criteria:**
- Given I have a certificate ID or QR code
- When I enter it on the public verification portal
- Then I see the certificate's validity status
- And I see basic transaction information (anonymized)
- And I see the CVA organization that verified it
- And I see if the certificate has been revoked

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** Public API, certificate database  

---

## Epic 4: Platform Administration

### US-030: Monitor Transactions in Real-time
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
**Dependencies:** Real-time data pipeline, admin permissions  

---

### US-031: Manage User Accounts
**As an** admin  
**I want to** manage user accounts and resolve issues  
**So that** users have smooth experience on the platform

**Acceptance Criteria:**
- Given I am reviewing user accounts
- When I search for a user
- Then I see their complete profile and activity
- And I can reset their password
- And I can suspend or unsuspend their account
- And I can upgrade/downgrade KYC level
- And all actions are logged

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** User database, audit logging  

---

### US-032: Review KYC Documents
**As an** admin  
**I want to** review and approve KYC documents  
**So that** users can access platform features

**Acceptance Criteria:**
- Given there are pending KYC submissions
- When I review a KYC request
- Then I can view uploaded documents (ID, business license, etc.)
- And I can zoom, rotate, and enhance images
- And I can approve or reject with reason
- And user is notified of the decision
- And processing completes within SLA (24h for Level 1)

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** Document storage, OCR (optional)  

---

### US-033: Detect and Prevent Fraud
**As an** admin  
**I want to** detect suspicious activities and potential fraud  
**So that** I can protect the platform and users

**Acceptance Criteria:**
- Given the fraud detection system is active
- When suspicious activity is detected
- Then I receive an alert with details
- And I can review user history and transaction patterns
- And I can flag accounts for manual review
- And I can suspend accounts temporarily
- And I can add users/devices to blacklist

**Priority:** MUST HAVE  
**Story Points:** 13  
**Dependencies:** ML-based fraud detection, alerting system  
**Technical Notes:** Anomaly detection, velocity checks, pattern matching

---

### US-034: Process Settlements
**As an** admin  
**I want to** process batch settlements to sellers  
**So that** they receive their earnings on time

**Acceptance Criteria:**
- Given there are completed transactions awaiting settlement
- When I run the daily settlement batch
- Then all eligible payouts are processed
- And sellers receive funds within T+2 days
- And failed settlements are flagged for retry
- And reconciliation report is generated

**Priority:** MUST HAVE  
**Story Points:** 8  
**Dependencies:** Payment integration, transaction database  

---

### US-035: View Platform Analytics
**As an** admin/executive  
**I want to** view comprehensive platform analytics  
**So that** I can make data-driven business decisions

**Acceptance Criteria:**
- Given I access the analytics dashboard
- When I view the executive overview
- Then I see key metrics: users, GMV, transactions, CO₂ offset
- And I see growth trends and MoM/YoY comparisons
- And I can drill down into user, marketplace, or operational metrics
- And I can export reports to PDF/Excel
- And data is refreshed every 15 minutes

**Priority:** MUST HAVE  
**Story Points:** 13  
**Dependencies:** Data warehouse, BI tools  

---

### US-036: Manage Platform Configuration
**As an** admin  
**I want to** manage platform settings and configurations  
**So that** I can adjust operations without code changes

**Acceptance Criteria:**
- Given I have super admin privileges
- When I access platform configuration
- Then I can adjust fees (transaction, listing, withdrawal)
- And I can set limits (daily withdrawal, listing maximum)
- And I can enable/disable features via feature flags
- And I can configure emission factors for calculation
- And changes are logged and can be reverted

**Priority:** SHOULD HAVE  
**Story Points:** 5  
**Dependencies:** Configuration management system  

---

## Epic 5: User Account Management

### US-040: Register as EV Owner
**As a** potential user  
**I want to** register as an EV owner  
**So that** I can start earning from carbon credits

**Acceptance Criteria:**
- Given I am on the registration page
- When I provide email/phone and create password
- Then I receive OTP for verification
- And I complete basic profile (name, ID number)
- And I can optionally register via Google/Facebook/Apple
- And my account is created within 2 minutes

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** Authentication service, OTP provider  

---

### US-041: Register as Corporate Buyer
**As a** company representative  
**I want to** register my company as a buyer  
**So that** I can purchase carbon credits

**Acceptance Criteria:**
- Given I am registering a company account
- When I provide company details and upload documents
- Then my registration is submitted for verification
- And I receive confirmation email
- And I'm notified when verification is complete (within 24-48h)
- And I can add team members with different roles

**Priority:** MUST HAVE  
**Story Points:** 8  
**Dependencies:** Business verification service  

---

### US-042: Complete KYC Verification
**As a** user  
**I want to** complete KYC verification  
**So that** I can access full platform features

**Acceptance Criteria:**
- Given I need higher withdrawal limits
- When I submit KYC Level 2 documents
- Then I upload ID, selfie, and address proof
- And documents are reviewed within 5 business days
- And I'm notified of approval or rejection with reason
- And approved users get increased limits

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** Document verification workflow  

---

### US-043: Manage Profile Settings
**As a** user  
**I want to** manage my profile and settings  
**So that** I can keep my information up-to-date

**Acceptance Criteria:**
- Given I am logged in
- When I access profile settings
- Then I can update personal information
- And I can change password (requires current password)
- And I can enable/disable 2FA
- And I can manage notification preferences
- And I can add/remove bank accounts
- And sensitive changes require re-authentication

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** User management service  

---

### US-044: Reset Forgotten Password
**As a** user who forgot my password  
**I want to** reset my password securely  
**So that** I can regain access to my account

**Acceptance Criteria:**
- Given I forgot my password
- When I click "Forgot Password"
- Then I receive a password reset link via email
- And the link expires after 1 hour
- And I create a new password meeting requirements
- And I'm automatically logged in after reset
- And all sessions except current are terminated

**Priority:** MUST HAVE  
**Story Points:** 3  
**Dependencies:** Email service, authentication system  

---

## Epic 6: Payments & Withdrawals

### US-050: Purchase Credits with E-Wallet
**As a** corporate buyer  
**I want to** pay for carbon credits using e-wallet  
**So that** I can complete purchases conveniently

**Acceptance Criteria:**
- Given I am at checkout
- When I select MoMo/VNPay/ZaloPay
- Then I'm redirected to payment gateway
- And I complete payment in the e-wallet app
- And I'm redirected back to platform
- And transaction is marked as paid
- And credits are transferred to my wallet

**Priority:** MUST HAVE  
**Story Points:** 8  
**Dependencies:** Payment gateway integration, escrow system  

---

### US-051: Purchase with Bank Transfer
**As a** corporate buyer  
**I want to** pay via bank transfer  
**So that** I can use my company's preferred payment method

**Acceptance Criteria:**
- Given I choose bank transfer at checkout
- When I proceed
- Then I receive a unique virtual account number or reference code
- And I see bank details to transfer to
- And payment is auto-reconciled when received
- And I receive confirmation within 15 minutes of bank confirmation

**Priority:** MUST HAVE  
**Story Points:** 8  
**Dependencies:** Napas integration, reconciliation service  

---

### US-052: Withdraw Earnings
**As an** EV owner  
**I want to** withdraw my earnings to my bank account  
**So that** I can access my money

**Acceptance Criteria:**
- Given I have sufficient balance (min 500,000 VND)
- When I request withdrawal
- Then I complete 2FA authentication
- And I select or add bank account
- And I specify amount (max 50M VND/day)
- And I see fee and net amount
- And withdrawal is processed within 2 business days
- And I receive confirmation email

**Priority:** MUST HAVE  
**Story Points:** 8  
**Dependencies:** Banking integration, 2FA system  

---

### US-053: Request Refund
**As a** buyer  
**I want to** request a refund for a disputed transaction  
**So that** I can recover my payment if there's an issue

**Acceptance Criteria:**
- Given I have a completed transaction
- When I request a refund
- Then I select reason from predefined list
- And I provide explanation (optional)
- And request is submitted to admin
- And I'm notified of decision within 5 business days
- And approved refunds are processed within 7 days

**Priority:** SHOULD HAVE  
**Story Points:** 5  
**Dependencies:** Dispute resolution workflow  

---

### US-054: View Transaction History
**As a** user  
**I want to** view my complete transaction history  
**So that** I can track my purchases/sales

**Acceptance Criteria:**
- Given I am logged in
- When I navigate to Transaction History
- Then I see all my transactions (purchases and sales)
- And I can filter by date range, type, status
- And I can search by transaction ID
- And I can export to CSV/PDF
- And I can view detailed receipts

**Priority:** MUST HAVE  
**Story Points:** 5  
**Dependencies:** Transaction database  

---

## Epic 7: Analytics & Reporting

### US-060: View Environmental Impact Dashboard
**As an** EV owner  
**I want to** see my environmental impact  
**So that** I feel rewarded for my contribution

**Acceptance Criteria:**
- Given I have driven my EV
- When I view impact dashboard
- Then I see total CO₂ saved with equivalents (trees, flights)
- And I see monthly trend chart
- And I see comparison with average EV owner
- And I can share infographic on social media

**Priority:** SHOULD HAVE  
**Story Points:** 5  
**Dependencies:** Data visualization library  

---

### US-061: Generate Financial Report
**As an** EV owner  
**I want to** generate financial performance reports  
**So that** I can track my earnings

**Acceptance Criteria:**
- Given I have sold carbon credits
- When I generate financial report
- Then I see total revenue, avg price per ton, # transactions
- And I see monthly earnings trend
- And I can export to PDF/Excel
- And report includes tax information

**Priority:** SHOULD HAVE  
**Story Points:** 5  
**Dependencies:** Reporting engine  

---

### US-062: Download ESG Report
**As a** corporate buyer  
**I want to** generate ESG compliance reports  
**So that** I can meet regulatory requirements

**Acceptance Criteria:**
- Given I have purchased carbon credits
- When I generate ESG report
- Then I can select format (GHG Protocol, ISO 14064, custom)
- And report includes all purchases with certificates
- And I can specify date range (quarter, year, custom)
- And report is professionally formatted with charts
- And I can schedule automated quarterly reports

**Priority:** MUST HAVE  
**Story Points:** 8  
**Dependencies:** Report templates, scheduling service  

---

### US-063: View Portfolio Breakdown
**As a** corporate buyer  
**I want to** view my carbon credit portfolio breakdown  
**So that** I can understand my offsetting composition

**Acceptance Criteria:**
- Given I have purchased credits
- When I view portfolio page
- Then I see breakdown by vintage year, region, verification body
- And I see cumulative offset over time
- And I see cost per ton trends
- And I can compare against my offsetting goals

**Priority:** SHOULD HAVE  
**Story Points:** 5  
**Dependencies:** Data aggregation  

---

## Use Cases

## Use Case 1: Complete Transaction Flow (Fixed Price)

**Use Case ID:** UC-001  
**Use Case Name:** Purchase Carbon Credits - Fixed Price  
**Actor:** Corporate Buyer  
**Preconditions:**  
- Buyer has registered and verified account (KYC Level 1)
- Seller has listed carbon credits at fixed price
- Credits are verified by CVA
- Buyer has sufficient funds/payment method

**Main Success Scenario:**

1. Buyer searches for carbon credits filtering by region "TP.HCM"
2. System displays matching listings sorted by relevance
3. Buyer selects a listing (1.5 tons @ 2,500,000 VND/ton)
4. Buyer views listing details including verification documents
5. Buyer clicks "Buy Now"
6. System calculates total:
   - Subtotal: 1.5 × 2,500,000 = 3,750,000 VND
   - Platform fee (5%): 187,500 VND
   - **Total: 3,937,500 VND**
7. Buyer proceeds to checkout
8. Buyer enters billing information (if not saved)
9. Buyer selects payment method (MoMo e-wallet)
10. System redirects to MoMo payment gateway
11. Buyer authenticates and confirms payment in MoMo app
12. MoMo processes payment successfully
13. System receives payment confirmation via webhook
14. System moves funds to escrow account
15. System transfers 1.5 ton credits from seller wallet to buyer wallet
16. System generates certificate with unique ID and QR code
17. System releases payment to seller (3,750,000 - 187,500 = 3,562,500 VND)
18. System sends confirmation emails to both parties with:
    - Buyer: Certificate PDF, transaction receipt
    - Seller: Payment confirmation, net earnings
19. Transaction status updated to "Completed"

**Alternative Flows:**

**3a. Listing No Longer Available**
- 3a1. System shows "Sold Out" message
- 3a2. System suggests similar listings
- 3a3. Buyer can save search for notifications

**9a. Payment Fails**
- 9a1. System returns to checkout with error message
- 9a2. Credits remain in seller's wallet (not locked)
- 9a3. Buyer can retry with different payment method

**11a. Buyer Cancels Payment**
- 11a1. MoMo returns cancel status
- 11a2. System returns to checkout
- 11a3. Listing remains available for other buyers

**14a. Credit Transfer Fails**
- 14a1. System automatically refunds buyer via original payment method
- 14a2. System sends notification to admin for investigation
- 14a3. Credits remain with seller

**Postconditions:**
- Credits successfully transferred to buyer wallet
- Payment released to seller (minus platform fee)
- Certificate issued and available for download
- Transaction recorded in both parties' history
- Platform fee collected
- All events logged in audit trail

**Performance Requirements:**
- Total transaction time: <3 minutes
- Payment confirmation: <30 seconds after successful payment
- Certificate generation: <10 seconds

---

## Use Case 2: Auction Flow

**Use Case ID:** UC-002  
**Use Case Name:** Win and Purchase Carbon Credits - Auction  
**Actor:** Corporate Buyer  
**Preconditions:**  
- Buyer has registered and verified account
- Seller has created auction listing
- Auction is active (not ended)

**Main Success Scenario:**

1. Buyer browses marketplace and finds active auction
2. Buyer views auction details:
   - Current bid: 2,200,000 VND/ton
   - Minimum increment: 50,000 VND
   - Time remaining: 2 hours 15 minutes
   - Number of bids: 5
3. Buyer decides to bid 2,300,000 VND/ton
4. System validates bid (meets minimum increment)
5. System records bid with timestamp
6. System updates auction display with new current bid
7. System notifies previous highest bidder they've been outbid
8. [30 minutes later] Another bidder bids 2,400,000 VND/ton
9. System sends real-time notification to Buyer that they've been outbid
10. Buyer sets max auto-bid amount: 2,800,000 VND/ton
11. System automatically increments Buyer's bid to 2,450,000 VND/ton
12. [2 hours later] Auction ends with Buyer as winner at 2,450,000 VND/ton
13. System sends winner notification to Buyer
14. System gives Buyer 24 hours to complete payment
15. Buyer completes payment within 6 hours
16. System proceeds with standard transaction flow (as in UC-001 steps 14-18)

**Alternative Flows:**

**3a. Bid Below Minimum Increment**
- 3a1. System shows error message
- 3a2. System displays required minimum bid
- 3a3. Buyer adjusts bid amount

**12a. Bid in Last 5 Minutes**
- 12a1. System extends auction by 10 minutes
- 12a2. System notifies all watchers of extension
- 12a3. Auction continues until no bids in last 5 minutes

**15a. Winner Doesn't Pay Within 24 Hours**
- 15a1. System cancels winner's bid
- 15a2. System offers credits to second-highest bidder
- 15a3. System applies penalty to non-paying winner (reduce trust score)

**15b. Payment Fails**
- 15b1. System allows retry within 24-hour window
- 15b2. If continues to fail, follows alternative flow 15a

**Postconditions:**
- Same as UC-001
- Auction status updated to "Completed"
- All bids logged in history

---

## Use Case 3: Verification Flow

**Use Case ID:** UC-003  
**Use Case Name:** Verify Carbon Credits  
**Actor:** CVA Auditor  
**Preconditions:**  
- EV owner has submitted verification request
- Auditor has CVA role and appropriate permissions
- Trip data is available for review

**Main Success Scenario:**

1. Auditor logs into CVA portal
2. Auditor views pending verification queue
3. System auto-assigns next high-priority request to Auditor
4. Auditor opens verification request REQ-12345
5. System displays:
   - EV owner details and KYC status
   - Vehicle information (VinFast VF8, VIN: VIN123...)
   - Trip data: 45 trips, 1,250 km total, Jan 1-31, 2025
   - Data source: VinFast API (verified)
   - Calculated CO₂ saved: 125 kg (0.125 tons)
6. Auditor downloads raw trip data CSV
7. Auditor runs data validation tool:
   - Check 1: GPS coordinates valid ✓
   - Check 2: Distance vs energy ratio reasonable ✓
   - Check 3: No duplicate trips ✓
   - Check 4: Trip patterns match expected behavior ✓
8. Auditor spot-checks 10% of trips (5 random trips)
9. Auditor views trips on map to validate routes
10. Auditor recalculates CO₂ using standard formula
11. System confirms calculation matches within ±2% ✓
12. Auditor marks request as "Approved"
13. Auditor adds note: "All checks passed. Data quality: Excellent"
14. System triggers credit issuance:
    - Generates serial number: VN-EV-2025-TUV-000012345
    - Mints 0.125 tons of credits
    - Transfers to EV owner's wallet
15. System generates issuance certificate (PDF)
16. System sends notification to EV owner with certificate
17. System logs all actions in immutable audit trail
18. Verification status updated to "Approved"

**Alternative Flows:**

**7a. Data Validation Finds Anomalies**
- 7a1. System flags outlier trips (e.g., 500 km trip in 1 hour)
- 7a2. Auditor reviews flagged trips manually
- 7a3. If explainable (e.g., highway trip), auditor proceeds
- 7a4. If suspicious, auditor requests more info from owner

**11a. Calculation Variance >2%**
- 11a1. Auditor investigates discrepancy
- 11a2. Auditor recalculates manually
- 11a3. If owner's calculation wrong, auditor adjusts amount
- 11a4. System notifies owner of adjusted amount

**12a. Auditor Rejects Request**
- 12a1. Auditor selects rejection reason (e.g., "Insufficient data quality")
- 12a2. Auditor provides detailed explanation
- 12a3. System sends rejection notification to owner
- 12a4. Owner can fix issues and resubmit

**12b. Auditor Requests More Information**
- 12b1. Auditor sends query to owner via in-app messaging
- 12b2. Request status changed to "Awaiting Owner Response"
- 12b3. Owner provides additional documents/explanation
- 12b4. Auditor reviews and proceeds with approval/rejection

**Postconditions:**
- Verification request status updated
- If approved: Credits issued to owner wallet
- If rejected: Owner notified with reason
- All actions logged in audit trail
- Auditor performance metrics updated

**Performance Requirements:**
- Verification completion: <3 hours for standard requests
- 80% of requests completed within 48 hours (SLA)
- Data validation tools run in <30 seconds

---

## Use Case 4: Withdrawal Flow

**Use Case ID:** UC-004  
**Use Case Name:** Withdraw Earnings to Bank Account  
**Actor:** EV Owner  
**Preconditions:**  
- Owner has sold carbon credits
- Available balance ≥ 500,000 VND (minimum)
- Owner has completed KYC Level 1
- Owner has added and verified bank account

**Main Success Scenario:**

1. Owner navigates to Earnings page
2. Owner sees available balance: 5,750,000 VND
3. Owner clicks "Withdraw"
4. System prompts for 2FA authentication
5. Owner enters 2FA code from authenticator app
6. System validates 2FA code ✓
7. Owner selects previously verified bank account:
   - Bank: Vietcombank
   - Account: 1234567890
   - Name: NGUYEN VAN A
8. Owner enters withdrawal amount: 5,000,000 VND
9. System calculates and displays:
   - Withdrawal amount: 5,000,000 VND
   - Fee (2%): 100,000 VND
   - **Net amount to receive: 4,900,000 VND**
   - Remaining balance: 750,000 VND
10. Owner reviews and confirms
11. System creates withdrawal request WD-67890
12. System deducts amount from available balance
13. System queues withdrawal for processing
14. System sends confirmation email with request ID
15. [Within 4 hours] Admin reviews and approves withdrawal
16. System initiates bank transfer via banking partner API
17. [Next business day] Bank processes transfer
18. System receives transfer confirmation
19. System updates withdrawal status to "Completed"
20. System sends completion email to Owner
21. Owner receives money in bank account

**Alternative Flows:**

**5a. 2FA Code Invalid**
- 5a1. System shows error
- 5a2. Owner can retry (max 3 attempts)
- 5a3. After 3 failures, account temporarily locked for 30 minutes

**8a. Amount Below Minimum**
- 8a1. System shows error "Minimum withdrawal: 500,000 VND"
- 8a2. Owner adjusts amount

**8b. Amount Exceeds Daily Limit**
- 8b1. System shows error "Daily limit: 50,000,000 VND"
- 8b2. Owner adjusts amount or waits for next day

**8c. Amount Exceeds Available Balance**
- 8c1. System shows error with available balance
- 8c2. Owner adjusts amount

**15a. Admin Flags Withdrawal as Suspicious**
- 15a1. Admin requests additional verification
- 15a2. Owner receives email to provide proof
- 15a3. Owner uploads requested documents
- 15a4. Admin reviews and approves/rejects

**17a. Bank Transfer Fails**
- 17a1. System receives failure notification from bank
- 17a2. System automatically refunds amount to Owner's wallet (within 1 hour)
- 17a3. System sends notification explaining failure reason
- 17a4. Owner can retry with corrected information

**Postconditions:**
- If successful: Money in owner's bank account
- Withdrawal request logged in transaction history
- Available balance updated
- Platform fee collected
- Tax records generated (if applicable)

**Performance Requirements:**
- 2FA validation: <2 seconds
- Withdrawal request creation: <5 seconds
- Admin review: <4 hours (business hours)
- Total processing time: T+2 business days

---

## Story Dependencies Graph

```
US-001 (Connect Vehicle)
  ↓
US-002 (View CO₂)
  ↓
US-003 (Submit Verification)
  ↓
US-020 (CVA Review) → US-023 (Issue Credits)
  ↓                         ↓
US-013 (Create Listing) ← ←
  ↓
US-011 (Search Credits)
  ↓
US-016 (View Details)
  ↓
US-050 (Purchase)
  ↓
US-021 (Receive Certificate)
```

---

## Acceptance Testing Checklist

### For Each User Story:
- [ ] Acceptance criteria written in Given-When-Then format
- [ ] Happy path tested
- [ ] All alternative flows identified and tested
- [ ] Edge cases covered
- [ ] Performance requirements defined and tested
- [ ] Security requirements validated
- [ ] Accessibility tested (WCAG 2.1 AA)
- [ ] Mobile responsiveness verified
- [ ] Integration points tested
- [ ] Error messages user-friendly
- [ ] Logging and monitoring in place

---

**Document Version**: 1.0  
**Last Updated**: October 25, 2025  
**Total Stories**: 50+  
**Total Story Points**: ~300  
**Estimated Development Time**: 6-8 months (team of 8-10)

