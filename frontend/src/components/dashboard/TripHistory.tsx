// components/dashboard/TripHistory.tsx
import React, { useState, useEffect } from 'react';
import {
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TablePagination,
    Box,
    Typography,
    Chip,
    IconButton,
    Button,
    TextField,
    InputAdornment,
    Stack,
    Avatar,
    Tooltip,
    LinearProgress,
    Menu,
    MenuItem,
    FormControl,
    Select,
    InputLabel,
    Grid,
} from '@mui/material';
import {
    DirectionsCar,
    ElectricCar,
    EmojiTransportation,
    Search,
    FilterList,
    Download,
    Visibility,
    LocationOn,
    Schedule,
    Speed,
    Eco,
    CheckCircle,
    Cancel,
    Pending,
    MoreVert,
} from '@mui/icons-material';
import { format, formatDistanceToNow, parseISO } from 'date-fns';

interface Trip {
    id: string;
    date: string;
    startLocation: string;
    endLocation: string;
    distance: number;
    duration: number;
    vehicleType: 'EV' | 'HYBRID' | 'PUBLIC';
    vehicleModel?: string;
    co2Saved: number;
    creditsEarned: number;
    status: 'COMPLETED' | 'PENDING' | 'CANCELLED' | 'VERIFIED';
    verificationStatus?: 'PENDING' | 'VERIFIED' | 'REJECTED';
    route?: string;
    fuelSaved?: number;
    averageSpeed?: number;
}

interface TripHistoryProps {
    userId?: string;
    showStats?: boolean;
    compact?: boolean;
}

