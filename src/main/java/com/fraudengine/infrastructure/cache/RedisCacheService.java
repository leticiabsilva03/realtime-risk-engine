package com.fraudengine.infrastructure.cache;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisCacheService {

    private static final String CB_NAME = "redis-cache";

    private final StringRedisTemplate redis;

    public RedisCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyLong")
    public Optional<Long> getTransactionCount(String userId, String windowKey) {
        String key = "velocity:" + userId + ":" + windowKey;
        String value = redis.opsForValue().get(key);
        return value == null ? Optional.of(0L) : Optional.of(Long.parseLong(value));
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyVoid")
    public void incrementTransactionCount(String userId, String windowKey, Duration ttl) {
        String key = "velocity:" + userId + ":" + windowKey;
        redis.opsForValue().increment(key);
        redis.expire(key, ttl);
    }


    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyLong")
    public Optional<Long> getUniqueUserCountForDevice(String deviceId, String windowKey) {
        String key = "device:" + deviceId + ":" + windowKey;
        Long count = redis.opsForHyperLogLog().size(key);
        return Optional.of(count == null ? 0L : count);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyVoid")
    public void registerUserForDevice(String deviceId, String windowKey, String userId, Duration ttl) {
        String key = "device:" + deviceId + ":" + windowKey;
        redis.opsForHyperLogLog().add(key, userId);
        redis.expire(key, ttl);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyLocation")
    public Optional<LastTransactionLocation> getLastTransactionLocation(String userId) {
        String key = "geo:last:" + userId;
        String country = redis.opsForHash().get(key, "country") instanceof String s ? s : null;
        String ts = redis.opsForHash().get(key, "ts") instanceof String s ? s : null;
        if (country == null || ts == null) return Optional.empty();
        return Optional.of(new LastTransactionLocation(country,
                java.time.Instant.ofEpochSecond(Long.parseLong(ts))));
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyVoid")
    public void saveLastTransactionLocation(String userId, String country,
                                            java.time.Instant transactionAt, Duration ttl) {
        String key = "geo:last:" + userId;
        redis.opsForHash().put(key, "country", country);
        redis.opsForHash().put(key, "ts", String.valueOf(transactionAt.getEpochSecond()));
        redis.expire(key, ttl);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyBoolean")
    public Optional<Boolean> isFirstTransactionAtMerchant(String userId, String merchantId) {
        String key = "merchant:" + userId;
        Boolean isMember = redis.opsForSet().isMember(key, merchantId);
        return Optional.of(isMember == null || !isMember);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyVoid")
    public void registerMerchantForUser(String userId, String merchantId, Duration ttl) {
        String key = "merchant:" + userId;
        redis.opsForSet().add(key, merchantId);
        redis.expire(key, ttl);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "emptyBoolean")
    public Optional<Boolean> isBlacklisted(String identifier) {
        Boolean isMember = redis.opsForSet().isMember("blacklist:global", identifier);
        return Optional.of(Boolean.TRUE.equals(isMember));
    }


    private Optional<Long> emptyLong(String a, String b, Throwable t) {
        return Optional.empty();
    }

    private Optional<Long> emptyLong(String a, Throwable t) {
        return Optional.empty();
    }

    private Optional<Boolean> emptyBoolean(String a, Throwable t) {
        return Optional.empty();
    }

    private Optional<LastTransactionLocation> emptyLocation(String a, Throwable t) {
        return Optional.empty();
    }

    private void emptyVoid(String a, String b, String c, Duration d, Throwable t) {}
    private void emptyVoid(String a, String b, Duration c, Throwable t) {}
    private void emptyVoid(String a, String b, Throwable t) {}
}