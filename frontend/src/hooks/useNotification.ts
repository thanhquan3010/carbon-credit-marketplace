// hooks/useNotification.ts
import { useEffect, useCallback } from 'react';
import { toast, ToastOptions } from 'react-toastify';
import { useNotificationWebSocket } from './useWebSocket';
import { useAuth } from './useAuth';

interface Notification {
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    title: string;
    message: string;
    timestamp: string;
    read: boolean;
    actionUrl?: string;
    data?: any;
}

const defaultToastOptions: ToastOptions = {
    position: 'top-right',
    autoClose: 5000,
    hideProgressBar: false,
    closeOnClick: true,
    pauseOnHover: true,
    draggable: true,
};

export const useNotification = () => {
    const { user } = useAuth();
    const { notifications, connected, clearNotifications, removeNotification } =
        useNotificationWebSocket(user?.id || '');

    // Display notifications as toasts
    useEffect(() => {
        notifications.forEach((notification: Notification) => {
            if (!notification.read) {
                const message = `${notification.title}: ${notification.message}`;

                switch (notification.type) {
                    case 'success':
                        toast.success(message, {
                            ...defaultToastOptions,
                            toastId: notification.id,
                        });
                        break;
                    case 'error':
                        toast.error(message, {
                            ...defaultToastOptions,
                            toastId: notification.id,
                        });
                        break;
                    case 'warning':
                        toast.warning(message, {
                            ...defaultToastOptions,
                            toastId: notification.id,
                        });
                        break;
                    case 'info':
                    default:
                        toast.info(message, {
                            ...defaultToastOptions,
                            toastId: notification.id,
                        });
                        break;
                }

                // Mark as read after displaying
                // This would typically call an API to mark the notification as read
                // For now, we'll just remove it from the local state after display
                setTimeout(() => {
                    removeNotification(notification.id);
                }, defaultToastOptions.autoClose as number);
            }
        });
    }, [notifications, removeNotification]);

    const showNotification = useCallback((
        type: 'success' | 'error' | 'warning' | 'info',
        message: string,
        title?: string,
        options?: ToastOptions
    ) => {
        const content = title ? `${title}: ${message}` : message;

        switch (type) {
            case 'success':
                toast.success(content, { ...defaultToastOptions, ...options });
                break;
            case 'error':
                toast.error(content, { ...defaultToastOptions, ...options });
                break;
            case 'warning':
                toast.warning(content, { ...defaultToastOptions, ...options });
                break;
            case 'info':
            default:
                toast.info(content, { ...defaultToastOptions, ...options });
                break;
        }
    }, []);

    const showSuccess = useCallback((message: string, title?: string, options?: ToastOptions) => {
        showNotification('success', message, title, options);
    }, [showNotification]);

    const showError = useCallback((message: string, title?: string, options?: ToastOptions) => {
        showNotification('error', message, title, options);
    }, [showNotification]);

    const showWarning = useCallback((message: string, title?: string, options?: ToastOptions) => {
        showNotification('warning', message, title, options);
    }, [showNotification]);

    const showInfo = useCallback((message: string, title?: string, options?: ToastOptions) => {
        showNotification('info', message, title, options);
    }, [showNotification]);

    const dismissNotification = useCallback((toastId?: string | number) => {
        if (toastId) {
            toast.dismiss(toastId);
        } else {
            toast.dismiss();
        }
    }, []);

    return {
        notifications,
        connected,
        showNotification,
        showSuccess,
        showError,
        showWarning,
        showInfo,
        dismissNotification,
        clearNotifications,
    };
};