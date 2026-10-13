package com.wcy.shorturl.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "short-url")
public class ContentClint {
    /** 发号策略：segment（号段+双Buffer）| snowflake（雪花ID） */
    private String idStrategy = "segment";

    /** 短链域名，如 http://s.cn/ */
    private String domain;

    /** 跳转状态码：302 或 301 */
    private int redirectCode = 302;

    private final Bloom bloom = new Bloom();
    private final Cache cache = new Cache();
//    private final Snowflake snowflake = new Snowflake();
    @Data
    public static class Cache {
        /** L1 本地缓存最大条目 */
        private int maximumSize = 100_000;
        /** L1 本地缓存 TTL（秒） */
        private long ttlSeconds = 300;
    }
    @Data
    public static class Bloom {
        /** 预期插入量 n */
        private long expectedInsertions = 10_000_000;
        /** 期望误判率 p */
        private double fpp = 0.01;
    }
}
