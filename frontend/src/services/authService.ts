// services/authService.ts
import { apiClient } from './api';

export interface LoginRequest {
    email: string;
    password: string;
}

export interface LoginResponse {
    token: string;
    refreshToken: string;
    user: {
        id: string;
        email: string;
        username: string;
        roles: string[];
        firstName?: string;
        lastName?: string;
        profilePicture?: string;
        emailVerified: boolean;
        kycStatus: string;
    };
}

export interface RegisterRequest {
    email: string;
    username: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber?: string;
    accountType: 'BUYER' | 'SELLER' | 'BOTH';
}

export interface RegisterResponse {
    token: string;
    refreshToken: string;
    user: {
        id: string;
        email: string;
        username: string;
        roles: string[];
    };
    message: string;
}

export interface KycUploadRequest {
    documentType: 'PASSPORT' | 'DRIVERS_LICENSE' | 'NATIONAL_ID';
    documentNumber: string;
    expiryDate: string;
    country: string;
    frontImage: File;
    backImage?: File;
    selfieImage: File;
}

export interface PasswordResetRequest {
    email: string;
}

export interface PasswordResetConfirmRequest {
    token: string;
    newPassword: string;
}

export const authService = {
    // Login
    login: async (email: string, password: string): Promise<LoginResponse> => {
        return await apiClient.post<LoginResponse>('/auth/login', { email, password });
    },

    // Register
    register: async (data: RegisterRequest): Promise<RegisterResponse> => {
        return await apiClient.post<RegisterResponse>('/auth/register', data);
    },

    // Logout
    logout: async (): Promise<void> => {
        try {
            await apiClient.post('/auth/logout');
        } catch (error) {
            // Silent fail, clear local storage anyway
        } finally {
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
        }
    },

    // Refresh token
    refreshToken: async (refreshToken: string): Promise<string> => {
        const response = await apiClient.post<{ token: string }>('/auth/refresh', { refreshToken });
        return response.token;
    },

    // Verify email
    verifyEmail: async (token: string): Promise<{ message: string }> => {
        return await apiClient.post(`/auth/verify-email`, { token });
    },

    // Request password reset
    requestPasswordReset: async (email: string): Promise<{ message: string }> => {
        return await apiClient.post('/auth/password-reset', { email });
    },

    // Confirm password reset
    confirmPasswordReset: async (token: string, newPassword: string): Promise<{ message: string }> => {
        return await apiClient.post('/auth/password-reset/confirm', { token, newPassword });
    },

    // Upload KYC documents
    uploadKycDocuments: async (data: KycUploadRequest, onProgress?: (progress: number) => void): Promise<{ message: string }> => {
        const formData = new FormData();
        formData.append('documentType', data.documentType);
        formData.append('documentNumber', data.documentNumber);
        formData.append('expiryDate', data.expiryDate);
        formData.append('country', data.country);
        formData.append('frontImage', data.frontImage);
        if (data.backImage) {
            formData.append('backImage', data.backImage);
        }
        formData.append('selfieImage', data.selfieImage);

        return await apiClient.upload('/auth/kyc/upload', formData, onProgress);
    },

    // Get KYC status
    getKycStatus: async (): Promise<{
        status: 'PENDING' | 'VERIFIED' | 'REJECTED' | 'NOT_SUBMITTED';
        message?: string;
        submittedAt?: string;
        verifiedAt?: string;
    }> => {
        return await apiClient.get('/auth/kyc/status');
    },

    // Get current user profile
    getCurrentUser: async (): Promise<LoginResponse['user']> => {
        return await apiClient.get('/auth/me');
    },

    // Update user profile
    updateProfile: async (data: Partial<{
        firstName: string;
        lastName: string;
        phoneNumber: string;
        bio: string;
        profilePicture: File;
    }>): Promise<LoginResponse['user']> => {
        if (data.profilePicture) {
            const formData = new FormData();
            Object.keys(data).forEach(key => {
                if (key === 'profilePicture') {
                    formData.append(key, data[key] as File);
                } else {
                    formData.append(key, (data as any)[key]);
                }
            });
            return await apiClient.upload('/auth/profile', formData);
        } else {
            return await apiClient.put('/auth/profile', data);
        }
    },

    // Change password
    changePassword: async (currentPassword: string, newPassword: string): Promise<{ message: string }> => {
        return await apiClient.post('/auth/change-password', { currentPassword, newPassword });
    },

    // Enable 2FA
    enable2FA: async (): Promise<{ qrCode: string; secret: string }> => {
        return await apiClient.post('/auth/2fa/enable');
    },

    // Verify 2FA
    verify2FA: async (code: string): Promise<{ message: string }> => {
        return await apiClient.post('/auth/2fa/verify', { code });
    },

    // Disable 2FA
    disable2FA: async (code: string): Promise<{ message: string }> => {
        return await apiClient.post('/auth/2fa/disable', { code });
    },
};


