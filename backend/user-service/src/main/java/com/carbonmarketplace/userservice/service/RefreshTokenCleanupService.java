package com.carbonmarketplace.userservice.service;

import com.carbonmarketplace.userservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for scheduled refresh token maintenance tasks
 * - Cleans up expired tokens
 * - Removes old revoked tokens
 * - Monitors token usage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Delete expired and old revoked refresh tokens
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired refresh tokens");

        try {
            LocalDateTime now = LocalDateTime.now();
            // Keep revoked tokens for 7 days for audit purposes, then delete
            LocalDateTime cutoffDate = now.minusDays(7);

            refreshTokenRepository.deleteExpiredAndRevokedTokens(now, cutoffDate);

            log.info("Completed cleanup of expired refresh tokens");
        } catch (Exception e) {
            log.error("Error during refresh token cleanup", e);
        }
    }

    /**
     * Log token statistics
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour
    public void logTokenStatistics() {
        try {
            long totalTokens = refreshTokenRepository.count();
            log.info("Total refresh tokens in database: {}", totalTokens);
        } catch (Exception e) {
            log.error("Error logging token statistics", e);
        }
    }

    /**
     * Manual cleanup method that can be called via API
     * 
     * @return Number of tokens deleted
     */
    @Transactional
    public int performManualCleanup() {
        log.info("Performing manual cleanup of expired tokens");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.minusDays(7);

        long countBefore = refreshTokenRepository.count();
        refreshTokenRepository.deleteExpiredAndRevokedTokens(now, cutoffDate);
        long countAfter = refreshTokenRepository.count();

        int deleted = (int) (countBefore - countAfter);
        log.info("Manual cleanup completed. Deleted {} tokens", deleted);

        return deleted;
    }
}
