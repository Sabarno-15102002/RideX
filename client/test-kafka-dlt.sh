
#!/usr/bin/env bash
set -Eeuo pipefail

# Run from the RideX repository root.
KAFKA_SERVICE="kafka"
BOOTSTRAP_SERVER="kafka:9092"
SOURCE_TOPIC="ridex.dlt.test"
DLT_TOPIC="${SOURCE_TOPIC}.DLT"

# Unique ID prevents old DLT messages from passing this test.
TEST_ID="dlt-test-$(date +%s)-$$"
PAYLOAD="{\"testId\":\"${TEST_ID}\",\"message\":\"Intentional DLT test\"}"

TEMP_OUTPUT="$(mktemp)"
trap 'rm -f "$TEMP_OUTPUT"' EXIT

kafka() {
  docker compose exec -T "$KAFKA_SERVICE" \
    /opt/kafka/bin/"$@"
}

echo "==> Creating test topics if needed..."

kafka kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --create --if-not-exists \
  --topic "$SOURCE_TOPIC" \
  --partitions 3 \
  --replication-factor 1

kafka kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --create --if-not-exists \
  --topic "$DLT_TOPIC" \
  --partitions 3 \
  --replication-factor 1

echo "==> Publishing test message: $TEST_ID"

printf '%s\n' "${TEST_ID}:${PAYLOAD}" |
  kafka kafka-console-producer.sh \
    --bootstrap-server "$BOOTSTRAP_SERVER" \
    --topic "$SOURCE_TOPIC" \
    --property "parse.key=true" \
    --property "key.separator=:"

echo "==> Waiting for Spring Kafka retries and DLT publication..."
sleep 5

echo "==> Reading DLT topic..."

# The timeout is expected: the consumer exits after waiting for
# more records. We verify the unique test ID in its captured output.
set +e
kafka kafka-console-consumer.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic "$DLT_TOPIC" \
  --from-beginning \
  --timeout-ms 15000 \
  --property "print.key=true" \
  --property "key.separator=:" \
  > "$TEMP_OUTPUT" 2>&1
set -e

if grep -Fq "$TEST_ID" "$TEMP_OUTPUT"; then
  echo
  echo "✅ DLT TEST PASSED"
  echo "Found test message in $DLT_TOPIC:"
  grep -F "$TEST_ID" "$TEMP_OUTPUT"
  exit 0
else
  echo
  echo "❌ DLT TEST FAILED"
  echo "Test message not found in $DLT_TOPIC."
  echo "Consumer output:"
  cat "$TEMP_OUTPUT"
  echo
  echo "Check that driver-service is running and"
  echo "RIDEX_KAFKA_DLT_TEST_ENABLED=true is configured."
  exit 1
fi
