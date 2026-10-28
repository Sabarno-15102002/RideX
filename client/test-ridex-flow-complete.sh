#!/usr/bin/env bash

set -euo pipefail

# ============================================================
# RideX - End-to-End Driver + Rider + Trip Test
#
# Flow:
#   Register Driver
#   -> Login Driver
#   -> Get Driver ID
#   -> Onboard Driver
#   -> Set Driver AVAILABLE
#   -> Register Rider
#   -> Login Rider
#   -> Create Trip
#
# All external calls go through API Gateway.
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"

# ------------------------------------------------------------
# Test users
# ------------------------------------------------------------

TIMESTAMP="$(date +%s)"

DRIVER_NAME="RideX Test Driver"
DRIVER_EMAIL="driver.${TIMESTAMP}@ridex.local"
DRIVER_PASSWORD="Password@123"

RIDER_NAME="RideX Test Rider"
RIDER_EMAIL="rider.${TIMESTAMP}@ridex.local"
RIDER_PASSWORD="Password@123"

# ------------------------------------------------------------
# Location
# Bengaluru
# ------------------------------------------------------------

PICKUP_LAT="12.9716"
PICKUP_LON="77.5946"

DROPOFF_LAT="12.9352"
DROPOFF_LON="77.6245"

# ------------------------------------------------------------
# Helpers
# ------------------------------------------------------------

print_section() {
    echo
    echo "============================================================"
    echo "$1"
    echo "============================================================"
}

print_response() {
    local status="$1"
    local body="$2"

    echo
    echo "HTTP Status: $status"
    echo "Response:"
    echo "$body"
}

create_phone() {
    echo "$((6 + RANDOM % 4))$(printf '%09d' $((RANDOM % 1000000000)))"
}

create_registration() {
    echo "KA$(printf '%02d' $((RANDOM % 100)))AB$(printf '%04d' $((RANDOM % 10000)))"
}

create_license() {
    echo "KA-DRIVER-TEST-$(date +%s)"
}

extract_json() {
    local json="$1"
    shift

    python3 - "$json" "$@" <<'PY'
import json
import sys

data = json.loads(sys.argv[1])

for key in sys.argv[2:]:
    if isinstance(data, dict) and key in data:
        value = data[key]

        if value is not None:
            print(value)

        sys.exit(0)
PY
}

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "ERROR: '$1' is required."
        exit 1
    fi
}

require_command curl
require_command python3

# ============================================================
# 1. Gateway health
# ============================================================

print_section "1. API Gateway Health"

curl -fsS "$BASE_URL/actuator/health"

echo
echo "Gateway is UP."

# ============================================================
# 2. Register DRIVER
# ============================================================

print_section "2. Registering DRIVER"

DRIVER_REGISTER_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"$DRIVER_NAME\",
            \"email\": \"$DRIVER_EMAIL\",
            \"password\": \"$DRIVER_PASSWORD\",
            \"phone\": \"+91$(create_phone)\",
            \"role\": \"DRIVER\"
        }"
)

DRIVER_REGISTER_STATUS="$(echo "$DRIVER_REGISTER_RESPONSE" | tail -n1)"
DRIVER_REGISTER_BODY="$(echo "$DRIVER_REGISTER_RESPONSE" | sed '$d')"

print_response \
    "$DRIVER_REGISTER_STATUS" \
    "$DRIVER_REGISTER_BODY"

if [[ "$DRIVER_REGISTER_STATUS" != 2* ]]; then
    echo "ERROR: Driver registration failed."
    exit 1
fi

DRIVER_USER_ID="$(
    extract_json "$DRIVER_REGISTER_BODY" userId id
)"

echo
echo "Driver userId: ${DRIVER_USER_ID:-NOT_RETURNED}"

# ============================================================
# 3. Login DRIVER
# ============================================================

print_section "3. Logging in DRIVER"

DRIVER_LOGIN_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$DRIVER_EMAIL\",
            \"password\": \"$DRIVER_PASSWORD\"
        }"
)

DRIVER_LOGIN_STATUS="$(echo "$DRIVER_LOGIN_RESPONSE" | tail -n1)"
DRIVER_LOGIN_BODY="$(echo "$DRIVER_LOGIN_RESPONSE" | sed '$d')"

print_response \
    "$DRIVER_LOGIN_STATUS" \
    "$DRIVER_LOGIN_BODY"

if [[ "$DRIVER_LOGIN_STATUS" != 2* ]]; then
    echo "ERROR: Driver login failed."
    exit 1
fi

DRIVER_TOKEN="$(
    extract_json "$DRIVER_LOGIN_BODY" accessToken token
)"

if [[ -z "${DRIVER_TOKEN:-}" ]]; then
    echo "ERROR: Could not extract driver access token."
    exit 1
fi

echo
echo "Driver JWT acquired."

