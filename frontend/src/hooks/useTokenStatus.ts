// hooks/useTokenStatus.ts
import { useEffect, useState } from 'react';
import { tokenManager } from '../services/tokenManager';

interface TokenStatus {
    hasToken: boolean;
    expiresIn: number;
    shouldRefresh: boolean;
    isExpired: boolean;
    expiresInMinutes: number;
    expiresInSeconds: number;
}

/**
 * React hook to monitor JWT token status
 * Updates every 5 seconds
 * 
 * @example
 * ```tsx
 * const { expiresIn, shouldRefresh, expiresInMinutes } = useTokenStatus();
 * 
 * if (shouldRefresh) {
 *   console.log('Token will expire soon!');
 * }
 * ```
 */
export const useTokenStatus = () => {
    const [status, setStatus] = useState<TokenStatus>({
        hasToken: false,
        expiresIn: 0,
        shouldRefresh: false,
        isExpired: false,
        expiresInMinutes: 0,
        expiresInSeconds: 0,
    });

    useEffect(() => {
        // Update token status
        const updateStatus = () => {
            const tokenStatus = tokenManager.getTokenStatus();
            const expiresIn = tokenStatus.expiresIn;

            setStatus({
                ...tokenStatus,
                expiresInMinutes: Math.floor(expiresIn / 60),
                expiresInSeconds: expiresIn % 60,
            });
        };

        // Initial update
        updateStatus();

        // Update every 5 seconds
        const interval = setInterval(updateStatus, 5000);

        return () => clearInterval(interval);
    }, []);

    return status;
};

/**
 * React hook to get formatted token expiration time
 * 
 * @example
 * ```tsx
 * const expirationText = useTokenExpirationText();
 * // Returns: "Expires in 14m 32s" or "Expired" or "No token"
 * ```
 */
export const useTokenExpirationText = (): string => {
    const status = useTokenStatus();

    if (!status.hasToken) {
        return 'No token';
    }

    if (status.isExpired) {
        return 'Expired';
    }

    const minutes = status.expiresInMinutes;
    const seconds = status.expiresInSeconds;

    if (minutes > 0) {
        return `Expires in ${minutes}m ${seconds}s`;
    }

    return `Expires in ${seconds}s`;
};

export default useTokenStatus;

