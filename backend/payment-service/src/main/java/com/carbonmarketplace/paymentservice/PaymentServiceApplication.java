package com.carbonmarketplace.paymentservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableAsync
@EnableRetry
@EnableTransactionManagement
@OpenAPIDefinition(
    info = @Info(
        title = "Payment Service API",
        version = "1.0.0",
        description = "Payment processing service for Carbon Credit Marketplace. " +
                      "Handles payment gateway integrations (MoMo, VNPay, Bank Transfer), " +
                      "webhook processing, and settlement management.",
        contact = @Contact(
            name = "Carbon Marketplace Team",
            email = "support@carbonmarketplace.vn"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "http://www.apache.org/licenses/LICENSE-2.0.html"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8085", description = "Local server"),
        @Server(url = "https://api.carbonmarketplace.vn", description = "Production server")
    }
)
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
