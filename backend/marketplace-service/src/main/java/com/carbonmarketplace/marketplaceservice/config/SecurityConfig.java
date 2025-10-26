package com.carbonmarketplace.marketplaceservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Marketplace Service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    // TODO: Add JwtAuthenticationFilter when implementing authentication
    // private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configure(http))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/marketplace/listings",
                    "/api/marketplace/listings/{id}",
                    "/api/marketplace/trending",
                    "/api/marketplace/auctions/{id}/bids",
                    "/ws/**",
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                // Seller endpoints
                .requestMatchers(
                    "/api/marketplace/listings/create",
                    "/api/marketplace/listings/*/update",
                    "/api/marketplace/listings/*/cancel",
                    "/api/marketplace/my-listings",
                    "/api/marketplace/price-recommendation"
                ).hasAnyRole("SELLER", "ADMIN")
                // Buyer endpoints
                .requestMatchers(
                    "/api/marketplace/auctions/bid",
                    "/api/marketplace/auctions/my-bids",
                    "/api/marketplace/cart/**"
                ).hasAnyRole("BUYER", "ADMIN")
                // All other requests need authentication
                .anyRequest().authenticated()
            );
        
        // TODO: Add JWT filter when available
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
