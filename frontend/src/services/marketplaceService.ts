// services/marketplaceService.ts
import { apiClient } from './api';

export interface CarbonCredit {
    id: string;
    projectId: string;
    projectName: string;
    projectType: string;
    vintage: number;
    quantity: number;
    pricePerUnit: number;
    currency: string;
    status: 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'RETIRED';
    verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
    verificationDate?: string;
    certificationBody: string;
    location: {
        country: string;
        region: string;
        coordinates?: {
            lat: number;
            lng: number;
        };
    };
    co2Reduced: number;
    methodology: string;
    additionalInfo?: string;
    images?: string[];
    documents?: Array<{
        id: string;
        name: string;
        type: string;
        url: string;
    }>;
}

export interface Listing {
    id: string;
    creditId: string;
    credit: CarbonCredit;
    sellerId: string;
    sellerName: string;
    sellerRating: number;
    sellerVerified: boolean;
    quantity: number;
    minPurchaseQuantity: number;
    maxPurchaseQuantity?: number;
    pricePerUnit: number;
    currency: string;
    listingType: 'FIXED_PRICE' | 'AUCTION' | 'NEGOTIABLE';
    status: 'ACTIVE' | 'SOLD' | 'CANCELLED' | 'EXPIRED';
    description: string;
    listingDate: string;
    expiryDate?: string;

    // Auction specific fields
    auctionDetails?: {
        startingPrice: number;
        currentBid: number;
        bidIncrement: number;
        numberOfBids: number;
        highestBidderId?: string;
        highestBidderName?: string;
        auctionEndTime: string;
        reservePrice?: number;
        reserveMet: boolean;
    };

    views: number;
    watchlistCount: number;
    tags?: string[];
}

export interface ListingFilters {
    projectType?: string[];
    vintage?: {
        min?: number;
        max?: number;
    };
    priceRange?: {
        min?: number;
        max?: number;
    };
    quantity?: {
        min?: number;
        max?: number;
    };
    location?: {
        country?: string;
        region?: string;
    };
    certificationBody?: string[];
    listingType?: ('FIXED_PRICE' | 'AUCTION' | 'NEGOTIABLE')[];
    verificationStatus?: ('VERIFIED' | 'PENDING')[];
    sellerVerified?: boolean;
    sortBy?: 'price_asc' | 'price_desc' | 'date_newest' | 'date_oldest' | 'quantity_asc' | 'quantity_desc';
    search?: string;
    page?: number;
    limit?: number;
}

export interface CreateListingRequest {
    creditId: string;
    quantity: number;
    minPurchaseQuantity: number;
    maxPurchaseQuantity?: number;
    pricePerUnit: number;
    currency: string;
    listingType: 'FIXED_PRICE' | 'AUCTION' | 'NEGOTIABLE';
    description: string;
    expiryDate?: string;
    tags?: string[];

    // Auction specific fields
    auctionDetails?: {
        startingPrice: number;
        bidIncrement: number;
        auctionEndTime: string;
        reservePrice?: number;
    };
}

export interface Bid {
    id: string;
    listingId: string;
    bidderId: string;
    bidderName: string;
    amount: number;
    timestamp: string;
    status: 'ACTIVE' | 'OUTBID' | 'WON' | 'CANCELLED';
}

export interface Transaction {
    id: string;
    listingId: string;
    listing: Listing;
    buyerId: string;
    buyerName: string;
    sellerId: string;
    sellerName: string;
    quantity: number;
    pricePerUnit: number;
    totalAmount: number;
    currency: string;
    transactionType: 'PURCHASE' | 'AUCTION_WIN';
    status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
    paymentMethod: string;
    paymentStatus: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
    transactionDate: string;
    completionDate?: string;
    certificateUrl?: string;
    transactionHash?: string;
    fees: {
        platformFee: number;
        transactionFee: number;
        totalFees: number;
    };
}

