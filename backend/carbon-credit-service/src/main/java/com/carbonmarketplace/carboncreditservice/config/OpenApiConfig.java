package com.carbonmarketplace.carboncreditservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for API documentation
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Carbon Credit Service API")
                        .version("1.0.0")
                        .description("REST API for managing carbon credits, calculations, and verification requests")
                        .contact(new Contact()
                                .name("Carbon Marketplace Team")
                                .email("support@carbonmarketplace.com")
                                .url("https://carbonmarketplace.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8083")
                                .description("Development server"),
                        new Server()
                                .url("https://api.carbonmarketplace.com/carbon")
                                .description("Production server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer token authentication")));
    }
}
