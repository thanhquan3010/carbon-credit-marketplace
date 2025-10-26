// components/dashboard/EarningsChart.tsx
import React, { useState, useEffect } from 'react';
import {
    Paper,
    Box,
    Typography,
    ToggleButton,
    ToggleButtonGroup,
    CircularProgress,
    Grid,
    Card,
    CardContent,
    Chip,
    Stack,
    IconButton,
    Menu,
    MenuItem,
    Tooltip,
} from '@mui/material';
import {
    TrendingUp,
    TrendingDown,
    AttachMoney,
    AccountBalanceWallet,
    CreditCard,
    MoreVert,
    Download,
    CalendarToday,
} from '@mui/icons-material';
import {
    LineChart,
    Line,
    AreaChart,
    Area,
    BarChart,
    Bar,
    PieChart,
    Pie,
    Cell,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip as RechartsTooltip,
    Legend,
    ResponsiveContainer,
} from 'recharts';
import { format, subDays, subMonths, startOfWeek, endOfWeek } from 'date-fns';

interface EarningsData {
    date: string;
    credits: number;
    trips: number;
    bonuses: number;
    total: number;
}

interface EarningsBreakdown {
    source: string;
    amount: number;
    percentage: number;
    color: string;
}

interface EarningsChartProps {
    userId?: string;
    timeRange?: 'week' | 'month' | 'quarter' | 'year' | 'all';
    showBreakdown?: boolean;
}

const COLORS = {
    credits: '#4ECDC4',
    trips: '#95E77E',
    bonuses: '#FFE66D',
    referrals: '#FF6B6B',
    achievements: '#A78BFA',
};

