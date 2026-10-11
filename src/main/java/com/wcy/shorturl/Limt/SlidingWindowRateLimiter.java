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
public class SlidingWindowRateLimiter {
    private static final StringRedisTemplate redisTemplate;
    private static final  DefaultRedisScript<Long> redisScript;
    static {
        redisTemplate=new StringRedisTemplate();
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setLocation(new ClassPathResource("lua/limiter.lua"));
        s.setResultType(Long.class);
        redisScript=s;
    }

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
