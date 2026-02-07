package com.codebuddy.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 */
@Configuration
@EnableRetry
public class RestTemplateConfig {

    @Bean
    @ConfigurationProperties(prefix = "rest-template")
    public RestTemplateProperties restTemplateProperties() {
        return new RestTemplateProperties();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());
        return new RestTemplate(factory);
    }

    /**
     * RestTemplate 配置属性
     */
    public static class RestTemplateProperties {
        private int connectTimeout = 5000;
        private int readTimeout = 10000;

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
