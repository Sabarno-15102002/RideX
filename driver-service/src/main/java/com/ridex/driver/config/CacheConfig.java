package com.ridex.driver.config;

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

        private final DriverCacheErrorHandler driverCacheErrorHandler;

        public CacheConfig(DriverCacheErrorHandler driverCacheErrorHandler) {
                this.driverCacheErrorHandler = driverCacheErrorHandler;
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
                        "driverProfile",
                        defaultCacheConfig.entryTtl(Duration.ofMinutes(10)),

                        "driverRating",
                        defaultCacheConfig.entryTtl(Duration.ofMinutes(2)),

                        "vehicleDetails",
                        defaultCacheConfig.entryTtl(Duration.ofMinutes(30))
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
                return driverCacheErrorHandler;
        }
}