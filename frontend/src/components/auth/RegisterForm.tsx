// components/auth/RegisterForm.tsx
import React, { useState } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { useForm, Controller } from 'react-hook-form';
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
    Paper,
    FormControl,
    FormLabel,
    RadioGroup,
    FormControlLabel,
    Radio,
    Stepper,
    Step,
    StepLabel,
    Grid,
    Checkbox,
} from '@mui/material';
import {
    Visibility,
    VisibilityOff,
    Email as EmailIcon,
    Person as PersonIcon,
    Phone as PhoneIcon,
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
    username: yup
        .string()
        .min(3, 'Username must be at least 3 characters')
        .max(20, 'Username must be at most 20 characters')
        .matches(/^[a-zA-Z0-9_]+$/, 'Username can only contain letters, numbers, and underscores')
        .required('Username is required'),
    password: yup
        .string()
        .min(8, 'Password must be at least 8 characters')
        .matches(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
            'Password must contain uppercase, lowercase, number, and special character'
        )
        .required('Password is required'),
    confirmPassword: yup
        .string()
        .oneOf([yup.ref('password')], 'Passwords must match')
        .required('Please confirm your password'),
    firstName: yup
        .string()
        .min(2, 'First name must be at least 2 characters')
        .required('First name is required'),
    lastName: yup
        .string()
        .min(2, 'Last name must be at least 2 characters')
        .required('Last name is required'),
    phoneNumber: yup
        .string()
        .matches(/^[+]?[(]?[0-9]{1,3}[)]?[-\s.]?[(]?[0-9]{1,4}[)]?[-\s.]?[0-9]{1,4}[-\s.]?[0-9]{1,9}$/,
            'Invalid phone number format')
        .optional(),
    accountType: yup
        .string()
        .oneOf(['BUYER', 'SELLER', 'BOTH'])
        .required('Account type is required'),
    agreeTerms: yup
        .boolean()
        .oneOf([true], 'You must agree to the terms and conditions')
        .required(),
}).required();

interface RegisterFormData {
    email: string;
    username: string;
    password: string;
    confirmPassword: string;
    firstName: string;
    lastName: string;
    phoneNumber?: string;
    accountType: 'BUYER' | 'SELLER' | 'BOTH';
    agreeTerms: boolean;
}

const steps = ['Account Details', 'Personal Information', 'Account Type'];

