import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Subscribes to /topic/group/{groupId} for as long as the calling component
 * is mounted, and invokes onMessage(parsedPayload) for every message received.
 *
 * Why SockJS + STOMP instead of a raw WebSocket:
 * - SockJS gives us a fallback (long-polling etc.) for networks/browsers that
 *   block raw WebSocket connections — it transparently upgrades to a real
 *   socket when possible. This matches the backend's .withSockJS() config.
 * - STOMP is a lightweight messaging protocol on TOP of the socket that adds
 *   the concept of "destinations" (topics) and subscriptions — without it,
 *   we'd have one raw bidirectional pipe and would have to invent our own
 *   message-routing convention by hand.
 *
 * This is a custom hook (not a component) because "manage a socket connection
 * for the lifetime of a component" is reusable logic with no UI of its own —
 * exactly what hooks are for.
 */
export function useGroupSocket(groupId, onMessage) {
  // useRef (not useState) for the client instance: we need a mutable box that
  // survives re-renders WITHOUT causing a re-render itself when it changes —
  // it's plumbing, not something that should ever appear in the UI.
  const clientRef = useRef(null);

  useEffect(() => {
    if (!groupId) return;

    const client = new Client({
      // SockJS wraps the actual transport; STOMP talks over it.
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080/ws'),
      reconnectDelay: 5000, // auto-reconnect if the connection drops
      onConnect: () => {
        client.subscribe(`/topic/group/${groupId}`, (message) => {
          const payload = JSON.parse(message.body);
          onMessage(payload);
        });
      },
    });

    client.activate();
    clientRef.current = client;

    // Cleanup: runs when groupId changes or the component unmounts.
    // Without this, navigating between groups would leave old subscriptions
    // open forever — a classic memory/connection leak with sockets in React.
    return () => {
      client.deactivate();
    };
  }, [groupId, onMessage]);
}
