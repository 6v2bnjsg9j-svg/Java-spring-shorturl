package com.wcy.shorturl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wcy.shorturl.codec.Base62;
import com.wcy.shorturl.domain.po.TUrlMapping;
import com.wcy.shorturl.mapper.TUrlMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitStatsRecorder {
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String PV_KEY_PREFIX = "su:stats:pv:";
    private static final String UV_KEY_PREFIX = "su:stats:uv:";
    private static final String SET_KEY_PREFIX = "su:set:uv:";


    private final StringRedisTemplate redisTemplate;
    private final TUrlMappingMapper urlMappingMapper;



    @Async("statsExecutor")
    public void record(String code, String clientIp) {
        try {
            String day = DAY_FMT.format(LocalDateTime.now());
            String pvKey = PV_KEY_PREFIX + day;
            String uvKey = UV_KEY_PREFIX + day + ":" + code;
            String setKey = SET_KEY_PREFIX + day ;
            redisTemplate.opsForHash().increment(pvKey, code, 1);
            redisTemplate.expire(pvKey, 2, TimeUnit.DAYS);
            redisTemplate.opsForHyperLogLog().add(uvKey, clientIp);
            redisTemplate.expire(uvKey, 2, TimeUnit.DAYS);
            redisTemplate.opsForSet().add(setKey, code);
            redisTemplate.expire(setKey, 2, TimeUnit.DAYS);
        }
        catch (Exception e) {
            log.error("Failed to record visit stats", e);
        }
    }
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void recordpv() {
        String day = DAY_FMT.format(LocalDateTime.now());
        String pvKey = PV_KEY_PREFIX + day;
        try{
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(pvKey);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String clientIp = null;
            try {
                Object shortCode = entry.getKey();
                clientIp = (String) shortCode;
                Object value = entry.getValue();
                long count = Long.parseLong(String.valueOf(entry.getValue()));
                long id = Base62.decode(clientIp);
                urlMappingMapper.updatepv(id, count);
                redisTemplate.opsForHash().increment(pvKey,clientIp, -count);
            } catch (IllegalArgumentException e) {
                redisTemplate.opsForHash().delete(pvKey, clientIp);
            }
        }
        } catch (Exception e) {
            log.warn("PV 刷库失败(下轮重试): {}", e.getMessage());
        }
    }


    @Scheduled(cron = "0 0 1 * * ?" )
    public void recorduv() {
        String yesterday = LocalDate.now().minusDays(1).format(DAY_FMT);
        String setkey=SET_KEY_PREFIX + yesterday;
        try{
            Set<String> members = redisTemplate.opsForSet().members(setkey);
            for (String code : members) {
                String uvkey=UV_KEY_PREFIX + yesterday + ":" + code;
                Long size = redisTemplate.opsForHyperLogLog().size(uvkey);
                if (size > 0) {
                    try{
                        long id=Base62.decode(code);
                        urlMappingMapper.updateuv(id,size);
                        redisTemplate.opsForHyperLogLog().delete(uvkey);
                    }catch (IllegalArgumentException e) {
                        log.warn("非法短码: {}  code: {}", e.getMessage(),code);
                    }
                }
            }
            redisTemplate.delete(setkey);
        }catch (Exception e){
            log.warn("UV 刷库失败: {}", e.getMessage());
        }
    }
    @Async("statsExecutor")
    public void recordByUserId(String code, String userId) {
       try {
           String day = DAY_FMT.format(LocalDateTime.now());
           String pvKey = PV_KEY_PREFIX + day;
           String uvKey = UV_KEY_PREFIX + day + ":" + code;
           String setKey = SET_KEY_PREFIX + day ;

           redisTemplate.opsForHash().increment(pvKey, code, 1);
           redisTemplate.expire(pvKey, 2, TimeUnit.DAYS);
           redisTemplate.opsForHyperLogLog().add(uvKey, userId);
           redisTemplate.expire(uvKey, 2, TimeUnit.DAYS);
           redisTemplate.opsForSet().add(setKey, code);
           redisTemplate.expire(setKey, 2, TimeUnit.DAYS);
       } catch (Exception e) {
           log.error("Failed to record visit stats", e);
       }

    }

}
