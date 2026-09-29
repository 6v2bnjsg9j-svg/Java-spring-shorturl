package com.wcy.shorturl.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "short-url")
public class CacheContent {
    private final Bloom bloom = new Bloom();
    private final Cache cache = new Cache();
    private final Snowflake snowflake = new Snowflake();
    @Data
    public static class Bloom{


    }
    @Data
    public static class Cache {
        /** L1 本地缓存最大条目 */
        private int maximumSize = 100_000;
        /** L1 本地缓存 TTL（秒） */
        private long ttlSeconds = 300;
    }



}
