package com.carbonmarketplace.marketplaceservice.service;

import com.carbonmarketplace.marketplaceservice.dto.CartItemRequest;
import com.carbonmarketplace.marketplaceservice.dto.CartResponse;
import com.carbonmarketplace.marketplaceservice.entity.CartItem;
import com.carbonmarketplace.marketplaceservice.entity.Listing;
import com.carbonmarketplace.marketplaceservice.exception.InsufficientInventoryException;
import com.carbonmarketplace.marketplaceservice.exception.InvalidRequestException;
import com.carbonmarketplace.marketplaceservice.exception.ResourceNotFoundException;
import com.carbonmarketplace.marketplaceservice.repository.jpa.CartItemRepository;
import com.carbonmarketplace.marketplaceservice.repository.jpa.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing shopping cart functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {
    
    private final CartItemRepository cartItemRepository;
    private final ListingRepository listingRepository;
    
    @Value("${marketplace.listing.cart.reservation-minutes}")
    private int reservationMinutes;
    
    @Value("${marketplace.listing.cart.max-items}")
    private int maxCartItems;
    
    @Value("${marketplace.fees.platform-percentage}")
    private BigDecimal platformFeePercentage;
    
    /**
     * Add item to user's cart.
     */
    public CartResponse addToCart(UUID userId, CartItemRequest request) {
        log.info("Adding item to cart for user {}: listing {}, quantity {}",
                userId, request.getListingId(), request.getQuantityTons());
        
        // Check cart size limit
        Integer currentCartSize = cartItemRepository.countByUserId(userId);
        if (currentCartSize >= maxCartItems) {
            throw new InvalidRequestException(
                String.format("Cart limit exceeded. Maximum %d items allowed", maxCartItems)
            );
        }
        
        // Get listing
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        // Validate listing is available
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new InvalidRequestException("Listing is not available for purchase");
        }
        
        if (listing.getListingType() != Listing.ListingType.FIXED) {
            throw new InvalidRequestException("Only fixed-price listings can be added to cart");
        }
        
        // Check available quantity
        BigDecimal reservedQuantity = getReservedQuantity(listing.getId());
        BigDecimal availableQuantity = listing.getAvailableAmountTons().subtract(reservedQuantity);
        
        if (request.getQuantityTons().compareTo(availableQuantity) > 0) {
            throw new InsufficientInventoryException(
                String.format("Only %.3f tons available", availableQuantity)
            );
        }
        
        // Check if item already in cart
        CartItem existingItem = cartItemRepository
                .findByUserIdAndListingId(userId, request.getListingId())
                .orElse(null);
        
        if (existingItem != null) {
            // Update quantity
            BigDecimal newQuantity = existingItem.getQuantityTons().add(request.getQuantityTons());
            if (newQuantity.compareTo(availableQuantity) > 0) {
                throw new InsufficientInventoryException(
                    String.format("Total quantity exceeds available amount: %.3f tons", availableQuantity)
                );
            }
            existingItem.setQuantityTons(newQuantity);
            existingItem.setPricePerTonVnd(listing.getPricePerTonVnd());
            cartItemRepository.save(existingItem);
        } else {
            // Create new cart item
            CartItem cartItem = CartItem.builder()
                    .userId(userId)
                    .listing(listing)
                    .quantityTons(request.getQuantityTons())
                    .pricePerTonVnd(listing.getPricePerTonVnd())
                    .isReserved(false)
                    .build();
            
            cartItemRepository.save(cartItem);
        }
        
        return getCart(userId);
    }
    
    /**
     * Get user's cart.
     */
    public CartResponse getCart(UUID userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAt(userId);
        
        if (cartItems.isEmpty()) {
            return CartResponse.builder()
                    .items(List.of())
                    .totalAmountVnd(BigDecimal.ZERO)
                    .platformFeeVnd(BigDecimal.ZERO)
                    .grandTotalVnd(BigDecimal.ZERO)
                    .itemCount(0)
                    .build();
        }
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CartResponse.CartItemResponse> itemResponses = new java.util.ArrayList<>();
        
        for (CartItem item : cartItems) {
            Listing listing = item.getListing();
            
            // Check if listing is still available
            boolean isAvailable = listing.getStatus() == Listing.ListingStatus.ACTIVE &&
                                  checkAvailability(listing, item.getQuantityTons());
            
            CartResponse.CartItemResponse itemResponse = CartResponse.CartItemResponse.builder()
                    .cartItemId(item.getId())
                    .listingId(listing.getId())
                    .sellerName(listing.getSellerName())
                    .quantityTons(item.getQuantityTons())
                    .pricePerTonVnd(item.getPricePerTonVnd())
                    .subtotalVnd(item.getTotalPrice())
                    .region(listing.getRegion())
                    .verificationStatus(listing.getVerificationStatus())
                    .isReserved(item.getIsReserved())
                    .reservationExpiresAt(item.getReservationExpiresAt())
                    .isAvailable(isAvailable)
                    .build();
            
            itemResponses.add(itemResponse);
            
            if (isAvailable) {
                totalAmount = totalAmount.add(item.getTotalPrice());
            }
        }
        
        BigDecimal platformFee = totalAmount.multiply(platformFeePercentage)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal grandTotal = totalAmount.add(platformFee);
        
        // Get earliest reservation expiry
        LocalDateTime earliestExpiry = cartItems.stream()
                .filter(CartItem::getIsReserved)
                .map(CartItem::getReservationExpiresAt)
                .filter(exp -> exp != null && exp.isAfter(LocalDateTime.now()))
                .min(LocalDateTime::compareTo)
                .orElse(null);
        
        return CartResponse.builder()
                .items(itemResponses)
                .totalAmountVnd(totalAmount)
                .platformFeeVnd(platformFee)
                .grandTotalVnd(grandTotal)
                .itemCount(cartItems.size())
                .reservationExpiresAt(earliestExpiry)
                .build();
    }
    
    /**
     * Update cart item quantity.
     */
    public CartResponse updateCartItem(UUID userId, UUID cartItemId, BigDecimal newQuantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cartItem.getUserId().equals(userId)) {
            throw new InvalidRequestException("Cart item does not belong to user");
        }
        
        // Validate new quantity
        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Quantity must be positive");
        }
        
        // Check availability
        Listing listing = cartItem.getListing();
        BigDecimal reservedQuantity = getReservedQuantity(listing.getId());
        BigDecimal availableQuantity = listing.getAvailableAmountTons()
                .subtract(reservedQuantity)
                .add(cartItem.getQuantityTons()); // Add back current quantity
        
        if (newQuantity.compareTo(availableQuantity) > 0) {
            throw new InsufficientInventoryException(
                String.format("Only %.3f tons available", availableQuantity)
            );
        }
        
        cartItem.setQuantityTons(newQuantity);
        cartItemRepository.save(cartItem);
        
        return getCart(userId);
    }
    
    /**
     * Remove item from cart.
     */
    public CartResponse removeFromCart(UUID userId, UUID cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cartItem.getUserId().equals(userId)) {
            throw new InvalidRequestException("Cart item does not belong to user");
        }
        
        // Release reservation if exists
        if (cartItem.getIsReserved()) {
            cartItem.releaseReservation();
            cartItemRepository.save(cartItem);
        }
        
        cartItemRepository.delete(cartItem);
        
        return getCart(userId);
    }
    
    /**
     * Clear user's cart.
     */
    public void clearCart(UUID userId) {
        // Release all reservations
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAt(userId);
        cartItems.stream()
                .filter(CartItem::getIsReserved)
                .forEach(item -> {
                    item.releaseReservation();
                    cartItemRepository.save(item);
                });
        
        cartItemRepository.deleteAllByUserId(userId);
    }
    
    /**
     * Reserve cart items for checkout.
     */
    public CartResponse reserveCartItems(UUID userId) {
        log.info("Reserving cart items for user {}", userId);
        
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAt(userId);
        
        if (cartItems.isEmpty()) {
            throw new InvalidRequestException("Cart is empty");
        }
        
        LocalDateTime reservationExpiry = LocalDateTime.now().plusMinutes(reservationMinutes);
        
        for (CartItem item : cartItems) {
            Listing listing = item.getListing();
            
            // Check availability
            if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
                throw new InvalidRequestException(
                    String.format("Listing %s is no longer available", listing.getId())
                );
            }
            
            BigDecimal reservedQuantity = getReservedQuantity(listing.getId());
            BigDecimal availableQuantity = listing.getAvailableAmountTons().subtract(reservedQuantity);
            
            if (item.getQuantityTons().compareTo(availableQuantity) > 0) {
                throw new InsufficientInventoryException(
                    String.format("Insufficient inventory for listing %s", listing.getId())
                );
            }
            
            // Reserve the item
            item.reserve(reservationMinutes);
            cartItemRepository.save(item);
        }
        
        log.info("Reserved {} items for user {}, expires at {}", 
                cartItems.size(), userId, reservationExpiry);
        
        return getCart(userId);
    }
    
    /**
     * Release expired reservations.
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<CartItem> expiredReservations = cartItemRepository.findExpiredReservations(now);
        
        if (!expiredReservations.isEmpty()) {
            log.info("Releasing {} expired reservations", expiredReservations.size());
            
            expiredReservations.forEach(item -> {
                item.releaseReservation();
                cartItemRepository.save(item);
            });
        }
    }
    
    /**
     * Get total reserved quantity for a listing.
     */
    private BigDecimal getReservedQuantity(UUID listingId) {
        Double reservedQuantity = cartItemRepository
                .findReservedQuantityForListing(listingId, LocalDateTime.now());
        return reservedQuantity != null ? BigDecimal.valueOf(reservedQuantity) : BigDecimal.ZERO;
    }
    
    /**
     * Check if requested quantity is available.
     */
    private boolean checkAvailability(Listing listing, BigDecimal requestedQuantity) {
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            return false;
        }
        
        BigDecimal reservedQuantity = getReservedQuantity(listing.getId());
        BigDecimal availableQuantity = listing.getAvailableAmountTons().subtract(reservedQuantity);
        
        return requestedQuantity.compareTo(availableQuantity) <= 0;
    }
}
