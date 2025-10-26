# Payment Webhook Testing with ngrok

This guide explains how to set up and use ngrok for testing payment webhooks in your local development environment.

## Table of Contents
- [Why ngrok?](#why-ngrok)
- [Installation](#installation)
- [Basic Setup](#basic-setup)
- [Configuration for Carbon Credit Marketplace](#configuration-for-carbon-credit-marketplace)
- [Testing Webhooks](#testing-webhooks)
- [Security Considerations](#security-considerations)
- [Troubleshooting](#troubleshooting)

## Why ngrok?

Payment gateways (MoMo, VNPay, Stripe, etc.) need to send webhook notifications to your application when payment events occur. During local development, your application runs on localhost and is not accessible from the internet. ngrok creates a secure tunnel from a public URL to your local machine, allowing external services to reach your local development server.

## Installation

### Windows
```powershell
# Using Chocolatey
choco install ngrok

# Or download directly from https://ngrok.com/download
# Extract and add to PATH
```

### macOS
```bash
# Using Homebrew
brew install ngrok/ngrok/ngrok

# Or download directly from https://ngrok.com/download
```

### Linux
```bash
# Using snap
snap install ngrok

# Or download the binary
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin
```

### Authentication (Required for v3+)
1. Sign up for a free account at https://ngrok.com
2. Get your auth token from the dashboard
3. Configure ngrok:
```bash
ngrok config add-authtoken YOUR_AUTH_TOKEN_HERE
```

## Basic Setup

### 1. Start the Payment Service
```bash
# Using Docker
docker-compose up payment-service

# Or run locally
cd backend/payment-service
mvn spring-boot:run
```

### 2. Start ngrok Tunnel
```bash
# Create tunnel to payment service (port 8087 for Docker, 8085 for local)
ngrok http 8087

# Or with a subdomain (requires paid plan)
ngrok http 8087 --subdomain=carbon-payment
```

### 3. Note the Public URL
ngrok will display output like:
```
Session Status                online
Account                       your-email@example.com
Version                       3.3.5
Region                        United States (us)
Forwarding                    https://abc123def456.ngrok-free.app -> http://localhost:8087
```

The `https://abc123def456.ngrok-free.app` is your public webhook URL.

## Configuration for Carbon Credit Marketplace

### Option 1: Using Environment Variables (Recommended)

Create a `.env` file in the project root:
```bash
# Webhook Configuration for ngrok
WEBHOOK_BASE_URL=https://abc123def456.ngrok-free.app
MOMO_WEBHOOK_URL=https://abc123def456.ngrok-free.app/api/v1/webhooks/momo
VNPAY_IPN_URL=https://abc123def456.ngrok-free.app/api/v1/webhooks/vnpay
BANK_WEBHOOK_URL=https://abc123def456.ngrok-free.app/api/v1/webhooks/bank-transfer
```

Start services with environment file:
```bash
# Windows PowerShell
docker-compose --env-file .env up payment-service

# Linux/macOS
docker-compose --env-file .env up payment-service
```

### Option 2: Direct Environment Variables

```bash
# Windows PowerShell
$env:WEBHOOK_BASE_URL="https://abc123def456.ngrok-free.app"
$env:MOMO_WEBHOOK_URL="https://abc123def456.ngrok-free.app/api/v1/webhooks/momo"
$env:VNPAY_IPN_URL="https://abc123def456.ngrok-free.app/api/v1/webhooks/vnpay"
$env:BANK_WEBHOOK_URL="https://abc123def456.ngrok-free.app/api/v1/webhooks/bank-transfer"
docker-compose up payment-service

# Linux/macOS
export WEBHOOK_BASE_URL="https://abc123def456.ngrok-free.app"
export MOMO_WEBHOOK_URL="https://abc123def456.ngrok-free.app/api/v1/webhooks/momo"
export VNPAY_IPN_URL="https://abc123def456.ngrok-free.app/api/v1/webhooks/vnpay"
export BANK_WEBHOOK_URL="https://abc123def456.ngrok-free.app/api/v1/webhooks/bank-transfer"
docker-compose up payment-service
```

### Option 3: Update docker-compose.yml Directly

Temporarily update the webhook URLs in `docker-compose.yml`:
```yaml
payment-service:
  environment:
    WEBHOOK_BASE_URL: https://abc123def456.ngrok-free.app
    MOMO_WEBHOOK_URL: https://abc123def456.ngrok-free.app/api/v1/webhooks/momo
    VNPAY_IPN_URL: https://abc123def456.ngrok-free.app/api/v1/webhooks/vnpay
    BANK_WEBHOOK_URL: https://abc123def456.ngrok-free.app/api/v1/webhooks/bank-transfer
```

## Testing Webhooks

### 1. Configure Payment Gateway
Update your payment gateway settings with the ngrok URLs:

#### MoMo Test Environment
- Log into MoMo Partner Portal (test environment)
- Navigate to Settings > Webhook Configuration
- Set IPN URL: `https://abc123def456.ngrok-free.app/api/v1/webhooks/momo`

#### VNPay Sandbox
- Access VNPay Merchant Portal (sandbox)
- Go to Configuration > IPN Settings
- Set IPN URL: `https://abc123def456.ngrok-free.app/api/v1/webhooks/vnpay`

### 2. Monitor ngrok Traffic
ngrok provides a web interface to inspect requests:
```
http://localhost:4040
```

Features:
- View all incoming requests
- Inspect request headers and body
- Replay requests for debugging
- See response status codes

### 3. Test with Sample Requests

#### Test MoMo Webhook
```bash
curl -X POST https://abc123def456.ngrok-free.app/api/v1/webhooks/momo \
  -H "Content-Type: application/json" \
  -H "X-Signature: test-signature" \
  -d '{
    "partnerCode": "MOMOIQA420180417",
    "orderId": "TEST-ORDER-001",
    "requestId": "TEST-REQUEST-001",
    "amount": 100000,
    "orderInfo": "Test payment",
    "orderType": "momo_wallet",
    "transId": 2304963877,
    "resultCode": 0,
    "message": "Successful",
    "payType": "qr",
    "responseTime": 1700000000000,
    "extraData": "",
    "signature": "test-signature"
  }'
```

#### Test VNPay IPN
```bash
curl -X POST https://abc123def456.ngrok-free.app/api/v1/webhooks/vnpay \
  -H "Content-Type: application/json" \
  -d '{
    "vnpTmnCode": "CARBON01",
    "vnpAmount": "10000000",
    "vnpBankCode": "NCB",
    "vnpBankTranNo": "VNP13984862",
    "vnpCardType": "ATM",
    "vnpPayDate": "20231120150000",
    "vnpOrderInfo": "Test payment for order TEST-001",
    "vnpTransactionNo": "13984862",
    "vnpResponseCode": "00",
    "vnpTxnRef": "TEST-ORDER-001",
    "vnpSecureHashType": "SHA256",
    "vnpSecureHash": "test-hash"
  }'
```

### 4. Verify in Application Logs
```bash
# View payment service logs
docker logs carbon-marketplace-payment-service -f

# Or if running locally
tail -f backend/payment-service/logs/payment-service.log
```

## Security Considerations

### For Development Only
⚠️ **WARNING**: ngrok exposes your local service to the internet. Use only for development and testing.

### Best Practices
1. **Use HTTPS**: ngrok provides HTTPS by default
2. **Rotate URLs**: Don't use the same ngrok URL for extended periods
3. **Verify Signatures**: Always verify webhook signatures in production
4. **IP Whitelisting**: In production, whitelist payment gateway IPs
5. **Request Validation**: Validate all incoming webhook data

### Basic Authentication (Optional)
Add basic auth to your ngrok tunnel:
```bash
ngrok http 8087 --basic-auth="username:password"
```

### Webhook Signature Verification
Ensure signature verification is enabled in the payment service:
```java
// WebhookService.java
if (!verifySignature(request, signature)) {
    log.error("Invalid webhook signature");
    throw new InvalidSignatureException("Invalid webhook signature");
}
```

## Troubleshooting

### Common Issues and Solutions

#### Issue 1: ngrok Connection Refused
**Problem**: `Failed to complete tunnel connection`
```bash
ERROR:  Failed to complete tunnel connection
The connection to http://localhost:8087 was refused.
```

**Solution**: Ensure the payment service is running on the correct port:
```bash
# Check if service is running
docker ps | grep payment-service

# Or check port directly
netstat -an | grep 8087
```

#### Issue 2: Webhook Not Received
**Problem**: Payment gateway sends webhook but application doesn't receive it

**Solutions**:
1. Check ngrok web interface (http://localhost:4040) for requests
2. Verify the webhook URL is correctly configured in payment gateway
3. Check for typos in the URL
4. Ensure the service is healthy:
```bash
curl http://localhost:8087/actuator/health
```

#### Issue 3: Signature Verification Fails
**Problem**: Webhook received but signature verification fails

**Solution**: 
- For testing, temporarily disable signature verification
- Ensure secret keys match between application and payment gateway
- Check if signature algorithm matches (SHA256, HMAC-SHA256, etc.)

#### Issue 4: ngrok Session Expired
**Problem**: Free ngrok sessions expire after 8 hours

**Solution**:
1. Restart ngrok with new session
2. Update webhook URLs with new ngrok URL
3. Consider ngrok paid plan for persistent URLs

#### Issue 5: Rate Limiting
**Problem**: ngrok free tier has connection limits

**Solution**:
- Free tier: 40 connections/minute
- For higher limits, upgrade to paid plan
- Implement request throttling in tests

### Debugging Commands

```bash
# Check ngrok status
ngrok diagnose

# View ngrok configuration
ngrok config check

# Test direct connection to payment service
curl http://localhost:8087/api/v1/webhooks/momo

# Test through ngrok tunnel
curl https://abc123def456.ngrok-free.app/api/v1/webhooks/momo

# Monitor Docker logs
docker-compose logs -f payment-service

# Check service health
curl http://localhost:8087/actuator/health
```

## Advanced Configuration

### Custom Domain (Paid Feature)
```bash
# Use custom domain
ngrok http 8087 --domain=webhooks.yourcompany.dev
```

### Multiple Tunnels
Create `ngrok.yml`:
```yaml
version: 2
tunnels:
  payment:
    addr: 8087
    proto: http
    subdomain: carbon-payment
  api:
    addr: 8080
    proto: http
    subdomain: carbon-api
```

Run multiple tunnels:
```bash
ngrok start payment api
```

### Request/Response Logging
Enable detailed logging in ngrok:
```bash
ngrok http 8087 --log=stdout --log-level=debug
```

## Quick Reference

### Start Everything
```bash
# 1. Start services
docker-compose up -d

# 2. Start ngrok
ngrok http 8087

# 3. Update webhook URLs with ngrok URL
export WEBHOOK_BASE_URL="https://your-ngrok-url.ngrok-free.app"

# 4. Restart payment service with new URLs
docker-compose restart payment-service

# 5. Monitor
open http://localhost:4040  # ngrok web interface
docker logs -f carbon-marketplace-payment-service
```

### Stop Everything
```bash
# Stop ngrok (Ctrl+C in terminal)
# Stop services
docker-compose down
```

## Additional Resources
- [ngrok Documentation](https://ngrok.com/docs)
- [ngrok Pricing](https://ngrok.com/pricing)
- [MoMo Payment Gateway Docs](https://developers.momo.vn)
- [VNPay Integration Guide](https://sandbox.vnpayment.vn/apis/)
- [Webhook Best Practices](https://webhooks.dev)