# ============================================================
# 4. Get DRIVER
# ============================================================

print_section "4. Waiting for DRIVER Profile"

MAX_ATTEMPTS=15
SLEEP_SECONDS=1

DRIVER_ID=""

for ((attempt=1; attempt<=MAX_ATTEMPTS; attempt++)); do

    echo "Attempt $attempt/$MAX_ATTEMPTS: checking Driver Service..."

    DRIVER_GET_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X GET \
            "$BASE_URL/api/v1/drivers/" \
            -H "Authorization: Bearer $DRIVER_TOKEN"
    )

    DRIVER_GET_STATUS="$(echo "$DRIVER_GET_RESPONSE" | tail -n1)"
    DRIVER_GET_BODY="$(echo "$DRIVER_GET_RESPONSE" | sed '$d')"

    if [[ "$DRIVER_GET_STATUS" == 2* ]]; then

        print_response \
            "$DRIVER_GET_STATUS" \
            "$DRIVER_GET_BODY"

        DRIVER_ID="$(
            extract_json "$DRIVER_GET_BODY" id
        )"

        if [[ -n "${DRIVER_ID:-}" ]]; then
            break
        fi
    fi

    echo "Driver profile not available yet."
    echo "Waiting ${SLEEP_SECONDS}s..."
    sleep "$SLEEP_SECONDS"
done

if [[ -z "${DRIVER_ID:-}" ]]; then
    echo
    echo "ERROR: Driver profile was not created within the expected time."
    echo
    echo "Check Driver Service logs:"
    echo "  docker compose logs driver-service --tail=100"
    exit 1
fi

echo
echo "============================================================"
echo "DRIVER ID = $DRIVER_ID"
echo "USER ID   = $DRIVER_USER_ID"
echo "============================================================"

# ============================================================
# 5. Onboard DRIVER
# ============================================================

print_section "5. Onboarding DRIVER"

DRIVER_ONBOARD_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X PUT \
        "$BASE_URL/api/v1/drivers/$DRIVER_ID/onboarding" \
        -H "Authorization: Bearer $DRIVER_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "licenseNumber": "'$(create_license)'",
            "vehicle": {
                "registrationNumber": "'$(create_registration)'",
                "make": "Maruti",
                "model": "Swift",
                "color": "White",
                "vehicleType": "HATCHBACK"
            }
        }'
)

DRIVER_ONBOARD_STATUS="$(echo "$DRIVER_ONBOARD_RESPONSE" | tail -n1)"
DRIVER_ONBOARD_BODY="$(echo "$DRIVER_ONBOARD_RESPONSE" | sed '$d')"

print_response \
    "$DRIVER_ONBOARD_STATUS" \
    "$DRIVER_ONBOARD_BODY"

if [[ "$DRIVER_ONBOARD_STATUS" != 2* ]]; then
    echo "ERROR: Driver onboarding failed."
    exit 1
fi

echo
echo "Driver onboarding successful."

# ============================================================
# 6. Set DRIVER AVAILABLE
# ============================================================

print_section "6. Setting DRIVER AVAILABLE"

DRIVER_STATUS_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X PATCH \
        "$BASE_URL/api/v1/drivers/$DRIVER_ID/status" \
        -H "Authorization: Bearer $DRIVER_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "status": "AVAILABLE"
        }'
)

DRIVER_STATUS_CODE="$(echo "$DRIVER_STATUS_RESPONSE" | tail -n1)"
DRIVER_STATUS_BODY="$(echo "$DRIVER_STATUS_RESPONSE" | sed '$d')"

print_response \
    "$DRIVER_STATUS_CODE" \
    "$DRIVER_STATUS_BODY"

if [[ "$DRIVER_STATUS_CODE" != 2* ]]; then
    echo "ERROR: Could not set driver AVAILABLE."
    exit 1
fi

echo
echo "Driver is AVAILABLE."

# ============================================================
# 7. Register RIDER
# ============================================================

print_section "7. Registering RIDER"

RIDER_REGISTER_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"$RIDER_NAME\",
            \"email\": \"$RIDER_EMAIL\",
            \"password\": \"$RIDER_PASSWORD\",
            \"phone\": \"+91$(create_phone)\",
            \"role\": \"RIDER\"
        }"
)

RIDER_REGISTER_STATUS="$(echo "$RIDER_REGISTER_RESPONSE" | tail -n1)"
RIDER_REGISTER_BODY="$(echo "$RIDER_REGISTER_RESPONSE" | sed '$d')"

print_response \
    "$RIDER_REGISTER_STATUS" \
    "$RIDER_REGISTER_BODY"

if [[ "$RIDER_REGISTER_STATUS" != 2* ]]; then
    echo "ERROR: Rider registration failed."
    exit 1
fi

RIDER_USER_ID="$(
    extract_json "$RIDER_REGISTER_BODY" userId id
)"

