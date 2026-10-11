package com.wcy.shorturl.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.hash.Hashing;
import com.wcy.shorturl.codec.Base62;
import com.wcy.shorturl.config.ContentClint;
import com.wcy.shorturl.domain.dto.CreateShortUrlRequest;
import com.wcy.shorturl.domain.po.TUrlMapping;
import com.wcy.shorturl.domain.pro.Url_lock;
import com.wcy.shorturl.id.SegmentAllocator;
import com.wcy.shorturl.mapper.TUrlMappingMapper;
import com.wcy.shorturl.service.ITUrlMappingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * <p>
 * 短链映射表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TUrlMappingServiceImpl extends ServiceImpl<TUrlMappingMapper, TUrlMapping> implements ITUrlMappingService {

    private static final Pattern URL_PATTERN =
            Pattern.compile("^https?://[\\w.-]+(:\\d+)?(/.*)?$");
    private static final String LOCK_KEY_PREFIX = "lock:url:";
    /** Redis 缓存 key 前缀 */
    private static final String URL_KEY_PREFIX = "su:url:";

    /** 长链 -> 短码 的映射缓存 TTL（与过期时间取 min） */
    private static final long DEFAULT_CACHE_TTL_SECONDS = 3600;

    private static final long DAY_MS = 24 * 3600 * 1000L;

    private final TUrlMappingMapper tUrlMappingMapper;
    private final ContentClint contentClint ;
    private final SegmentAllocator allocation ;
    private final BloomService bloomService;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedissonClient redissonClient;



    @Override
    public String getShort_Url(CreateShortUrlRequest request) {
        String LongUrl = request.getUrl();
        LongUrl=panUrl(LongUrl);
        // 对url进行哈希计算，并将结果存储到hash变量中
        Long hash = unsignedHash(LongUrl);
        //=====开始加锁
        TUrlMapping tUrlMapping = tUrlMappingMapper.selectOne(new LambdaQueryWrapper<TUrlMapping>()
                .eq(TUrlMapping::getLongUrl, LongUrl)
                .eq(TUrlMapping::getUrlHash, hash)
                .eq(TUrlMapping::getStatus, 1).last("LIMIT 1"));
        if(tUrlMapping!=null &&!isExpired(tUrlMapping)){
            redisTemplate.opsForValue().set(URL_KEY_PREFIX + tUrlMapping.getShortCode(), LongUrl, cacheTtlSeconds(tUrlMapping), TimeUnit.SECONDS);
            return contentClint.getDomain()+tUrlMapping.getShortCode();
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + hash.toString());
        boolean isLocked = false;
        try{
            isLocked = lock.tryLock(30, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("获取分布式锁失败, hash: " + hash);
            }
            //==================
            Url_lock iscontinue=((ITUrlMappingService) AopContext.currentProxy()).SelectAndGet(LongUrl,hash,request);
            if(iscontinue.getIscontinue()==false)
            {
                TUrlMapping SelectedtUrlMapping = iscontinue.getTUrlMapping();
                redisTemplate.opsForValue().set(
                        URL_KEY_PREFIX + iscontinue.getCode(),
                        SelectedtUrlMapping.getLongUrl(),
                        cacheTtlSeconds(SelectedtUrlMapping),
                        TimeUnit.SECONDS
                );
                return contentClint.getDomain()+iscontinue.getCode();
            }
            else{
                String code = iscontinue.getCode();
                TUrlMapping mapping=iscontinue.getTUrlMapping();
                bloomService.add(code); // 向布隆过滤器中添加短链接码

                redisTemplate.opsForValue().set(URL_KEY_PREFIX + code, LongUrl, cacheTtlSeconds(mapping), TimeUnit.SECONDS); // 将长链接保存到Redis中
                log.info("创建短链接成功，短链接码为：" + code);
                return contentClint.getDomain()+code;
            }
            //==============
        }catch (Exception e){
            log.error("生成短链接失败, url: {}", LongUrl, e);
            // 抛出真实的异常信息，不要一律伪装成 "Lock acquisition failed"
            throw new RuntimeException("生成短链接失败: " + e.getMessage(), e);
        }
        finally {
            if (isLocked &&lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private long  cacheTtlSeconds(TUrlMapping mapping) {
        Long expireTime = mapping.getExpireTime();
        if (expireTime == null) {
            return DEFAULT_CACHE_TTL_SECONDS;
        }
        long remainSeconds = (expireTime - System.currentTimeMillis()) / 1000;
        return Math.min(Math.max(remainSeconds, 1), DEFAULT_CACHE_TTL_SECONDS);
    }

    private boolean isExpired(TUrlMapping tUrlMapping)
    {
        Long expireTime = tUrlMapping.getExpireTime();
        return expireTime != null && expireTime <= System.currentTimeMillis();
    }

    private Long unsignedHash(String longUrl) {
        return Hashing.murmur3_32_fixed()
                .hashString(longUrl, StandardCharsets.UTF_8)
                .asInt() & 0xFFFFFFFFL;
    }
    private String panUrl(String LongUrl)
    {
        // 检查url是否为空，如果为空则抛出异常
        if(LongUrl==null)
            throw new NullPointerException("LongUrl is null");
        // 判断是否两边有空格，并删除空格
        LongUrl=LongUrl.trim();
        // 检查是否符合url格式，如果不符合则抛出异常
        if(!URL_PATTERN.matcher(LongUrl).matches())
            throw new RuntimeException();
        // 检查url长度是否超过2048，如果超过则抛出异常
        if(LongUrl.length()>2048)
            throw new RuntimeException();
        return LongUrl;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Url_lock SelectAndGet(String LongUrl, Long hash, CreateShortUrlRequest request)
    {
        String code="";
        TUrlMapping tUrlMapping = tUrlMappingMapper.selectOne(new LambdaQueryWrapper<TUrlMapping>()
                .eq(TUrlMapping::getLongUrl, LongUrl)
                .eq(TUrlMapping::getUrlHash, hash)
                .eq(TUrlMapping::getStatus, 1).last("LIMIT 1"));
        Url_lock urlLock=new Url_lock();
        if(tUrlMapping!=null&& !isExpired(tUrlMapping)){
            urlLock.setCode(tUrlMapping.getShortCode());
            urlLock.setIscontinue(false);
            urlLock.setTUrlMapping(tUrlMapping);
            return  urlLock;
        }
        else {
            Long id;
            try {
                id = allocation.nextId("shortUrl"); // 获取下一个id
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to allocate ID", e);
            }
            code = Base62.encode(id); // id转换为Base62编码
            // 如果数据库中不存在相同的url和hash值的记录，则从分配器中获取下一个id，并将其映射到数据库中
            // 从分配器中获取下一个id，并将其映射到数据库中
            TUrlMapping mapping = new TUrlMapping(); // 创建一个新的TUrlMapping对象
            mapping.setId(id); // 设置id
            mapping.setShortCode(code); // 设置短链接码
            mapping.setLongUrl(LongUrl); // 设置长链接
            mapping.setUrlHash(hash); // 设置哈希值
            mapping.setStatus(1); // 设置状态为1，表示有效
            mapping.setPv(0L); // 初始化PV为0
            mapping.setUv(0L); // 初始化UV为0
            mapping.setExpireTime(request.getExpireDays() != null && request.getExpireDays() > 0
                    ? System.currentTimeMillis() + request.getExpireDays() * DAY_MS
                    : null); // 设置过期时间
            save(mapping); // 保存到数据库中

            urlLock.setTUrlMapping(mapping);
            urlLock.setIscontinue(true);
            urlLock.setCode(code);
            return urlLock; // 返回短链接地址
        }
    }
}
