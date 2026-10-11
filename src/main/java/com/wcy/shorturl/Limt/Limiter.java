package com.wcy.shorturl.Limt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME )
public @interface Limiter {

    /** 窗口内允许的最大请求数 */
    int limit() default 10;

    /** 窗口长度（秒） */
    int windowSeconds() default 1;

    /** 限流 key 的业务前缀，默认取方法签名 */
    String key() default "";
}
