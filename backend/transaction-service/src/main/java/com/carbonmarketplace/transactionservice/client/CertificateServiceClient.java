package com.carbonmarketplace.transactionservice.client;

import com.carbonmarketplace.transactionservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign client for Certificate Service
 */
@FeignClient(name = "certificate-service", url = "${services.certificate.url:http://certificate-service:8086}")
public interface CertificateServiceClient {
    
    @PostMapping("/api/certificates/generate")
    CertificateResult generateCertificate(@RequestBody GenerateCertificateRequest request);
    
    @PostMapping("/api/certificates/{certificateId}/void")
    void voidCertificate(@PathVariable UUID certificateId);
    
    @GetMapping("/api/certificates/{certificateId}")
    CertificateDetails getCertificate(@PathVariable UUID certificateId);
}
