// components/marketplace/AuctionBidder.tsx
import React, { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    TextField,
    Button,
    Stack,
    Chip,
    Alert,
    List,
    ListItem,
    ListItemAvatar,
    ListItemText,
    Avatar,
    Divider,
    IconButton,
    InputAdornment,
    CircularProgress,
    LinearProgress,
    Tooltip,
    Badge,
} from '@mui/material';
import {
    Gavel,
    Timer,
    TrendingUp,
    Add,
    Remove,
    Info,
    CheckCircle,
    Warning,
    Person,
    EmojiEvents,
    Notifications,
    NotificationsActive,
} from '@mui/icons-material';
import { formatDistanceToNow, format } from 'date-fns';
import { useAuctionWebSocket } from '../../hooks/useWebSocket';
import { useAuth } from '../../hooks/useAuth';
import { useNotification } from '../../hooks/useNotification';
import { Listing, Bid } from '../../services/marketplaceService';

interface AuctionBidderProps {
    listing: Listing;
    bids: Bid[];
    onPlaceBid: (amount: number) => Promise<void>;
}

export const AuctionBidder: React.FC<AuctionBidderProps> = ({
    listing,
    bids,
    onPlaceBid,
}) => {
    const { user, isAuthenticated } = useAuth();
    const { showSuccess, showError, showWarning, showInfo } = useNotification();
    const { currentBid, connected, placeBid } = useAuctionWebSocket(listing.id);

    const [bidAmount, setBidAmount] = useState<number>(0);
    const [isPlacingBid, setIsPlacingBid] = useState(false);
    const [timeRemaining, setTimeRemaining] = useState<string>('');
    const [auctionEnded, setAuctionEnded] = useState(false);
    const [notificationsEnabled, setNotificationsEnabled] = useState(false);
    const [autoBidEnabled, setAutoBidEnabled] = useState(false);
    const [maxAutoBid, setMaxAutoBid] = useState<number>(0);

    const auctionDetails = listing.auctionDetails!;
    const currentPrice = currentBid?.amount || auctionDetails.currentBid;
    const minBidAmount = currentPrice + auctionDetails.bidIncrement;
    const userIsHighestBidder = currentBid?.bidderId === user?.id ||
        auctionDetails.highestBidderId === user?.id;
    const reserveMet = auctionDetails.reserveMet ||
        (auctionDetails.reservePrice && currentPrice >= auctionDetails.reservePrice);

    useEffect(() => {
        setBidAmount(minBidAmount);
    }, [minBidAmount]);

    useEffect(() => {
        const updateTimer = () => {
            const endTime = new Date(auctionDetails.auctionEndTime);
            const now = new Date();

            if (endTime <= now) {
                setAuctionEnded(true);
                setTimeRemaining('Auction ended');
            } else {
                const distance = endTime.getTime() - now.getTime();
                const days = Math.floor(distance / (1000 * 60 * 60 * 24));
                const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
                const seconds = Math.floor((distance % (1000 * 60)) / 1000);

                if (days > 0) {
                    setTimeRemaining(`${days}d ${hours}h ${minutes}m`);
                } else if (hours > 0) {
                    setTimeRemaining(`${hours}h ${minutes}m ${seconds}s`);
                } else {
                    setTimeRemaining(`${minutes}m ${seconds}s`);
                }

                // Show warning when less than 5 minutes remaining
                if (distance < 5 * 60 * 1000 && !userIsHighestBidder) {
                    showWarning('Auction ending soon!', 'Less than 5 minutes remaining');
                }
            }
        };

        updateTimer();
        const interval = setInterval(updateTimer, 1000);
        return () => clearInterval(interval);
    }, [auctionDetails.auctionEndTime, userIsHighestBidder, showWarning]);

    const handleBidAmountChange = (change: number) => {
        const newAmount = bidAmount + change;
        if (newAmount >= minBidAmount) {
            setBidAmount(newAmount);
        }
    };

    const handlePlaceBid = async () => {
        if (!isAuthenticated) {
            showError('Please login to place a bid');
            return;
        }

        if (bidAmount < minBidAmount) {
            showError(`Minimum bid amount is $${minBidAmount.toFixed(2)}`);
            return;
        }

        setIsPlacingBid(true);
        try {
            await onPlaceBid(bidAmount);
            placeBid(bidAmount); // Also send via WebSocket for real-time update
            showSuccess('Bid placed successfully!');
            setBidAmount(bidAmount + auctionDetails.bidIncrement);
        } catch (error: any) {
            showError(error.message || 'Failed to place bid');
        } finally {
            setIsPlacingBid(false);
        }
    };

    const handleToggleNotifications = () => {
        setNotificationsEnabled(!notificationsEnabled);
        if (!notificationsEnabled) {
            // Request notification permissions
            if ('Notification' in window && Notification.permission === 'default') {
                Notification.requestPermission();
            }
            showSuccess('Auction notifications enabled');
        } else {
            showInfo('Auction notifications disabled');
        }
    };

    const handleAutoBid = () => {
        if (!autoBidEnabled) {
            if (maxAutoBid <= currentPrice) {
                showError('Maximum auto-bid must be greater than current price');
                return;
            }
            setAutoBidEnabled(true);
            showSuccess(`Auto-bid enabled up to $${maxAutoBid.toFixed(2)}`);
        } else {
            setAutoBidEnabled(false);
            showInfo('Auto-bid disabled');
        }
    };

    const getBidderRank = (bidderId: string): number => {
        const uniqueBidders = [...new Set(bids.map(b => b.bidderId))];
        return uniqueBidders.indexOf(bidderId) + 1;
    };

    return (
        <Box sx={{ display: 'grid', gap: 3 }}>
            {/* Auction Status */}
            <Paper elevation={2} sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h5" fontWeight="bold">
                        <Gavel sx={{ mr: 1, verticalAlign: 'middle' }} />
                        Live Auction
                    </Typography>
                    <Stack direction="row" spacing={1}>
                        {connected ? (
                            <Chip
                                icon={<CheckCircle />}
                                label="Connected"
                                color="success"
                                size="small"
                            />
                        ) : (
                            <Chip
                                icon={<Warning />}
                                label="Connecting..."
                                color="warning"
                                size="small"
                            />
                        )}
                        <Tooltip title={notificationsEnabled ? 'Disable notifications' : 'Enable notifications'}>
                            <IconButton size="small" onClick={handleToggleNotifications}>
                                {notificationsEnabled ? <NotificationsActive color="primary" /> : <Notifications />}
                            </IconButton>
                        </Tooltip>
                    </Stack>
                </Box>

                <Stack spacing={2}>
                    {/* Time Remaining */}
                    <Box>
                        <Typography variant="body2" color="text.secondary" gutterBottom>
                            Time Remaining
                        </Typography>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Timer color={auctionEnded ? 'disabled' : 'action'} />
                            <Typography
                                variant="h6"
                                color={auctionEnded ? 'text.disabled' : 'text.primary'}
                                fontWeight="bold"
                            >
                                {timeRemaining}
                            </Typography>
                            {!auctionEnded && (
                                <Typography variant="body2" color="text.secondary">
                                    (Ends {format(new Date(auctionDetails.auctionEndTime), 'PPp')})
                                </Typography>
                            )}
                        </Box>
                    </Box>

                    {/* Current Bid */}
                    <Box>
                        <Typography variant="body2" color="text.secondary" gutterBottom>
                            Current Bid
                        </Typography>
                        <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 2 }}>
                            <Typography variant="h4" color="primary" fontWeight="bold">
                                ${currentPrice.toFixed(2)}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                ({auctionDetails.numberOfBids} bids)
                            </Typography>
                        </Box>
                        {userIsHighestBidder && (
                            <Alert severity="success" sx={{ mt: 1 }}>
                                <Typography variant="body2">
                                    <EmojiEvents sx={{ fontSize: 16, mr: 0.5, verticalAlign: 'middle' }} />
                                    You are the highest bidder!
                                </Typography>
                            </Alert>
                        )}
                    </Box>

                    {/* Reserve Price */}
                    {auctionDetails.reservePrice && (
                        <Box>
                            <Typography variant="body2" color="text.secondary" gutterBottom>
                                Reserve Price
                            </Typography>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                <LinearProgress
                                    variant="determinate"
                                    value={Math.min((currentPrice / auctionDetails.reservePrice) * 100, 100)}
                                    sx={{ flex: 1, height: 8, borderRadius: 4 }}
                                    color={reserveMet ? 'success' : 'warning'}
                                />
                                <Chip
                                    label={reserveMet ? 'Met' : 'Not Met'}
                                    color={reserveMet ? 'success' : 'warning'}
                                    size="small"
                                />
                            </Box>
                            {!reserveMet && (
                                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                                    ${(auctionDetails.reservePrice - currentPrice).toFixed(2)} to meet reserve
                                </Typography>
                            )}
                        </Box>
                    )}
                </Stack>
            </Paper>

            {/* Bidding Interface */}
            {!auctionEnded && (
                <Paper elevation={2} sx={{ p: 3 }}>
                    <Typography variant="h6" gutterBottom>
                        Place Your Bid
                    </Typography>

                    <Stack spacing={2}>
                        <Box>
                            <Typography variant="body2" color="text.secondary" gutterBottom>
                                Enter bid amount (minimum: ${minBidAmount.toFixed(2)})
                            </Typography>
                            <Box sx={{ display: 'flex', gap: 1 }}>
                                <TextField
                                    type="number"
                                    value={bidAmount}
                                    onChange={(e) => setBidAmount(parseFloat(e.target.value))}
                                    fullWidth
                                    InputProps={{
                                        startAdornment: <InputAdornment position="start">$</InputAdornment>,
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                <IconButton
                                                    size="small"
                                                    onClick={() => handleBidAmountChange(-auctionDetails.bidIncrement)}
                                                >
                                                    <Remove />
                                                </IconButton>
                                                <IconButton
                                                    size="small"
                                                    onClick={() => handleBidAmountChange(auctionDetails.bidIncrement)}
                                                >
                                                    <Add />
                                                </IconButton>
                                            </InputAdornment>
                                        ),
                                    }}
                                />
                            </Box>
                            <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                                {[1, 5, 10, 20].map((multiplier) => (
                                    <Chip
                                        key={multiplier}
                                        label={`+$${(auctionDetails.bidIncrement * multiplier).toFixed(0)}`}
                                        onClick={() => setBidAmount(minBidAmount + (auctionDetails.bidIncrement * multiplier))}
                                        clickable
                                        size="small"
                                    />
                                ))}
                            </Stack>
                        </Box>

                        <Button
                            variant="contained"
                            size="large"
                            onClick={handlePlaceBid}
                            disabled={isPlacingBid || auctionEnded || bidAmount < minBidAmount}
                            startIcon={isPlacingBid ? <CircularProgress size={20} /> : <Gavel />}
                            fullWidth
                        >
                            {isPlacingBid ? 'Placing Bid...' : `Place Bid of $${bidAmount.toFixed(2)}`}
                        </Button>

                        {/* Auto-bid Feature */}
                        <Box sx={{ p: 2, backgroundColor: 'background.default', borderRadius: 1 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
                                <Typography variant="body2" fontWeight="bold">
                                    <TrendingUp sx={{ fontSize: 16, mr: 0.5, verticalAlign: 'middle' }} />
                                    Auto-Bid
                                </Typography>
                                <Tooltip title="Automatically place bids up to your maximum amount">
                                    <Info fontSize="small" color="action" />
                                </Tooltip>
                            </Box>
                            <Box sx={{ display: 'flex', gap: 1 }}>
                                <TextField
                                    size="small"
                                    type="number"
                                    placeholder="Max bid"
                                    value={maxAutoBid}
                                    onChange={(e) => setMaxAutoBid(parseFloat(e.target.value))}
                                    disabled={autoBidEnabled}
                                    InputProps={{
                                        startAdornment: <InputAdornment position="start">$</InputAdornment>,
                                    }}
                                />
                                <Button
                                    variant={autoBidEnabled ? 'outlined' : 'contained'}
                                    size="small"
                                    onClick={handleAutoBid}
                                    color={autoBidEnabled ? 'error' : 'primary'}
                                >
                                    {autoBidEnabled ? 'Cancel' : 'Enable'}
                                </Button>
                            </Box>
                            {autoBidEnabled && (
                                <Typography variant="caption" color="success.main" sx={{ mt: 0.5 }}>
                                    Auto-bidding active up to ${maxAutoBid.toFixed(2)}
                                </Typography>
                            )}
                        </Box>
                    </Stack>
                </Paper>
            )}

            {/* Bid History */}
            <Paper elevation={2} sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                    Bid History
                </Typography>

                <List sx={{ maxHeight: 400, overflow: 'auto' }}>
                    {bids.length === 0 ? (
                        <ListItem>
                            <ListItemText
                                primary="No bids yet"
                                secondary="Be the first to place a bid!"
                            />
                        </ListItem>
                    ) : (
                        bids.slice(0, 10).map((bid, index) => (
                            <React.Fragment key={bid.id}>
                                <ListItem>
                                    <ListItemAvatar>
                                        <Badge
                                            badgeContent={index === 0 ? '👑' : getBidderRank(bid.bidderId)}
                                            color={index === 0 ? 'primary' : 'default'}
                                        >
                                            <Avatar>
                                                <Person />
                                            </Avatar>
                                        </Badge>
                                    </ListItemAvatar>
                                    <ListItemText
                                        primary={
                                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                <Typography variant="body1">
                                                    {bid.bidderId === user?.id ? 'You' : bid.bidderName}
                                                </Typography>
                                                <Typography variant="body1" fontWeight="bold" color="primary">
                                                    ${bid.amount.toFixed(2)}
                                                </Typography>
                                            </Box>
                                        }
                                        secondary={formatDistanceToNow(new Date(bid.timestamp), { addSuffix: true })}
                                    />
                                </ListItem>
                                {index < bids.length - 1 && <Divider variant="inset" component="li" />}
                            </React.Fragment>
                        ))
                    )}
                </List>

                {bids.length > 10 && (
                    <Button fullWidth size="small" sx={{ mt: 1 }}>
                        View All {bids.length} Bids
                    </Button>
                )}
            </Paper>

            {/* Auction Winner */}
            {auctionEnded && auctionDetails.highestBidderId && (
                <Alert
                    severity={userIsHighestBidder ? 'success' : 'info'}
                    icon={userIsHighestBidder ? <EmojiEvents /> : <Info />}
                >
                    <Typography variant="body1" fontWeight="bold">
                        {userIsHighestBidder ? 'Congratulations! You won the auction!' : 'Auction Ended'}
                    </Typography>
                    <Typography variant="body2">
                        Winning bid: ${auctionDetails.currentBid.toFixed(2)} by{' '}
                        {userIsHighestBidder ? 'you' : auctionDetails.highestBidderName}
                    </Typography>
                    {userIsHighestBidder && (
                        <Button variant="contained" size="small" sx={{ mt: 1 }}>
                            Complete Purchase
                        </Button>
                    )}
                </Alert>
            )}
        </Box>
    );
};
