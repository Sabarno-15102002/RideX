
#!/usr/bin/env bash
set -Eeuo pipefail

SOURCE_TOPIC="ridex.dlt.test"
DLT_TOPIC="${SOURCE_TOPIC}.DLT"
BOOTSTRAP="kafka:9092"
TEST_ID="dlt-replay-$(date +%s)-$$"
PAYLOAD="{\"testId\":\"${TEST_ID}\",\"message\":\"DLT replay test\"}"

TEMP_DLT="$(mktemp)"
TEMP_SOURCE="$(mktemp)"
trap 'rm -f "$TEMP_DLT" "$TEMP_SOURCE"' EXIT

echo "==> Building driver-service image..."
docker compose build driver-service

echo "==> Creating test topics if needed..."
docker compose exec -T kafka \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP" \
  --create --if-not-exists \
  --topic "$SOURCE_TOPIC" \
  --partitions 3 \
  --replication-factor 1

docker compose exec -T kafka \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP" \
  --create --if-not-exists \
  --topic "$DLT_TOPIC" \
  --partitions 3 \
  --replication-factor 1

echo "==> Publishing test event: $TEST_ID"
printf '%s\n' "${TEST_ID}:${PAYLOAD}" |
  docker compose exec -T kafka \
    /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server "$BOOTSTRAP" \
    --topic "$SOURCE_TOPIC" \
    --property "parse.key=true" \
    --property "key.separator=:"

echo "==> Waiting for the event to reach the DLT..."
DLT_FOUND=false

for attempt in $(seq 1 20); do
  set +e
  docker compose exec -T kafka \
    /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server "$BOOTSTRAP" \
    --topic "$DLT_TOPIC" \
    --from-beginning \
    --timeout-ms 1500 \
    --property "print.key=true" \
    --property "key.separator=:" \
    > "$TEMP_DLT" 2>&1
  set -e

  if grep -Fq "$TEST_ID" "$TEMP_DLT"; then
    DLT_FOUND=true
    break
  fi

  sleep 1
done

if [[ "$DLT_FOUND" != true ]]; then
  echo "❌ Event did not appear in $DLT_TOPIC."
  echo "Check driver-service logs and the DLT test listener flag."
  exit 1
fi

echo "✅ Event found in DLT."

echo "==> Replaying this exact test ID in a one-off Docker container..."
docker compose run -d --rm --no-deps \
  -e RIDEX_KAFKA_DLT_REPLAY_ENABLED=true \
  -e RIDEX_KAFKA_DLT_TEST_ENABLED=false \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS="$BOOTSTRAP" \
  driver-service \
  --spring.kafka.listener.auto-startup=false \
  --test-id="$TEST_ID"

echo "==> Checking that the event was republished to $SOURCE_TOPIC..."
set +e
docker compose exec -T kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server "$BOOTSTRAP" \
  --topic "$SOURCE_TOPIC" \
  --from-beginning \
  --timeout-ms 5000 \
  --property "print.key=true" \
  --property "key.separator=:" \
  > "$TEMP_SOURCE" 2>&1
set -e

if grep -Fq "$TEST_ID" "$TEMP_SOURCE"; then
  echo
  echo "✅ DLT REPLAY TEST PASSED"
  echo "Test ID: $TEST_ID"
  echo "Original DLT record was retained."
else
  echo
  echo "❌ Replay verification failed."
  echo "The event was not found in $SOURCE_TOPIC."
  cat "$TEMP_SOURCE"
  exit 1
fi
