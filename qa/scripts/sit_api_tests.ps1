$ErrorActionPreference = "Stop"
$BASE_URL = "http://localhost:8080"
$LOG_FILE = "qa\reports\api_sit_log.txt"

Function Log-Message {
    param([string]$message)
    Write-Host $message
    $message | Out-File -FilePath $LOG_FILE -Append -Encoding UTF8
}

"======================================" | Out-File -FilePath $LOG_FILE -Encoding UTF8
"SIT API EXECUTION LOG: $(Get-Date)" | Out-File -FilePath $LOG_FILE -Append -Encoding UTF8
"======================================" | Out-File -FilePath $LOG_FILE -Append -Encoding UTF8

Log-Message "1. Checking Application Health..."
try {
    $health = Invoke-RestMethod -Uri "$BASE_URL/actuator/health" -Method Get
    if ($health.status -eq "UP") {
        Log-Message "✅ Health check PASSED."
    } else {
        Log-Message "❌ Health check FAILED."
        exit 1
    }
} catch {
    Log-Message "❌ Health check FAILED."
    exit 1
}

Log-Message "`n2. Creating a new Order..."
$orderBody = @{
    customerName = "QA Engineer"
    items = @(
        @{ productName = "Integration Book"; quantity = 2; price = 45.00 },
        @{ productName = "Coffee"; quantity = 1; price = 5.50 }
    )
} | ConvertTo-Json

try {
    $order = Invoke-RestMethod -Uri "$BASE_URL/api/orders" -Method Post -Body $orderBody -ContentType "application/json"
    Log-Message "Response: $($order | ConvertTo-Json -Compress)"
    $script:ORDER_ID = $order.id
    Log-Message "✅ Order creation PASSED. ID: $ORDER_ID"
} catch {
    Log-Message "❌ Order creation FAILED."
    exit 1
}

Log-Message "`n3. Retrieving Order Details..."
try {
    $get = Invoke-RestMethod -Uri "$BASE_URL/api/orders/$ORDER_ID" -Method Get
    if ($get.id -eq $ORDER_ID) {
        Log-Message "✅ Order retrieval PASSED."
    } else {
        Log-Message "❌ Order retrieval FAILED."
    }
} catch {
    Log-Message "❌ Order retrieval FAILED."
}

Log-Message "`n4. Testing Pagination (List Orders)..."
try {
    $list = Invoke-RestMethod -Uri "$BASE_URL/api/orders?page=0&size=5" -Method Get
    if ($null -ne $list.totalElements) {
        Log-Message "✅ Pagination PASSED."
    } else {
        Log-Message "❌ Pagination FAILED."
    }
} catch {
    Log-Message "❌ Pagination FAILED."
}

Log-Message "`n5. Cancelling the Order..."
try {
    $cancel = Invoke-RestMethod -Uri "$BASE_URL/api/orders/$ORDER_ID/cancel" -Method Put
    if ($cancel.status -eq "CANCELLED") {
        Log-Message "✅ Order cancellation PASSED."
    } else {
        Log-Message "❌ Order cancellation FAILED."
    }
} catch {
    Log-Message "❌ Order cancellation FAILED."
}

Log-Message "`n======================================"
Log-Message "SIT AUTOMATION COMPLETE"
Log-Message "======================================"
