package com.wcy.shorturl.domain.vo;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Builder
public class ShortUrlResponse {

    /** 短码 */
    private String code;

    /** 完整短链 */
    private String shortUrl;

    /** 原始长链接 */
    private String longUrl;

}