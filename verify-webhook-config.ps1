# Carbon Credit Marketplace - Webhook Configuration Verification Script (Windows PowerShell)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Carbon Credit Marketplace Configuration Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to test service health
function Test-ServiceHealth {
    param(
        [string]$ServiceName,
        [string]$HealthUrl
    )
    
    Write-Host "Testing $ServiceName..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 5
        if ($response.status -eq "UP") {
            Write-Host "  ✓ $ServiceName is healthy" -ForegroundColor Green
            return $true
        } else {
            Write-Host "  ✗ $ServiceName is unhealthy: $($response.status)" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "  ✗ $ServiceName is not reachable at $HealthUrl" -ForegroundColor Red
        Write-Host "    Error: $_" -ForegroundColor DarkRed
        return $false
    }
}

# Function to check Elasticsearch memory
function Test-ElasticsearchMemory {
    Write-Host "Checking Elasticsearch Configuration..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:9200/_nodes/stats/jvm" -Method Get -TimeoutSec 5
        $node = $response.nodes.PSObject.Properties[0].Value
        $heapMax = [math]::Round($node.jvm.mem.heap_max_in_bytes / 1GB, 2)
        
        if ($heapMax -ge 1.5) {
            Write-Host "  ✓ Elasticsearch heap size: ${heapMax}GB (Configured correctly)" -ForegroundColor Green
            return $true
        } else {
            Write-Host "  ⚠ Elasticsearch heap size: ${heapMax}GB (Should be 2GB)" -ForegroundColor Yellow
            return $false
        }
    } catch {
        Write-Host "  ✗ Could not check Elasticsearch memory" -ForegroundColor Red
        Write-Host "    Error: $_" -ForegroundColor DarkRed
        return $false
    }
}

# Function to check webhook configuration
function Test-WebhookConfig {
    Write-Host "Checking Webhook Configuration..." -ForegroundColor Yellow
    
    # Check environment variables
    $webhookVars = @{
        "WEBHOOK_BASE_URL" = $env:WEBHOOK_BASE_URL
        "MOMO_WEBHOOK_URL" = $env:MOMO_WEBHOOK_URL
        "VNPAY_IPN_URL" = $env:VNPAY_IPN_URL
        "BANK_WEBHOOK_URL" = $env:BANK_WEBHOOK_URL
    }
    
    $hasNgrok = $false
    foreach ($key in $webhookVars.Keys) {
        $value = $webhookVars[$key]
        if ($value) {
            if ($value -like "*ngrok*") {
                Write-Host "  ✓ $key = $value" -ForegroundColor Green
                $hasNgrok = $true
            } else {
                Write-Host "  ⚠ $key = $value (Not using ngrok)" -ForegroundColor Yellow
            }
        } else {
            Write-Host "  ⚠ $key not set (using default localhost)" -ForegroundColor Yellow
        }
    }
    
    if ($hasNgrok) {
        Write-Host "  ✓ Webhook URLs configured for ngrok" -ForegroundColor Green
    } else {
        Write-Host "  ℹ Webhook URLs using localhost (ngrok not configured)" -ForegroundColor Cyan
    }
    
    return $true
}

