# Carbon Credit Marketplace - Docker Compose Stop Script (PowerShell)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Stopping All Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$choice = Read-Host "Do you want to remove volumes (delete data)? (y/N)"

if ($choice -eq "y" -or $choice -eq "Y") {
    Write-Host ""
    Write-Host "Stopping services and removing volumes..." -ForegroundColor Yellow
    docker-compose down -v
    Write-Host ""
    Write-Host "✓ All services stopped and data removed" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Stopping services (keeping data)..." -ForegroundColor Yellow
    docker-compose down
    Write-Host ""
    Write-Host "✓ All services stopped (data preserved)" -ForegroundColor Green
}

Write-Host ""

