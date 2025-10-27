package com.carbonmarketplace.transactionservice.client;

import com.carbonmarketplace.transactionservice.dto.*;
import com.carbonmarketplace.transactionservice.dto.TransactionDTOs.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Notification Service
 */
@FeignClient(name = "notification-service", url = "${services.notification.url:http://notification-service:8087}")
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/transaction")
    void sendTransactionNotification(@RequestBody TransactionNotificationRequest request);

    @PostMapping("/api/notifications/settlement")
    void sendSettlementNotification(@RequestBody SettlementNotificationRequest request);

    @PostMapping("/api/notifications/refund")
    void sendRefundNotification(@RequestBody RefundNotificationRequest request);
}
