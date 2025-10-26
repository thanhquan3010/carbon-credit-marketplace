// hooks/useAuth.ts
import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { useDispatch, useSelector } from 'react-redux';
import jwt_decode from 'jwt-decode';
import { RootState } from '../store';
import { setUser, clearUser, setLoading } from '../store/slices/authSlice';
import { authService } from '../services/authService';

interface DecodedToken {
    sub: string;
    exp: number;
    roles: string[];
    userId: string;
    email: string;
}

export const useAuth = () => {
    const dispatch = useDispatch();
    const router = useRouter();
    const { user, token, isAuthenticated, loading } = useSelector(
        (state: RootState) => state.auth
    );
    const [isInitialized, setIsInitialized] = useState(false);

    // Check token validity on mount and periodically
    useEffect(() => {
        const checkAuth = async () => {
            dispatch(setLoading(true));
            const storedToken = localStorage.getItem('token');

            if (storedToken) {
                try {
                    const decoded = jwt_decode<DecodedToken>(storedToken);

                    // Check if token is expired
                    if (decoded.exp * 1000 < Date.now()) {
                        // Token expired, try to refresh
                        const refreshToken = localStorage.getItem('refreshToken');
                        if (refreshToken) {
                            const newToken = await authService.refreshToken(refreshToken);
                            if (newToken) {
                                localStorage.setItem('token', newToken);
                                const newDecoded = jwt_decode<DecodedToken>(newToken);
                                dispatch(setUser({
                                    user: {
                                        id: newDecoded.userId,
                                        email: newDecoded.email,
                                        roles: newDecoded.roles,
                                    },
                                    token: newToken,
                                }));
                            } else {
                                // Refresh failed, clear auth
                                logout();
                            }
                        } else {
                            // No refresh token, clear auth
                            logout();
                        }
                    } else {
                        // Token still valid
                        dispatch(setUser({
                            user: {
                                id: decoded.userId,
                                email: decoded.email,
                                roles: decoded.roles,
                            },
                            token: storedToken,
                        }));
                    }
                } catch (error) {
                    console.error('Invalid token:', error);
                    logout();
                }
            }

            dispatch(setLoading(false));
            setIsInitialized(true);
        };

        checkAuth();

        // Check token validity every 5 minutes
        const interval = setInterval(checkAuth, 5 * 60 * 1000);

        return () => clearInterval(interval);
    }, [dispatch]);

    const login = async (email: string, password: string) => {
        dispatch(setLoading(true));
        try {
            const response = await authService.login(email, password);

            localStorage.setItem('token', response.token);
            localStorage.setItem('refreshToken', response.refreshToken);

            const decoded = jwt_decode<DecodedToken>(response.token);
            dispatch(setUser({
                user: {
                    id: decoded.userId,
                    email: decoded.email,
                    roles: decoded.roles,
                },
                token: response.token,
            }));

            // Redirect based on role
            if (decoded.roles.includes('ADMIN')) {
                router.push('/admin');
            } else if (decoded.roles.includes('VERIFIER')) {
                router.push('/verification');
            } else {
                router.push('/dashboard');
            }

            return { success: true };
        } catch (error: any) {
            dispatch(setLoading(false));
            return { success: false, error: error.message || 'Login failed' };
        }
    };

    const register = async (userData: any) => {
        dispatch(setLoading(true));
        try {
            const response = await authService.register(userData);

            // Auto-login after successful registration
            if (response.token) {
                localStorage.setItem('token', response.token);
                localStorage.setItem('refreshToken', response.refreshToken);

                const decoded = jwt_decode<DecodedToken>(response.token);
                dispatch(setUser({
                    user: {
                        id: decoded.userId,
                        email: decoded.email,
                        roles: decoded.roles,
                    },
                    token: response.token,
                }));

                router.push('/dashboard');
            }

            return { success: true };
        } catch (error: any) {
            dispatch(setLoading(false));
            return { success: false, error: error.message || 'Registration failed' };
        }
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        dispatch(clearUser());
        router.push('/');
    };

    const hasRole = (role: string): boolean => {
        return user?.roles?.includes(role) || false;
    };

    const hasAnyRole = (roles: string[]): boolean => {
        return roles.some((role) => hasRole(role));
    };

    const isVerified = (): boolean => {
        return user?.roles?.includes('VERIFIED_BUYER') ||
            user?.roles?.includes('VERIFIED_SELLER') ||
            false;
    };

    return {
        user,
        token,
        isAuthenticated,
        loading,
        isInitialized,
        login,
        register,
        logout,
        hasRole,
        hasAnyRole,
        isVerified,
    };
};


