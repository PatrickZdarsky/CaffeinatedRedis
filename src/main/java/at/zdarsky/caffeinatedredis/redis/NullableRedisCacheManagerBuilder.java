package at.zdarsky.caffeinatedredis.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.LinkedHashMap;
import java.util.Map;

public class NullableRedisCacheManagerBuilder {

    protected RedisCacheWriter cacheWriter;
    protected ObjectMapper objectMapper;

    protected boolean transactionAware = true;
    protected final Map<String, RedisCacheConfiguration> initialCaches = new LinkedHashMap<>();
    protected RedisCacheConfiguration defaultCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();

    NullableRedisCacheManagerBuilder(@NonNull RedisCacheWriter cacheWriter, @NonNull ObjectMapper objectMapper) {
        this.cacheWriter = cacheWriter;
        this.objectMapper = objectMapper;
    }

    public NullableRedisCacheManagerBuilder defaultCacheConfiguration(@NonNull RedisCacheConfiguration defaultCacheConfiguration) {
        this.defaultCacheConfiguration = defaultCacheConfiguration;
        return this;
    }

    public RedisCacheConfiguration defaultCacheConfiguration() {
        return defaultCacheConfiguration;
    }

    public NullableRedisCacheManagerBuilder configureCache(@NonNull String cacheName,
                                                           @NonNull RedisCacheConfiguration cacheConfiguration) {
        initialCaches.put(cacheName, cacheConfiguration);

        return this;
    }

    public NullableRedisCacheManagerBuilder transactionAware(boolean transactionAware) {
        this.transactionAware = transactionAware;

        return this;
    }

    public NullableRedisCacheManager build() {
        var cacheManager = new NullableRedisCacheManager(cacheWriter, defaultCacheConfiguration, objectMapper, initialCaches);
        cacheManager.setTransactionAware(transactionAware);
        return cacheManager;
    }
}

