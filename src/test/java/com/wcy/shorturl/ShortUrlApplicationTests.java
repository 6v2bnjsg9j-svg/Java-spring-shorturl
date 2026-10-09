package com.wcy.shorturl;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

@SpringBootTest
class ShortUrlApplicationTests {

    @Test
    void contextLoads() {
        BloomFilter<String> filter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                1_000_000L,
                0.01
        );
        // 添加
        filter.put("user:1001");
        filter.put("user:1002");
        filter.put("user:1003");

        // 查询
        System.out.println(filter.mightContain("user:1001")); // true
        System.out.println(filter.mightContain("user:1002")); // true
        System.out.println(filter.mightContain("user:9999")); // 大概率 false
    }

}