echo
echo "Rider userId: ${RIDER_USER_ID:-NOT_RETURNED}"

# ============================================================
# 8. Login RIDER
# ============================================================

print_section "8. Logging in RIDER"

RIDER_LOGIN_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$RIDER_EMAIL\",
            \"password\": \"$RIDER_PASSWORD\"
        }"
)

RIDER_LOGIN_STATUS="$(echo "$RIDER_LOGIN_RESPONSE" | tail -n1)"
RIDER_LOGIN_BODY="$(echo "$RIDER_LOGIN_RESPONSE" | sed '$d')"

print_response \
    "$RIDER_LOGIN_STATUS" \
    "$RIDER_LOGIN_BODY"

if [[ "$RIDER_LOGIN_STATUS" != 2* ]]; then
    echo "ERROR: Rider login failed."
    exit 1
fi

RIDER_TOKEN="$(
    extract_json "$RIDER_LOGIN_BODY" accessToken token
)"

if [[ -z "${RIDER_TOKEN:-}" ]]; then
    echo "ERROR: Could not extract rider access token."
    exit 1
fi

echo
echo "Rider JWT acquired."

# ============================================================
# 9. Set DRIVER LOCATION
# ============================================================

print_section "9. Setting DRIVER LOCATION"

MAX_ATTEMPTS=15
SLEEP_SECONDS=1

LOCATION_BODY="{
    \"latitude\": $PICKUP_LAT,
    \"longitude\": $PICKUP_LON
}"

echo "Location request:"
echo "$LOCATION_BODY"

for ((attempt=1; attempt<=MAX_ATTEMPTS; attempt++)); do
    echo
    echo "Attempt $attempt/$MAX_ATTEMPTS: setting location..."

    DRIVER_LOCATION_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X PUT \
            "$BASE_URL/api/v1/location/drivers/me" \
            -H "Authorization: Bearer $DRIVER_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$LOCATION_BODY"
    )

    DRIVER_LOCATION_STATUS="$(echo "$DRIVER_LOCATION_RESPONSE" | tail -n1)"
    DRIVER_LOCATION_BODY="$(echo "$DRIVER_LOCATION_RESPONSE" | sed '$d')"

    print_response \
        "$DRIVER_LOCATION_STATUS" \
        "$DRIVER_LOCATION_BODY"

    if [[ "$DRIVER_LOCATION_STATUS" != 2* ]]; then
        echo "ERROR: Could not update driver location."
    else
        echo
        echo "Driver location updated."
        break
    fi
    if (( attempt < MAX_ATTEMPTS )); then
        echo "Waiting ${SLEEP_SECONDS}s before retrying..."
        sleep "$SLEEP_SECONDS"
    fi
done



# # ============================================================
# # 10. Register RIDER
# # ============================================================

# print_section "10. Registering RIDER"

# RIDER_REGISTER_RESPONSE=$(
#     curl -sS \
#         -w '\n%{http_code}' \
#         -X POST \
#         "$BASE_URL/api/v1/auth/register" \
#         -H "Content-Type: application/json" \
#         -d "{
#             \"name\": \"$RIDER_NAME\",
#             \"email\": \"$RIDER_EMAIL\",
#             \"password\": \"$RIDER_PASSWORD\",
#             \"phone\": \"+91$(create_phone)\",
#             \"role\": \"RIDER\"
#         }"
# )

# RIDER_REGISTER_STATUS="$(echo "$RIDER_REGISTER_RESPONSE" | tail -n1)"
# RIDER_REGISTER_BODY="$(echo "$RIDER_REGISTER_RESPONSE" | sed '$d')"

# print_response \
#     "$RIDER_REGISTER_STATUS" \
#     "$RIDER_REGISTER_BODY"

# if [[ "$RIDER_REGISTER_STATUS" != 2* ]]; then
#     echo "ERROR: Rider registration failed."
#     exit 1
# fi

# RIDER_USER_ID="$(
#     extract_json "$RIDER_REGISTER_BODY" userId id
# )"

# echo
# echo "Rider userId: ${RIDER_USER_ID:-NOT_RETURNED}"

# ============================================================
# 11. Login RIDER
# ============================================================

print_section "11. Logging in RIDER"

RIDER_LOGIN_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$RIDER_EMAIL\",
            \"password\": \"$RIDER_PASSWORD\"
        }"
)

RIDER_LOGIN_STATUS="$(echo "$RIDER_LOGIN_RESPONSE" | tail -n1)"
RIDER_LOGIN_BODY="$(echo "$RIDER_LOGIN_RESPONSE" | sed '$d')"

print_response \
    "$RIDER_LOGIN_STATUS" \
    "$RIDER_LOGIN_BODY"

if [[ "$RIDER_LOGIN_STATUS" != 2* ]]; then
    echo "ERROR: Rider login failed."
    exit 1
