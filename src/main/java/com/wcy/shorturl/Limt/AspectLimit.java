package com.wcy.shorturl.Limt;

import com.wcy.shorturl.common.BizException;
import com.wcy.shorturl.common.ErrorCode;
import com.wcy.shorturl.util.IpUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import com.wcy.shorturl.Limt.Limiter;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@RequiredArgsConstructor
public class AspectLimit {
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;

    @Around("@annotation(limiter)")
    public Object around(ProceedingJoinPoint joinPoint, Limiter limiter) throws Throwable
    {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            // 非 Web 上下文（定时任务/测试直调）不限流
            return joinPoint.proceed();
        }

        String ip = IpUtil.getClientIp(attrs.getRequest());
        String bizKey = limiter.key().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : limiter.key();
        String redisKey = "rl:ip:" + ip + ":" + bizKey;

        if (!slidingWindowRateLimiter.IsContinue(redisKey, limiter.limit(), limiter.windowSeconds())) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return joinPoint.proceed();
    }
}