export const EarningsChart: React.FC<EarningsChartProps> = ({
    userId,
    timeRange: initialTimeRange = 'month',
    showBreakdown = true,
}) => {
    const [data, setData] = useState<EarningsData[]>([]);
    const [breakdown, setBreakdown] = useState<EarningsBreakdown[]>([]);
    const [loading, setLoading] = useState(true);
    const [timeRange, setTimeRange] = useState(initialTimeRange);
    const [chartType, setChartType] = useState<'area' | 'bar'>('area');
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    // Summary statistics
    const [summary, setSummary] = useState({
        totalEarnings: 0,
        creditsSold: 0,
        tripsCompleted: 0,
        bonusesEarned: 0,
        averageDaily: 0,
        bestDay: { date: '', amount: 0 },
        trend: 0,
        pendingPayouts: 0,
    });

    useEffect(() => {
        fetchEarningsData();
    }, [userId, timeRange]);

    const fetchEarningsData = async () => {
        setLoading(true);
        try {
            // Simulate API call
            await new Promise(resolve => setTimeout(resolve, 1000));

            // Generate mock data
            const mockData = generateMockData(timeRange);
            setData(mockData);

            // Calculate summary
            const totalEarnings = mockData.reduce((sum, d) => sum + d.total, 0);
            const creditsSold = mockData.reduce((sum, d) => sum + d.credits, 0);
            const tripsCompleted = mockData.reduce((sum, d) => sum + d.trips, 0);
            const bonusesEarned = mockData.reduce((sum, d) => sum + d.bonuses, 0);
            const averageDaily = totalEarnings / mockData.length;

            const bestDay = mockData.reduce((best, current) =>
                current.total > best.amount ? { date: current.date, amount: current.total } : best,
                { date: '', amount: 0 }
            );

            // Calculate trend (comparing last period with previous)
            const midPoint = Math.floor(mockData.length / 2);
            const firstHalf = mockData.slice(0, midPoint).reduce((sum, d) => sum + d.total, 0);
            const secondHalf = mockData.slice(midPoint).reduce((sum, d) => sum + d.total, 0);
            const trend = ((secondHalf - firstHalf) / firstHalf) * 100;

            setSummary({
                totalEarnings,
                creditsSold,
                tripsCompleted,
                bonusesEarned,
                averageDaily,
                bestDay,
                trend,
                pendingPayouts: totalEarnings * 0.1, // Mock 10% pending
            });

            // Generate breakdown
            setBreakdown([
                { source: 'Carbon Credits', amount: creditsSold, percentage: (creditsSold / totalEarnings) * 100, color: COLORS.credits },
                { source: 'Trip Rewards', amount: tripsCompleted, percentage: (tripsCompleted / totalEarnings) * 100, color: COLORS.trips },
                { source: 'Bonuses', amount: bonusesEarned, percentage: (bonusesEarned / totalEarnings) * 100, color: COLORS.bonuses },
                { source: 'Referrals', amount: totalEarnings * 0.05, percentage: 5, color: COLORS.referrals },
                { source: 'Achievements', amount: totalEarnings * 0.03, percentage: 3, color: COLORS.achievements },
            ]);
        } catch (error) {
            console.error('Failed to fetch earnings data:', error);
        } finally {
            setLoading(false);
        }
    };

    const generateMockData = (range: string): EarningsData[] => {
        const now = new Date();
        let days: number;

        switch (range) {
            case 'week':
                days = 7;
                break;
            case 'month':
                days = 30;
                break;
            case 'quarter':
                days = 90;
                break;
            case 'year':
                days = 365;
                break;
            default:
                days = 730;
        }

        const data: EarningsData[] = [];

        for (let i = days - 1; i >= 0; i--) {
            const date = subDays(now, i);
            const credits = Math.random() * 500 + 100;
            const trips = Math.random() * 200 + 50;
            const bonuses = Math.random() * 100;

            data.push({
                date: format(date, days <= 30 ? 'MMM dd' : 'MMM yyyy'),
                credits: parseFloat(credits.toFixed(2)),
                trips: parseFloat(trips.toFixed(2)),
                bonuses: parseFloat(bonuses.toFixed(2)),
                total: parseFloat((credits + trips + bonuses).toFixed(2)),
            });
        }

        return data;
    };

    const handleMenuClick = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
    };

    const handleExport = () => {
        console.log('Exporting earnings data...');
        handleMenuClose();
    };

    const CustomTooltip = ({ active, payload, label }: any) => {
        if (active && payload && payload.length) {
            return (
                <Paper sx={{ p: 1.5 }}>
                    <Typography variant="body2" fontWeight="bold">{label}</Typography>
                    {payload.map((entry: any, index: number) => (
                        <Typography key={index} variant="body2" style={{ color: entry.color }}>
                            {entry.name}: ${entry.value.toFixed(2)}
                        </Typography>
                    ))}
                    <Typography variant="body2" fontWeight="bold" sx={{ mt: 0.5 }}>
                        Total: ${payload.reduce((sum: number, entry: any) => sum + entry.value, 0).toFixed(2)}
                    </Typography>
                </Paper>
            );
        }
        return null;
    };

    if (loading) {
        return (
            <Paper sx={{ p: 3, display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
                <CircularProgress />
            </Paper>
        );
    }

    return (
        <Box>
            {/* Summary Cards */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Box>
                                    <Typography color="text.secondary" variant="body2">
                                        Total Earnings
                                    </Typography>
                                    <Typography variant="h5" fontWeight="bold">
                                        ${summary.totalEarnings.toFixed(2)}
                                    </Typography>
                                    <Chip
                                        size="small"
                                        icon={summary.trend > 0 ? <TrendingUp /> : <TrendingDown />}
                                        label={`${summary.trend > 0 ? '+' : ''}${summary.trend.toFixed(1)}%`}
                                        color={summary.trend > 0 ? 'success' : 'error'}
                                        variant="outlined"
                                    />
                                </Box>
                                <AttachMoney sx={{ fontSize: 40, color: 'primary.main', opacity: 0.3 }} />
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Box>
                                    <Typography color="text.secondary" variant="body2">
                                        Credits Sold
                                    </Typography>
                                    <Typography variant="h5" fontWeight="bold">
                                        ${summary.creditsSold.toFixed(2)}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        {((summary.creditsSold / summary.totalEarnings) * 100).toFixed(0)}% of total
                                    </Typography>
                                </Box>
                                <AccountBalanceWallet sx={{ fontSize: 40, color: 'success.main', opacity: 0.3 }} />
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Box>
                                    <Typography color="text.secondary" variant="body2">
                                        Daily Average
                                    </Typography>
                                    <Typography variant="h5" fontWeight="bold">
                                        ${summary.averageDaily.toFixed(2)}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        Per day
                                    </Typography>
                                </Box>
                                <CalendarToday sx={{ fontSize: 40, color: 'info.main', opacity: 0.3 }} />
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <Card>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Box>
                                    <Typography color="text.secondary" variant="body2">
                                        Pending Payout
                                    </Typography>
                                    <Typography variant="h5" fontWeight="bold">
                                        ${summary.pendingPayouts.toFixed(2)}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        Next payment: 3 days
                                    </Typography>
                                </Box>
                                <CreditCard sx={{ fontSize: 40, color: 'warning.main', opacity: 0.3 }} />
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>

            {/* Main Chart */}
            <Paper sx={{ p: 3, mb: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Typography variant="h6" fontWeight="bold">
                        Earnings Over Time
                    </Typography>

                    <Stack direction="row" spacing={2} alignItems="center">
                        <ToggleButtonGroup
                            value={timeRange}
                            exclusive
                            onChange={(e, newRange) => newRange && setTimeRange(newRange)}
                            size="small"
                        >
                            <ToggleButton value="week">Week</ToggleButton>
                            <ToggleButton value="month">Month</ToggleButton>
                            <ToggleButton value="quarter">Quarter</ToggleButton>
                            <ToggleButton value="year">Year</ToggleButton>
                            <ToggleButton value="all">All</ToggleButton>
                        </ToggleButtonGroup>

                        <ToggleButtonGroup
                            value={chartType}
                            exclusive
                            onChange={(e, newType) => newType && setChartType(newType)}
                            size="small"
                        >
                            <ToggleButton value="area">Area</ToggleButton>
                            <ToggleButton value="bar">Bar</ToggleButton>
                        </ToggleButtonGroup>

                        <IconButton onClick={handleMenuClick}>
                            <MoreVert />
                        </IconButton>
                    </Stack>
                </Box>

                <ResponsiveContainer width="100%" height={350}>
                    {chartType === 'area' ? (
                        <AreaChart data={data}>
                            <defs>
                                <linearGradient id="colorCredits" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor={COLORS.credits} stopOpacity={0.8} />
                                    <stop offset="95%" stopColor={COLORS.credits} stopOpacity={0} />
                                </linearGradient>
                                <linearGradient id="colorTrips" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor={COLORS.trips} stopOpacity={0.8} />
                                    <stop offset="95%" stopColor={COLORS.trips} stopOpacity={0} />
                                </linearGradient>
                                <linearGradient id="colorBonuses" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor={COLORS.bonuses} stopOpacity={0.8} />
                                    <stop offset="95%" stopColor={COLORS.bonuses} stopOpacity={0} />
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="date" />
                            <YAxis />
                            <RechartsTooltip content={<CustomTooltip />} />
                            <Legend />
                            <Area type="monotone" dataKey="credits" stackId="1" stroke={COLORS.credits} fill="url(#colorCredits)" name="Credits" />
                            <Area type="monotone" dataKey="trips" stackId="1" stroke={COLORS.trips} fill="url(#colorTrips)" name="Trips" />
                            <Area type="monotone" dataKey="bonuses" stackId="1" stroke={COLORS.bonuses} fill="url(#colorBonuses)" name="Bonuses" />
                        </AreaChart>
                    ) : (
                        <BarChart data={data}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="date" />
                            <YAxis />
                            <RechartsTooltip content={<CustomTooltip />} />
                            <Legend />
                            <Bar dataKey="credits" stackId="a" fill={COLORS.credits} name="Credits" />
                            <Bar dataKey="trips" stackId="a" fill={COLORS.trips} name="Trips" />
                            <Bar dataKey="bonuses" stackId="a" fill={COLORS.bonuses} name="Bonuses" />
                        </BarChart>
                    )}
                </ResponsiveContainer>
            </Paper>

            {/* Earnings Breakdown */}
            {showBreakdown && (
                <Grid container spacing={3}>
                    <Grid item xs={12} md={6}>
                        <Paper sx={{ p: 3 }}>
                            <Typography variant="h6" fontWeight="bold" gutterBottom>
                                Earnings Breakdown
                            </Typography>
                            <ResponsiveContainer width="100%" height={250}>
                                <PieChart>
                                    <Pie
                                        data={breakdown}
                                        cx="50%"
                                        cy="50%"
                                        labelLine={false}
                                        label={({ percentage }) => `${percentage.toFixed(0)}%`}
                                        outerRadius={80}
                                        fill="#8884d8"
                                        dataKey="amount"
                                    >
                                        {breakdown.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={entry.color} />
                                        ))}
                                    </Pie>
                                    <RechartsTooltip />
                                </PieChart>
                            </ResponsiveContainer>

                            <Stack spacing={1} sx={{ mt: 2 }}>
                                {breakdown.map((item) => (
                                    <Box key={item.source} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <Box sx={{ width: 12, height: 12, backgroundColor: item.color, borderRadius: '50%' }} />
                                            <Typography variant="body2">{item.source}</Typography>
                                        </Box>
                                        <Typography variant="body2" fontWeight="bold">
                                            ${item.amount.toFixed(2)}
                                        </Typography>
                                    </Box>
                                ))}
                            </Stack>
                        </Paper>
                    </Grid>

                    <Grid item xs={12} md={6}>
                        <Paper sx={{ p: 3 }}>
                            <Typography variant="h6" fontWeight="bold" gutterBottom>
                                Best Performing Days
                            </Typography>

                            <Stack spacing={2}>
                                <Box sx={{ p: 2, backgroundColor: 'primary.light', borderRadius: 1, color: 'primary.contrastText' }}>
                                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                        Best Day This Period
                                    </Typography>
                                    <Typography variant="h5" fontWeight="bold">
                                        ${summary.bestDay.amount.toFixed(2)}
                                    </Typography>
                                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                        on {summary.bestDay.date}
                                    </Typography>
                                </Box>

                                <Box>
                                    <Typography variant="body2" color="text.secondary" gutterBottom>
                                        Performance Metrics
                                    </Typography>
                                    <Stack spacing={1}>
                                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                            <Typography variant="body2">Average Daily</Typography>
                                            <Typography variant="body2" fontWeight="bold">
                                                ${summary.averageDaily.toFixed(2)}
                                            </Typography>
                                        </Box>
                                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                            <Typography variant="body2">Trip Rewards</Typography>
                                            <Typography variant="body2" fontWeight="bold">
                                                ${summary.tripsCompleted.toFixed(2)}
                                            </Typography>
                                        </Box>
                                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                            <Typography variant="body2">Bonuses Earned</Typography>
                                            <Typography variant="body2" fontWeight="bold">
                                                ${summary.bonusesEarned.toFixed(2)}
                                            </Typography>
                                        </Box>
                                    </Stack>
                                </Box>
                            </Stack>
                        </Paper>
                    </Grid>
                </Grid>
            )}

            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
                <MenuItem onClick={handleExport}>
                    <Download sx={{ mr: 1 }} /> Export CSV
                </MenuItem>
                <MenuItem onClick={handleMenuClose}>
                    <CalendarToday sx={{ mr: 1 }} /> Schedule Report
                </MenuItem>
            </Menu>
        </Box>
    );
};
