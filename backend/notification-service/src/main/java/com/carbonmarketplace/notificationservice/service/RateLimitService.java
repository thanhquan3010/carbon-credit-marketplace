package com.carbonmarketplace.notificationservice.service;

import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.entity.NotificationPreference;
import com.carbonmarketplace.notificationservice.repository.NotificationPreferenceRepository;
import com.carbonmarketplace.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * Check if a notification can be sent based on rate limits
     */
    public boolean checkRateLimit(UUID userId, Notification.NotificationChannel channel) {
        // Get user preferences
        NotificationPreference preferences = preferenceRepository.findByUserId(userId)
                .orElse(null);
        
        if (preferences == null) {
            return true; // No preferences set, allow notification
        }
        
        // Check daily limits based on channel
        Integer dailyLimit = getDailyLimit(preferences, channel);
        if (dailyLimit == null || dailyLimit <= 0) {
            return true; // No limit set
        }
        
        // Check Redis for quick rate limit check
        String rateLimitKey = buildRateLimitKey(userId, channel, "daily");
        String currentCount = redisTemplate.opsForValue().get(rateLimitKey);
        
        if (currentCount != null) {
            int count = Integer.parseInt(currentCount);
            if (count >= dailyLimit) {
                log.warn("Daily rate limit exceeded for user {} on channel {}: {}/{}", 
                        userId, channel, count, dailyLimit);
                return false;
            }
            // Increment counter
            redisTemplate.opsForValue().increment(rateLimitKey);
        } else {
            // First notification of the day, set counter with expiry
            redisTemplate.opsForValue().set(rateLimitKey, "1", 24, TimeUnit.HOURS);
        }
        
        // Check hourly rate limit for critical channels
        if (channel == Notification.NotificationChannel.SMS || 
            channel == Notification.NotificationChannel.PUSH) {
            return checkHourlyRateLimit(userId, channel);
        }
        
        return true;
    }
    
    /**
     * Check hourly rate limit for sensitive channels
     */
    private boolean checkHourlyRateLimit(UUID userId, Notification.NotificationChannel channel) {
        String rateLimitKey = buildRateLimitKey(userId, channel, "hourly");
        String currentCount = redisTemplate.opsForValue().get(rateLimitKey);
        
        int hourlyLimit = getHourlyLimit(channel);
        
        if (currentCount != null) {
            int count = Integer.parseInt(currentCount);
            if (count >= hourlyLimit) {
                log.warn("Hourly rate limit exceeded for user {} on channel {}: {}/{}", 
                        userId, channel, count, hourlyLimit);
                return false;
            }
            redisTemplate.opsForValue().increment(rateLimitKey);
        } else {
            redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.HOURS);
        }
        
        return true;
    }
    
    /**
     * Check if user is in quiet hours
     */
    public boolean isInQuietHours(UUID userId) {
        NotificationPreference preferences = preferenceRepository.findByUserId(userId)
                .orElse(null);
        
        if (preferences == null || !preferences.getQuietHoursEnabled()) {
            return false;
        }
        
        // TODO: Implement timezone-aware quiet hours check
        // This would require parsing the quiet hours times and comparing with current time in user's timezone
        
        return false;
    }
    
    /**
     * Reset rate limits for a user (used for testing or admin override)
     */
    public void resetRateLimits(UUID userId) {
        for (Notification.NotificationChannel channel : Notification.NotificationChannel.values()) {
            String dailyKey = buildRateLimitKey(userId, channel, "daily");
            String hourlyKey = buildRateLimitKey(userId, channel, "hourly");
            redisTemplate.delete(dailyKey);
            redisTemplate.delete(hourlyKey);
        }
        log.info("Rate limits reset for user: {}", userId);
    }
    
    /**
     * Get current usage statistics for a user
     */
    public RateLimitStats getUserRateLimitStats(UUID userId) {
        RateLimitStats stats = new RateLimitStats();
        
        NotificationPreference preferences = preferenceRepository.findByUserId(userId)
                .orElse(null);
        
        if (preferences == null) {
            return stats;
        }
        
        for (Notification.NotificationChannel channel : Notification.NotificationChannel.values()) {
            String dailyKey = buildRateLimitKey(userId, channel, "daily");
            String currentCount = redisTemplate.opsForValue().get(dailyKey);
            int count = currentCount != null ? Integer.parseInt(currentCount) : 0;
            Integer limit = getDailyLimit(preferences, channel);
            
            stats.addChannelStats(channel, count, limit);
        }
        
        return stats;
    }
    
    private String buildRateLimitKey(UUID userId, Notification.NotificationChannel channel, String period) {
        return String.format("rate_limit:%s:%s:%s", userId, channel.name(), period);
    }
    
    private Integer getDailyLimit(NotificationPreference preferences, Notification.NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> preferences.getDailyEmailLimit();
            case SMS -> preferences.getDailySmsLimit();
            case PUSH -> preferences.getDailyPushLimit();
            case IN_APP -> null; // No limit for in-app notifications
        };
    }
    
    private int getHourlyLimit(Notification.NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> 5;
            case SMS -> 2;
            case PUSH -> 10;
            case IN_APP -> 50;
        };
    }
    
    public static class RateLimitStats {
        private final java.util.Map<Notification.NotificationChannel, ChannelStats> channelStats = new java.util.HashMap<>();
        
        public void addChannelStats(Notification.NotificationChannel channel, int current, Integer limit) {
            channelStats.put(channel, new ChannelStats(current, limit));
        }
        
        public java.util.Map<Notification.NotificationChannel, ChannelStats> getChannelStats() {
            return channelStats;
        }
        
        public static class ChannelStats {
            private final int current;
            private final Integer limit;
            
            public ChannelStats(int current, Integer limit) {
                this.current = current;
                this.limit = limit;
            }
            
            public int getCurrent() { return current; }
            public Integer getLimit() { return limit; }
            public boolean isExceeded() { return limit != null && current >= limit; }
        }
    }
}
