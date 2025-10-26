// components/marketplace/PriceChart.tsx
import React, { useState, useEffect } from 'react';
import {
    Paper,
    Box,
    Typography,
    ToggleButton,
    ToggleButtonGroup,
    CircularProgress,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    Stack,
    Chip,
    Tooltip,
    IconButton,
} from '@mui/material';
import {
    TrendingUp,
    TrendingDown,
    ShowChart,
    BarChart,
    Info,
    Download,
    Fullscreen,
} from '@mui/icons-material';
import {
    LineChart,
    Line,
    AreaChart,
    Area,
    BarChart as RechartsBarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip as RechartsTooltip,
    Legend,
    ResponsiveContainer,
    Brush,
} from 'recharts';
import { format, subDays, subMonths } from 'date-fns';

interface PriceData {
    date: string;
    price: number;
    volume: number;
    high: number;
    low: number;
    projectType?: string;
}

interface PriceChartProps {
    projectType?: string;
    timeRange?: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'ALL';
    height?: number;
    showVolume?: boolean;
    showComparison?: boolean;
}

export const PriceChart: React.FC<PriceChartProps> = ({
    projectType,
    timeRange: initialTimeRange = '1M',
    height = 400,
    showVolume = true,
    showComparison = false,
}) => {
    const [data, setData] = useState<PriceData[]>([]);
    const [loading, setLoading] = useState(true);
    const [chartType, setChartType] = useState<'line' | 'area' | 'bar'>('area');
    const [timeRange, setTimeRange] = useState(initialTimeRange);
    const [selectedMetric, setSelectedMetric] = useState<'price' | 'volume'>('price');
    const [comparisonData, setComparisonData] = useState<PriceData[]>([]);

    // Statistics
    const [statistics, setStatistics] = useState({
        currentPrice: 0,
        change: 0,
        changePercent: 0,
        high: 0,
        low: 0,
        avgPrice: 0,
        totalVolume: 0,
    });

    useEffect(() => {
        fetchPriceData();
    }, [projectType, timeRange]);

    const fetchPriceData = async () => {
        setLoading(true);
        try {
            // Simulate API call
            await new Promise(resolve => setTimeout(resolve, 1000));

            // Generate mock data based on time range
            const mockData = generateMockData(timeRange);
            setData(mockData);

            // Calculate statistics
            if (mockData.length > 0) {
                const currentPrice = mockData[mockData.length - 1].price;
                const previousPrice = mockData[0].price;
                const change = currentPrice - previousPrice;
                const changePercent = (change / previousPrice) * 100;
                const prices = mockData.map(d => d.price);
                const high = Math.max(...prices);
                const low = Math.min(...prices);
                const avgPrice = prices.reduce((a, b) => a + b, 0) / prices.length;
                const totalVolume = mockData.reduce((sum, d) => sum + d.volume, 0);

                setStatistics({
                    currentPrice,
                    change,
                    changePercent,
                    high,
                    low,
                    avgPrice,
                    totalVolume,
                });
            }

            if (showComparison) {
                const compData = generateMockData(timeRange, 'Renewable Energy');
                setComparisonData(compData);
            }
        } catch (error) {
            console.error('Failed to fetch price data:', error);
        } finally {
            setLoading(false);
        }
    };

    const generateMockData = (range: string, type?: string): PriceData[] => {
        const now = new Date();
        let startDate: Date;
        let points: number;

        switch (range) {
            case '1D':
                startDate = subDays(now, 1);
                points = 24;
                break;
            case '1W':
                startDate = subDays(now, 7);
                points = 7;
                break;
            case '1M':
                startDate = subMonths(now, 1);
                points = 30;
                break;
            case '3M':
                startDate = subMonths(now, 3);
                points = 90;
                break;
            case '6M':
                startDate = subMonths(now, 6);
                points = 180;
                break;
            case '1Y':
                startDate = subMonths(now, 12);
                points = 365;
                break;
            default:
                startDate = subMonths(now, 24);
                points = 730;
        }

        const data: PriceData[] = [];
        const basePrice = type === 'Renewable Energy' ? 45 : 50;
        let currentPrice = basePrice;

        for (let i = 0; i < points; i++) {
            const date = new Date(startDate);
            date.setDate(date.getDate() + Math.floor(i * ((now.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)) / points));

            // Random walk with trend
            const change = (Math.random() - 0.48) * 2;
            currentPrice = Math.max(20, Math.min(80, currentPrice + change));

            const dailyHigh = currentPrice + Math.random() * 2;
            const dailyLow = currentPrice - Math.random() * 2;

            data.push({
                date: format(date, range === '1D' ? 'HH:mm' : 'MMM dd'),
                price: parseFloat(currentPrice.toFixed(2)),
                volume: Math.floor(Math.random() * 10000) + 1000,
                high: parseFloat(dailyHigh.toFixed(2)),
                low: parseFloat(dailyLow.toFixed(2)),
                projectType: type,
            });
        }

        return data;
    };

    const handleTimeRangeChange = (event: React.MouseEvent<HTMLElement>, newRange: string | null) => {
        if (newRange !== null) {
            setTimeRange(newRange as typeof timeRange);
        }
    };

    const handleDownload = () => {
        // Implement CSV download
        console.log('Downloading price data...');
    };

    const CustomTooltip = ({ active, payload, label }: any) => {
        if (active && payload && payload.length) {
            return (
                <Paper sx={{ p: 1 }}>
                    <Typography variant="body2">{label}</Typography>
                    {payload.map((entry: any, index: number) => (
                        <Typography key={index} variant="body2" style={{ color: entry.color }}>
                            {entry.name}: ${entry.value.toFixed(2)}
                            {entry.name === 'Volume' ? ' tCO2' : ''}
                        </Typography>
                    ))}
                </Paper>
            );
        }
        return null;
    };

    if (loading) {
        return (
            <Paper sx={{ p: 3, height, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <CircularProgress />
            </Paper>
        );
    }

    const renderChart = () => {
        const chartData = showComparison ? [...data, ...comparisonData] : data;

        switch (chartType) {
            case 'line':
                return (
                    <LineChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis />
                        <RechartsTooltip content={<CustomTooltip />} />
                        <Legend />
                        <Line
                            type="monotone"
                            dataKey="price"
                            stroke="#8884d8"
                            name={projectType || 'Average Price'}
                            strokeWidth={2}
                            dot={false}
                        />
                        {showComparison && (
                            <Line
                                type="monotone"
                                dataKey="price"
                                data={comparisonData}
                                stroke="#82ca9d"
                                name="Renewable Energy"
                                strokeWidth={2}
                                dot={false}
                            />
                        )}
                    </LineChart>
                );

            case 'area':
                return (
                    <AreaChart data={chartData}>
                        <defs>
                            <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#8884d8" stopOpacity={0.8} />
                                <stop offset="95%" stopColor="#8884d8" stopOpacity={0} />
                            </linearGradient>
                            <linearGradient id="colorVolume" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#82ca9d" stopOpacity={0.8} />
                                <stop offset="95%" stopColor="#82ca9d" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis />
                        <RechartsTooltip content={<CustomTooltip />} />
                        <Legend />
                        <Area
                            type="monotone"
                            dataKey={selectedMetric}
                            stroke="#8884d8"
                            fillOpacity={1}
                            fill="url(#colorPrice)"
                            name={selectedMetric === 'price' ? 'Price ($/tCO2)' : 'Volume (tCO2)'}
                        />
                        {showVolume && selectedMetric === 'price' && (
                            <Bar
                                dataKey="volume"
                                fill="#82ca9d"
                                opacity={0.3}
                                name="Volume"
                                yAxisId="right"
                            />
                        )}
                        {['1D', '1W', '1M'].includes(timeRange) && (
                            <Brush dataKey="date" height={30} stroke="#8884d8" />
                        )}
                    </AreaChart>
                );

            case 'bar':
                return (
                    <RechartsBarChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="date" />
                        <YAxis />
                        <RechartsTooltip content={<CustomTooltip />} />
                        <Legend />
                        <Bar dataKey="high" fill="#82ca9d" name="High" />
                        <Bar dataKey="low" fill="#8884d8" name="Low" />
                        <Bar dataKey="price" fill="#ffc658" name="Close" />
                    </RechartsBarChart>
                );

            default:
                return null;
        }
    };

    return (
        <Paper sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Box>
                    <Typography variant="h6" fontWeight="bold">
                        Carbon Credit Price Trends
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 1 }}>
                        <Typography variant="h4" fontWeight="bold">
                            ${statistics.currentPrice.toFixed(2)}
                        </Typography>
                        <Chip
                            icon={statistics.change >= 0 ? <TrendingUp /> : <TrendingDown />}
                            label={`${statistics.change >= 0 ? '+' : ''}${statistics.change.toFixed(2)} (${statistics.changePercent.toFixed(2)}%)`}
                            color={statistics.change >= 0 ? 'success' : 'error'}
                            variant="outlined"
                        />
                    </Box>
                </Box>

                <Stack direction="row" spacing={1} alignItems="center">
                    <ToggleButtonGroup
                        value={chartType}
                        exclusive
                        onChange={(e, newType) => newType && setChartType(newType)}
                        size="small"
                    >
                        <ToggleButton value="line">
                            <ShowChart />
                        </ToggleButton>
                        <ToggleButton value="area">
                            <BarChart />
                        </ToggleButton>
                        <ToggleButton value="bar">
                            <BarChart />
                        </ToggleButton>
                    </ToggleButtonGroup>

                    <Tooltip title="Download data">
                        <IconButton size="small" onClick={handleDownload}>
                            <Download />
                        </IconButton>
                    </Tooltip>

                    <Tooltip title="Fullscreen">
                        <IconButton size="small">
                            <Fullscreen />
                        </IconButton>
                    </Tooltip>
                </Stack>
            </Box>

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <ToggleButtonGroup
                    value={timeRange}
                    exclusive
                    onChange={handleTimeRangeChange}
                    size="small"
                >
                    <ToggleButton value="1D">1D</ToggleButton>
                    <ToggleButton value="1W">1W</ToggleButton>
                    <ToggleButton value="1M">1M</ToggleButton>
                    <ToggleButton value="3M">3M</ToggleButton>
                    <ToggleButton value="6M">6M</ToggleButton>
                    <ToggleButton value="1Y">1Y</ToggleButton>
                    <ToggleButton value="ALL">ALL</ToggleButton>
                </ToggleButtonGroup>

                <Stack direction="row" spacing={2}>
                    <FormControl size="small" sx={{ minWidth: 120 }}>
                        <InputLabel>Metric</InputLabel>
                        <Select
                            value={selectedMetric}
                            onChange={(e) => setSelectedMetric(e.target.value as 'price' | 'volume')}
                            label="Metric"
                        >
                            <MenuItem value="price">Price</MenuItem>
                            <MenuItem value="volume">Volume</MenuItem>
                        </Select>
                    </FormControl>
                </Stack>
            </Box>

            <Box sx={{ display: 'flex', gap: 3, mb: 2 }}>
                <Box>
                    <Typography variant="caption" color="text.secondary">24h High</Typography>
                    <Typography variant="body2" fontWeight="bold">${statistics.high.toFixed(2)}</Typography>
                </Box>
                <Box>
                    <Typography variant="caption" color="text.secondary">24h Low</Typography>
                    <Typography variant="body2" fontWeight="bold">${statistics.low.toFixed(2)}</Typography>
                </Box>
                <Box>
                    <Typography variant="caption" color="text.secondary">Avg Price</Typography>
                    <Typography variant="body2" fontWeight="bold">${statistics.avgPrice.toFixed(2)}</Typography>
                </Box>
                <Box>
                    <Typography variant="caption" color="text.secondary">Total Volume</Typography>
                    <Typography variant="body2" fontWeight="bold">{statistics.totalVolume.toLocaleString()} tCO2</Typography>
                </Box>
            </Box>

            <ResponsiveContainer width="100%" height={height}>
                {renderChart()}
            </ResponsiveContainer>

            {projectType && (
                <Box sx={{ mt: 2, p: 2, backgroundColor: 'background.default', borderRadius: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <Info fontSize="small" color="info" />
                        <Typography variant="body2" fontWeight="bold">
                            Market Insights
                        </Typography>
                    </Box>
                    <Typography variant="body2" color="text.secondary">
                        {projectType} credits are currently trading {statistics.changePercent > 0 ? 'above' : 'below'} the
                        30-day average. Volume has {statistics.totalVolume > 50000 ? 'increased' : 'decreased'} compared
                        to the previous period, indicating {statistics.totalVolume > 50000 ? 'strong' : 'moderate'} market interest.
                    </Typography>
                </Box>
            )}
        </Paper>
    );
};
