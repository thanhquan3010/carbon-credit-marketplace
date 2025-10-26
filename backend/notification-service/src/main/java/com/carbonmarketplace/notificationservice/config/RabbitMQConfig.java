package com.carbonmarketplace.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.notification}")
    private String notificationExchange;
    
    @Value("${rabbitmq.queue.email}")
    private String emailQueueName;
    
    @Value("${rabbitmq.queue.sms}")
    private String smsQueueName;
    
    @Value("${rabbitmq.queue.push}")
    private String pushQueueName;
    
    @Value("${rabbitmq.queue.in-app}")
    private String inAppQueueName;
    
    @Value("${rabbitmq.routing.email}")
    private String emailRoutingKey;
    
    @Value("${rabbitmq.routing.sms}")
    private String smsRoutingKey;
    
    @Value("${rabbitmq.routing.push}")
    private String pushRoutingKey;
    
    @Value("${rabbitmq.routing.in-app}")
    private String inAppRoutingKey;
    
    // Exchange Configuration
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(notificationExchange);
    }
    
    // Email Queue Configuration - Priority 5 (Medium)
    @Bean
    public Queue emailQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-priority", 5);
        args.put("x-max-priority", 10);
        args.put("x-message-ttl", 3600000); // 1 hour TTL
        args.put("x-dead-letter-exchange", "dlx.notification");
        args.put("x-dead-letter-routing-key", "dlx.email");
        
        return QueueBuilder.durable(emailQueueName)
                .withArguments(args)
                .build();
    }
    
    // SMS Queue Configuration - Priority 10 (High)
    @Bean
    public Queue smsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-priority", 10);
        args.put("x-max-priority", 10);
        args.put("x-message-ttl", 300000); // 5 minutes TTL
        args.put("x-dead-letter-exchange", "dlx.notification");
        args.put("x-dead-letter-routing-key", "dlx.sms");
        
        return QueueBuilder.durable(smsQueueName)
                .withArguments(args)
                .build();
    }
    
    // Push Queue Configuration - Priority 7 (Medium-High)
    @Bean
    public Queue pushQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-priority", 7);
        args.put("x-max-priority", 10);
        args.put("x-message-ttl", 600000); // 10 minutes TTL
        args.put("x-dead-letter-exchange", "dlx.notification");
        args.put("x-dead-letter-routing-key", "dlx.push");
        
        return QueueBuilder.durable(pushQueueName)
                .withArguments(args)
                .build();
    }
    
    // In-App Queue Configuration - Priority 3 (Low)
    @Bean
    public Queue inAppQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-priority", 3);
        args.put("x-max-priority", 10);
        args.put("x-message-ttl", 86400000); // 24 hours TTL
        args.put("x-dead-letter-exchange", "dlx.notification");
        args.put("x-dead-letter-routing-key", "dlx.in-app");
        
        return QueueBuilder.durable(inAppQueueName)
                .withArguments(args)
                .build();
    }
    
    // Dead Letter Exchange
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange("dlx.notification");
    }
    
    // Dead Letter Queues
    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable("dlq.email").build();
    }
    
    @Bean
    public Queue smsDeadLetterQueue() {
        return QueueBuilder.durable("dlq.sms").build();
    }
    
    @Bean
    public Queue pushDeadLetterQueue() {
        return QueueBuilder.durable("dlq.push").build();
    }
    
    @Bean
    public Queue inAppDeadLetterQueue() {
        return QueueBuilder.durable("dlq.in-app").build();
    }
    
    // Bindings
    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(notificationExchange())
                .with(emailRoutingKey);
    }
    
    @Bean
    public Binding smsBinding() {
        return BindingBuilder
                .bind(smsQueue())
                .to(notificationExchange())
                .with(smsRoutingKey);
    }
    
    @Bean
    public Binding pushBinding() {
        return BindingBuilder
                .bind(pushQueue())
                .to(notificationExchange())
                .with(pushRoutingKey);
    }
    
    @Bean
    public Binding inAppBinding() {
        return BindingBuilder
                .bind(inAppQueue())
                .to(notificationExchange())
                .with(inAppRoutingKey);
    }
    
    // Dead Letter Bindings
    @Bean
    public Binding emailDeadLetterBinding() {
        return BindingBuilder
                .bind(emailDeadLetterQueue())
                .to(deadLetterExchange())
                .with("dlx.email");
    }
    
    @Bean
    public Binding smsDeadLetterBinding() {
        return BindingBuilder
                .bind(smsDeadLetterQueue())
                .to(deadLetterExchange())
                .with("dlx.sms");
    }
    
    @Bean
    public Binding pushDeadLetterBinding() {
        return BindingBuilder
                .bind(pushDeadLetterQueue())
                .to(deadLetterExchange())
                .with("dlx.push");
    }
    
    @Bean
    public Binding inAppDeadLetterBinding() {
        return BindingBuilder
                .bind(inAppDeadLetterQueue())
                .to(deadLetterExchange())
                .with("dlx.in-app");
    }
    
    // Message Converter
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // RabbitTemplate Configuration
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage() + 
                    " from exchange: " + returned.getExchange() + 
                    " with routing key: " + returned.getRoutingKey());
        });
        return rabbitTemplate;
    }
}
