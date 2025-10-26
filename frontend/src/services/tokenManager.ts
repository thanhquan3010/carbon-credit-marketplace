// services/tokenManager.ts
import axios from 'axios';

interface TokenInfo {
    token: string;
    refreshToken: string;
    expiresAt: number; // Unix timestamp in seconds
}

class TokenManager {
    private static instance: TokenManager;
    private refreshPromise: Promise<string> | null = null;
    private checkInterval: NodeJS.Timeout | null = null;
    private readonly REFRESH_THRESHOLD = 60; // Refresh when < 60 seconds remaining

    private constructor() {
        this.startTokenMonitoring();
    }

    public static getInstance(): TokenManager {
        if (!TokenManager.instance) {
            TokenManager.instance = new TokenManager();
        }
        return TokenManager.instance;
    }

    /**
     * Store token information
     */
    public setTokens(accessToken: string, refreshToken: string, expiresIn: number): void {
        const now = Math.floor(Date.now() / 1000);
        const tokenInfo: TokenInfo = {
            token: accessToken,
            refreshToken: refreshToken,
            expiresAt: now + expiresIn
        };

        localStorage.setItem('token', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('tokenExpiresAt', tokenInfo.expiresAt.toString());

        console.log('[TokenManager] Tokens stored. Expires at:', new Date(tokenInfo.expiresAt * 1000).toLocaleTimeString());
    }

    /**
     * Get the current access token
     */
    public getToken(): string | null {
        return localStorage.getItem('token');
    }

    /**
     * Get the refresh token
     */
    public getRefreshToken(): string | null {
        return localStorage.getItem('refreshToken');
    }

    /**
     * Get remaining time until token expires (in seconds)
     */
    public getTokenRemainingTime(): number {
        const expiresAt = localStorage.getItem('tokenExpiresAt');
        if (!expiresAt) return 0;

        const now = Math.floor(Date.now() / 1000);
        const remaining = parseInt(expiresAt) - now;
        return Math.max(0, remaining);
    }

    /**
     * Check if token should be refreshed (< 60 seconds remaining)
     */
    public shouldRefreshToken(): boolean {
        const remaining = this.getTokenRemainingTime();
        return remaining > 0 && remaining < this.REFRESH_THRESHOLD;
    }

    /**
     * Check if token is expired
     */
    public isTokenExpired(): boolean {
        return this.getTokenRemainingTime() <= 0;
    }

    /**
     * Proactively refresh the token before it expires
     * Uses a singleton promise to prevent multiple simultaneous refresh requests
     */
    public async refreshTokenIfNeeded(): Promise<string | null> {
        const token = this.getToken();
        if (!token) return null;

        // If token doesn't need refresh yet, return current token
        if (!this.shouldRefreshToken() && !this.isTokenExpired()) {
            return token;
        }

        // If already refreshing, wait for that promise
        if (this.refreshPromise) {
            console.log('[TokenManager] Refresh already in progress, waiting...');
            return this.refreshPromise;
        }

        // Start new refresh
        console.log('[TokenManager] Auto-refreshing token (remaining: ' + this.getTokenRemainingTime() + 's)');

        this.refreshPromise = this.performTokenRefresh();

        try {
            const newToken = await this.refreshPromise;
            return newToken;
        } finally {
            this.refreshPromise = null;
        }
    }

    /**
     * Perform the actual token refresh API call
     */
    private async performTokenRefresh(): Promise<string> {
        const refreshToken = this.getRefreshToken();

        if (!refreshToken) {
            throw new Error('No refresh token available');
        }

        try {
            const response = await axios.post(
                `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'}/auth/refresh`,
                { refreshToken }
            );

            const { data } = response.data;
            const newAccessToken = data.accessToken || data.token;
            const newRefreshToken = data.refreshToken;
            const expiresIn = data.expiresIn || 900; // Default 15 minutes

            // Store new tokens
            this.setTokens(newAccessToken, newRefreshToken, expiresIn);

            console.log('[TokenManager] Token refreshed successfully');
            return newAccessToken;

        } catch (error) {
            console.error('[TokenManager] Token refresh failed:', error);
            this.clearTokens();

            // Redirect to login page
            if (typeof window !== 'undefined') {
                window.location.href = '/auth/login';
            }

            throw error;
        }
    }

    /**
     * Start monitoring token expiration
     * Checks every 30 seconds if token needs refresh
     */
    private startTokenMonitoring(): void {
        if (typeof window === 'undefined') return; // Skip on server-side

        // Clear existing interval if any
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
        }

        // Check every 30 seconds
        this.checkInterval = setInterval(async () => {
            const token = this.getToken();
            if (!token) return;

            const remaining = this.getTokenRemainingTime();

            // Log warning when < 2 minutes remaining
            if (remaining > 0 && remaining < 120) {
                console.log('[TokenManager] Token expires in ' + remaining + ' seconds');
            }

            // Auto-refresh if needed
            if (this.shouldRefreshToken()) {
                try {
                    await this.refreshTokenIfNeeded();
                } catch (error) {
                    console.error('[TokenManager] Auto-refresh failed:', error);
                }
            }
        }, 30000); // Check every 30 seconds

        console.log('[TokenManager] Token monitoring started');
    }

    /**
     * Stop monitoring token expiration
     */
    public stopTokenMonitoring(): void {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
            this.checkInterval = null;
            console.log('[TokenManager] Token monitoring stopped');
        }
    }

    /**
     * Clear all tokens
     */
    public clearTokens(): void {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('tokenExpiresAt');
        console.log('[TokenManager] Tokens cleared');
    }

    /**
     * Get token status for debugging
     */
    public getTokenStatus(): {
        hasToken: boolean;
        expiresIn: number;
        shouldRefresh: boolean;
        isExpired: boolean;
    } {
        return {
            hasToken: !!this.getToken(),
            expiresIn: this.getTokenRemainingTime(),
            shouldRefresh: this.shouldRefreshToken(),
            isExpired: this.isTokenExpired()
        };
    }
}

// Export singleton instance
export const tokenManager = TokenManager.getInstance();
export default tokenManager;

