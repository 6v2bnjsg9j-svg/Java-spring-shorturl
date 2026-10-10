package com.wcy.shorturl;

import com.github.benmanes.caffeine.cache.Cache;
import com.wcy.shorturl.config.ContentClint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(ContentClint.class)
public class ShortUrlApplication {


    public static void main(String[] args) {
        SpringApplication.run(ShortUrlApplication.class, args);
    }

}
