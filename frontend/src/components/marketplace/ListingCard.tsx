// components/marketplace/ListingCard.tsx
import React from 'react';
import { useRouter } from 'next/router';
import {
    Card,
    CardContent,
    CardMedia,
    CardActions,
    Typography,
    Button,
    Chip,
    Box,
    IconButton,
    Tooltip,
    Stack,
    Avatar,
    Rating,
    LinearProgress,
} from '@mui/material';
import {
    Favorite,
    FavoriteBorder,
    Visibility,
    LocationOn,
    Schedule,
    Verified,
    TrendingUp,
    Gavel,
    LocalOffer,
} from '@mui/icons-material';
import { format, formatDistanceToNow } from 'date-fns';
import { Listing } from '../../services/marketplaceService';
import { useAuth } from '../../hooks/useAuth';

interface ListingCardProps {
    listing: Listing;
    onToggleWatchlist?: (listingId: string) => void;
    isWatched?: boolean;
    variant?: 'grid' | 'list';
}

export const ListingCard: React.FC<ListingCardProps> = ({
    listing,
    onToggleWatchlist,
    isWatched = false,
    variant = 'grid',
}) => {
    const router = useRouter();
    const { isAuthenticated } = useAuth();

    const handleViewDetails = () => {
        router.push(`/marketplace/listings/${listing.id}`);
    };

    const handleWatchlistToggle = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (!isAuthenticated) {
            router.push('/auth/login');
            return;
        }
        onToggleWatchlist?.(listing.id);
    };

    const getListingTypeIcon = () => {
        switch (listing.listingType) {
            case 'AUCTION':
                return <Gavel fontSize="small" />;
            case 'FIXED_PRICE':
                return <LocalOffer fontSize="small" />;
            case 'NEGOTIABLE':
                return <TrendingUp fontSize="small" />;
            default:
                return null;
        }
    };

    const getTimeRemaining = () => {
        if (listing.listingType === 'AUCTION' && listing.auctionDetails) {
            const endTime = new Date(listing.auctionDetails.auctionEndTime);
            if (endTime > new Date()) {
                return formatDistanceToNow(endTime, { addSuffix: true });
            }
            return 'Ended';
        }
        if (listing.expiryDate) {
            const expiry = new Date(listing.expiryDate);
            if (expiry > new Date()) {
                return formatDistanceToNow(expiry, { addSuffix: true });
            }
            return 'Expired';
        }
        return null;
    };

    const getBidProgress = () => {
        if (listing.listingType === 'AUCTION' && listing.auctionDetails?.reservePrice) {
            const progress = (listing.auctionDetails.currentBid / listing.auctionDetails.reservePrice) * 100;
            return Math.min(progress, 100);
        }
        return 0;
    };

    if (variant === 'list') {
        return (
            <Card
                sx={{
                    display: 'flex',
                    mb: 2,
                    cursor: 'pointer',
                    transition: 'all 0.3s',
                    '&:hover': {
                        boxShadow: 4,
                        transform: 'translateY(-2px)',
                    },
                }}
                onClick={handleViewDetails}
            >
                <CardMedia
                    component="img"
                    sx={{ width: 200 }}
                    image={listing.credit.images?.[0] || '/images/placeholder-credit.jpg'}
                    alt={listing.credit.projectName}
                />
                <Box sx={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
                    <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                            <Box sx={{ flex: 1 }}>
                                <Typography variant="h6" component="h3" gutterBottom>
                                    {listing.credit.projectName}
                                </Typography>

                                <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 1 }}>
                                    <Chip
                                        icon={getListingTypeIcon()}
                                        label={listing.listingType.replace('_', ' ')}
                                        size="small"
                                        color={listing.listingType === 'AUCTION' ? 'secondary' : 'primary'}
                                    />
                                    {listing.credit.verificationStatus === 'VERIFIED' && (
                                        <Chip
                                            icon={<Verified />}
                                            label="Verified"
                                            size="small"
                                            color="success"
                                        />
                                    )}
                                    <Chip
                                        label={listing.credit.projectType}
                                        size="small"
                                        variant="outlined"
                                    />
                                </Stack>

                                <Typography variant="body2" color="text.secondary" paragraph>
                                    {listing.description.length > 150
                                        ? `${listing.description.substring(0, 150)}...`
                                        : listing.description}
                                </Typography>

                                <Stack direction="row" spacing={3}>
                                    <Box>
                                        <Typography variant="caption" color="text.secondary">
                                            Quantity
                                        </Typography>
                                        <Typography variant="body2" fontWeight="bold">
                                            {listing.quantity.toLocaleString()} tCO2
                                        </Typography>
                                    </Box>
                                    <Box>
                                        <Typography variant="caption" color="text.secondary">
                                            Vintage
                                        </Typography>
                                        <Typography variant="body2" fontWeight="bold">
                                            {listing.credit.vintage}
                                        </Typography>
                                    </Box>
                                    <Box>
                                        <Typography variant="caption" color="text.secondary">
                                            Location
                                        </Typography>
                                        <Typography variant="body2" fontWeight="bold">
                                            {listing.credit.location.country}
                                        </Typography>
                                    </Box>
                                </Stack>
                            </Box>

                            <Box sx={{ textAlign: 'right', ml: 2 }}>
                                <Typography variant="h5" color="primary" fontWeight="bold">
                                    ${listing.pricePerUnit.toFixed(2)}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    per tCO2
                                </Typography>

                                {listing.listingType === 'AUCTION' && listing.auctionDetails && (
                                    <Box sx={{ mt: 1 }}>
                                        <Typography variant="body2" color="text.secondary">
                                            Current bid
                                        </Typography>
                                        <Typography variant="h6" color="secondary">
                                            ${listing.auctionDetails.currentBid.toFixed(2)}
                                        </Typography>
                                        <Typography variant="caption">
                                            {listing.auctionDetails.numberOfBids} bids
                                        </Typography>
                                    </Box>
                                )}
                            </Box>
                        </Box>
                    </CardContent>

                    <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
                        <Stack direction="row" spacing={2} alignItems="center">
                            <Avatar
                                src={`/api/users/${listing.sellerId}/avatar`}
                                sx={{ width: 24, height: 24 }}
                            />
                            <Box>
                                <Typography variant="caption" color="text.secondary">
                                    Seller
                                </Typography>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                    <Typography variant="body2">
                                        {listing.sellerName}
                                    </Typography>
                                    {listing.sellerVerified && (
                                        <Verified sx={{ fontSize: 14, color: 'primary.main' }} />
                                    )}
                                </Box>
                            </Box>
                            <Rating value={listing.sellerRating} readOnly size="small" />
                        </Stack>

                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="caption" color="text.secondary">
                                <Visibility sx={{ fontSize: 14, mr: 0.5 }} />
                                {listing.views}
                            </Typography>
                            <IconButton size="small" onClick={handleWatchlistToggle}>
                                {isWatched ? <Favorite color="error" /> : <FavoriteBorder />}
                            </IconButton>
                            <Button variant="contained" size="small" onClick={handleViewDetails}>
                                View Details
                            </Button>
                        </Box>
                    </CardActions>
                </Box>
            </Card>
        );
    }

    // Grid variant (default)
    return (
        <Card
            sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                cursor: 'pointer',
                transition: 'all 0.3s',
                '&:hover': {
                    boxShadow: 4,
                    transform: 'translateY(-4px)',
                },
            }}
            onClick={handleViewDetails}
        >
            <Box sx={{ position: 'relative' }}>
                <CardMedia
                    component="img"
                    height="200"
                    image={listing.credit.images?.[0] || '/images/placeholder-credit.jpg'}
                    alt={listing.credit.projectName}
                />

                <IconButton
                    sx={{
                        position: 'absolute',
                        top: 8,
                        right: 8,
                        backgroundColor: 'rgba(255, 255, 255, 0.8)',
                        '&:hover': {
                            backgroundColor: 'rgba(255, 255, 255, 0.95)',
                        },
                    }}
                    onClick={handleWatchlistToggle}
                >
                    {isWatched ? (
                        <Favorite color="error" />
                    ) : (
                        <FavoriteBorder />
                    )}
                </IconButton>

                {listing.listingType === 'AUCTION' && (
                    <Chip
                        icon={<Gavel />}
                        label="AUCTION"
                        size="small"
                        color="secondary"
                        sx={{
                            position: 'absolute',
                            top: 8,
                            left: 8,
                        }}
                    />
                )}

                {getTimeRemaining() && (
                    <Chip
                        icon={<Schedule />}
                        label={getTimeRemaining()}
                        size="small"
                        sx={{
                            position: 'absolute',
                            bottom: 8,
                            left: 8,
                            backgroundColor: 'rgba(255, 255, 255, 0.9)',
                        }}
                    />
                )}
            </Box>

            <CardContent sx={{ flex: 1 }}>
                <Typography
                    variant="h6"
                    component="h3"
                    gutterBottom
                    noWrap
                    sx={{ fontWeight: 'bold' }}
                >
                    {listing.credit.projectName}
                </Typography>

                <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
                    {listing.credit.verificationStatus === 'VERIFIED' && (
                        <Chip
                            icon={<Verified />}
                            label="Verified"
                            size="small"
                            color="success"
                            variant="outlined"
                        />
                    )}
                    <Chip
                        label={listing.credit.projectType}
                        size="small"
                        variant="outlined"
                    />
                </Stack>

                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{
                        mb: 2,
                        height: 40,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                    }}
                >
                    {listing.description}
                </Typography>

                <Box sx={{ mb: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="body2" color="text.secondary">
                            <LocationOn sx={{ fontSize: 14, mr: 0.5 }} />
                            {listing.credit.location.country}
                        </Typography>
                        <Typography variant="body2" fontWeight="bold">
                            {listing.quantity} tCO2
                        </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="body2" color="text.secondary">
                            Vintage {listing.credit.vintage}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            Min: {listing.minPurchaseQuantity} tCO2
                        </Typography>
                    </Box>
                </Box>

                {listing.listingType === 'AUCTION' && listing.auctionDetails && (
                    <>
                        <Box sx={{ mb: 1 }}>
                            <Typography variant="caption" color="text.secondary">
                                Reserve Price Progress
                            </Typography>
                            <LinearProgress
                                variant="determinate"
                                value={getBidProgress()}
                                color={listing.auctionDetails.reserveMet ? 'success' : 'warning'}
                                sx={{ height: 6, borderRadius: 3 }}
                            />
                        </Box>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                            <Typography variant="body2" color="text.secondary">
                                Current bid
                            </Typography>
                            <Typography variant="body2" color="secondary" fontWeight="bold">
                                ${listing.auctionDetails.currentBid.toFixed(2)}
                            </Typography>
                        </Box>
                        <Typography variant="caption" color="text.secondary">
                            {listing.auctionDetails.numberOfBids} bids
                        </Typography>
                    </>
                )}

                {listing.listingType === 'FIXED_PRICE' && (
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                        <Typography variant="body2" color="text.secondary">
                            Price per tCO2
                        </Typography>
                        <Typography variant="h6" color="primary" fontWeight="bold">
                            ${listing.pricePerUnit.toFixed(2)}
                        </Typography>
                    </Box>
                )}
            </CardContent>

            <CardActions sx={{ p: 2, pt: 0 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', flex: 1, mr: 1 }}>
                    <Avatar
                        src={`/api/users/${listing.sellerId}/avatar`}
                        sx={{ width: 24, height: 24, mr: 1 }}
                    />
                    <Box sx={{ flex: 1 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                            <Typography variant="caption" noWrap>
                                {listing.sellerName}
                            </Typography>
                            {listing.sellerVerified && (
                                <Verified sx={{ fontSize: 12, color: 'primary.main' }} />
                            )}
                        </Box>
                        <Rating value={listing.sellerRating} readOnly size="small" />
                    </Box>
                </Box>
                <Button
                    variant="contained"
                    size="small"
                    onClick={handleViewDetails}
                >
                    {listing.listingType === 'AUCTION' ? 'Place Bid' : 'Buy Now'}
                </Button>
            </CardActions>
        </Card>
    );
};

