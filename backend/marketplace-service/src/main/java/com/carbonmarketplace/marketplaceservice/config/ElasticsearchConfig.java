package com.carbonmarketplace.marketplaceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration for search functionality.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.carbonmarketplace.marketplaceservice.repository.elasticsearch")
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    
    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;
    
    @Value("${spring.elasticsearch.username:}")
    private String username;
    
    @Value("${spring.elasticsearch.password:}")
    private String password;
    
    @Value("${spring.elasticsearch.connection-timeout:10s}")
    private String connectionTimeout;
    
    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private String socketTimeout;
    
    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = 
            ClientConfiguration.builder()
                .connectedTo(extractHostAndPort(elasticsearchUri));
        
        // Add authentication if provided
        if (!username.isEmpty() && !password.isEmpty()) {
            builder.withBasicAuth(username, password);
        }
        
        return builder.build();
    }
    
    private String extractHostAndPort(String uri) {
        // Remove protocol (http:// or https://)
        String hostAndPort = uri.replaceFirst("^https?://", "");
        return hostAndPort;
    }
}
