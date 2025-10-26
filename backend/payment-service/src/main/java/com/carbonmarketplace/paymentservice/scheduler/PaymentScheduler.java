package com.carbonmarketplace.paymentservice.scheduler;

import com.carbonmarketplace.paymentservice.service.PaymentService;
import com.carbonmarketplace.paymentservice.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduled.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentScheduler {

    private final PaymentService paymentService;
    private final WebhookService webhookService;

    /**
     * Process scheduled payouts
     * Runs at 10 AM and 3 PM daily
     */
    @Scheduled(cron = "${scheduled.payout.cron:0 0 10,15 * * ?}")
    public void processScheduledPayouts() {
        try {
            log.info("Starting scheduled payout processing");
            paymentService.processScheduledPayouts();
            log.info("Completed scheduled payout processing");
        } catch (Exception e) {
            log.error("Error processing scheduled payouts", e);
        }
    }

    /**
     * Check and expire pending payments
     * Runs every 30 minutes
     */
    @Scheduled(cron = "${scheduled.expired-payments.cron:0 */30 * * * ?}")
    public void processExpiredPayments() {
        try {
            log.info("Checking for expired payments");
            paymentService.processExpiredPayments();
            log.info("Completed expired payment processing");
        } catch (Exception e) {
            log.error("Error processing expired payments", e);
        }
    }

    /**
     * Retry failed webhooks
     * Runs every 15 minutes
     */
    @Scheduled(cron = "${scheduled.webhook-retry.cron:0 */15 * * * ?}")
    public void retryFailedWebhooks() {
        try {
            log.info("Starting webhook retry process");
            webhookService.retryFailedWebhooks();
            log.info("Completed webhook retry process");
        } catch (Exception e) {
            log.error("Error retrying failed webhooks", e);
        }
    }
}