fi

RIDER_TOKEN="$(
    extract_json "$RIDER_LOGIN_BODY" accessToken token
)"

if [[ -z "${RIDER_TOKEN:-}" ]]; then
    echo "ERROR: Could not extract rider access token."
    exit 1
fi

echo
echo "Rider JWT acquired."

# ============================================================
# 12. Create TRIP
# ============================================================

print_section "12. Creating TRIP"

MAX_ATTEMPTS=15
SLEEP_SECONDS=1
TRIP_ID=""

TRIP_BODY="{
    \"pickupLatitude\": $PICKUP_LAT,
    \"pickupLongitude\": $PICKUP_LON,
    \"dropoffLatitude\": $DROPOFF_LAT,
    \"dropoffLongitude\": $DROPOFF_LON
}"

echo "Trip request:"
echo "$TRIP_BODY"

for ((attempt=1; attempt<=MAX_ATTEMPTS; attempt++)); do
    echo
    echo "Attempt $attempt/$MAX_ATTEMPTS: creating trip..."

    TRIP_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X POST \
            "$BASE_URL/api/v1/trips" \
            -H "Authorization: Bearer $RIDER_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$TRIP_BODY"
    )

    TRIP_STATUS="$(echo "$TRIP_RESPONSE" | tail -n1)"
    TRIP_RESPONSE_BODY="$(echo "$TRIP_RESPONSE" | sed '$d')"

    print_response "$TRIP_STATUS" "$TRIP_RESPONSE_BODY"

    if [[ "$TRIP_STATUS" == 2* ]]; then
        TRIP_ID="$(
            extract_json "$TRIP_RESPONSE_BODY" tripId id
        )"

        if [[ -n "${TRIP_ID:-}" ]]; then
            break
        fi

        echo "Trip creation succeeded, but no trip ID was returned."
    else
        echo "Trip creation attempt failed."
    fi

    if (( attempt < MAX_ATTEMPTS )); then
        echo "Waiting ${SLEEP_SECONDS}s before retrying..."
        sleep "$SLEEP_SECONDS"
    fi
done

if [[ -z "${TRIP_ID:-}" ]]; then
    echo
    echo "ERROR: Trip was not created successfully after $MAX_ATTEMPTS attempts."
    exit 1
fi

echo
echo "Trip ID: $TRIP_ID"

# ============================================================
# Helper: get trip status
# ============================================================

get_trip_status() {
    local response body code

    response=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X GET \
            "$BASE_URL/api/v1/trips/status/$TRIP_ID" \
            -H "Authorization: Bearer $RIDER_TOKEN"
    )

    code="$(echo "$response" | tail -n1)"
    body="$(echo "$response" | sed '$d')"

    echo "DEBUG HTTP STATUS: $code" >&2
    echo "DEBUG RESPONSE BODY: $body" >&2

    if [[ "$code" != 2* ]]; then
        echo "HTTP_$code"
        return 0
    fi

    extract_json "$body" status
}

wait_for_trip_status() {
    local expected="$1"
    local max_attempts="${2:-30}"
    local sleep_seconds="${3:-1}"
    local current

    for ((attempt=1; attempt<=max_attempts; attempt++)); do
        current="$(get_trip_status)"

        echo "Trip status: $current (waiting for $expected) [$attempt/$max_attempts]"

        if [[ "$current" == "$expected" ]]; then
            return 0
        fi

        if [[ "$current" == "CANCELLED" ]]; then
            echo "ERROR: Trip was cancelled while waiting for $expected."
            return 1
        fi

        sleep "$sleep_seconds"
    done

    echo
    echo "ERROR: Timed out waiting for trip status $expected."
    return 1
}

# ============================================================
# 13. Wait for DRIVER_ASSIGNED
# ============================================================

print_section "13. Waiting for DRIVER ASSIGNMENT"

wait_for_trip_status "DRIVER_ASSIGNED" 30 1

echo
echo "Driver assigned successfully."

# ============================================================
# 14. DRIVER ACCEPTS TRIP
# ============================================================

print_section "14. DRIVER ACCEPTING TRIP"

DRIVER_ACCEPT_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/drivers/me/trips/$TRIP_ID/accept" \
        -H "Authorization: Bearer $DRIVER_TOKEN"
)

DRIVER_ACCEPT_STATUS="$(echo "$DRIVER_ACCEPT_RESPONSE" | tail -n1)"
DRIVER_ACCEPT_BODY="$(echo "$DRIVER_ACCEPT_RESPONSE" | sed '$d')"

print_response \
    "$DRIVER_ACCEPT_STATUS" \
    "$DRIVER_ACCEPT_BODY"

if [[ "$DRIVER_ACCEPT_STATUS" != 2* ]]; then
    echo "ERROR: Driver could not accept trip."
    exit 1
