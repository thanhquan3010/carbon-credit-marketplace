// hooks/useWebSocket.ts
import { useEffect, useState, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';

interface BidUpdate {
    id: string;
    listingId: string;
    bidderId: string;
    bidderName: string;
    amount: number;
    timestamp: string;
}

export const useAuctionWebSocket = (listingId: string) => {
    const [currentBid, setCurrentBid] = useState<BidUpdate | null>(null);
    const [connected, setConnected] = useState(false);
    const [client, setClient] = useState<Client | null>(null);

    useEffect(() => {
        if (!listingId) return;

        const stompClient = new Client({
            webSocketFactory: () => new SockJS(process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws'),
            debug: (str) => {
                console.log('STOMP: ' + str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        stompClient.onConnect = () => {
            setConnected(true);
            console.log('Connected to WebSocket');

            // Subscribe to auction updates for the specific listing
            stompClient.subscribe(`/topic/auction/${listingId}`, (message: IMessage) => {
                try {
                    const bid: BidUpdate = JSON.parse(message.body);
                    setCurrentBid(bid);
                } catch (error) {
                    console.error('Error parsing bid update:', error);
                }
            });

            // Subscribe to auction status updates
            stompClient.subscribe(`/topic/auction/${listingId}/status`, (message: IMessage) => {
                try {
                    const status = JSON.parse(message.body);
                    console.log('Auction status update:', status);
                    // Handle auction status changes (e.g., ended, paused, etc.)
                } catch (error) {
                    console.error('Error parsing status update:', error);
                }
            });
        };

        stompClient.onDisconnect = () => {
            setConnected(false);
            console.log('Disconnected from WebSocket');
        };

        stompClient.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        };

        stompClient.activate();
        setClient(stompClient);

        // Cleanup on unmount
        return () => {
            if (stompClient.active) {
                stompClient.deactivate();
            }
        };
    }, [listingId]);

    const placeBid = useCallback(
        (amount: number) => {
            if (!client || !connected) {
                console.error('WebSocket not connected');
                return;
            }

            const bidData = {
                listingId,
                amount,
                timestamp: new Date().toISOString(),
            };

            client.publish({
                destination: `/app/auction/${listingId}/bid`,
                body: JSON.stringify(bidData),
            });
        },
        [client, connected, listingId]
    );

    return { currentBid, connected, placeBid };
};

// Hook for general notifications
export const useNotificationWebSocket = (userId: string) => {
    const [notifications, setNotifications] = useState<any[]>([]);
    const [connected, setConnected] = useState(false);

    useEffect(() => {
        if (!userId) return;

        const stompClient = new Client({
            webSocketFactory: () => new SockJS(process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws'),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        stompClient.onConnect = () => {
            setConnected(true);

            // Subscribe to user-specific notifications
            stompClient.subscribe(`/user/${userId}/queue/notifications`, (message: IMessage) => {
                try {
                    const notification = JSON.parse(message.body);
                    setNotifications((prev) => [notification, ...prev]);
                } catch (error) {
                    console.error('Error parsing notification:', error);
                }
            });

            // Subscribe to broadcast notifications
            stompClient.subscribe('/topic/notifications/broadcast', (message: IMessage) => {
                try {
                    const notification = JSON.parse(message.body);
                    setNotifications((prev) => [notification, ...prev]);
                } catch (error) {
                    console.error('Error parsing broadcast notification:', error);
                }
            });
        };

        stompClient.onDisconnect = () => {
            setConnected(false);
        };

        stompClient.activate();

        return () => {
            if (stompClient.active) {
                stompClient.deactivate();
            }
        };
    }, [userId]);

    const clearNotifications = useCallback(() => {
        setNotifications([]);
    }, []);

    const removeNotification = useCallback((notificationId: string) => {
        setNotifications((prev) => prev.filter((n) => n.id !== notificationId));
    }, []);

    return { notifications, connected, clearNotifications, removeNotification };
};


