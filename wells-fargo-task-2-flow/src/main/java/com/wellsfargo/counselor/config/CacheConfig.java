package com.wellsfargo.counselor.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Enables caching. Volatile market data (FX rates) is cached with a short TTL so repeated conversions
 * within a window avoid hitting the upstream feed, while staleness stays bounded.
 *
 * <p>The TTL customizer applies when Redis is the backend (prod profile). In dev/tests the cache type is
 * {@code simple} (in-memory), so no Redis is required and the customizer is simply unused.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String FX_RATES_CACHE = "fxRates";

    @Bean
    public RedisCacheManagerBuilderCustomizer fxRatesCacheCustomizer() {
        return builder -> builder.withCacheConfiguration(FX_RATES_CACHE,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(60)));
    }
}
