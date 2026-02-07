package com.codebuddy.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 限流配置类（使用 Bucket4j）
 */
@Configuration
@EnableConfigurationProperties(RateLimitConfig.DictQueryRateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public ConcurrentMap<String, Bucket> dictQueryBuckets(DictQueryRateLimitProperties properties) {
        return new ConcurrentHashMap<>();
    }

    /**
     * 获取或创建限流 Bucket
     * 对于字典查询接口，我们使用全局限流
     */
    @Bean
    public Bucket dictQueryBucket(DictQueryRateLimitProperties properties) {
        Bandwidth limit = Bandwidth.classic(properties.getCapacity(),
                Refill.greedy(properties.getRefillTokens(), Duration.ofMillis(properties.getRefillMillis())));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * 限流配置属性
     */
    @Data
    @ConfigurationProperties(prefix = "rate-limit.dict-query")
    public static class DictQueryRateLimitProperties {
        /**
         * 桶容量
         */
        private int capacity = 100;

        /**
         * 补充令牌数量
         */
        private int refillTokens = 10;

        /**
         * 补充周期（毫秒）
         */
        private long refillMillis = 1000;
    }
}