# Function to test webhook endpoints
function Test-WebhookEndpoints {
    Write-Host "Testing Webhook Endpoints..." -ForegroundColor Yellow
    
    $endpoints = @(
        @{Name="MoMo Webhook"; Url="http://localhost:8087/api/v1/webhooks/momo"; Method="POST"},
        @{Name="VNPay Webhook"; Url="http://localhost:8087/api/v1/webhooks/vnpay"; Method="POST"},
        @{Name="Bank Transfer Webhook"; Url="http://localhost:8087/api/v1/webhooks/bank-transfer"; Method="POST"}
    )
    
    foreach ($endpoint in $endpoints) {
        try {
            $body = @{
                orderId = "TEST-VERIFY-001"
                amount = 100000
                resultCode = 0
                message = "Configuration test"
            } | ConvertTo-Json
            
            $response = Invoke-WebRequest -Uri $endpoint.Url -Method $endpoint.Method `
                -ContentType "application/json" -Body $body -TimeoutSec 5
            
            if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 400) {
                Write-Host "  ✓ $($endpoint.Name) endpoint is accessible" -ForegroundColor Green
            } else {
                Write-Host "  ⚠ $($endpoint.Name) returned status: $($response.StatusCode)" -ForegroundColor Yellow
            }
        } catch {
            if ($_.Exception.Response.StatusCode -eq 400 -or $_.Exception.Response.StatusCode -eq 401) {
                Write-Host "  ✓ $($endpoint.Name) endpoint is accessible (auth required)" -ForegroundColor Green
            } else {
                Write-Host "  ✗ $($endpoint.Name) is not accessible" -ForegroundColor Red
            }
        }
    }
}

# Function to check ngrok status
function Test-NgrokStatus {
    Write-Host "Checking ngrok Status..." -ForegroundColor Yellow
    
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -Method Get -TimeoutSec 2
        if ($response.tunnels.Count -gt 0) {
            Write-Host "  ✓ ngrok is running" -ForegroundColor Green
            foreach ($tunnel in $response.tunnels) {
                Write-Host "    - $($tunnel.public_url) -> $($tunnel.config.addr)" -ForegroundColor Cyan
            }
            return $true
        } else {
            Write-Host "  ⚠ ngrok is running but no tunnels active" -ForegroundColor Yellow
            return $false
        }
    } catch {
        Write-Host "  ℹ ngrok is not running (this is OK for local-only testing)" -ForegroundColor Cyan
        return $false
    }
}

# Main verification process
Write-Host "1. SERVICE HEALTH CHECKS" -ForegroundColor White
Write-Host "------------------------" -ForegroundColor White
$services = @(
    @{Name="Elasticsearch"; Url="http://localhost:9200/_cluster/health"},
    @{Name="Payment Service"; Url="http://localhost:8087/actuator/health"},
    @{Name="Redis"; Url="http://localhost:6379"} # Note: Redis doesn't have HTTP health endpoint
)

$allHealthy = $true
foreach ($service in $services) {
    if ($service.Name -eq "Redis") {
        Write-Host "Testing Redis..." -ForegroundColor Yellow
        try {
            # Test Redis using redis-cli if available, otherwise skip
            Write-Host "  ℹ Redis check requires redis-cli (manual verification needed)" -ForegroundColor Cyan
        } catch {
            Write-Host "  ⚠ Cannot automatically verify Redis" -ForegroundColor Yellow
        }
    } else {
        $healthy = Test-ServiceHealth -ServiceName $service.Name -HealthUrl $service.Url
        if (-not $healthy) { $allHealthy = $false }
    }
}

Write-Host ""
Write-Host "2. ELASTICSEARCH MEMORY CHECK" -ForegroundColor White
Write-Host "-----------------------------" -ForegroundColor White
Test-ElasticsearchMemory

Write-Host ""
Write-Host "3. WEBHOOK CONFIGURATION" -ForegroundColor White
Write-Host "------------------------" -ForegroundColor White
Test-WebhookConfig

Write-Host ""
Write-Host "4. WEBHOOK ENDPOINTS" -ForegroundColor White
Write-Host "-------------------" -ForegroundColor White
Test-WebhookEndpoints

Write-Host ""
Write-Host "5. NGROK STATUS" -ForegroundColor White
Write-Host "--------------" -ForegroundColor White
$ngrokRunning = Test-NgrokStatus

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " VERIFICATION SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($allHealthy) {
    Write-Host "✓ Core services are healthy" -ForegroundColor Green
} else {
    Write-Host "⚠ Some services need attention" -ForegroundColor Yellow
}

if ($ngrokRunning) {
    Write-Host "✓ ngrok tunnel is active" -ForegroundColor Green
    Write-Host ""
    Write-Host "NEXT STEPS:" -ForegroundColor Yellow
    Write-Host "1. Update payment gateway settings with your ngrok URL" -ForegroundColor White
    Write-Host "2. Monitor webhook traffic at http://localhost:4040" -ForegroundColor White
} else {
    Write-Host "ℹ ngrok is not running" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "TO ENABLE WEBHOOK TESTING:" -ForegroundColor Yellow
    Write-Host "1. Start ngrok: ngrok http 8087" -ForegroundColor White
    Write-Host "2. Set webhook URLs with ngrok URL:" -ForegroundColor White
    Write-Host '   $env:WEBHOOK_BASE_URL="https://your-ngrok-url.ngrok-free.app"' -ForegroundColor Gray
    Write-Host "3. Restart payment service: docker-compose restart payment-service" -ForegroundColor White
}

Write-Host ""
Write-Host "For detailed webhook setup, see: WEBHOOK_NGROK_SETUP.md" -ForegroundColor Cyan
Write-Host ""
