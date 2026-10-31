#!/bin/bash

set -e

KAFKA_BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"

create_topic() {
    local topic=$1
    local partitions=$2

    /opt/kafka/bin/kafka-topics.sh \
        --create \
        --if-not-exists \
        --topic "$topic" \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --partitions "$partitions" \
        --replication-factor 1

    echo "Created/verified topic: $topic"
}

configure_topic_retention() {
    local topic=$1
    local retention_ms=$2

    /opt/kafka/bin/kafka-configs.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --entity-type topics \
        --entity-name "$topic" \
        --alter \
        --add-config "retention.ms=$retention_ms"

    echo "Configured retention for $topic: ${retention_ms}ms"
}

create_topic "user.registered" 3
create_topic "user.registered.DLT" 3
create_topic "driver.status.changed" 3
create_topic "driver.status.changed.DLT" 3
create_topic "rider.created" 3
create_topic "rider.created.DLT" 3
create_topic "trip.requested" 3
create_topic "trip.requested.DLT" 3
create_topic "driver.match.requested" 3
create_topic "driver.match.requested.DLT" 3
create_topic "driver.ride.accepted" 3
create_topic "driver.ride.accepted.DLT" 3
create_topic "driver.ride.rejected" 3
create_topic "driver.ride.rejected.DLT" 3
create_topic "driver.ride.expired" 3
create_topic "driver.ride.expired.DLT" 3
create_topic "driver.arrived" 3
create_topic "driver.arrived.DLT" 3
create_topic "driver.trip.started" 3
create_topic "driver.trip.started.DLT" 3
create_topic "driver.trip.completed" 3
create_topic "driver.trip.completed.DLT" 3
create_topic "trip.rematching" 3
create_topic "trip.rematching.DLT" 3
create_topic "trip.completed" 3
create_topic "trip.completed.DLT" 3
create_topic "payment.succeeded" 3
create_topic "payment.succeeded.DLT" 3
create_topic "payment.failed" 3
create_topic "payment.failed.DLT" 3
create_topic "payment.refunded" 3
create_topic "payment.refunded.DLT" 3
create_topic "payment.created" 3
create_topic "payment.created.DLT" 3
create_topic "ridex.lag.test" 1

# Configure dead-letter topics to retain messages for 14 days.
DLT_RETENTION_MS=1209600000

for topic in \
    user.registered.DLT \
    driver.status.changed.DLT \
    rider.created.DLT \
    trip.requested.DLT \
    driver.match.requested.DLT \
    driver.ride.accepted.DLT \
    driver.ride.rejected.DLT \
    driver.ride.expired.DLT \
    driver.arrived.DLT \
    driver.trip.started.DLT \
    driver.trip.completed.DLT \
    trip.rematching.DLT \
    trip.completed.DLT \
    payment.succeeded.DLT \
    payment.failed.DLT \
    payment.refunded.DLT \
    payment.created.DLT
do
    configure_topic_retention "$topic" "$DLT_RETENTION_MS"
done

echo "Kafka topic setup and retention configuration completed."

echo "Kafka topic setup completed."