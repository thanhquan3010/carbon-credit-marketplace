import React from 'react';
import { Box, Container, Typography, Button, Stack } from '@mui/material';
import { useRouter } from 'next/router';
import ElectricCarIcon from '@mui/icons-material/ElectricCar';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';

const HeroSection: React.FC = () => {
  const router = useRouter();

  return (
    <Box
      sx={{
        background: 'linear-gradient(135deg, #4caf50 0%, #2196f3 100%)',
        color: 'white',
        pt: 12,
        pb: 10,
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <Container maxWidth="lg">
        <Stack spacing={4} alignItems="center" textAlign="center">
          <Box>
            <ElectricCarIcon sx={{ fontSize: 80, mb: 2 }} />
          </Box>
          
          <Typography
            variant="h2"
            component="h1"
            fontWeight="bold"
            sx={{
              fontSize: { xs: '2.5rem', md: '3.5rem' },
              textShadow: '2px 2px 4px rgba(0,0,0,0.2)',
            }}
          >
            Turn Your EV Miles into Money
          </Typography>

          <Typography
            variant="h5"
            sx={{
              maxWidth: '800px',
              opacity: 0.95,
              fontSize: { xs: '1.1rem', md: '1.5rem' },
            }}
          >
            Vietnam's first carbon credit marketplace for electric vehicle owners. 
            Convert your CO₂ savings into verified carbon credits and earn passive income.
          </Typography>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mt={4}>
            <Button
              variant="contained"
              size="large"
              startIcon={<ElectricCarIcon />}
              onClick={() => router.push('/register')}
              sx={{
                bgcolor: 'white',
                color: 'primary.main',
                px: 4,
                py: 1.5,
                fontSize: '1.1rem',
                '&:hover': {
                  bgcolor: 'grey.100',
                },
              }}
            >
              Start Earning Now
            </Button>
            
            <Button
              variant="outlined"
              size="large"
              startIcon={<AccountBalanceWalletIcon />}
              onClick={() => router.push('/marketplace')}
              sx={{
                borderColor: 'white',
                color: 'white',
                px: 4,
                py: 1.5,
                fontSize: '1.1rem',
                '&:hover': {
                  borderColor: 'white',
                  bgcolor: 'rgba(255,255,255,0.1)',
                },
              }}
            >
              Buy Carbon Credits
            </Button>
          </Stack>

          <Box sx={{ mt: 6, display: 'flex', gap: 6, flexWrap: 'wrap', justifyContent: 'center' }}>
            <Box>
              <Typography variant="h3" fontWeight="bold">10,000+</Typography>
              <Typography variant="subtitle1">EV Owners</Typography>
            </Box>
            <Box>
              <Typography variant="h3" fontWeight="bold">15,000</Typography>
              <Typography variant="subtitle1">Tons CO₂ Offset</Typography>
            </Box>
            <Box>
              <Typography variant="h3" fontWeight="bold">₫50B</Typography>
              <Typography variant="subtitle1">Credits Traded</Typography>
            </Box>
          </Box>
        </Stack>
      </Container>

      {/* Background decoration */}
      <Box
        sx={{
          position: 'absolute',
          top: -100,
          right: -100,
          width: 400,
          height: 400,
          borderRadius: '50%',
          background: 'rgba(255,255,255,0.1)',
          filter: 'blur(100px)',
        }}
      />
    </Box>
  );
};

export default HeroSection;
