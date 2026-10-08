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

echo "Kafka topic setup completed."