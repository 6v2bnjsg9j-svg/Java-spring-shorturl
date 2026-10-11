package com.wcy.shorturl.Limt;

import com.wcy.shorturl.common.BizException;
import com.wcy.shorturl.common.ErrorCode;
import com.wcy.shorturl.util.CurrentHolder;
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
    private static final String USER_KEY = "rl:user:";

    @Around("@annotation(limiter)")
    public Object around(ProceedingJoinPoint joinPoint, Limiter limiter) throws Throwable
    {
        //TODO 可以用我们用户的id作为key
//        Long id= CurrentHolder.getCurrentId();
//        if(id==null)
//            throw new RuntimeException("未登录");
//        String Bizkey  = limiter.key().isEmpty()
//            ? USER_KEY + id.toString()
//            : limiter.key();
//        if (!slidingWindowRateLimiter.IsContinue(Bizkey, limiter.limit(), limiter.windowSeconds())) {
//            throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
//        }
//        return joinPoint.proceed();

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
