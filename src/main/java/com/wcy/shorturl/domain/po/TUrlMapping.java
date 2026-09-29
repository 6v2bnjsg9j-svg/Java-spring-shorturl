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
 * 短链映射表
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_url_mapping")
public class TUrlMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 发号器全局唯一ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Base62 短码
     */
    private String shortCode;

    /**
     * 原始长链接
     */
    private String longUrl;

    /**
     * 长链接 MurmurHash（无符号）
     */
    private Long urlHash;

    /**
     * 1=正常 0=禁用
     */
    private Integer status;

    /**
     * 过期时间戳(毫秒)，NULL=永久
     */
    private Long expireTime;

    /**
     * 累计访问量（Redis 异步批量刷回）
     */
    private Long pv;

    /**
     * 累计独立访客（HyperLogLog）
     */
    private Long uv;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
