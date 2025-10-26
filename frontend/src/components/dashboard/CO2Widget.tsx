// components/dashboard/CO2Widget.tsx
import React, { useState, useEffect } from 'react';
import {
    Paper,
    Box,
    Typography,
    CircularProgress,
    LinearProgress,
    Stack,
    Chip,
    IconButton,
    Tooltip,
    Button,
    Menu,
    MenuItem,
    Grid,
    Alert,
} from '@mui/material';
import {
    Eco as EcoIcon,
    TrendingUp,
    TrendingDown,
    Info,
    MoreVert,
    Download,
    Share,
    CalendarToday,
} from '@mui/icons-material';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';
import { format } from 'date-fns';

interface CO2Data {
    totalEmissions: number;
    totalOffsets: number;
    netEmissions: number;
    percentageOffset: number;
    trend: 'up' | 'down' | 'stable';
    trendPercentage: number;
    breakdown: Array<{
        category: string;
        value: number;
        color: string;
    }>;
    monthlyData: Array<{
        month: string;
        emissions: number;
        offsets: number;
    }>;
    goals: {
        annual: number;
        achieved: number;
    };
}

interface CO2WidgetProps {
    userId?: string;
    period?: 'week' | 'month' | 'year' | 'all';
    showBreakdown?: boolean;
    compact?: boolean;
}

const COLORS = {
    emissions: '#FF6B6B',
    offsets: '#4ECDC4',
    net: '#95E77E',
    target: '#FFE66D',
};

