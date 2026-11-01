#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

# ------------------------------------------------------------
# Test users
# ------------------------------------------------------------

TIMESTAMP="$(date +%s)"

RIDER_NAME="RideX Test Rider${TIMESTAMP}"
RIDER_EMAIL="rider.${TIMESTAMP}@ridex.local"
RIDER_PASSWORD="Password@123"

# ------------------------------------------------------------
# Helpers
# ------------------------------------------------------------

print_section() {
    echo
    echo "============================================================"
    echo "$1"
    echo "============================================================"
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

print_response() {
    local status="$1"
    local body="$2"

    echo
    echo "HTTP Status: $status"
    echo "Response:"
    echo "$body"
}

inspect_rider_profile() {
    local user_id="$1"

    echo
    echo "----- Matching Redis keys -----"
    docker exec ridex-redis redis-cli --scan \
      --pattern "ridex:*${user_id}*"

    echo
    echo "----- All Rider profile cache keys -----"
    docker exec ridex-redis redis-cli --scan \
      --pattern 'ridex:riderProfile::*'
}

create_phone() {
    echo "$((6 + RANDOM % 4))$(printf '%09d' $((RANDOM % 1000000000)))"
}

redis_key_for_user() {
    local user_id="$1"
    echo "ridex:riderProfile::${user_id}"
}

inspect_cache_entry() {
    local user_id="$1"
    local key

    key="$(redis_key_for_user "$user_id")"

    echo
    echo "----- Redis entry for user: $user_id -----"

    if docker exec ridex-redis redis-cli EXISTS "$key" | grep -q '^1$'; then
        echo "Key exists: $key"

        echo "TTL:"
        docker exec ridex-redis redis-cli TTL "$key"

        echo "Value:"
        docker exec ridex-redis redis-cli GET "$key"
    else
        echo "No cache entry found for: $key"
    fi
}

clear_cache_for_user() {
    local user_id="$1"
    local key

    key="$(redis_key_for_user "$user_id")"

    docker exec ridex-redis redis-cli DEL "$key"
}

get_rider_profile() {
    local token="$1"

    curl -sS \
        -w '\n%{http_code}' \
        -X GET \
        "$BASE_URL/api/v1/riders/me" \
        -H "Authorization: Bearer $token"
}

# ============================================================
# 1. Gateway health
# ============================================================

print_section "1. API Gateway Health"

curl -fsS "$BASE_URL/actuator/health"

echo
echo "Gateway is UP."

# ============================================================
# 2. Register RIDER
# ============================================================

print_section "2. Registering RIDER"

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
# 3. Login RIDER
# ============================================================

print_section "3. Logging in RIDER"

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
# 4. Get RIDER Profile
# ============================================================

print_section "4. Waiting for RIDER Profile"

MAX_ATTEMPTS=10
SLEEP_SECONDS=1

RIDER_ID=""

for ((attempt=1; attempt<=MAX_ATTEMPTS; attempt++)); do

    echo
    echo "Attempt $attempt/$MAX_ATTEMPTS: checking Rider Service..."

    RIDER_GET_RESPONSE=$(
        curl -sS \
            -w '\n%{http_code}' \
            -X GET \
            "$BASE_URL/api/v1/riders/me" \
            -H "Authorization: Bearer $RIDER_TOKEN"
    )

    RIDER_GET_STATUS="$(echo "$RIDER_GET_RESPONSE" | tail -n1)"
    RIDER_GET_BODY="$(echo "$RIDER_GET_RESPONSE" | sed '$d')"

    print_response \
        "$RIDER_GET_STATUS" \
        "$RIDER_GET_BODY"

    echo
    echo "----- Redis state after GET -----"
    inspect_rider_profile "$RIDER_USER_ID"

    if [[ "$RIDER_GET_STATUS" == 2* ]]; then

        RIDER_ID="$(
            extract_json "$RIDER_GET_BODY" id
        )"

        if [[ -n "${RIDER_ID:-}" ]]; then
            break
        fi
    fi

    echo
    echo "Rider profile not available yet."
    echo "Waiting ${SLEEP_SECONDS}s..."
    sleep "$SLEEP_SECONDS"
