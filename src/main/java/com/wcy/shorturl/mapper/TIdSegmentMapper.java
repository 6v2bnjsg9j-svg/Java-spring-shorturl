package com.wcy.shorturl.mapper;

import com.wcy.shorturl.domain.po.TIdSegment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 号段发号器表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@Mapper
public interface TIdSegmentMapper extends BaseMapper<TIdSegment> {
    @Update("update t_id_segment set max_id=max_id+step where biz_tag=#{biztag}")
    int updateMaxid(String bizTag);
}