fi

echo
echo "Driver accepted trip."

# ============================================================
# 15. Wait for DRIVER_ARRIVING
# ============================================================

print_section "15. Waiting for DRIVER_ARRIVING"

wait_for_trip_status "DRIVER_ARRIVING" 30 1

echo
echo "Trip is now DRIVER_ARRIVING."

# ============================================================
# 16. DRIVER ARRIVES
# ============================================================

print_section "16. DRIVER ARRIVING AT PICKUP"

DRIVER_ARRIVE_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/drivers/me/trips/$TRIP_ID/arrive" \
        -H "Authorization: Bearer $DRIVER_TOKEN"
)

DRIVER_ARRIVE_STATUS="$(echo "$DRIVER_ARRIVE_RESPONSE" | tail -n1)"
DRIVER_ARRIVE_BODY="$(echo "$DRIVER_ARRIVE_RESPONSE" | sed '$d')"

print_response \
    "$DRIVER_ARRIVE_STATUS" \
    "$DRIVER_ARRIVE_BODY"

if [[ "$DRIVER_ARRIVE_STATUS" != 2* ]]; then
    echo "ERROR: Driver could not mark arrival."
    exit 1
fi

echo
echo "Driver arrived at pickup."

# ============================================================
# 17. Wait for DRIVER_ARRIVED
# ============================================================

print_section "17. Waiting for DRIVER_ARRIVED"

wait_for_trip_status "DRIVER_ARRIVED" 30 1

echo
echo "Trip is now DRIVER_ARRIVED."

# ============================================================
# 18. DRIVER STARTS TRIP
# ============================================================

print_section "18. DRIVER STARTING TRIP"

DRIVER_START_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/drivers/me/trips/$TRIP_ID/start" \
        -H "Authorization: Bearer $DRIVER_TOKEN"
)

DRIVER_START_STATUS="$(echo "$DRIVER_START_RESPONSE" | tail -n1)"
DRIVER_START_BODY="$(echo "$DRIVER_START_RESPONSE" | sed '$d')"

print_response \
    "$DRIVER_START_STATUS" \
    "$DRIVER_START_BODY"

if [[ "$DRIVER_START_STATUS" != 2* ]]; then
    echo "ERROR: Driver could not start trip."
    exit 1
fi

echo
echo "Driver started trip."

# ============================================================
# 19. Wait for TRIP_STARTED
# ============================================================

print_section "19. Waiting for TRIP_STARTED"

wait_for_trip_status "TRIP_STARTED" 30 1

echo
echo "Trip is now TRIP_STARTED."

# ============================================================
# 20. DRIVER COMPLETES TRIP
# ============================================================

print_section "20. DRIVER COMPLETING TRIP"

# Give the trip a small non-zero duration so pricing has a realistic
# duration value.
sleep 2

DRIVER_COMPLETE_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/drivers/me/trips/$TRIP_ID/complete" \
        -H "Authorization: Bearer $DRIVER_TOKEN"
)

DRIVER_COMPLETE_STATUS="$(echo "$DRIVER_COMPLETE_RESPONSE" | tail -n1)"
DRIVER_COMPLETE_BODY="$(echo "$DRIVER_COMPLETE_RESPONSE" | sed '$d')"

print_response \
    "$DRIVER_COMPLETE_STATUS" \
    "$DRIVER_COMPLETE_BODY"

if [[ "$DRIVER_COMPLETE_STATUS" != 2* ]]; then
    echo "ERROR: Driver could not complete trip."
    exit 1
fi

echo
echo "Driver completed trip."

# ============================================================
# 21. Wait for TRIP_COMPLETED
# ============================================================

print_section "21. Waiting for TRIP_COMPLETED"

wait_for_trip_status "TRIP_COMPLETED" 30 1

echo
echo "Trip completed."

# ============================================================
# 22. Wait for FARE
# ============================================================

print_section "22. Waiting for FARE"

FARE_BODY=""
FARE_TOTAL=""

for ((attempt=1; attempt<=30; attempt++)); do
    FARE_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X GET \
            "$BASE_URL/api/v1/pricing/trips/$TRIP_ID" \
            -H "Authorization: Bearer $RIDER_TOKEN"
    )

    FARE_STATUS_CODE="$(echo "$FARE_RESPONSE" | tail -n1)"
    FARE_BODY="$(echo "$FARE_RESPONSE" | sed '$d')"

    echo "DEBUG FARE HTTP STATUS: $FARE_STATUS_CODE" >&2

    if [[ "$FARE_STATUS_CODE" == 2* ]]; then
        FARE_TOTAL="$(
            extract_json "$FARE_BODY" totalFare
        )"

        if [[ -n "${FARE_TOTAL:-}" ]]; then
            break
        fi
    fi

    echo "Fare not available yet [$attempt/30]. Waiting 1s..."
    sleep 1
