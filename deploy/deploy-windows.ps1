$ErrorActionPreference = "Stop"

$SERVER = "103.149.93.129"
$USER = "root"
$PROJECT = "F:\study\workspace\AI\qinglv"
$ARCHIVE = "$env:TEMP\qinglv-update.tar.gz"
$SSH_OPTS = "-o StrictHostKeyChecking=no -o ConnectTimeout=15"
$SCP = "scp.exe"
$SSH = "ssh.exe"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  QingLv v2.0 Deploy Script" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Create archive
Write-Host "[1/5] Packaging project files..." -ForegroundColor Yellow
Push-Location $PROJECT
& tar czf $ARCHIVE --exclude='.git' --exclude='node_modules' --exclude='.idea' --exclude='dist' --exclude='target' --exclude='.env' backend/ frontend/src/ frontend/public/ frontend/index.html frontend/package.json frontend/vite.config.js frontend/Dockerfile frontend/nginx.conf sql/ docker-compose.yml deploy/
Pop-Location
$size = (Get-Item $ARCHIVE).Length / 1KB
Write-Host "  Archive: $([math]::Round($size, 1)) KB" -ForegroundColor Green

# Step 2: Upload archive
Write-Host "[2/5] Uploading to server (password required)..." -ForegroundColor Yellow
& $SCP $SSH_OPTS $ARCHIVE "${USER}@${SERVER}:/tmp/qinglv-update.tar.gz"
if ($LASTEXITCODE -ne 0) { Write-Host "  Upload failed!" -ForegroundColor Red; exit 1 }
Write-Host "  Upload complete" -ForegroundColor Green

# Step 3: Upload update script
Write-Host "[3/5] Uploading update script..." -ForegroundColor Yellow
& $SCP $SSH_OPTS "$PROJECT\deploy\update-v2.sh" "${USER}@${SERVER}:/tmp/update-v2.sh"
if ($LASTEXITCODE -ne 0) { Write-Host "  Script upload failed!" -ForegroundColor Red; exit 1 }
Write-Host "  Script uploaded" -ForegroundColor Green

# Step 4: Execute update on server
Write-Host "[4/5] Running update on server..." -ForegroundColor Yellow
& $SSH $SSH_OPTS "${USER}@${SERVER}" "chmod +x /tmp/update-v2.sh && bash /tmp/update-v2.sh"
if ($LASTEXITCODE -ne 0) { Write-Host "  Remote execution had errors (check output above)" -ForegroundColor Yellow }

# Step 5: Verify
Write-Host ""
Write-Host "[5/5] Verifying..." -ForegroundColor Yellow
$result = & $SSH $SSH_OPTS "${USER}@${SERVER}" "docker compose -f /opt/qinglv/docker-compose.yml ps --format '{{.Name}} {{.State}}' 2>/dev/null || docker ps --filter name=qinglv --format '{{.Names}} {{.Status}}'" 2>&1
Write-Host $result

# Cleanup local archive
Remove-Item $ARCHIVE -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "  Deploy finished!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "  http://$SERVER`:8090" -ForegroundColor White
Write-Host ""
Write-Host "  New: search, wish pool, stats, dark mode, PWA" -ForegroundColor Cyan
Write-Host ""
