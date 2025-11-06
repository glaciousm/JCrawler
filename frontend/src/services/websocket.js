/**
 * WebSocket Service for Real-time Updates
 */

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.sessionId = null;
        this.listeners = {
            PAGE_DISCOVERED: [],
            FLOW_DISCOVERED: [],
            DATA_EXTRACTED: [],
            FILE_DOWNLOADED: [],
            EXTERNAL_URL_FOUND: [],
            METRICS: [],
            LOG: [],
            CRAWL_COMPLETED: [],
            CRAWL_ERROR: []
        };
    }

    /**
     * Connect to WebSocket server
     */
    connect(sessionId, onConnect) {
        this.sessionId = sessionId;
        const socket = new SockJS('http://localhost:8080/ws');
        this.client = StompJs.Stomp.over(socket);

        // Disable debug logging
        this.client.debug = () => {};

        this.client.connect({}, () => {
            this.connected = true;
            console.log('WebSocket connected for session:', sessionId);

            // Subscribe to progress updates
            this.client.subscribe(`/topic/crawler/${sessionId}/progress`, (message) => {
                const update = JSON.parse(message.body);
                this.handleUpdate(update);
            });

            if (onConnect) {
                onConnect();
            }
        }, (error) => {
            this.connected = false;
            console.error('WebSocket connection error:', error);
        });
    }

    /**
     * Disconnect from WebSocket
     */
    disconnect() {
        if (this.client && this.connected) {
            this.client.disconnect();
            this.connected = false;
            this.sessionId = null;
            console.log('WebSocket disconnected');
        }
    }

    /**
     * Handle incoming updates
     */
    handleUpdate(update) {
        const { type, data } = update;

        if (this.listeners[type]) {
            this.listeners[type].forEach(callback => callback(data, update));
        }
    }

    /**
     * Register event listener
     */
    on(eventType, callback) {
        if (this.listeners[eventType]) {
            this.listeners[eventType].push(callback);
        }
    }

    /**
     * Remove event listener
     */
    off(eventType, callback) {
        if (this.listeners[eventType]) {
            this.listeners[eventType] = this.listeners[eventType].filter(cb => cb !== callback);
        }
    }

    /**
     * Clear all listeners
     */
    clearListeners() {
        Object.keys(this.listeners).forEach(key => {
            this.listeners[key] = [];
        });
    }

    /**
     * Check if connected
     */
    isConnected() {
        return this.connected;
    }
}

// Create singleton instance
const wsService = new WebSocketService();

// Make it available globally
window.wsService = wsService;
