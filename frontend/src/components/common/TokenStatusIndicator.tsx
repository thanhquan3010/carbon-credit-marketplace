// components/common/TokenStatusIndicator.tsx
import React from 'react';
import { useTokenStatus } from '../../hooks/useTokenStatus';

interface TokenStatusIndicatorProps {
    showDetails?: boolean;
}

/**
 * Visual indicator for JWT token status
 * Shows expiration time and refresh status
 * 
 * @example
 * ```tsx
 * // Simple indicator
 * <TokenStatusIndicator />
 * 
 * // With detailed information
 * <TokenStatusIndicator showDetails={true} />
 * ```
 */
export const TokenStatusIndicator: React.FC<TokenStatusIndicatorProps> = ({
    showDetails = false
}) => {
    const status = useTokenStatus();

    if (!status.hasToken) {
        return null; // Don't show anything if no token
    }

    // Determine color based on time remaining
    const getStatusColor = () => {
        if (status.isExpired) return 'bg-red-500';
        if (status.shouldRefresh) return 'bg-yellow-500';
        if (status.expiresIn < 300) return 'bg-orange-500'; // < 5 minutes
        return 'bg-green-500';
    };

    const getStatusText = () => {
        if (status.isExpired) return 'Expired';
        if (status.shouldRefresh) return 'Refreshing...';
        return 'Active';
    };

    const formatTime = () => {
        const minutes = status.expiresInMinutes;
        const seconds = status.expiresInSeconds;

        if (minutes > 0) {
            return `${minutes}m ${seconds}s`;
        }
        return `${seconds}s`;
    };

    if (!showDetails) {
        // Simple dot indicator
        return (
            <div className="flex items-center space-x-2" title={`Token ${getStatusText()}`}>
                <div className={`w-2 h-2 rounded-full ${getStatusColor()}`} />
                {status.shouldRefresh && (
                    <span className="text-xs text-gray-500">Refreshing...</span>
                )}
            </div>
        );
    }

    // Detailed view
    return (
        <div className="flex items-center space-x-3 px-3 py-2 bg-gray-100 dark:bg-gray-800 rounded-lg">
            <div className={`w-3 h-3 rounded-full ${getStatusColor()}`} />
            <div className="flex flex-col">
                <span className="text-xs font-medium text-gray-700 dark:text-gray-300">
                    {getStatusText()}
                </span>
                <span className="text-xs text-gray-500 dark:text-gray-400">
                    {status.isExpired ? 'Please refresh' : `Expires in ${formatTime()}`}
                </span>
            </div>
        </div>
    );
};

export default TokenStatusIndicator;

