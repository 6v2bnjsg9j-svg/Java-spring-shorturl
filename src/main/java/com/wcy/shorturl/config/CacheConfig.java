package com.wcy.shorturl.config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, String> localUrlCache(CacheContent properties) {
        CacheContent.Cache conf = properties.getCache();
        return Caffeine.newBuilder()
                .maximumSize(conf.getMaximumSize())
                .expireAfterWrite(Duration.ofSeconds(conf.getTtlSeconds()))
                .recordStats()
                .build();
    }
}