export const marketplaceService = {
    // Listings
    getListings: async (filters?: ListingFilters): Promise<{
        listings: Listing[];
        total: number;
        page: number;
        limit: number;
    }> => {
        return await apiClient.get('/marketplace/listings', { params: filters });
    },

    getListingById: async (id: string): Promise<Listing> => {
        return await apiClient.get(`/marketplace/listings/${id}`);
    },

    createListing: async (data: CreateListingRequest): Promise<Listing> => {
        return await apiClient.post('/marketplace/listings', data);
    },

    updateListing: async (id: string, data: Partial<CreateListingRequest>): Promise<Listing> => {
        return await apiClient.put(`/marketplace/listings/${id}`, data);
    },

    cancelListing: async (id: string): Promise<{ message: string }> => {
        return await apiClient.post(`/marketplace/listings/${id}/cancel`);
    },

    // Bidding
    placeBid: async (listingId: string, amount: number): Promise<Bid> => {
        return await apiClient.post(`/marketplace/listings/${listingId}/bid`, { amount });
    },

    getBids: async (listingId: string): Promise<Bid[]> => {
        return await apiClient.get(`/marketplace/listings/${listingId}/bids`);
    },

    getMyBids: async (): Promise<Bid[]> => {
        return await apiClient.get('/marketplace/my-bids');
    },

    // Purchases
    purchaseListing: async (listingId: string, quantity: number, paymentMethodId: string): Promise<Transaction> => {
        return await apiClient.post(`/marketplace/listings/${listingId}/purchase`, {
            quantity,
            paymentMethodId,
        });
    },

    // Transactions
    getTransactions: async (filters?: {
        status?: string;
        type?: string;
        startDate?: string;
        endDate?: string;
        page?: number;
        limit?: number;
    }): Promise<{
        transactions: Transaction[];
        total: number;
        page: number;
        limit: number;
    }> => {
        return await apiClient.get('/marketplace/transactions', { params: filters });
    },

    getTransactionById: async (id: string): Promise<Transaction> => {
        return await apiClient.get(`/marketplace/transactions/${id}`);
    },

    // Watchlist
    addToWatchlist: async (listingId: string): Promise<{ message: string }> => {
        return await apiClient.post(`/marketplace/listings/${listingId}/watch`);
    },

    removeFromWatchlist: async (listingId: string): Promise<{ message: string }> => {
        return await apiClient.delete(`/marketplace/listings/${listingId}/watch`);
    },

    getWatchlist: async (): Promise<Listing[]> => {
        return await apiClient.get('/marketplace/watchlist');
    },

    // Analytics
    getMarketAnalytics: async (): Promise<{
        totalListings: number;
        averagePrice: number;
        totalVolume: number;
        priceHistory: Array<{ date: string; price: number }>;
        topProjects: Array<{ name: string; volume: number }>;
        marketTrends: Array<{ metric: string; value: number; change: number }>;
    }> => {
        return await apiClient.get('/marketplace/analytics');
    },

    // Search suggestions
    getSearchSuggestions: async (query: string): Promise<string[]> => {
        return await apiClient.get('/marketplace/search-suggestions', { params: { q: query } });
    },

    // Categories and filters
    getCategories: async (): Promise<Array<{ id: string; name: string; count: number }>> => {
        return await apiClient.get('/marketplace/categories');
    },

    getFilterOptions: async (): Promise<{
        projectTypes: string[];
        certificationBodies: string[];
        countries: string[];
        vintageYears: number[];
        priceRanges: Array<{ min: number; max: number; label: string }>;
    }> => {
        return await apiClient.get('/marketplace/filter-options');
    },

    // Seller profiles
    getSellerProfile: async (sellerId: string): Promise<{
        id: string;
        name: string;
        rating: number;
        reviewCount: number;
        verified: boolean;
        joinDate: string;
        totalSales: number;
        activeListings: number;
        bio?: string;
        profileImage?: string;
        badges?: string[];
    }> => {
        return await apiClient.get(`/marketplace/sellers/${sellerId}`);
    },

    getSellerListings: async (sellerId: string): Promise<Listing[]> => {
        return await apiClient.get(`/marketplace/sellers/${sellerId}/listings`);
    },

    // Reviews and ratings
    submitReview: async (transactionId: string, data: {
        rating: number;
        comment: string;
    }): Promise<{ message: string }> => {
        return await apiClient.post(`/marketplace/transactions/${transactionId}/review`, data);
    },

    getReviews: async (sellerId: string): Promise<Array<{
        id: string;
        buyerId: string;
        buyerName: string;
        rating: number;
        comment: string;
        date: string;
        verified: boolean;
    }>> => {
        return await apiClient.get(`/marketplace/sellers/${sellerId}/reviews`);
    },
};


