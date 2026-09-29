package com.wcy.shorturl.service.impl;

import com.wcy.shorturl.domain.po.TUrlMapping;
import com.wcy.shorturl.mapper.TUrlMappingMapper;
import com.wcy.shorturl.service.ITUrlMappingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 短链映射表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@Service
public class TUrlMappingServiceImpl extends ServiceImpl<TUrlMappingMapper, TUrlMapping> implements ITUrlMappingService {

}
