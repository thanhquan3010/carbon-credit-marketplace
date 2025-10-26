package com.carbonmarketplace.marketplaceservice.controller;

import com.carbonmarketplace.marketplaceservice.dto.CartItemRequest;
import com.carbonmarketplace.marketplaceservice.dto.CartResponse;
import com.carbonmarketplace.marketplaceservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for shopping cart operations.
 */
@RestController
@RequestMapping("/marketplace/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping cart operations")
public class CartController {
    
    private final CartService cartService;
    
    @Operation(summary = "Add item to cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    @PostMapping("/items")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody CartItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        CartResponse cart = cartService.addToCart(userId, request);
        
        return ResponseEntity.ok(cart);
    }
    
    @Operation(summary = "Get cart contents")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        CartResponse cart = cartService.getCart(userId);
        
        return ResponseEntity.ok(cart);
    }
    
    @Operation(summary = "Update cart item quantity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid quantity"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @PutMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> updateCartItem(
            @Parameter(description = "Cart item ID", required = true) @PathVariable UUID cartItemId,
            @Parameter(description = "New quantity", required = true) @RequestParam BigDecimal quantity,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        CartResponse cart = cartService.updateCartItem(userId, cartItemId, quantity);
        
        return ResponseEntity.ok(cart);
    }
    
    @Operation(summary = "Remove item from cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> removeFromCart(
            @Parameter(description = "Cart item ID", required = true) @PathVariable UUID cartItemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        CartResponse cart = cartService.removeFromCart(userId, cartItemId);
        
        return ResponseEntity.ok(cart);
    }
    
    @Operation(summary = "Clear cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        cartService.clearCart(userId);
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Reserve cart items for checkout")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Items reserved successfully"),
        @ApiResponse(responseCode = "400", description = "Cart is empty or items unavailable"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/reserve")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponse> reserveCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = getUserIdFromPrincipal(userDetails);
        CartResponse cart = cartService.reserveCartItems(userId);
        
        return ResponseEntity.ok(cart);
    }
    
    /**
     * Extract user ID from authentication principal.
     */
    private UUID getUserIdFromPrincipal(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