done

if [[ -z "${FARE_TOTAL:-}" ]]; then
    echo "ERROR: Fare was not available after trip completion."
    echo "Last response:"
    echo "$FARE_BODY"
    exit 1
fi

echo
echo "Fare response:"
echo "$FARE_BODY"
echo
echo "Total fare: $FARE_TOTAL"

# ============================================================
# 23. Wait for PAYMENT RECORD
# ============================================================

print_section "23. Waiting for PAYMENT RECORD"

PAYMENT_ID=""
PAYMENT_STATUS=""

for ((attempt=1; attempt<=30; attempt++)); do
    PAYMENT_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X GET \
            "$BASE_URL/api/v1/payments/trips/$TRIP_ID" \
            -H "Authorization: Bearer $RIDER_TOKEN"
    )

    PAYMENT_HTTP_STATUS="$(echo "$PAYMENT_RESPONSE" | tail -n1)"
    PAYMENT_BODY="$(echo "$PAYMENT_RESPONSE" | sed '$d')"
    echo "DEBUG PAYMENT HTTP STATUS: $PAYMENT_HTTP_STATUS" >&2

    if [[ "$PAYMENT_HTTP_STATUS" == 2* ]]; then
        PAYMENT_ID="$(
            extract_json "$PAYMENT_BODY" id paymentId
        )"
        PAYMENT_STATUS="$(
            extract_json "$PAYMENT_BODY" status
        )"

        if [[ -n "${PAYMENT_ID:-}" ]]; then
            break
        fi
    fi

    echo "Payment record not available yet [$attempt/30]. Waiting 1s..."
    sleep 1
done

if [[ -z "${PAYMENT_ID:-}" ]]; then
    echo "ERROR: Payment record was not created after trip completion."
    echo "Last response:"
    echo "$PAYMENT_BODY"
    exit 1
fi

echo
echo "Payment ID: $PAYMENT_ID"
echo "Payment status: $PAYMENT_STATUS"

# ============================================================
# 24. Initiate PAYMENT
# ============================================================

print_section "24. Initiating PAYMENT"

PAYMENT_INITIATE_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/payments/$PAYMENT_ID/initiate" \
        -H "Authorization: Bearer $RIDER_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "paymentMethod": "UPI"
        }'
)

PAYMENT_INITIATE_STATUS="$(echo "$PAYMENT_INITIATE_RESPONSE" | tail -n1)"
PAYMENT_INITIATE_BODY="$(echo "$PAYMENT_INITIATE_RESPONSE" | sed '$d')"

print_response \
    "$PAYMENT_INITIATE_STATUS" \
    "$PAYMENT_INITIATE_BODY"

if [[ "$PAYMENT_INITIATE_STATUS" != 2* ]]; then
    echo "ERROR: Payment initiation failed."
    exit 1
fi

PAYMENT_STATUS="$(
    extract_json "$PAYMENT_INITIATE_BODY" status
)"

echo
echo "Payment after initiation: ${PAYMENT_STATUS:-UNKNOWN}"

# ============================================================
# 25. Simulate successful MOCK PAYMENT WEBHOOK
# ============================================================

print_section "25. Sending MOCK PAYMENT SUCCESS webhook"

# The mock provider generates providerPaymentId as:
#   mock_<payment idempotency key>
# Prefer providerPaymentId returned by the initiate response. If the response
# instead exposes idempotencyKey, derive the mock provider ID from that.
# MOCK_PROVIDER_PAYMENT_ID can be supplied explicitly if the API response
# doesn't expose either value.
PAYMENT_IDEMPOTENCY_KEY="$(extract_json "$PAYMENT_INITIATE_BODY" idempotencyKey)"
PROVIDER_PAYMENT_ID="${MOCK_PROVIDER_PAYMENT_ID:-$(extract_json "$PAYMENT_INITIATE_BODY" providerPaymentId)}"

if [[ -z "$PROVIDER_PAYMENT_ID" && -n "$PAYMENT_IDEMPOTENCY_KEY" ]]; then
    PROVIDER_PAYMENT_ID="mock_${PAYMENT_IDEMPOTENCY_KEY}"
fi

if [[ -z "$PROVIDER_PAYMENT_ID" ]]; then
    echo "ERROR: Could not determine providerPaymentId for the mock webhook."
    echo "The payment initiation response must include providerPaymentId or idempotencyKey,"
    echo "or rerun with MOCK_PROVIDER_PAYMENT_ID=<actual-provider-payment-id>."
    echo "Initiation response:"
    echo "$PAYMENT_INITIATE_BODY"
    exit 1
fi

WEBHOOK_EVENT_ID="ridex-test-success-${TIMESTAMP}-$(date +%s)-${RANDOM}"
WEBHOOK_PAYLOAD="$(python3 - "$PROVIDER_PAYMENT_ID" "$PAYMENT_IDEMPOTENCY_KEY" "$WEBHOOK_EVENT_ID" <<'PY'
import json
import sys

