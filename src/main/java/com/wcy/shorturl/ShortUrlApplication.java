package com.wcy.shorturl;

import com.github.benmanes.caffeine.cache.Cache;
import com.wcy.shorturl.config.ContentClint;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@MapperScan("com.wcy.shorturl.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class ShortUrlApplication {


    public static void main(String[] args) {
        SpringApplication.run(ShortUrlApplication.class, args);
    }

}
