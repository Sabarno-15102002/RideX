#!/bin/bash

set -e

KAFKA_CONTAINER="${KAFKA_CONTAINER:-ridex-kafka}"

create_topic() {
    local topic=$1
    local partitions=$2

    docker exec "$KAFKA_CONTAINER" \
        /opt/kafka/bin/kafka-topics.sh \
        --create \
        --if-not-exists \
        --topic "$topic" \
        --bootstrap-server localhost:9092 \
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
create_topic "trip.rematching" 3
create_topic "trip.rematching.DLT" 3

echo "Kafka topic setup completed."