print(json.dumps({
    "providerPaymentId": sys.argv[1],
    "idempotencyKey": sys.argv[2],
    "status": "SUCCESS",
    "eventId": sys.argv[3],
    "failureReason": None
}))
PY
)"

echo "Provider payment ID: $PROVIDER_PAYMENT_ID"
echo "Webhook event ID: $WEBHOOK_EVENT_ID"

echo "Sending mock SUCCESS webhook..."
PAYMENT_WEBHOOK_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/payments/webhooks/mock" \
        -H "Content-Type: application/json" \
        -H "X-Payment-Signature: mock-valid-signature" \
        -d "$WEBHOOK_PAYLOAD"
)

PAYMENT_WEBHOOK_STATUS="$(echo "$PAYMENT_WEBHOOK_RESPONSE" | tail -n1)"
PAYMENT_WEBHOOK_BODY="$(echo "$PAYMENT_WEBHOOK_RESPONSE" | sed '$d')"

print_response "$PAYMENT_WEBHOOK_STATUS" "$PAYMENT_WEBHOOK_BODY"

if [[ "$PAYMENT_WEBHOOK_STATUS" != 2* ]]; then
    echo "ERROR: Mock payment SUCCESS webhook was rejected."
    echo "Check that the payment attempt is PROCESSING and providerPaymentId matches."
    exit 1
fi

echo "Mock payment SUCCESS webhook accepted."

# ============================================================
# 26. Wait for PAYMENT SUCCESS
# ============================================================

print_section "26. Waiting for PAYMENT SUCCESS"

PAYMENT_SUCCESS=false

for ((attempt=1; attempt<=30; attempt++)); do
    PAYMENT_GET_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X GET \
            "$BASE_URL/api/v1/payments/trips/$TRIP_ID" \
            -H "Authorization: Bearer $RIDER_TOKEN"
    )

    PAYMENT_GET_HTTP_STATUS="$(echo "$PAYMENT_GET_RESPONSE" | tail -n1)"
    PAYMENT_BODY="$(echo "$PAYMENT_GET_RESPONSE" | sed '$d')"

    if [[ "$PAYMENT_GET_HTTP_STATUS" == 2* ]]; then
        PAYMENT_STATUS="$(
            extract_json "$PAYMENT_BODY" status
        )"

        echo "Payment status: $PAYMENT_STATUS [$attempt/30]"

        if [[ "$PAYMENT_STATUS" == "SUCCESS" ]]; then
            PAYMENT_SUCCESS=true
            break
        fi

        if [[ "$PAYMENT_STATUS" == "FAILED" ]]; then
            echo "ERROR: Payment transitioned to FAILED."
            echo "$PAYMENT_BODY"
            exit 1
        fi
    else
        echo "Payment lookup returned HTTP $PAYMENT_GET_HTTP_STATUS [$attempt/30]"
    fi

    sleep 1
done

if [[ "$PAYMENT_SUCCESS" != true ]]; then
    echo
    echo "ERROR: Payment did not reach SUCCESS."
    echo
    echo "Current payment response:"
    echo "$PAYMENT_BODY"
    echo
    echo "If your MockPaymentProvider requires an explicit webhook,"
    echo "the webhook contract/signature implementation must be added"
    echo "to this script before the payment flow can be automated."
    exit 1
fi

echo
echo "Payment succeeded."


# ============================================================
# 27. Verify TRIP PAYMENT PROJECTION (WITH RETRIES)
# ============================================================

print_section "27. Verifying TRIP PAYMENT PROJECTION"

MAX_RETRIES=15
RETRY_INTERVAL=2
PROJECTION_VERIFIED=false

