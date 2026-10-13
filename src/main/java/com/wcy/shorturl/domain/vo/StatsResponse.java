package com.wcy.shorturl.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 访问统计响应
 */
@Data
@Builder
public class StatsResponse {

    private String code;

    private long pv;

    private long uv;
}
