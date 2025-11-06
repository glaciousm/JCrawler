/**
 * API Service for JCrawler Backend Communication
 */

const API_BASE_URL = 'http://localhost:8080/api';

const api = {
    /**
     * Start a new crawl session
     */
    async startCrawl(config) {
        try {
            const response = await axios.post(`${API_BASE_URL}/crawler/start`, config);
            return response.data;
        } catch (error) {
            console.error('Error starting crawl:', error);
            throw error;
        }
    },

    /**
     * Pause an active crawl
     */
    async pauseCrawl(sessionId) {
        try {
            const response = await axios.post(`${API_BASE_URL}/crawler/${sessionId}/pause`);
            return response.data;
        } catch (error) {
            console.error('Error pausing crawl:', error);
            throw error;
        }
    },

    /**
     * Resume a paused crawl
     */
    async resumeCrawl(sessionId) {
        try {
            const response = await axios.post(`${API_BASE_URL}/crawler/${sessionId}/resume`);
            return response.data;
        } catch (error) {
            console.error('Error resuming crawl:', error);
            throw error;
        }
    },

    /**
     * Stop a crawl
     */
    async stopCrawl(sessionId) {
        try {
            const response = await axios.post(`${API_BASE_URL}/crawler/${sessionId}/stop`);
            return response.data;
        } catch (error) {
            console.error('Error stopping crawl:', error);
            throw error;
        }
    },

    /**
     * Get crawl status
     */
    async getCrawlStatus(sessionId) {
        try {
            const response = await axios.get(`${API_BASE_URL}/crawler/${sessionId}/status`);
            return response.data;
        } catch (error) {
            console.error('Error getting crawl status:', error);
            throw error;
        }
    },

    /**
     * Get discovered pages
     */
    async getPages(sessionId) {
        try {
            const response = await axios.get(`${API_BASE_URL}/crawler/${sessionId}/pages`);
            return response.data;
        } catch (error) {
            console.error('Error getting pages:', error);
            throw error;
        }
    },

    /**
     * Get navigation flows
     */
    async getFlows(sessionId) {
        try {
            const response = await axios.get(`${API_BASE_URL}/crawler/${sessionId}/flows`);
            return response.data;
        } catch (error) {
            console.error('Error getting flows:', error);
            throw error;
        }
    },

    /**
     * Get extracted data
     */
    async getExtractedData(sessionId) {
        try {
            const response = await axios.get(`${API_BASE_URL}/crawler/${sessionId}/extracted`);
            return response.data;
        } catch (error) {
            console.error('Error getting extracted data:', error);
            throw error;
        }
    },

    /**
     * Get downloaded files
     */
    async getDownloadedFiles(sessionId) {
        try {
            const response = await axios.get(`${API_BASE_URL}/crawler/${sessionId}/downloads`);
            return response.data;
        } catch (error) {
            console.error('Error getting downloaded files:', error);
            throw error;
        }
    },

    /**
     * Export crawl results
     */
    async exportResults(sessionId, exportConfig) {
        try {
            const response = await axios.post(`${API_BASE_URL}/crawler/${sessionId}/export`, exportConfig);
            return response.data;
        } catch (error) {
            console.error('Error exporting results:', error);
            throw error;
        }
    },

    /**
     * Import browser cookies
     */
    async importCookies(cookieString) {
        try {
            const response = await axios.post(`${API_BASE_URL}/session/import`, {
                cookieString
            });
            return response.data;
        } catch (error) {
            console.error('Error importing cookies:', error);
            throw error;
        }
    },

    /**
     * Add extraction rule
     */
    async addRule(sessionId, rule) {
        try {
            const response = await axios.post(`${API_BASE_URL}/rules`, {
                sessionId,
                ...rule
            });
            return response.data;
        } catch (error) {
            console.error('Error adding rule:', error);
            throw error;
        }
    },

    /**
     * Get extraction rules
     */
    async getRules(sessionId) {
        try {
            const response = await axios.get(`${API_BASE_URL}/rules/${sessionId}`);
            return response.data;
        } catch (error) {
            console.error('Error getting rules:', error);
            throw error;
        }
    },

    /**
     * Delete extraction rule
     */
    async deleteRule(ruleId) {
        try {
            const response = await axios.delete(`${API_BASE_URL}/rules/${ruleId}`);
            return response.data;
        } catch (error) {
            console.error('Error deleting rule:', error);
            throw error;
        }
    },

    /**
     * Toggle extraction rule
     */
    async toggleRule(ruleId) {
        try {
            const response = await axios.put(`${API_BASE_URL}/rules/${ruleId}/toggle`);
            return response.data;
        } catch (error) {
            console.error('Error toggling rule:', error);
            throw error;
        }
    }
};

// Make api available globally
window.api = api;
