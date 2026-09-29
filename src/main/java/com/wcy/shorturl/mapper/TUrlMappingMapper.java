package com.wcy.shorturl.mapper;

import com.wcy.shorturl.domain.po.TUrlMapping;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 短链映射表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@Mapper
public interface TUrlMappingMapper extends BaseMapper<TUrlMapping> {

}
