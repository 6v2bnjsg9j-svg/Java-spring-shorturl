package com.wcy.shorturl.Limt;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SlidingWindowRateLimiter {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> redisScript;

    public Boolean IsContinue(String key, int limit, int windowSeconds)
    {
        Long result=redisTemplate.execute(redisScript,
                Collections.singletonList(key),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(windowSeconds * 1000L),
                String.valueOf(limit),
                UUID.randomUUID().toString()
                );
        return result != null && result == 1L;
    }
}
