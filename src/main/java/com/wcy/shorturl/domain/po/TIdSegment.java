package com.wcy.shorturl.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 号段发号器表
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_id_segment")
public class TIdSegment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务标识
     */
    @TableId(value = "biz_tag", type = IdType.AUTO)
    private String bizTag;

    /**
     * 当前已分配的最大ID
     */
    private Long maxId;

    /**
     * 每次加载的号段长度
     */
    private Integer step;

    private String description;

    private LocalDateTime updateTime;


}
