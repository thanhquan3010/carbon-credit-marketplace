package com.carbonmarketplace.notificationservice.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

@org.springframework.context.annotation.Configuration
public class FreemarkerConfig {
    
    @Bean
    public Configuration freemarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        
        // Set template loading from strings (not files)
        cfg.setClassForTemplateLoading(this.getClass(), "/");
        
        // Set default encoding
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        
        // Set error handling
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
        // Don't log exceptions inside FreeMarker that it will thrown at you anyway
        cfg.setLogTemplateExceptions(false);
        
        // Wrap unchecked exceptions thrown during template processing into TemplateException-s
        cfg.setWrapUncheckedExceptions(true);
        
        // Do not fall back to higher scopes when reading a null loop variable
        cfg.setFallbackOnNullLoopVariable(false);
        
        // Set number formatting
        cfg.setNumberFormat("0.######");
        
        // Set date/time formatting
        cfg.setDateFormat("yyyy-MM-dd");
        cfg.setTimeFormat("HH:mm:ss");
        cfg.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
        
        return cfg;
    }
}
