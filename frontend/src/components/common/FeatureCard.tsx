import React from 'react';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { SvgIconComponent } from '@mui/icons-material';

interface FeatureCardProps {
  title: string;
  description: string;
  icon: SvgIconComponent;
  color?: string;
}

const FeatureCard: React.FC<FeatureCardProps> = ({ title, description, icon: Icon, color = 'primary.main' }) => {
  return (
    <Card
      sx={{
        height: '100%',
        transition: 'all 0.3s ease',
        '&:hover': {
          transform: 'translateY(-8px)',
          boxShadow: 4,
        },
      }}
    >
      <CardContent sx={{ textAlign: 'center', p: 4 }}>
        <Box
          sx={{
            display: 'inline-flex',
            p: 2,
            borderRadius: '50%',
            bgcolor: `${color}15`,
            mb: 3,
          }}
        >
          <Icon sx={{ fontSize: 48, color }} />
        </Box>
        
        <Typography variant="h5" component="h3" gutterBottom fontWeight="600">
          {title}
        </Typography>
        
        <Typography variant="body1" color="text.secondary">
          {description}
        </Typography>
      </CardContent>
    </Card>
  );
};

export default FeatureCard;
