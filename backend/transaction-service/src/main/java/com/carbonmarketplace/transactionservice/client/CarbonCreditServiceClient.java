package com.carbonmarketplace.transactionservice.client;

import com.carbonmarketplace.transactionservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign client for Carbon Credit Service
 */
@FeignClient(name = "carbon-credit-service", url = "${services.carbon.url:http://carbon-credit-service:8081}")
public interface CarbonCreditServiceClient {
    
    @PostMapping("/api/carbon-credits/lock")
    CreditLockResult lockCredits(@RequestBody LockCreditsRequest request);
    
    @PostMapping("/api/carbon-credits/unlock/{lockId}")
    void unlockCredits(@PathVariable String lockId);
    
    @PostMapping("/api/carbon-credits/transfer")
    CreditTransferResult transferCredits(@RequestBody TransferCreditsRequest request);
    
    @PostMapping("/api/carbon-credits/reverse/{transferId}")
    void reverseTransfer(@PathVariable UUID transferId);
    
    @PostMapping("/api/carbon-credits/refund")
    UUID refundCredits(@RequestBody RefundCreditRequest request);
}
