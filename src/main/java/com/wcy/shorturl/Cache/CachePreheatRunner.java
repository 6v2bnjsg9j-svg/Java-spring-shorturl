package com.wcy.shorturl.Cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.wcy.shorturl.domain.po.TUrlMapping;
import com.wcy.shorturl.mapper.TUrlMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CachePreheatRunner implements ApplicationRunner {
    private final Cache<String,String> cache;
    private final TUrlMappingMapper tUrlMappingMapper;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<TUrlMapping> objects = tUrlMappingMapper.selectList(new LambdaQueryWrapper<TUrlMapping>().eq(
                TUrlMapping::getStatus, 1
        ));
        if(objects.isEmpty())
        {
            log.info("没有数据");
        }
        Map<String,String>  map=objects.stream()
                .filter(o->o.getLongUrl()!=null&&o.getShortCode()!=null)
                .collect(Collectors.toMap(TUrlMapping::getLongUrl,TUrlMapping::getShortCode));
        cache.putAll(map);
        log.info("缓存预热完成");
    }
}