done

if [[ -z "${RIDER_ID:-}" ]]; then
    echo
    echo "ERROR: Rider profile was not returned within the expected time."
    echo
    echo "Check Rider Service logs:"
    echo "  docker compose logs rider-service --tail=100"
    exit 1
fi

echo
echo "============================================================"
echo "RIDER ID = $RIDER_ID"
echo "USER ID   = $RIDER_USER_ID"
echo "============================================================"

# ============================================================
# 5. Repeat GET - Cache Hit
# ============================================================

print_section "5. Repeat GET - Cache Hit"

echo "First, inspect the existing cache entry:"
inspect_cache_entry "$RIDER_USER_ID"

echo
echo "Calling profile endpoint again..."

REPEAT_GET_RESPONSE="$(
    get_rider_profile "$RIDER_TOKEN"
)"

REPEAT_GET_STATUS="$(echo "$REPEAT_GET_RESPONSE" | tail -n1)"
REPEAT_GET_BODY="$(echo "$REPEAT_GET_RESPONSE" | sed '$d')"

print_response "$REPEAT_GET_STATUS" "$REPEAT_GET_BODY"

if [[ "$REPEAT_GET_STATUS" != 2* ]]; then
    echo "ERROR: Repeated GET failed."
    exit 1
fi

echo
echo "Redis after repeated GET:"
inspect_cache_entry "$RIDER_USER_ID"

# ============================================================
# 6. Second Rider - Separate Cache Entry
# ============================================================

print_section "6. Second Rider - Separate Cache Entry"

SECOND_TIMESTAMP="$(date +%s)"
SECOND_RIDER_NAME="RideX Second Rider${SECOND_TIMESTAMP}"
SECOND_RIDER_EMAIL="rider.second.${SECOND_TIMESTAMP}@ridex.local"
SECOND_RIDER_PASSWORD="Password@123"

SECOND_REGISTER_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"$SECOND_RIDER_NAME\",
            \"email\": \"$SECOND_RIDER_EMAIL\",
            \"password\": \"$SECOND_RIDER_PASSWORD\",
            \"phone\": \"+91$(create_phone)\",
            \"role\": \"RIDER\"
        }"
)

SECOND_REGISTER_STATUS="$(echo "$SECOND_REGISTER_RESPONSE" | tail -n1)"
SECOND_REGISTER_BODY="$(echo "$SECOND_REGISTER_RESPONSE" | sed '$d')"

print_response "$SECOND_REGISTER_STATUS" "$SECOND_REGISTER_BODY"

if [[ "$SECOND_REGISTER_STATUS" != 2* ]]; then
    echo "ERROR: Second rider registration failed."
    exit 1
fi

SECOND_RIDER_USER_ID="$(
    extract_json "$SECOND_REGISTER_BODY" userId
)"

SECOND_LOGIN_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X POST \
        "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$SECOND_RIDER_EMAIL\",
            \"password\": \"$SECOND_RIDER_PASSWORD\"
        }"
)

SECOND_LOGIN_STATUS="$(echo "$SECOND_LOGIN_RESPONSE" | tail -n1)"
SECOND_LOGIN_BODY="$(echo "$SECOND_LOGIN_RESPONSE" | sed '$d')"

if [[ "$SECOND_LOGIN_STATUS" != 2* ]]; then
    echo "ERROR: Second rider login failed."
    exit 1
fi

SECOND_RIDER_TOKEN="$(
    extract_json "$SECOND_LOGIN_BODY" accessToken
)"

echo "Second rider userId: $SECOND_RIDER_USER_ID"

# Wait until the Kafka consumer creates the second profile.
SECOND_RIDER_ID=""

