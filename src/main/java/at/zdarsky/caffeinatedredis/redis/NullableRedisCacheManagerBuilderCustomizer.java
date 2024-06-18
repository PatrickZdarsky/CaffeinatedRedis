package at.zdarsky.caffeinatedredis.redis;

@FunctionalInterface
public interface NullableRedisCacheManagerBuilderCustomizer {
    void customize(NullableRedisCacheManagerBuilder builder);
}