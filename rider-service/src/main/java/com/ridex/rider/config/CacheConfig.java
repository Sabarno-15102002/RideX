package com.ridex.rider.config;

import java.time.Duration;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer{

        private final RiderCacheErrorHandler riderCacheErrorHandler;

    public CacheConfig(
            RiderCacheErrorHandler riderCacheErrorHandler
    ) {
        this.riderCacheErrorHandler = riderCacheErrorHandler;
    }
        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

                RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .disableCachingNullValues()
                                .computePrefixWith(cacheName -> "ridex:" + cacheName + "::")
                                .serializeKeysWith(
                                        RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair
                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(cacheConfiguration)
                                .transactionAware()
                                .enableStatistics()
                                .build();
        }

        @Override
        public CacheErrorHandler errorHandler() {
                return riderCacheErrorHandler;
        }
}