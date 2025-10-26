// components/auth/LoginForm.tsx
import React, { useState } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
    TextField,
    Button,
    Box,
    Typography,
    Alert,
    IconButton,
    InputAdornment,
    CircularProgress,
    Divider,
    Checkbox,
    FormControlLabel,
    Paper,
} from '@mui/material';
import {
    Visibility,
    VisibilityOff,
    Email as EmailIcon,
    Lock as LockIcon,
    Google as GoogleIcon,
    GitHub as GitHubIcon,
} from '@mui/icons-material';
import { useAuth } from '../../hooks/useAuth';
import { useNotification } from '../../hooks/useNotification';

// Validation schema
const schema = yup.object({
    email: yup
        .string()
        .email('Invalid email format')
        .required('Email is required'),
    password: yup
        .string()
        .min(8, 'Password must be at least 8 characters')
        .required('Password is required'),
    rememberMe: yup.boolean(),
}).required();

interface LoginFormData {
    email: string;
    password: string;
    rememberMe?: boolean;
}

export const LoginForm: React.FC = () => {
    const router = useRouter();
    const { login, loading } = useAuth();
    const { showSuccess, showError } = useNotification();
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginFormData>({
        resolver: yupResolver(schema),
        defaultValues: {
            email: '',
            password: '',
            rememberMe: false,
        },
    });

    const onSubmit = async (data: LoginFormData) => {
        setError(null);
        try {
            const result = await login(data.email, data.password);

            if (result.success) {
                showSuccess('Login successful!', 'Welcome back');
                // Redirect is handled in the useAuth hook
            } else {
                setError(result.error || 'Login failed');
                showError(result.error || 'Login failed', 'Authentication Error');
            }
        } catch (err: any) {
            setError(err.message || 'An unexpected error occurred');
            showError(err.message || 'An unexpected error occurred');
        }
    };

    const handleSocialLogin = (provider: string) => {
        // TODO: Implement social login
        console.log(`Login with ${provider}`);
        showError('Social login coming soon!', 'Feature Not Available');
    };

    return (
        <Paper elevation={3} sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
            <Box sx={{ mb: 4, textAlign: 'center' }}>
                <Typography variant="h4" component="h1" gutterBottom fontWeight="bold">
                    Welcome Back
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Sign in to access your carbon credit marketplace account
                </Typography>
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            <form onSubmit={handleSubmit(onSubmit)}>
                <TextField
                    {...register('email')}
                    label="Email Address"
                    type="email"
                    fullWidth
                    margin="normal"
                    error={!!errors.email}
                    helperText={errors.email?.message}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <EmailIcon color="action" />
                            </InputAdornment>
                        ),
                    }}
                    autoComplete="email"
                    autoFocus
                />

                <TextField
                    {...register('password')}
                    label="Password"
                    type={showPassword ? 'text' : 'password'}
                    fullWidth
                    margin="normal"
                    error={!!errors.password}
                    helperText={errors.password?.message}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <LockIcon color="action" />
                            </InputAdornment>
                        ),
                        endAdornment: (
                            <InputAdornment position="end">
                                <IconButton
                                    onClick={() => setShowPassword(!showPassword)}
                                    edge="end"
                                    aria-label="toggle password visibility"
                                >
                                    {showPassword ? <VisibilityOff /> : <Visibility />}
                                </IconButton>
                            </InputAdornment>
                        ),
                    }}
                    autoComplete="current-password"
                />

                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', my: 2 }}>
                    <FormControlLabel
                        control={<Checkbox {...register('rememberMe')} color="primary" />}
                        label="Remember me"
                    />
                    <Link href="/auth/forgot-password" passHref>
                        <Typography
                            component="a"
                            variant="body2"
                            sx={{
                                color: 'primary.main',
                                textDecoration: 'none',
                                '&:hover': {
                                    textDecoration: 'underline',
                                },
                            }}
                        >
                            Forgot password?
                        </Typography>
                    </Link>
                </Box>

                <Button
                    type="submit"
                    fullWidth
                    variant="contained"
                    size="large"
                    disabled={isSubmitting || loading}
                    sx={{ mt: 2, mb: 3, py: 1.5 }}
                >
                    {isSubmitting || loading ? (
                        <CircularProgress size={24} color="inherit" />
                    ) : (
                        'Sign In'
                    )}
                </Button>

                <Divider sx={{ my: 3 }}>OR</Divider>

                <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
                    <Button
                        fullWidth
                        variant="outlined"
                        startIcon={<GoogleIcon />}
                        onClick={() => handleSocialLogin('google')}
                        sx={{ py: 1.5 }}
                    >
                        Google
                    </Button>
                    <Button
                        fullWidth
                        variant="outlined"
                        startIcon={<GitHubIcon />}
                        onClick={() => handleSocialLogin('github')}
                        sx={{ py: 1.5 }}
                    >
                        GitHub
                    </Button>
                </Box>

                <Box sx={{ textAlign: 'center' }}>
                    <Typography variant="body2" color="text.secondary">
                        Don't have an account?{' '}
                        <Link href="/auth/register" passHref>
                            <Typography
                                component="a"
                                variant="body2"
                                sx={{
                                    color: 'primary.main',
                                    fontWeight: 'medium',
                                    textDecoration: 'none',
                                    '&:hover': {
                                        textDecoration: 'underline',
                                    },
                                }}
                            >
                                Sign up now
                            </Typography>
                        </Link>
                    </Typography>
                </Box>
            </form>
        </Paper>
    );
};


