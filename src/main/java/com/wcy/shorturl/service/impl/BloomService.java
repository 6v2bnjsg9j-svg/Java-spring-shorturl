package com.wcy.shorturl.service.impl;

import com.google.common.hash.BloomFilter;
import org.springframework.stereotype.Service;

@Service
public class BloomService {
    private final BloomFilter<String> bloomFilter;

    public BloomService(BloomFilter<String> bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    // Guava BloomFilter 不是线程安全的，并发写要加锁
    public synchronized void add(String value) {
        bloomFilter.put(value);
    }

    public boolean mightContain(String value) {
        return bloomFilter.mightContain(value);
    }
}
