package com.wcy.shorturl.service.impl;

import com.wcy.shorturl.domain.po.TIdSegment;
import com.wcy.shorturl.mapper.TIdSegmentMapper;
import com.wcy.shorturl.service.ITIdSegmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 号段发号器表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@Service
public class TIdSegmentServiceImpl extends ServiceImpl<TIdSegmentMapper, TIdSegment> implements ITIdSegmentService {

}