export const RegisterForm: React.FC = () => {
    const router = useRouter();
    const { register: registerUser, loading } = useAuth();
    const { showSuccess, showError } = useNotification();
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [activeStep, setActiveStep] = useState(0);

    const {
        register,
        handleSubmit,
        control,
        formState: { errors, isSubmitting },
        trigger,
        watch,
    } = useForm<RegisterFormData>({
        resolver: yupResolver(schema),
        defaultValues: {
            email: '',
            username: '',
            password: '',
            confirmPassword: '',
            firstName: '',
            lastName: '',
            phoneNumber: '',
            accountType: 'BUYER',
            agreeTerms: false,
        },
    });

    const accountType = watch('accountType');

    const handleNext = async () => {
        let fieldsToValidate: (keyof RegisterFormData)[] = [];

        if (activeStep === 0) {
            fieldsToValidate = ['email', 'username', 'password', 'confirmPassword'];
        } else if (activeStep === 1) {
            fieldsToValidate = ['firstName', 'lastName', 'phoneNumber'];
        } else if (activeStep === 2) {
            fieldsToValidate = ['accountType', 'agreeTerms'];
        }

        const isValid = await trigger(fieldsToValidate);
        if (isValid) {
            setActiveStep((prevStep) => prevStep + 1);
        }
    };

    const handleBack = () => {
        setActiveStep((prevStep) => prevStep - 1);
    };

    const onSubmit = async (data: RegisterFormData) => {
        setError(null);
        try {
            const { confirmPassword, agreeTerms, ...registerData } = data;
            const result = await registerUser(registerData);

            if (result.success) {
                showSuccess('Registration successful!', 'Welcome to Carbon Credit Marketplace');
                // Redirect is handled in the useAuth hook
            } else {
                setError(result.error || 'Registration failed');
                showError(result.error || 'Registration failed', 'Registration Error');
            }
        } catch (err: any) {
            setError(err.message || 'An unexpected error occurred');
            showError(err.message || 'An unexpected error occurred');
        }
    };

    const handleSocialLogin = (provider: string) => {
        // TODO: Implement social login
        console.log(`Register with ${provider}`);
        showError('Social registration coming soon!', 'Feature Not Available');
    };

    const getStepContent = (step: number) => {
        switch (step) {
            case 0:
                return (
                    <>
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
                        />

                        <TextField
                            {...register('username')}
                            label="Username"
                            fullWidth
                            margin="normal"
                            error={!!errors.username}
                            helperText={errors.username?.message}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <PersonIcon color="action" />
                                    </InputAdornment>
                                ),
                            }}
                            autoComplete="username"
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
                                        >
                                            {showPassword ? <VisibilityOff /> : <Visibility />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                            }}
                            autoComplete="new-password"
                        />

                        <TextField
                            {...register('confirmPassword')}
                            label="Confirm Password"
                            type={showConfirmPassword ? 'text' : 'password'}
                            fullWidth
                            margin="normal"
                            error={!!errors.confirmPassword}
                            helperText={errors.confirmPassword?.message}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <LockIcon color="action" />
                                    </InputAdornment>
                                ),
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton
                                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                            edge="end"
                                        >
                                            {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                            }}
                            autoComplete="new-password"
                        />
                    </>
                );

            case 1:
                return (
                    <>
                        <Grid container spacing={2}>
                            <Grid item xs={12} sm={6}>
                                <TextField
                                    {...register('firstName')}
                                    label="First Name"
                                    fullWidth
                                    margin="normal"
                                    error={!!errors.firstName}
                                    helperText={errors.firstName?.message}
                                    autoComplete="given-name"
                                />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField
                                    {...register('lastName')}
                                    label="Last Name"
                                    fullWidth
                                    margin="normal"
                                    error={!!errors.lastName}
                                    helperText={errors.lastName?.message}
                                    autoComplete="family-name"
                                />
                            </Grid>
                        </Grid>

                        <TextField
                            {...register('phoneNumber')}
                            label="Phone Number (Optional)"
                            fullWidth
                            margin="normal"
                            error={!!errors.phoneNumber}
                            helperText={errors.phoneNumber?.message}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <PhoneIcon color="action" />
                                    </InputAdornment>
                                ),
                            }}
                            autoComplete="tel"
                        />
                    </>
                );

            case 2:
                return (
                    <>
                        <FormControl component="fieldset" margin="normal" fullWidth>
                            <FormLabel component="legend">Account Type</FormLabel>
                            <Controller
                                name="accountType"
                                control={control}
                                render={({ field }) => (
                                    <RadioGroup {...field}>
                                        <FormControlLabel
                                            value="BUYER"
                                            control={<Radio />}
                                            label={
                                                <Box>
                                                    <Typography variant="body1">Buyer</Typography>
                                                    <Typography variant="caption" color="text.secondary">
                                                        Purchase carbon credits to offset your emissions
                                                    </Typography>
                                                </Box>
                                            }
                                        />
                                        <FormControlLabel
                                            value="SELLER"
                                            control={<Radio />}
                                            label={
                                                <Box>
                                                    <Typography variant="body1">Seller</Typography>
                                                    <Typography variant="caption" color="text.secondary">
                                                        List and sell carbon credits from your projects
                                                    </Typography>
                                                </Box>
                                            }
                                        />
                                        <FormControlLabel
                                            value="BOTH"
                                            control={<Radio />}
                                            label={
                                                <Box>
                                                    <Typography variant="body1">Both</Typography>
                                                    <Typography variant="caption" color="text.secondary">
                                                        Buy and sell carbon credits on the marketplace
                                                    </Typography>
                                                </Box>
                                            }
                                        />
                                    </RadioGroup>
                                )}
                            />
                        </FormControl>

                        {accountType === 'SELLER' || accountType === 'BOTH' ? (
                            <Alert severity="info" sx={{ mt: 2 }}>
                                As a seller, you'll need to complete KYC verification before listing credits.
                            </Alert>
                        ) : null}

                        <FormControlLabel
                            control={<Checkbox {...register('agreeTerms')} />}
                            label={
                                <Typography variant="body2">
                                    I agree to the{' '}
                                    <Link href="/terms" passHref>
                                        <Typography component="a" variant="body2" sx={{ color: 'primary.main' }}>
                                            Terms and Conditions
                                        </Typography>
                                    </Link>{' '}
                                    and{' '}
                                    <Link href="/privacy" passHref>
                                        <Typography component="a" variant="body2" sx={{ color: 'primary.main' }}>
                                            Privacy Policy
                                        </Typography>
                                    </Link>
                                </Typography>
                            }
                            sx={{ mt: 2 }}
                        />
                        {errors.agreeTerms && (
                            <Typography color="error" variant="caption">
                                {errors.agreeTerms.message}
                            </Typography>
                        )}
                    </>
                );

            default:
                return null;
        }
    };

    return (
        <Paper elevation={3} sx={{ p: 4, maxWidth: 600, mx: 'auto' }}>
            <Box sx={{ mb: 4, textAlign: 'center' }}>
                <Typography variant="h4" component="h1" gutterBottom fontWeight="bold">
                    Create Account
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Join the carbon credit marketplace community
                </Typography>
            </Box>

            <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
                {steps.map((label) => (
                    <Step key={label}>
                        <StepLabel>{label}</StepLabel>
                    </Step>
                ))}
            </Stepper>

            {error && (
                <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            <form onSubmit={handleSubmit(onSubmit)}>
                {getStepContent(activeStep)}

                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
                    <Button
                        disabled={activeStep === 0}
                        onClick={handleBack}
                        sx={{ mr: 1 }}
                    >
                        Back
                    </Button>
                    {activeStep === steps.length - 1 ? (
                        <Button
                            type="submit"
                            variant="contained"
                            disabled={isSubmitting || loading}
                        >
                            {isSubmitting || loading ? (
                                <CircularProgress size={24} color="inherit" />
                            ) : (
                                'Create Account'
                            )}
                        </Button>
                    ) : (
                        <Button
                            variant="contained"
                            onClick={handleNext}
                        >
                            Next
                        </Button>
                    )}
                </Box>
            </form>

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
                    Already have an account?{' '}
                    <Link href="/auth/login" passHref>
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
                            Sign in
                        </Typography>
                    </Link>
                </Typography>
            </Box>
        </Paper>
    );
};


