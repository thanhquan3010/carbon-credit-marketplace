// store/slices/marketplaceSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Listing } from '../../services/marketplaceService';

interface MarketplaceState {
    listings: Listing[];
    selectedListing: Listing | null;
    watchlist: string[];
    filters: {
        projectType?: string[];
        priceRange?: { min: number; max: number };
        vintage?: { min: number; max: number };
        location?: string;
        sortBy?: string;
    };
    loading: boolean;
    error: string | null;
    pagination: {
        page: number;
        limit: number;
        total: number;
    };
}

const initialState: MarketplaceState = {
    listings: [],
    selectedListing: null,
    watchlist: [],
    filters: {},
    loading: false,
    error: null,
    pagination: {
        page: 1,
        limit: 20,
        total: 0,
    },
};

const marketplaceSlice = createSlice({
    name: 'marketplace',
    initialState,
    reducers: {
        setListings: (state, action: PayloadAction<{ listings: Listing[]; total: number }>) => {
            state.listings = action.payload.listings;
            state.pagination.total = action.payload.total;
            state.loading = false;
            state.error = null;
        },
        setSelectedListing: (state, action: PayloadAction<Listing | null>) => {
            state.selectedListing = action.payload;
        },
        addToWatchlist: (state, action: PayloadAction<string>) => {
            if (!state.watchlist.includes(action.payload)) {
                state.watchlist.push(action.payload);
            }
        },
        removeFromWatchlist: (state, action: PayloadAction<string>) => {
            state.watchlist = state.watchlist.filter(id => id !== action.payload);
        },
        setFilters: (state, action: PayloadAction<MarketplaceState['filters']>) => {
            state.filters = action.payload;
            state.pagination.page = 1; // Reset to first page when filters change
        },
        updateFilter: (state, action: PayloadAction<Partial<MarketplaceState['filters']>>) => {
            state.filters = { ...state.filters, ...action.payload };
            state.pagination.page = 1; // Reset to first page when filters change
        },
        clearFilters: (state) => {
            state.filters = {};
            state.pagination.page = 1;
        },
        setPage: (state, action: PayloadAction<number>) => {
            state.pagination.page = action.payload;
        },
        setLoading: (state, action: PayloadAction<boolean>) => {
            state.loading = action.payload;
        },
        setError: (state, action: PayloadAction<string | null>) => {
            state.error = action.payload;
            state.loading = false;
        },
    },
});

export const {
    setListings,
    setSelectedListing,
    addToWatchlist,
    removeFromWatchlist,
    setFilters,
    updateFilter,
    clearFilters,
    setPage,
    setLoading,
    setError,
} = marketplaceSlice.actions;

export default marketplaceSlice.reducer;
