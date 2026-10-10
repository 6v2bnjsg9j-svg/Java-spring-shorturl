package com.wcy.shorturl.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateShortUrlRequest {
    @NotBlank(message = "长链接不能为空")
    @Size(max = 2048, message = "长链接长度不能超过 2048")
    private String url;

    /** 过期天数；null 或 <=0 表示永久有效 */
    private Integer expireDays;
}
