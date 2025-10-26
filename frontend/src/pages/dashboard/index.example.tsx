// pages/dashboard/index.tsx
// Example dashboard page using the implemented components

import React from 'react';
import {
    Container,
    Grid,
    Box,
    Typography,
    Paper,
    Button,
    Stack,
    Avatar,
    Chip,
} from '@mui/material';
import {
    AccountCircle,
    Verified,
    Edit,
    Settings,
} from '@mui/icons-material';
import Head from 'next/head';

import { CO2Widget } from '../../components/dashboard/CO2Widget';
import { EarningsChart } from '../../components/dashboard/EarningsChart';
import { TripHistory } from '../../components/dashboard/TripHistory';
import { useAuth } from '../../hooks/useAuth';

export default function DashboardPage() {
    const { user, isAuthenticated } = useAuth();

    if (!isAuthenticated) {
        return (
            <Container maxWidth="xl" sx={{ py: 4 }}>
                <Box sx={{ textAlign: 'center', py: 8 }}>
                    <Typography variant="h5" gutterBottom>
                        Please log in to view your dashboard
                    </Typography>
                    <Button variant="contained" href="/auth/login" sx={{ mt: 2 }}>
                        Log In
                    </Button>
                </Box>
            </Container>
        );
    }

    return (
        <>
            <Head>
                <title>Dashboard - Carbon Credit Marketplace</title>
                <meta name="description" content="Your carbon credit dashboard" />
            </Head>

            <Container maxWidth="xl" sx={{ py: 4 }}>
                {/* Page Header with User Info */}
                <Paper sx={{ p: 3, mb: 4 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 3 }}>
                            <Avatar
                                sx={{ width: 80, height: 80 }}
                                src={user?.profilePicture}
                            >
                                <AccountCircle sx={{ fontSize: 60 }} />
                            </Avatar>

                            <Box>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                                    <Typography variant="h4" fontWeight="bold">
                                        Welcome back, {user?.firstName || user?.username}!
                                    </Typography>
                                    {user?.emailVerified && (
                                        <Verified color="primary" />
                                    )}
                                </Box>

                                <Typography variant="body1" color="text.secondary" gutterBottom>
                                    {user?.email}
                                </Typography>

                                <Stack direction="row" spacing={1}>
                                    {user?.roles?.map((role) => (
                                        <Chip key={role} label={role} size="small" variant="outlined" />
                                    ))}
                                    {user?.kycStatus && (
                                        <Chip
                                            label={`KYC: ${user.kycStatus}`}
                                            size="small"
                                            color={user.kycStatus === 'VERIFIED' ? 'success' : 'warning'}
                                        />
                                    )}
                                </Stack>
                            </Box>
                        </Box>

                        <Stack direction="row" spacing={2}>
                            <Button
                                variant="outlined"
                                startIcon={<Edit />}
                                href="/profile/edit"
                            >
                                Edit Profile
                            </Button>
                            <Button
                                variant="outlined"
                                startIcon={<Settings />}
                                href="/settings"
                            >
                                Settings
                            </Button>
                        </Stack>
                    </Box>
                </Paper>

                {/* KYC Alert */}
                {user?.kycStatus === 'NOT_SUBMITTED' && (
                    <Paper sx={{ p: 2, mb: 4, backgroundColor: 'warning.light' }}>
                        <Typography variant="body1">
                            Complete your KYC verification to unlock all marketplace features.{' '}
                            <Button href="/auth/kyc" variant="text" color="primary">
                                Start KYC
                            </Button>
                        </Typography>
                    </Paper>
                )}

                <Grid container spacing={3}>
                    {/* CO2 Widget */}
                    <Grid item xs={12}>
                        <CO2Widget
                            userId={user?.id}
                            period="month"
                            showBreakdown
                            compact={false}
                        />
                    </Grid>

                    {/* Earnings Chart */}
                    <Grid item xs={12}>
                        <Typography variant="h5" fontWeight="bold" gutterBottom>
                            Earnings Overview
                        </Typography>
                        <EarningsChart
                            userId={user?.id}
                            timeRange="month"
                            showBreakdown
                        />
                    </Grid>

                    {/* Trip History */}
                    <Grid item xs={12}>
                        <Typography variant="h5" fontWeight="bold" gutterBottom sx={{ mb: 2 }}>
                            Recent Trips
                        </Typography>
                        <TripHistory
                            userId={user?.id}
                            showStats
                            compact={false}
                        />
                    </Grid>

                    {/* Quick Actions */}
                    <Grid item xs={12}>
                        <Paper sx={{ p: 3 }}>
                            <Typography variant="h6" fontWeight="bold" gutterBottom>
                                Quick Actions
                            </Typography>
                            <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
                                <Button variant="contained" href="/marketplace">
                                    Browse Marketplace
                                </Button>
                                <Button variant="outlined" href="/marketplace/sell">
                                    List Credits
                                </Button>
                                <Button variant="outlined" href="/trips/new">
                                    Log New Trip
                                </Button>
                                <Button variant="outlined" href="/transactions">
                                    View Transactions
                                </Button>
                                <Button variant="outlined" href="/certificates">
                                    My Certificates
                                </Button>
                            </Stack>
                        </Paper>
                    </Grid>
                </Grid>
            </Container>
        </>
    );
}
