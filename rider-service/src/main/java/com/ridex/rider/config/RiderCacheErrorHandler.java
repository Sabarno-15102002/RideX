package com.ridex.rider.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class RiderCacheErrorHandler implements CacheErrorHandler {

        private static final Logger log = LoggerFactory.getLogger(RiderCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(
                        RuntimeException exception,
                        Cache cache,
                        Object key) {
                log.warn(
                                "Cache GET failed for cache '{}' and key '{}'. "
                                                + "Continuing without cache.",
                                cache.getName(),
                                key,
                                exception);
        }

        @Override
        public void handleCachePutError(
                        RuntimeException exception,
                        Cache cache,
                        Object key,
                        Object value) {
                log.warn(
                                "Cache PUT failed for cache '{}' and key '{}'. "
                                                + "Returning the database result without caching it.",
                                cache.getName(),
                                key,
                                exception);
        }

        @Override
        public void handleCacheEvictError(
                        RuntimeException exception,
                        Cache cache,
                        Object key) {
                log.error(
                                "Cache EVICT failed for cache '{}' and key '{}'. "
                                                + "The cached entry may remain until its TTL expires.",
                                cache.getName(),
                                key,
                                exception);
        }

        @Override
        public void handleCacheClearError(
                        RuntimeException exception,
                        Cache cache) {
                log.error(
                                "Cache CLEAR failed for cache '{}'.",
                                cache.getName(),
                                exception);
        }
}