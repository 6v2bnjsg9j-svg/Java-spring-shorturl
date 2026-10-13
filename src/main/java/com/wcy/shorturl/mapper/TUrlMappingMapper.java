package com.wcy.shorturl.mapper;

import com.wcy.shorturl.domain.po.TUrlMapping;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

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
    @Update("update t_url_mapping set pv = pv + #{count} where id = #{id}")
    void updatepv(long id, long count);
    @Update("update t_url_mapping set uv = uv + #{size} where id = #{id}")
    void updateuv(long id, Long size);
}
