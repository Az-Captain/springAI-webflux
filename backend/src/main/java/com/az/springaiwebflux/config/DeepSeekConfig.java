package com.az.springaiwebflux.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Az
 * @date 2025/4/24
 */
@Configuration
@ConfigurationProperties(prefix = "deepseek")
@Data
public class DeepSeekConfig {
    private String apiUrl;
    private String apiKey;
}