export const CO2Widget: React.FC<CO2WidgetProps> = ({
    userId,
    period = 'month',
    showBreakdown = true,
    compact = false,
}) => {
    const [data, setData] = useState<CO2Data | null>(null);
    const [loading, setLoading] = useState(true);
    const [selectedPeriod, setSelectedPeriod] = useState(period);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    useEffect(() => {
        fetchCO2Data();
    }, [userId, selectedPeriod]);

    const fetchCO2Data = async () => {
        setLoading(true);
        try {
            // Simulate API call
            await new Promise(resolve => setTimeout(resolve, 1000));

            // Mock data
            setData({
                totalEmissions: 24.5,
                totalOffsets: 18.3,
                netEmissions: 6.2,
                percentageOffset: 74.7,
                trend: 'down',
                trendPercentage: 12.5,
                breakdown: [
                    { category: 'Transportation', value: 12.3, color: '#FF6B6B' },
                    { category: 'Energy', value: 8.2, color: '#4ECDC4' },
                    { category: 'Waste', value: 2.5, color: '#95E77E' },
                    { category: 'Other', value: 1.5, color: '#FFE66D' },
                ],
                monthlyData: [
                    { month: 'Jan', emissions: 8.2, offsets: 5.1 },
                    { month: 'Feb', emissions: 7.9, offsets: 6.2 },
                    { month: 'Mar', emissions: 8.4, offsets: 7.0 },
                ],
                goals: {
                    annual: 100,
                    achieved: 74.7,
                },
            });
        } catch (error) {
            console.error('Error fetching CO2 data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
    };

    const handleExport = () => {
        // Implement export functionality
        console.log('Exporting CO2 data...');
        handleMenuClose();
    };

    const handleShare = () => {
        // Implement share functionality
        console.log('Sharing CO2 data...');
        handleMenuClose();
    };

    const getTrendIcon = () => {
        if (!data) return null;

        switch (data.trend) {
            case 'up':
                return <TrendingUp color="error" />;
            case 'down':
                return <TrendingDown color="success" />;
            default:
                return <TrendingUp color="action" />;
        }
    };

    if (loading) {
        return (
            <Paper sx={{ p: 3, textAlign: 'center' }}>
                <CircularProgress />
            </Paper>
        );
    }

    if (!data) {
        return (
            <Paper sx={{ p: 3, textAlign: 'center' }}>
                <Typography color="text.secondary">No data available</Typography>
            </Paper>
        );
    }

    if (compact) {
        return (
            <Paper sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <EcoIcon color="primary" />
                        <Typography variant="h6">CO₂ Balance</Typography>
                    </Box>
                    <IconButton size="small" onClick={handleMenuOpen}>
                        <MoreVert />
                    </IconButton>
                </Box>

                <Box sx={{ mt: 2 }}>
                    <Typography variant="h4" fontWeight="bold" color="primary">
                        {data.netEmissions.toFixed(1)} tCO₂
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Net emissions this {selectedPeriod}
                    </Typography>
                </Box>

                <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LinearProgress
                        variant="determinate"
                        value={data.percentageOffset}
                        sx={{ flex: 1, height: 8, borderRadius: 4 }}
                        color="success"
                    />
                    <Typography variant="body2" fontWeight="bold">
                        {data.percentageOffset.toFixed(1)}%
                    </Typography>
                </Box>

                <Box sx={{ mt: 1, display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    {getTrendIcon()}
                    <Typography
                        variant="caption"
                        color={data.trend === 'down' ? 'success.main' : 'error.main'}
                    >
                        {data.trendPercentage}% vs last {selectedPeriod}
                    </Typography>
                </Box>

                <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
                    <MenuItem onClick={handleExport}>
                        <Download sx={{ mr: 1 }} /> Export Data
                    </MenuItem>
                    <MenuItem onClick={handleShare}>
                        <Share sx={{ mr: 1 }} /> Share Report
                    </MenuItem>
                </Menu>
            </Paper>
        );
    }

    return (
        <Paper sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <EcoIcon color="primary" sx={{ fontSize: 32 }} />
                    <Box>
                        <Typography variant="h5" fontWeight="bold">
                            Carbon Footprint Dashboard
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            Track your emissions and offsets
                        </Typography>
                    </Box>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Stack direction="row" spacing={1}>
                        {(['week', 'month', 'year', 'all'] as const).map((p) => (
                            <Chip
                                key={p}
                                label={p.charAt(0).toUpperCase() + p.slice(1)}
                                onClick={() => setSelectedPeriod(p)}
                                color={selectedPeriod === p ? 'primary' : 'default'}
                                variant={selectedPeriod === p ? 'filled' : 'outlined'}
                                size="small"
                            />
                        ))}
                    </Stack>
                    <IconButton onClick={handleMenuOpen}>
                        <MoreVert />
                    </IconButton>
                </Box>
            </Box>

            <Grid container spacing={3}>
                {/* Summary Cards */}
                <Grid item xs={12} md={4}>
                    <Box sx={{ p: 2, backgroundColor: 'error.light', borderRadius: 2, color: 'error.contrastText' }}>
                        <Typography variant="body2" sx={{ opacity: 0.9 }}>
                            Total Emissions
                        </Typography>
                        <Typography variant="h4" fontWeight="bold">
                            {data.totalEmissions.toFixed(1)}
                        </Typography>
                        <Typography variant="body2" sx={{ opacity: 0.9 }}>
                            tCO₂ this {selectedPeriod}
                        </Typography>
                    </Box>
                </Grid>

                <Grid item xs={12} md={4}>
                    <Box sx={{ p: 2, backgroundColor: 'success.light', borderRadius: 2, color: 'success.contrastText' }}>
                        <Typography variant="body2" sx={{ opacity: 0.9 }}>
                            Total Offsets
                        </Typography>
                        <Typography variant="h4" fontWeight="bold">
                            {data.totalOffsets.toFixed(1)}
                        </Typography>
                        <Typography variant="body2" sx={{ opacity: 0.9 }}>
                            tCO₂ this {selectedPeriod}
                        </Typography>
                    </Box>
                </Grid>

                <Grid item xs={12} md={4}>
                    <Box sx={{ p: 2, backgroundColor: 'primary.light', borderRadius: 2, color: 'primary.contrastText' }}>
                        <Typography variant="body2" sx={{ opacity: 0.9 }}>
                            Net Balance
                        </Typography>
                        <Typography variant="h4" fontWeight="bold">
                            {data.netEmissions.toFixed(1)}
                        </Typography>
                        <Typography variant="body2" sx={{ opacity: 0.9 }}>
                            tCO₂ remaining
                        </Typography>
                    </Box>
                </Grid>

                {/* Progress Section */}
                <Grid item xs={12}>
                    <Box>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                            <Typography variant="subtitle1" fontWeight="bold">
                                Offset Progress
                            </Typography>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                {getTrendIcon()}
                                <Typography
                                    variant="body2"
                                    color={data.trend === 'down' ? 'success.main' : 'error.main'}
                                    fontWeight="bold"
                                >
                                    {data.trendPercentage}% vs last {selectedPeriod}
                                </Typography>
                            </Box>
                        </Box>

                        <Box sx={{ position: 'relative' }}>
                            <LinearProgress
                                variant="determinate"
                                value={data.percentageOffset}
                                sx={{ height: 24, borderRadius: 12, backgroundColor: 'grey.200' }}
                                color="success"
                            />
                            <Box
                                sx={{
                                    position: 'absolute',
                                    left: '50%',
                                    top: '50%',
                                    transform: 'translate(-50%, -50%)',
                                }}
                            >
                                <Typography variant="body2" fontWeight="bold" color="white">
                                    {data.percentageOffset.toFixed(1)}% Carbon Neutral
                                </Typography>
                            </Box>
                        </Box>

                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                            <Typography variant="caption" color="text.secondary">
                                0 tCO₂
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                                {data.totalEmissions.toFixed(1)} tCO₂
                            </Typography>
                        </Box>
                    </Box>
                </Grid>

                {/* Breakdown Chart */}
                {showBreakdown && (
                    <Grid item xs={12} md={6}>
                        <Box>
                            <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                                Emissions Breakdown
                            </Typography>
                            <ResponsiveContainer width="100%" height={250}>
                                <PieChart>
                                    <Pie
                                        data={data.breakdown}
                                        cx="50%"
                                        cy="50%"
                                        labelLine={false}
                                        label={({ percent }) => `${(percent * 100).toFixed(0)}%`}
                                        outerRadius={80}
                                        fill="#8884d8"
                                        dataKey="value"
                                    >
                                        {data.breakdown.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={entry.color} />
                                        ))}
                                    </Pie>
                                    <RechartsTooltip />
                                    <Legend />
                                </PieChart>
                            </ResponsiveContainer>
                        </Box>
                    </Grid>
                )}

                {/* Goals Progress */}
                <Grid item xs={12} md={showBreakdown ? 6 : 12}>
                    <Box>
                        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                            Annual Goal Progress
                        </Typography>

                        <Box sx={{ mb: 2 }}>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                <Typography variant="body2" color="text.secondary">
                                    Target: {data.goals.annual} tCO₂ offset
                                </Typography>
                                <Typography variant="body2" fontWeight="bold">
                                    {data.goals.achieved.toFixed(1)} tCO₂
                                </Typography>
                            </Box>
                            <LinearProgress
                                variant="determinate"
                                value={(data.goals.achieved / data.goals.annual) * 100}
                                sx={{ height: 10, borderRadius: 5 }}
                                color="primary"
                            />
                        </Box>

                        <Stack spacing={1}>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                <Typography variant="body2" color="text.secondary">
                                    Days remaining
                                </Typography>
                                <Typography variant="body2">
                                    {Math.floor((new Date('2024-12-31').getTime() - Date.now()) / (1000 * 60 * 60 * 24))}
                                </Typography>
                            </Box>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                <Typography variant="body2" color="text.secondary">
                                    Required daily offset
                                </Typography>
                                <Typography variant="body2">
                                    {((data.goals.annual - data.goals.achieved) / 365).toFixed(2)} tCO₂/day
                                </Typography>
                            </Box>
                        </Stack>

                        <Button
                            variant="outlined"
                            fullWidth
                            sx={{ mt: 2 }}
                            startIcon={<EcoIcon />}
                        >
                            Browse Offset Options
                        </Button>
                    </Box>
                </Grid>

                {/* Tips Section */}
                <Grid item xs={12}>
                    <Alert severity="info" icon={<Info />}>
                        <Typography variant="body2">
                            <strong>Tip:</strong> You're {data.percentageOffset.toFixed(0)}% carbon neutral!
                            Consider purchasing an additional {data.netEmissions.toFixed(1)} tCO₂ in credits to achieve
                            full carbon neutrality for this {selectedPeriod}.
                        </Typography>
                    </Alert>
                </Grid>
            </Grid>

            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
                <MenuItem onClick={handleExport}>
                    <Download sx={{ mr: 1 }} /> Export Report
                </MenuItem>
                <MenuItem onClick={handleShare}>
                    <Share sx={{ mr: 1 }} /> Share Dashboard
                </MenuItem>
                <MenuItem onClick={() => { handleMenuClose(); }}>
                    <CalendarToday sx={{ mr: 1 }} /> Schedule Report
                </MenuItem>
            </Menu>
        </Paper>
    );
};
