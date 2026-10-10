package com.wcy.shorturl.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class BloomConfig {

    @Bean
    public BloomFilter<String> bloomFilter(ContentClint cacheContent) {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                cacheContent.getBloom().getExpectedInsertions(),
                cacheContent.getBloom().getFpp()
        );
    }

}
