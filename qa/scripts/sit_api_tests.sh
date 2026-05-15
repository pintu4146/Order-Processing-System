#!/bin/bash
# System Integration Test Script
# Ensures that the REST API acts as expected end-to-end.

BASE_URL="http://localhost:8080"
LOG_FILE="../reports/api_sit_log.txt"

echo "======================================" > $LOG_FILE
echo "SIT API EXECUTION LOG: $(date)" >> $LOG_FILE
echo "======================================" >> $LOG_FILE

# Helper function to print and log
log() {
  echo "$1"
  echo "$1" >> $LOG_FILE
}

log "1. Checking Application Health..."
HEALTH_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/actuator/health")
if [ "$HEALTH_RESPONSE" -eq 200 ]; then
  log "✅ Health check PASSED."
else
  log "❌ Health check FAILED. HTTP Code: $HEALTH_RESPONSE"
  exit 1
fi

log "\n2. Creating a new Order..."
ORDER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
        "customerName": "QA Engineer",
        "items": [
            { "productName": "Integration Book", "quantity": 2, "price": 45.00 },
            { "productName": "Coffee", "quantity": 1, "price": 5.50 }
        ]
      }')
log "Response: $ORDER_RESPONSE"
ORDER_ID=$(echo $ORDER_RESPONSE | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ -n "$ORDER_ID" ]; then
  log "✅ Order creation PASSED. ID: $ORDER_ID"
else
  log "❌ Order creation FAILED."
  exit 1
fi

log "\n3. Retrieving Order Details..."
GET_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/api/orders/$ORDER_ID")
if [ "$GET_RESPONSE" -eq 200 ]; then
  log "✅ Order retrieval PASSED."
else
  log "❌ Order retrieval FAILED. HTTP Code: $GET_RESPONSE"
fi

log "\n4. Testing Pagination (List Orders)..."
LIST_RESPONSE=$(curl -s "$BASE_URL/api/orders?page=0&size=5")
if echo "$LIST_RESPONSE" | grep -q '"totalElements"'; then
  log "✅ Pagination PASSED."
else
  log "❌ Pagination FAILED."
fi

log "\n5. Cancelling the Order..."
CANCEL_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/orders/$ORDER_ID/cancel")
if echo "$CANCEL_RESPONSE" | grep -q '"status":"CANCELLED"'; then
  log "✅ Order cancellation PASSED."
else
  log "❌ Order cancellation FAILED."
fi

log "\n======================================"
log "SIT AUTOMATION COMPLETE"
log "======================================"