export const TripHistory: React.FC<TripHistoryProps> = ({
    userId,
    showStats = true,
    compact = false,
}) => {
    const [trips, setTrips] = useState<Trip[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(compact ? 5 : 10);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState<string>('ALL');
    const [filterVehicle, setFilterVehicle] = useState<string>('ALL');
    const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    // Statistics
    const [stats, setStats] = useState({
        totalTrips: 0,
        totalDistance: 0,
        totalCO2Saved: 0,
        totalCredits: 0,
        averageDistance: 0,
        favoriteRoute: '',
        mostUsedVehicle: '',
        verificationRate: 0,
    });

    useEffect(() => {
        fetchTripHistory();
    }, [userId]);

    const fetchTripHistory = async () => {
        setLoading(true);
        try {
            // Simulate API call
            await new Promise(resolve => setTimeout(resolve, 1000));

            // Generate mock data
            const mockTrips = generateMockTrips();
            setTrips(mockTrips);

            // Calculate statistics
            const totalTrips = mockTrips.length;
            const totalDistance = mockTrips.reduce((sum, t) => sum + t.distance, 0);
            const totalCO2Saved = mockTrips.reduce((sum, t) => sum + t.co2Saved, 0);
            const totalCredits = mockTrips.reduce((sum, t) => sum + t.creditsEarned, 0);
            const averageDistance = totalDistance / totalTrips;

            const verifiedTrips = mockTrips.filter(t => t.verificationStatus === 'VERIFIED').length;
            const verificationRate = (verifiedTrips / totalTrips) * 100;

            setStats({
                totalTrips,
                totalDistance,
                totalCO2Saved,
                totalCredits,
                averageDistance,
                favoriteRoute: 'Downtown - Airport',
                mostUsedVehicle: 'Tesla Model 3',
                verificationRate,
            });
        } catch (error) {
            console.error('Failed to fetch trip history:', error);
        } finally {
            setLoading(false);
        }
    };

    const generateMockTrips = (): Trip[] => {
        const locations = [
            'Downtown', 'Airport', 'University', 'Business Park', 'Shopping Mall',
            'Train Station', 'City Center', 'Residential Area', 'Industrial Zone'
        ];

        const vehicles = [
            { type: 'EV', model: 'Tesla Model 3' },
            { type: 'EV', model: 'Nissan Leaf' },
            { type: 'HYBRID', model: 'Toyota Prius' },
            { type: 'PUBLIC', model: 'City Bus' },
            { type: 'PUBLIC', model: 'Metro Train' },
        ];

        const trips: Trip[] = [];

        for (let i = 0; i < 50; i++) {
            const startLoc = locations[Math.floor(Math.random() * locations.length)];
            let endLoc = locations[Math.floor(Math.random() * locations.length)];
            while (endLoc === startLoc) {
                endLoc = locations[Math.floor(Math.random() * locations.length)];
            }

            const vehicle = vehicles[Math.floor(Math.random() * vehicles.length)];
            const distance = Math.random() * 50 + 5;
            const duration = distance * (2 + Math.random());
            const co2Saved = distance * 0.12;

            trips.push({
                id: `TRIP-${1000 + i}`,
                date: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
                startLocation: startLoc,
                endLocation: endLoc,
                distance: parseFloat(distance.toFixed(1)),
                duration: Math.round(duration),
                vehicleType: vehicle.type as 'EV' | 'HYBRID' | 'PUBLIC',
                vehicleModel: vehicle.model,
                co2Saved: parseFloat(co2Saved.toFixed(2)),
                creditsEarned: parseFloat((co2Saved * 2.5).toFixed(2)),
                status: Math.random() > 0.1 ? 'COMPLETED' : Math.random() > 0.5 ? 'PENDING' : 'CANCELLED',
                verificationStatus: Math.random() > 0.3 ? 'VERIFIED' : Math.random() > 0.5 ? 'PENDING' : 'REJECTED',
                route: `${startLoc} - ${endLoc}`,
                fuelSaved: parseFloat((distance * 0.08).toFixed(2)),
                averageSpeed: parseFloat((distance / (duration / 60)).toFixed(1)),
            });
        }

        return trips.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    };

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const handleMenuClick = (event: React.MouseEvent<HTMLElement>, trip: Trip) => {
        setAnchorEl(event.currentTarget);
        setSelectedTrip(trip);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
        setSelectedTrip(null);
    };

    const handleExportData = () => {
        console.log('Exporting trip data...');
    };

    const getVehicleIcon = (type: string) => {
        switch (type) {
            case 'EV':
                return <ElectricCar />;
            case 'HYBRID':
                return <DirectionsCar />;
            case 'PUBLIC':
                return <EmojiTransportation />;
            default:
                return <DirectionsCar />;
        }
    };

    const getStatusColor = (status: string): 'success' | 'warning' | 'error' | 'default' => {
        switch (status) {
            case 'COMPLETED':
            case 'VERIFIED':
                return 'success';
            case 'PENDING':
                return 'warning';
            case 'CANCELLED':
            case 'REJECTED':
                return 'error';
            default:
                return 'default';
        }
    };

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'COMPLETED':
            case 'VERIFIED':
                return <CheckCircle fontSize="small" />;
            case 'PENDING':
                return <Pending fontSize="small" />;
            case 'CANCELLED':
            case 'REJECTED':
                return <Cancel fontSize="small" />;
            default:
                return null;
        }
    };

    const filteredTrips = trips.filter(trip => {
        const matchesSearch =
            trip.startLocation.toLowerCase().includes(searchTerm.toLowerCase()) ||
            trip.endLocation.toLowerCase().includes(searchTerm.toLowerCase()) ||
            trip.vehicleModel?.toLowerCase().includes(searchTerm.toLowerCase());

        const matchesStatus = filterStatus === 'ALL' || trip.status === filterStatus;
        const matchesVehicle = filterVehicle === 'ALL' || trip.vehicleType === filterVehicle;

        return matchesSearch && matchesStatus && matchesVehicle;
    });

    const paginatedTrips = filteredTrips.slice(
        page * rowsPerPage,
        page * rowsPerPage + rowsPerPage
    );

    if (loading) {
        return (
            <Paper sx={{ p: 3 }}>
                <LinearProgress />
                <Typography sx={{ mt: 2, textAlign: 'center' }}>Loading trip history...</Typography>
            </Paper>
        );
    }

    return (
        <Box>
            {/* Statistics Summary */}
            {showStats && !compact && (
                <Grid container spacing={2} sx={{ mb: 3 }}>
                    <Grid item xs={6} sm={3}>
                        <Paper sx={{ p: 2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <DirectionsCar color="primary" />
                                <Box>
                                    <Typography variant="h6" fontWeight="bold">
                                        {stats.totalTrips}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Total Trips
                                    </Typography>
                                </Box>
                            </Box>
                        </Paper>
                    </Grid>

                    <Grid item xs={6} sm={3}>
                        <Paper sx={{ p: 2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Speed color="info" />
                                <Box>
                                    <Typography variant="h6" fontWeight="bold">
                                        {stats.totalDistance.toFixed(1)} km
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Distance Covered
                                    </Typography>
                                </Box>
                            </Box>
                        </Paper>
                    </Grid>

                    <Grid item xs={6} sm={3}>
                        <Paper sx={{ p: 2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Eco color="success" />
                                <Box>
                                    <Typography variant="h6" fontWeight="bold">
                                        {stats.totalCO2Saved.toFixed(1)} kg
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        CO₂ Saved
                                    </Typography>
                                </Box>
                            </Box>
                        </Paper>
                    </Grid>

                    <Grid item xs={6} sm={3}>
                        <Paper sx={{ p: 2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <CheckCircle color="success" />
                                <Box>
                                    <Typography variant="h6" fontWeight="bold">
                                        {stats.verificationRate.toFixed(0)}%
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Verified
                                    </Typography>
                                </Box>
                            </Box>
                        </Paper>
                    </Grid>
                </Grid>
            )}

            {/* Main Table */}
            <Paper>
                {/* Filters */}
                <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
                        <TextField
                            size="small"
                            placeholder="Search trips..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <Search />
                                    </InputAdornment>
                                ),
                            }}
                            sx={{ flex: 1, minWidth: 200 }}
                        />

                        <FormControl size="small" sx={{ minWidth: 120 }}>
                            <InputLabel>Status</InputLabel>
                            <Select
                                value={filterStatus}
                                onChange={(e) => setFilterStatus(e.target.value)}
                                label="Status"
                            >
                                <MenuItem value="ALL">All</MenuItem>
                                <MenuItem value="COMPLETED">Completed</MenuItem>
                                <MenuItem value="PENDING">Pending</MenuItem>
                                <MenuItem value="CANCELLED">Cancelled</MenuItem>
                            </Select>
                        </FormControl>

                        <FormControl size="small" sx={{ minWidth: 120 }}>
                            <InputLabel>Vehicle</InputLabel>
                            <Select
                                value={filterVehicle}
                                onChange={(e) => setFilterVehicle(e.target.value)}
                                label="Vehicle"
                            >
                                <MenuItem value="ALL">All</MenuItem>
                                <MenuItem value="EV">Electric</MenuItem>
                                <MenuItem value="HYBRID">Hybrid</MenuItem>
                                <MenuItem value="PUBLIC">Public</MenuItem>
                            </Select>
                        </FormControl>

                        <Button
                            variant="outlined"
                            startIcon={<Download />}
                            onClick={handleExportData}
                        >
                            Export
                        </Button>
                    </Stack>
                </Box>

                {/* Table */}
                <TableContainer>
                    <Table size={compact ? 'small' : 'medium'}>
                        <TableHead>
                            <TableRow>
                                <TableCell>Trip ID</TableCell>
                                <TableCell>Date</TableCell>
                                <TableCell>Route</TableCell>
                                <TableCell>Vehicle</TableCell>
                                <TableCell align="right">Distance</TableCell>
                                <TableCell align="right">CO₂ Saved</TableCell>
                                <TableCell align="right">Credits</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell align="center">Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {paginatedTrips.map((trip) => (
                                <TableRow key={trip.id} hover>
                                    <TableCell>
                                        <Typography variant="body2" fontWeight="medium">
                                            {trip.id}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        <Box>
                                            <Typography variant="body2">
                                                {format(parseISO(trip.date), 'MMM dd, yyyy')}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary">
                                                {formatDistanceToNow(parseISO(trip.date), { addSuffix: true })}
                                            </Typography>
                                        </Box>
                                    </TableCell>
                                    <TableCell>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <LocationOn fontSize="small" color="action" />
                                            <Box>
                                                <Typography variant="body2">
                                                    {trip.startLocation} → {trip.endLocation}
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    {trip.duration} min | {trip.averageSpeed} km/h
                                                </Typography>
                                            </Box>
                                        </Box>
                                    </TableCell>
                                    <TableCell>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <Avatar sx={{ width: 24, height: 24, bgcolor: 'primary.light' }}>
                                                {getVehicleIcon(trip.vehicleType)}
                                            </Avatar>
                                            <Box>
                                                <Typography variant="body2">
                                                    {trip.vehicleModel}
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    {trip.vehicleType}
                                                </Typography>
                                            </Box>
                                        </Box>
                                    </TableCell>
                                    <TableCell align="right">
                                        <Typography variant="body2" fontWeight="medium">
                                            {trip.distance} km
                                        </Typography>
                                    </TableCell>
                                    <TableCell align="right">
                                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 0.5 }}>
                                            <Eco fontSize="small" color="success" />
                                            <Typography variant="body2" color="success.main" fontWeight="medium">
                                                {trip.co2Saved} kg
                                            </Typography>
                                        </Box>
                                    </TableCell>
                                    <TableCell align="right">
                                        <Typography variant="body2" fontWeight="bold" color="primary">
                                            {trip.creditsEarned}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        <Stack direction="row" spacing={0.5}>
                                            <Chip
                                                label={trip.status}
                                                size="small"
                                                color={getStatusColor(trip.status)}
                                                icon={getStatusIcon(trip.status)}
                                            />
                                            {trip.verificationStatus && (
                                                <Chip
                                                    label={trip.verificationStatus}
                                                    size="small"
                                                    variant="outlined"
                                                    color={getStatusColor(trip.verificationStatus)}
                                                />
                                            )}
                                        </Stack>
                                    </TableCell>
                                    <TableCell align="center">
                                        <Stack direction="row" spacing={0.5} justifyContent="center">
                                            <Tooltip title="View Details">
                                                <IconButton size="small">
                                                    <Visibility fontSize="small" />
                                                </IconButton>
                                            </Tooltip>
                                            <IconButton
                                                size="small"
                                                onClick={(e) => handleMenuClick(e, trip)}
                                            >
                                                <MoreVert fontSize="small" />
                                            </IconButton>
                                        </Stack>
                                    </TableCell>
                                </TableRow>
                            ))}

                            {paginatedTrips.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={9} align="center" sx={{ py: 3 }}>
                                        <Typography color="text.secondary">
                                            No trips found matching your filters
                                        </Typography>
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>

                {/* Pagination */}
                <TablePagination
                    rowsPerPageOptions={compact ? [5, 10] : [5, 10, 25, 50]}
                    component="div"
                    count={filteredTrips.length}
                    rowsPerPage={rowsPerPage}
                    page={page}
                    onPageChange={handleChangePage}
                    onRowsPerPageChange={handleChangeRowsPerPage}
                />
            </Paper>

            {/* Context Menu */}
            <Menu
                anchorEl={anchorEl}
                open={Boolean(anchorEl)}
                onClose={handleMenuClose}
            >
                <MenuItem onClick={handleMenuClose}>
                    <Visibility sx={{ mr: 1 }} /> View Details
                </MenuItem>
                <MenuItem onClick={handleMenuClose}>
                    <Download sx={{ mr: 1 }} /> Download Receipt
                </MenuItem>
                <MenuItem onClick={handleMenuClose}>
                    <Eco sx={{ mr: 1 }} /> View Carbon Certificate
                </MenuItem>
            </Menu>
        </Box>
    );
};
