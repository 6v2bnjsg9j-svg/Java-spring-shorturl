package com.wcy.shorturl.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
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

    /** URL正则表达式模式 */
    private static final Pattern URL_PATTERN =
            Pattern.compile("^https?://[\\w.-]+(:\\d+)?(/.*)?$");
    /** 分布式锁key前缀 */
    private static final String LOCK_KEY_PREFIX = "lock:url:";
    /** 长链重建分布式锁key前缀 */
    private static final String LongUrl_Lock_KEY_PREFIX = "ReStart:lock:url:";
    /** Redis 缓存 key 前缀 */
    private static final String URL_KEY_PREFIX = "su:url:";
    private static final String NULL_MARKER = "__NULL__";

    /** 默认缓存过期时间（秒） */
    private static final long DEFAULT_CACHE_TTL_SECONDS = 3600;

    /** 一天的毫秒数 */
    private static final long DAY_MS = 24 * 3600 * 1000L;

    private final TUrlMappingMapper tUrlMappingMapper;
    private final ContentClint contentClint ;
    private final SegmentAllocator allocation ;
    private final BloomService bloomService;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final Cache<String,String> cache;



    /**
     * 创建短链接
     * @param request 创建短链接请求
     * @return 短链接地址
     */
    @Override
    public String getShort_Url(CreateShortUrlRequest request) {
        String LongUrl = request.getUrl();
        LongUrl=panUrl(LongUrl);
        // 对url进行哈希计算，并将结果存储到hash变量中
        Long hash = unsignedHash(LongUrl);
        //=====开始加锁
        // 先查询数据库，判断是否已存在该长链接的短码
        TUrlMapping tUrlMapping = tUrlMappingMapper.selectOne(new LambdaQueryWrapper<TUrlMapping>()
                .eq(TUrlMapping::getLongUrl, LongUrl)
                .eq(TUrlMapping::getUrlHash, hash)
                .eq(TUrlMapping::getStatus, 1).last("LIMIT 1"));
        // 如果存在且未过期，直接返回已有的短码
        if(tUrlMapping!=null &&!isExpired(tUrlMapping)){
            redisTemplate.opsForValue().set(URL_KEY_PREFIX + tUrlMapping.getShortCode(), LongUrl, cacheTtlSeconds(tUrlMapping), TimeUnit.SECONDS);
            return contentClint.getDomain()+tUrlMapping.getShortCode();
        }
        // 获取分布式锁，防止并发创建重复短码
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + hash.toString());
        boolean isLocked = false;
        try{
            isLocked = lock.tryLock(30, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("获取分布式锁失败, hash: " + hash);
            }
            //==================
            // 通过AOP代理调用SelectAndGet方法，确保事务生效
            Url_lock iscontinue=((ITUrlMappingService) AopContext.currentProxy()).SelectAndGet(LongUrl,hash,request);
            // 如果不需要继续创建（已存在有效短码），直接返回
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
            // 需要创建新的短码
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

    /**
     * 计算缓存过期时间（秒）
     * @param mapping URL映射对象
     * @return 缓存过期时间（秒）
     */
    private long  cacheTtlSeconds(TUrlMapping mapping) {
        Long expireTime = mapping.getExpireTime();
        if (expireTime == null) {
            return DEFAULT_CACHE_TTL_SECONDS;
        }
        long remainSeconds = (expireTime - System.currentTimeMillis()) / 1000;
        return Math.min(Math.max(remainSeconds, 1), DEFAULT_CACHE_TTL_SECONDS);
    }

    /**
     * 判断URL映射是否已过期
     * @param tUrlMapping URL映射对象
     * @return true-已过期，false-未过期
     */
    private boolean isExpired(TUrlMapping tUrlMapping)
    {
        Long expireTime = tUrlMapping.getExpireTime();
        return expireTime != null && expireTime <= System.currentTimeMillis();
    }

    /**
     * 计算URL的无符号哈希值（使用MurmurHash3算法）
     * @param longUrl 长链接
     * @return 无符号哈希值
     */
    private Long unsignedHash(String longUrl) {
        return Hashing.murmur3_32_fixed()
                .hashString(longUrl, StandardCharsets.UTF_8)
                .asInt() & 0xFFFFFFFFL;
    }
    /**
     * 校验并规范化URL
     * @param LongUrl 原始长链接
     * @return 规范化后的长链接
     */
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
    /**
     * 查询或创建短链接（带事务）
     * @param LongUrl 长链接
     * @param hash 长链接哈希值
     * @param request 创建短链接请求
     * @return URL锁对象，包含是否需要继续创建的标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Url_lock SelectAndGet(String LongUrl, Long hash, CreateShortUrlRequest request)
    {
        String code="";
        // 查询数据库中是否已存在该长链接的短码
        TUrlMapping tUrlMapping = tUrlMappingMapper.selectOne(new LambdaQueryWrapper<TUrlMapping>()
                .eq(TUrlMapping::getLongUrl, LongUrl)
                .eq(TUrlMapping::getUrlHash, hash)
                .eq(TUrlMapping::getStatus, 1).last("LIMIT 1"));
        Url_lock urlLock=new Url_lock();
        // 如果存在且未过期，直接返回已有的短码
        if(tUrlMapping!=null&& !isExpired(tUrlMapping)){
            urlLock.setCode(tUrlMapping.getShortCode());
            urlLock.setIscontinue(false);
            urlLock.setTUrlMapping(tUrlMapping);
            return  urlLock;
        }
        // 不存在或已过期，需要创建新的短码
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

    /**
     * 根据短码获取长链接
     * @param code 短链接码
     * @return 长链接，不存在返回null
     */
    @Override
    public String getlongUrl(String code) {
        long id;
        try {
            id = Base62.decode(code); // 将短码解码为ID
        } catch (IllegalArgumentException e) {
            return null;
        }
        // 先从本地缓存查询
        String local = cache.getIfPresent(code);
        if(local!=null)
            return NULL_MARKER.equals(local) ? null : local;
        // 从Redis缓存查询
        Object o = redisTemplate.opsForValue().get(URL_KEY_PREFIX + code);
        if(o!=null)
        {
            String longUrl=o.toString();
            String s = NULL_MARKER.equals(longUrl) ? null : longUrl;
            if(s==null)
            {
                cache.put(code,NULL_MARKER);
                return null;
            }
            cache.put(code,longUrl);
            return longUrl;
        }

        // 布隆过滤器快速判断是否存在
        boolean b1 = bloomService.mightContain(code);
        if(!b1)
            return null;
        //开始重建缓存
        RLock lock = redissonClient.getLock(LongUrl_Lock_KEY_PREFIX + code);
        Boolean locked=false;
        try {
            locked = lock.tryLock(10, TimeUnit.SECONDS);
            // 获取锁失败，等待后重试Redis
            if(!locked)
            {
                Thread.sleep(40);
                Object retry = redisTemplate.opsForValue().get(URL_KEY_PREFIX + code);
                if(retry!=null)
                {
                    return NULL_MARKER.equals(retry.toString()) ? null : retry.toString();
                }
                return null;
            }
            // 获取锁成功，检查是否已被其他线程重建
            if(locked)
            {
                //检查上一个是否已经重建完毕
                Object o1 = redisTemplate.opsForValue().get(URL_KEY_PREFIX + code);
                if(o1!=null)
                {
                    return NULL_MARKER.equals(o1.toString()) ? null : o1.toString();
                }
                //先查数据库
                TUrlMapping one = getOne(new LambdaQueryWrapper<TUrlMapping>()
                        .eq(TUrlMapping::getId, id)              // ← 主键查
                        .eq(TUrlMapping::getStatus, 1)
                        .last("LIMIT 1"));
                // 数据库不存在，缓存空值防止穿透
                if(one==null)
                {
                    redisTemplate.opsForValue().set(URL_KEY_PREFIX + code, NULL_MARKER, 60, TimeUnit.SECONDS);//未命中只做一分钟的空值
                    cache.put(code,NULL_MARKER);
                    return null;
                }
                // 数据库存在，重建缓存
                else{
                    redisTemplate.opsForValue().set(URL_KEY_PREFIX + code, one.getLongUrl(), cacheTtlSeconds(one), TimeUnit.SECONDS);
                    cache.put(code,one.getLongUrl());
                    return one.getLongUrl();
                }
            }
            
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询被中断", e);
        } catch (Exception e) {
            log.error("查询长链失败, code={}", code, e);
            throw new RuntimeException("查询失败", e);
        }
        finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return null;
    }

}
