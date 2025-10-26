package com.carbonmarketplace.carboncreditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for Carbon Credit Service
 * Handles CO2 calculations, carbon credits management, and verification requests
 */
@SpringBootApplication
@EnableFeignClients
public class CarbonCreditServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CarbonCreditServiceApplication.class, args);
    }
}
