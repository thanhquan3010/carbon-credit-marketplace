// pages/marketplace/index.tsx
// Example marketplace page using the implemented components

import React, { useState, useEffect } from 'react';
import {
    Container,
    Grid,
    Box,
    Typography,
    ToggleButton,
    ToggleButtonGroup,
    Pagination,
    CircularProgress,
} from '@mui/material';
import { ViewList, ViewModule } from '@mui/icons-material';
import { useDispatch, useSelector } from 'react-redux';
import Head from 'next/head';

import { ListingCard } from '../../components/marketplace/ListingCard';
import { SearchFilters } from '../../components/marketplace/SearchFilters';
import { PriceChart } from '../../components/marketplace/PriceChart';
import { marketplaceService, Listing } from '../../services/marketplaceService';
import { setListings, addToWatchlist, removeFromWatchlist, setLoading } from '../../store/slices/marketplaceSlice';
import { RootState } from '../../store';
import { useNotification } from '../../hooks/useNotification';

export default function MarketplacePage() {
    const dispatch = useDispatch();
    const { showSuccess, showError } = useNotification();

    const { listings, filters, loading, pagination, watchlist } = useSelector(
        (state: RootState) => state.marketplace
    );

    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [currentPage, setCurrentPage] = useState(1);

    // Fetch listings on mount and when filters change
    useEffect(() => {
        fetchListings();
    }, [filters, currentPage]);

    const fetchListings = async () => {
        dispatch(setLoading(true));
        try {
            const response = await marketplaceService.getListings({
                ...filters,
                page: currentPage,
                limit: pagination.limit,
            });

            dispatch(setListings({
                listings: response.listings,
                total: response.total,
            }));
        } catch (error: any) {
            showError(error.message || 'Failed to fetch listings');
        }
    };

    const handleToggleWatchlist = async (listingId: string) => {
        try {
            const isWatched = watchlist.includes(listingId);

            if (isWatched) {
                await marketplaceService.removeFromWatchlist(listingId);
                dispatch(removeFromWatchlist(listingId));
                showSuccess('Removed from watchlist');
            } else {
                await marketplaceService.addToWatchlist(listingId);
                dispatch(addToWatchlist(listingId));
                showSuccess('Added to watchlist');
            }
        } catch (error: any) {
            showError(error.message || 'Failed to update watchlist');
        }
    };

    const handlePageChange = (event: React.ChangeEvent<unknown>, page: number) => {
        setCurrentPage(page);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    return (
        <>
            <Head>
                <title>Carbon Credit Marketplace</title>
                <meta name="description" content="Buy and sell verified carbon credits" />
            </Head>

            <Container maxWidth="xl" sx={{ py: 4 }}>
                {/* Page Header */}
                <Box sx={{ mb: 4 }}>
                    <Typography variant="h3" component="h1" gutterBottom fontWeight="bold">
                        Carbon Credit Marketplace
                    </Typography>
                    <Typography variant="body1" color="text.secondary">
                        Discover and purchase verified carbon credits from trusted sellers
                    </Typography>
                </Box>

                {/* Price Chart */}
                <Box sx={{ mb: 4 }}>
                    <PriceChart
                        timeRange="1M"
                        height={300}
                        showVolume
                        showComparison={false}
                    />
                </Box>

                <Grid container spacing={3}>
                    {/* Filters Sidebar */}
                    <Grid item xs={12} md={3}>
                        <SearchFilters />
                    </Grid>

                    {/* Listings */}
                    <Grid item xs={12} md={9}>
                        {/* View Controls */}
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                            <Typography variant="body1" color="text.secondary">
                                {pagination.total} listings found
                            </Typography>

                            <ToggleButtonGroup
                                value={viewMode}
                                exclusive
                                onChange={(e, newMode) => newMode && setViewMode(newMode)}
                                size="small"
                            >
                                <ToggleButton value="grid">
                                    <ViewModule />
                                </ToggleButton>
                                <ToggleButton value="list">
                                    <ViewList />
                                </ToggleButton>
                            </ToggleButtonGroup>
                        </Box>

                        {/* Loading State */}
                        {loading && (
                            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                                <CircularProgress />
                            </Box>
                        )}

                        {/* Listings Grid/List */}
                        {!loading && (
                            <>
                                {viewMode === 'grid' ? (
                                    <Grid container spacing={3}>
                                        {listings.map((listing) => (
                                            <Grid item xs={12} sm={6} lg={4} key={listing.id}>
                                                <ListingCard
                                                    listing={listing}
                                                    variant="grid"
                                                    isWatched={watchlist.includes(listing.id)}
                                                    onToggleWatchlist={handleToggleWatchlist}
                                                />
                                            </Grid>
                                        ))}
                                    </Grid>
                                ) : (
                                    <Box>
                                        {listings.map((listing) => (
                                            <ListingCard
                                                key={listing.id}
                                                listing={listing}
                                                variant="list"
                                                isWatched={watchlist.includes(listing.id)}
                                                onToggleWatchlist={handleToggleWatchlist}
                                            />
                                        ))}
                                    </Box>
                                )}

                                {/* Empty State */}
                                {listings.length === 0 && (
                                    <Box sx={{ textAlign: 'center', py: 8 }}>
                                        <Typography variant="h6" gutterBottom>
                                            No listings found
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            Try adjusting your filters to see more results
                                        </Typography>
                                    </Box>
                                )}

                                {/* Pagination */}
                                {listings.length > 0 && (
                                    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                                        <Pagination
                                            count={Math.ceil(pagination.total / pagination.limit)}
                                            page={currentPage}
                                            onChange={handlePageChange}
                                            color="primary"
                                            size="large"
                                        />
                                    </Box>
                                )}
                            </>
                        )}
                    </Grid>
                </Grid>
            </Container>
        </>
    );
}
