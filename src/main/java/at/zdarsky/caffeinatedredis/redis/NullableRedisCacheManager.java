package at.zdarsky.caffeinatedredis.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.Map;

public class NullableRedisCacheManager extends RedisCacheManager {
    private final ObjectMapper objectMapper;

    public NullableRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration,
                                     ObjectMapper objectMapper, Map<String, RedisCacheConfiguration> initialCaches) {
        super(cacheWriter, defaultCacheConfiguration, true, initialCaches);
        this.objectMapper = objectMapper;
    }

    @Override
    @NonNull
    protected RedisCache createRedisCache(@NonNull String name, RedisCacheConfiguration cacheConfiguration) {
        return new NullableRedisCache(name, getCacheWriter(), cacheConfiguration, objectMapper);
    }

    @Override
    public RedisCacheConfiguration getDefaultCacheConfiguration() {
        return super.getDefaultCacheConfiguration();
    }

    public static NullableRedisCacheManagerBuilder builder(@NonNull RedisCacheWriter cacheWriter, @NonNull ObjectMapper objectMapper) {
        return new NullableRedisCacheManagerBuilder(cacheWriter, objectMapper);
    }
}

