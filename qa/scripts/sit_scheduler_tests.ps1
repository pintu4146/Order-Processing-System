$ErrorActionPreference = "Stop"
$BASE_URL = "http://localhost:8080"
$LOG_FILE = "qa\reports\scheduler_sit_log.txt"

Function Log-Message {
    param([string]$message)
    Write-Host $message
    $message | Out-File -FilePath $LOG_FILE -Append -Encoding UTF8
}

"======================================" | Out-File -FilePath $LOG_FILE -Encoding UTF8
"SCHEDULER SIT EXECUTION LOG: $(Get-Date)" | Out-File -FilePath $LOG_FILE -Append -Encoding UTF8
"======================================" | Out-File -FilePath $LOG_FILE -Append -Encoding UTF8

Log-Message "1. Creating a new PENDING order for Scheduler testing..."
$orderBody = @{
    customerName = "Scheduler QA Tester"
    items = @(
        @{ productName = "Wait Timer"; quantity = 1; price = 10.00 }
    )
} | ConvertTo-Json

try {
    $order = Invoke-RestMethod -Uri "$BASE_URL/api/orders" -Method Post -Body $orderBody -ContentType "application/json"
    $script:ORDER_ID = $order.id
    Log-Message "✅ Order creation PASSED. ID: $ORDER_ID. Initial Status: $($order.status)"
} catch {
    Log-Message "❌ Order creation FAILED."
    exit 1
}

Log-Message "`n2. Waiting 15 seconds for the background scheduler to trigger..."
Start-Sleep -Seconds 15

Log-Message "`n3. Verifying order status transition..."
try {
    $get = Invoke-RestMethod -Uri "$BASE_URL/api/orders/$ORDER_ID" -Method Get
    if ($get.status -eq "PROCESSING") {
        Log-Message "✅ Scheduler transition PASSED. Order $ORDER_ID is now PROCESSING."
    } else {
        Log-Message "❌ Scheduler transition FAILED. Order $ORDER_ID is still $($get.status)."
    }
} catch {
    Log-Message "❌ Order retrieval FAILED."
}

Log-Message "`n======================================"
Log-Message "SCHEDULER AUTOMATION COMPLETE"
Log-Message "======================================"
