package at.zdarsky.caffeinatedredis.redis;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.Arrays;

/**
 * This sub-class is necessary because the normal implementation uses a Java binary serialized value for null values,
 * which cannot be handled by jackson.
 * The normal RedisCache does not provide a neat way of configuring this behaviour.
 * Here we are intercepting this faulty serialization and providing a properly serialized value.
 */
@Slf4j
public class NullableRedisCache extends RedisCache {
    private final byte[] nullValueJson;

    @SneakyThrows
    public NullableRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfiguration,
                              ObjectMapper objectMapper) {
        super(name, cacheWriter, cacheConfiguration);

        nullValueJson = objectMapper.writeValueAsBytes(null);
    }

    @Override
    protected Object lookup(@NonNull Object key) {
        try {
            return super.lookup(key);
        } catch (Exception e) {
            log.error(String.format("Could not retrieve redis cache key '%s'. Simulating cache-miss.", key), e);
            //Simulate cache-miss
            return null;
        }
    }

    @Override
    public void put(Object key, Object value) {
        try {
            super.put(key, value);
        } catch (Exception e) {
            log.error(String.format("Could not set redis cache value of key '%s'. Failing silently", key), e);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        try {
            return super.putIfAbsent(key, value);
        } catch (Exception e) {
            log.error(String.format("Could not set redis cache value of key '%s'. Failing silently", key), e);

            return null;
        }
    }

    @Override
    protected byte[] serializeCacheValue(@NonNull Object value) {
        if (NullValue.INSTANCE.equals(value)) {
            return nullValueJson;
        }

        return super.serializeCacheValue(value);
    }

    @Override
    protected Object deserializeCacheValue(byte[] value) {
        if (Arrays.equals(nullValueJson, value)) {
            return NullValue.INSTANCE;
        }

        return super.deserializeCacheValue(value);
    }
}
