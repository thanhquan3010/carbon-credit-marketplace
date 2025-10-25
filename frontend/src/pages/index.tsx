import { useEffect } from 'react';
import { useRouter } from 'next/router';
import Head from 'next/head';
import { Box, Container, Typography, Button, Grid } from '@mui/material';
import { useSelector } from 'react-redux';

import { RootState } from '@/store';
import HeroSection from '@/components/common/HeroSection';
import FeatureCard from '@/components/common/FeatureCard';
import { features } from '@/utils/constants';

export default function Home() {
    const router = useRouter();
    const { isAuthenticated, user } = useSelector((state: RootState) => state.auth);

    useEffect(() => {
        if (isAuthenticated && user) {
            // Redirect to appropriate dashboard based on role
            switch (user.role) {
                case 'evowner':
                    router.push('/dashboard/ev-owner');
                    break;
                case 'buyer':
                    router.push('/dashboard/buyer');
                    break;
                case 'verifier':
                    router.push('/dashboard/verifier');
                    break;
                case 'admin':
                    router.push('/admin');
                    break;
            }
        }
    }, [isAuthenticated, user, router]);

    return (
        <>
            <Head>
                <title>Carbon Credit Marketplace - Trade Carbon Credits from Your EV</title>
                <meta name="description" content="Convert your electric vehicle's CO2 savings into tradable carbon credits. Join Vietnam's leading carbon credit marketplace." />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <link rel="icon" href="/favicon.ico" />
            </Head>

            <Box sx={{ minHeight: '100vh' }}>
                <HeroSection />

                <Container maxWidth="lg" sx={{ py: 8 }}>
                    <Typography variant="h3" component="h2" align="center" gutterBottom>
                        How It Works
                    </Typography>

                    <Grid container spacing={4} sx={{ mt: 4 }}>
                        {features.map((feature, index) => (
                            <Grid item xs={12} md={4} key={index}>
                                <FeatureCard {...feature} />
                            </Grid>
                        ))}
                    </Grid>

                    <Box sx={{ textAlign: 'center', mt: 8 }}>
                        <Button
                            variant="contained"
                            size="large"
                            onClick={() => router.push('/register')}
                            sx={{ mr: 2 }}
                        >
                            Get Started as EV Owner
                        </Button>
                        <Button
                            variant="outlined"
                            size="large"
                            onClick={() => router.push('/marketplace')}
                        >
                            Browse Marketplace
                        </Button>
                    </Box>
                </Container>
            </Box>
        </>
    );
}
