package com.carbonmarketplace.verificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for the verification service.
 */
@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.verification.submitted}")
    private String verificationSubmittedTopic;

    @Value("${kafka.topics.verification.reviewed}")
    private String verificationReviewedTopic;

    @Value("${kafka.topics.verification.assigned}")
    private String verificationAssignedTopic;

    @Value("${kafka.topics.verification.overdue}")
    private String verificationOverdueTopic;

    @Value("${kafka.topics.credits.issued}")
    private String creditsIssuedTopic;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public NewTopic verificationSubmittedTopic() {
        return TopicBuilder.name(verificationSubmittedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic verificationReviewedTopic() {
        return TopicBuilder.name(verificationReviewedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic verificationAssignedTopic() {
        return TopicBuilder.name(verificationAssignedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic verificationOverdueTopic() {
        return TopicBuilder.name(verificationOverdueTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic creditsIssuedTopic() {
        return TopicBuilder.name(creditsIssuedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
