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
    echo "$BASE_URL/api/v1/drivers/$DRIVER_ID/onboarding"
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
# 9. Create TRIP
# ============================================================

sleep 1 # To ensure driver is available before creating trip
print_section "9. Creating TRIP"

TRIP_BODY="{
    \"pickupLatitude\": $PICKUP_LAT,
    \"pickupLongitude\": $PICKUP_LON,
    \"dropoffLatitude\": $DROPOFF_LAT,
    \"dropoffLongitude\": $DROPOFF_LON
}"

echo "Trip request:"
echo "$TRIP_BODY"

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

print_response \
    "$TRIP_STATUS" \
    "$TRIP_RESPONSE_BODY"

if [[ "$TRIP_STATUS" != 2* ]]; then
    echo "ERROR: Trip creation failed."
    exit 1
fi

TRIP_ID="$(
    extract_json "$TRIP_RESPONSE_BODY" tripId id
)"

echo
echo "Trip ID: ${TRIP_ID:-NOT_RETURNED}"

# ============================================================
# 10. Check Trip Status
# ============================================================

if [[ -n "${TRIP_ID:-}" ]]; then

    print_section "10. Checking TRIP Status"

    TRIP_STATUS_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X GET \
            "$BASE_URL/api/v1/trips/$TRIP_ID" \
            -H "Authorization: Bearer $RIDER_TOKEN"
    )

    TRIP_STATUS_CODE="$(echo "$TRIP_STATUS_RESPONSE" | tail -n1)"
    TRIP_STATUS_BODY="$(echo "$TRIP_STATUS_RESPONSE" | sed '$d')"

    print_response \
        "$TRIP_STATUS_CODE" \
        "$TRIP_STATUS_BODY"
fi

# ============================================================
# FINAL SUMMARY
# ============================================================

print_section "RideX TEST SUMMARY"

echo "Driver"
echo "  Email    : $DRIVER_EMAIL"
echo "  User ID  : ${DRIVER_USER_ID:-N/A}"
echo "  Driver ID: $DRIVER_ID"

echo
echo "Rider"
echo "  Email    : $RIDER_EMAIL"
echo "  User ID  : ${RIDER_USER_ID:-N/A}"

echo
echo "Trip"
echo "  Trip ID  : ${TRIP_ID:-N/A}"

echo
echo "Coordinates"
echo "  Pickup   : $PICKUP_LAT, $PICKUP_LON"
echo "  Dropoff  : $DROPOFF_LAT, $DROPOFF_LON"

echo
echo "============================================================"
echo "RideX end-to-end test completed successfully."
echo "============================================================"