package com.ridex.trip.config;

import java.time.Duration;
import java.util.Map;

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
public class CacheConfig implements CachingConfigurer {

        private final TripCacheErrorHandler tripCacheErrorHandler;

        public CacheConfig(TripCacheErrorHandler tripCacheErrorHandler) {
                this.tripCacheErrorHandler = tripCacheErrorHandler;
        }

        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

                RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .disableCachingNullValues()
                                .computePrefixWith(cacheName -> "ridex:" + cacheName + "::")
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

                Map<String, RedisCacheConfiguration> cacheConfigurations =
                Map.of(
                        "tripPayment",
                        defaultCacheConfig.entryTtl(Duration.ofMinutes(10)),

                        "riderIdentity",
                        defaultCacheConfig.entryTtl(Duration.ofMinutes(20)),

                        "tripStatus",
                        defaultCacheConfig.entryTtl(Duration.ofSeconds(30)),

                        "tripDetails",
                        defaultCacheConfig.entryTtl(Duration.ofSeconds(90))
                );

                
                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultCacheConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .transactionAware()
                                .enableStatistics()
                                .build();
        }

        @Override
        public CacheErrorHandler errorHandler() {
                return tripCacheErrorHandler;
        }
}