package com.sirp.analytics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis-backed cache for AnalyticsServiceImpl's summary/by-severity/
 * by-priority/trend reads - see CLAUDE.md's "Redis cache" section for
 * why these specific endpoints were picked and the invalidation
 * strategy. TTL is a safety net, not the primary invalidation
 * mechanism: AnalyticsEventConsumer actively evicts all 4 caches on
 * every incident lifecycle event it processes, so in normal operation
 * a cache entry rarely lives anywhere near its full TTL.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // GenericJackson2JsonRedisSerializer's no-arg constructor builds its
        // own internal ObjectMapper via findAndRegisterModules(), which in
        // live testing did NOT reliably pick up JavaTimeModule - caching
        // DailyTrendResponse (which has a LocalDate field) failed with
        // "Java 8 date/time type not supported". Passing an explicit
        // ObjectMapper with JavaTimeModule registered avoids depending on
        // that auto-discovery at all.
        ObjectMapper redisObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(2))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(redisObjectMapper)));

        return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(config)
                                .build();
    }
}