for ((attempt=1; attempt<=30; attempt++)); do

    SECOND_GET_RESPONSE="$(
        get_rider_profile "$SECOND_RIDER_TOKEN"
    )"

    SECOND_GET_STATUS="$(echo "$SECOND_GET_RESPONSE" | tail -n1)"
    SECOND_GET_BODY="$(echo "$SECOND_GET_RESPONSE" | sed '$d')"

    if [[ "$SECOND_GET_STATUS" == 2* ]]; then
        SECOND_RIDER_ID="$(
            extract_json "$SECOND_GET_BODY" id
        )"

        if [[ -n "${SECOND_RIDER_ID:-}" ]]; then
            break
        fi
    fi

    sleep 1
done

if [[ -z "${SECOND_RIDER_ID:-}" ]]; then
    echo "ERROR: Second rider profile was not created."
    exit 1
fi

echo
echo "Redis entry for first rider:"
inspect_cache_entry "$RIDER_USER_ID"

echo
echo "Redis entry for second rider:"
inspect_cache_entry "$SECOND_RIDER_USER_ID"

# ============================================================
# 7. PUT - Update Profile and Evict Cache
# ============================================================

print_section "7. PUT - Update Profile and Evict Cache"

UPDATED_RIDER_NAME="Updated RideX Rider $(date +%s)"

echo "Cache before PUT:"
inspect_cache_entry "$RIDER_USER_ID"

PUT_RESPONSE=$(
    curl -sS \
        -w '\n%{http_code}' \
        -X PUT \
        "$BASE_URL/api/v1/riders/me" \
        -H "Authorization: Bearer $RIDER_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"$UPDATED_RIDER_NAME\"
        }"
)

PUT_STATUS="$(echo "$PUT_RESPONSE" | tail -n1)"
PUT_BODY="$(echo "$PUT_RESPONSE" | sed '$d')"

print_response "$PUT_STATUS" "$PUT_BODY"

if [[ "$PUT_STATUS" != 2* ]]; then
    echo "ERROR: Profile update failed."
    exit 1
fi

echo
echo "Cache after PUT:"
inspect_cache_entry "$RIDER_USER_ID"

RIDER_CACHE_KEY="$(redis_key_for_user "$RIDER_USER_ID")"

if [[ "$(docker exec ridex-redis redis-cli EXISTS "$RIDER_CACHE_KEY")" == "0" ]]; then
    echo "PASS: Rider profile cache was evicted."
else
    echo "FAIL: Rider profile cache still exists after PUT."
    exit 1
fi

# ============================================================
# 8. GET After PUT - Fresh Value and Cache Repopulation
# ============================================================

print_section "8. GET After PUT - Cache Repopulation"

POST_UPDATE_GET_RESPONSE="$(
    get_rider_profile "$RIDER_TOKEN"
)"

POST_UPDATE_GET_STATUS="$(echo "$POST_UPDATE_GET_RESPONSE" | tail -n1)"
POST_UPDATE_GET_BODY="$(echo "$POST_UPDATE_GET_RESPONSE" | sed '$d')"

print_response \
    "$POST_UPDATE_GET_STATUS" \
    "$POST_UPDATE_GET_BODY"

if [[ "$POST_UPDATE_GET_STATUS" != 2* ]]; then
    echo "ERROR: GET after update failed."
    exit 1
fi

RETURNED_NAME="$(
    extract_json "$POST_UPDATE_GET_BODY" name
)"

if [[ "$RETURNED_NAME" != "$UPDATED_RIDER_NAME" ]]; then
    echo "FAIL: Updated name was not returned."
    echo "Expected: $UPDATED_RIDER_NAME"
    echo "Actual:   $RETURNED_NAME"
    exit 1
fi

echo "PASS: Updated name returned."

echo
echo "Cache after GET:"
inspect_cache_entry "$RIDER_USER_ID"
