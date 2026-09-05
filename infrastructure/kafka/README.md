# RideX Kafka Infrastructure

Kafka is used as the event backbone for asynchronous communication
between RideX services.

## Broker

Local Kafka broker:

localhost:9092

## Topics

| Topic | Producer | Consumers |
|---|---|---|
| user.registered | Auth Service | Rider Service |

## Local Setup

Start Kafka:

docker compose up -d kafka

Create topics:

./infrastructure/kafka/topics/topics.sh