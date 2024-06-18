package at.zdarsky.caffeinatedredis.config;

import at.zdarsky.caffeinatedredis.redis.NullableRedisCacheManager;
import at.zdarsky.caffeinatedredis.redis.NullableRedisCacheManagerBuilder;
import at.zdarsky.caffeinatedredis.redis.NullableRedisCacheManagerBuilderCustomizer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
@EnableConfigurationProperties({RedisProperties.class, CacheProperties.class})
public class RedisCacheConfig {
    private final RedisProperties redisProperties;
    private final CacheProperties cacheProperties;
    private final String applicationName;
    private final ObjectMapper objectMapper;

    public RedisCacheConfig(RedisProperties redisProperties, CacheProperties cacheProperties,
                            @Value("${spring.application.name}") String applicationName,
                            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.redisProperties = redisProperties;
        this.cacheProperties = cacheProperties;
        this.applicationName = applicationName;
        this.objectMapper = objectMapper;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        var clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(redisProperties.getTimeout())
                .clientName(applicationName)
                .build();

        if (redisProperties.getCluster() != null && redisProperties.getCluster().getNodes() != null) {
            RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(redisProperties.getCluster().getNodes());
            clusterConfiguration.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());
            clusterConfiguration.setPassword(redisProperties.getPassword());
            clusterConfiguration.setUsername(redisProperties.getUsername());

            return new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
        } else {
            RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
            standaloneConfiguration.setHostName(redisProperties.getHost());
            standaloneConfiguration.setPort(redisProperties.getPort());
            standaloneConfiguration.setUsername(redisProperties.getUsername());
            standaloneConfiguration.setPassword(redisProperties.getPassword());
            standaloneConfiguration.setDatabase(redisProperties.getDatabase());

            return new LettuceConnectionFactory(standaloneConfiguration, clientConfiguration);
        }
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Configure key and hash key serializers
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        return template;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,
                                               ObjectProvider<NullableRedisCacheManagerBuilderCustomizer>
                                                       redisCacheManagerBuilderCustomizers) {
        var keyPrefix = cacheProperties.getRedis().getKeyPrefix();
        keyPrefix = keyPrefix.endsWith(":") ? keyPrefix : (keyPrefix + ":");

        var defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(keyPrefix)
                .entryTtl(cacheProperties.getRedis().getTimeToLive())
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)));

        var redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);

        NullableRedisCacheManagerBuilder builder = NullableRedisCacheManager.builder(redisCacheWriter, objectMapper)
                .defaultCacheConfiguration(defaultCacheConfig);

        if (redisCacheManagerBuilderCustomizers != null) {
            redisCacheManagerBuilderCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        }

        return builder.build();
    }
}