for ((attempt = 1; attempt <= MAX_RETRIES; attempt++)); do

    echo
    echo "Attempt $attempt/$MAX_RETRIES: Checking Trip payment projection..."

    TRIP_PAYMENT_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X GET \
            "$BASE_URL/api/v1/trips/$TRIP_ID/payment" \
            -H "Authorization: Bearer $RIDER_TOKEN"
    )

    TRIP_PAYMENT_HTTP_STATUS="$(echo "$TRIP_PAYMENT_RESPONSE" | tail -n1)"
    TRIP_PAYMENT_BODY="$(echo "$TRIP_PAYMENT_RESPONSE" | sed '$d')"

    print_response \
        "$TRIP_PAYMENT_HTTP_STATUS" \
        "$TRIP_PAYMENT_BODY"

    if [[ "$TRIP_PAYMENT_HTTP_STATUS" == 2* ]]; then

        TRIP_PAYMENT_ID="$(
            extract_json "$TRIP_PAYMENT_BODY" paymentId
        )"

        TRIP_PAYMENT_STATUS="$(
            extract_json "$TRIP_PAYMENT_BODY" status
        )"

        TRIP_PAYMENT_AMOUNT="$(
            extract_json "$TRIP_PAYMENT_BODY" amount
        )"

        # Verify that the projection refers to the expected payment.
        if [[ "$TRIP_PAYMENT_ID" != "$PAYMENT_ID" ]]; then
            echo "ERROR: Trip payment projection paymentId does not match Payment Service."
            echo "Payment Service: $PAYMENT_ID"
            echo "Trip projection: $TRIP_PAYMENT_ID"
            exit 1
        fi

        # SUCCESS may arrive asynchronously through Kafka.
        if [[ "$TRIP_PAYMENT_STATUS" == "SUCCESS" ]]; then
            PROJECTION_VERIFIED=true
            break
        fi

        echo "Projection status is '$TRIP_PAYMENT_STATUS'; waiting for SUCCESS..."

    else
        echo "Projection request returned HTTP $TRIP_PAYMENT_HTTP_STATUS; retrying..."
    fi

    if (( attempt < MAX_RETRIES )); then
        sleep "$RETRY_INTERVAL"
    fi
done

if [[ "$PROJECTION_VERIFIED" != true ]]; then
    echo "ERROR: Trip payment projection did not reach SUCCESS within $((MAX_RETRIES * RETRY_INTERVAL)) seconds."
    echo "Last HTTP status: $TRIP_PAYMENT_HTTP_STATUS"
    echo "Last projection status: ${TRIP_PAYMENT_STATUS:-UNKNOWN}"
    echo "Payment ID: $PAYMENT_ID"
    exit 1
fi

echo
echo "Trip payment projection verified."
echo "Payment ID : $TRIP_PAYMENT_ID"
echo "Amount     : $TRIP_PAYMENT_AMOUNT"
echo "Status     : $TRIP_PAYMENT_STATUS"

# ============================================================
# 28. FINAL TRIP VERIFICATION
# ============================================================

print_section "28. Final TRIP Verification"

FINAL_TRIP_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X GET \
        "$BASE_URL/api/v1/trips/$TRIP_ID" \
        -H "Authorization: Bearer $RIDER_TOKEN"
)

FINAL_TRIP_HTTP_STATUS="$(echo "$FINAL_TRIP_RESPONSE" | tail -n1)"
FINAL_TRIP_BODY="$(echo "$FINAL_TRIP_RESPONSE" | sed '$d')"

print_response \
    "$FINAL_TRIP_HTTP_STATUS" \
    "$FINAL_TRIP_BODY"

if [[ "$FINAL_TRIP_HTTP_STATUS" != 2* ]]; then
    echo "ERROR: Final trip lookup failed."
    exit 1
fi

FINAL_TRIP_STATUS="$(
    extract_json "$FINAL_TRIP_BODY" status
)"

FINAL_DRIVER_ID="$(
    extract_json "$FINAL_TRIP_BODY" driverId
)"

if [[ "$FINAL_TRIP_STATUS" != "TRIP_COMPLETED" ]]; then
    echo "ERROR: Final trip status is $FINAL_TRIP_STATUS."
    exit 1
fi

if [[ "$FINAL_DRIVER_ID" != "$DRIVER_ID" ]]; then
    echo "ERROR: Final trip driver does not match test driver."
    echo "Expected: $DRIVER_ID"
    echo "Actual  : $FINAL_DRIVER_ID"
    exit 1
fi

echo
echo "Final trip verification passed."

# ============================================================
# FINAL SUMMARY
# ============================================================

print_section "RideX TEST SUMMARY"

echo "Driver"
echo "  Email       : $DRIVER_EMAIL"
echo "  User ID     : ${DRIVER_USER_ID:-N/A}"
echo "  Driver ID   : $DRIVER_ID"

echo
echo "Rider"
echo "  Email       : $RIDER_EMAIL"
echo "  User ID     : ${RIDER_USER_ID:-N/A}"

echo
echo "Trip"
echo "  Trip ID     : $TRIP_ID"
echo "  Status      : $FINAL_TRIP_STATUS"
echo "  Driver ID   : $FINAL_DRIVER_ID"

echo
echo "Fare"
echo "  Total       : $FARE_TOTAL"

echo
echo "Payment"
echo "  Payment ID  : $PAYMENT_ID"
echo "  Amount      : ${TRIP_PAYMENT_AMOUNT:-N/A}"
echo "  Status      : $PAYMENT_STATUS"

echo
echo "Coordinates"
echo "  Pickup      : $PICKUP_LAT, $PICKUP_LON"
echo "  Dropoff     : $DROPOFF_LAT, $DROPOFF_LON"

echo
echo "============================================================"
echo "RideX end-to-end test completed successfully."
echo "============================================================"
