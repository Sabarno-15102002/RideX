const { Client } = require('@stomp/stompjs');
const WebSocket = require('ws');

const client = new Client({
    brokerURL: 'ws://localhost:8084/ws/location',

    onConnect: () => {
        console.log('STOMP CONNECTED');

        client.subscribe(
            '/topic/drivers/75b61d0d-aa46-4397-9ed5-79f981f41c71/location',
            message => {
                console.log('LOCATION:', message.body);
            }
        );
    },

    onStompError: frame => {
        console.error('STOMP ERROR:', frame);
    },

    onWebSocketError: error => {
        console.error('WEBSOCKET ERROR:', error);
    }
});

client.activate();
console.log('Connecting...');