package com.wcy.shorturl.controller;


import com.wcy.shorturl.Limt.Limiter;
import com.wcy.shorturl.domain.dto.CreateShortUrlRequest;
import com.wcy.shorturl.domain.vo.ShortUrlResponse;
import com.wcy.shorturl.service.ITUrlMappingService;
import com.wcy.shorturl.util.NoWrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * <p>
 * 短链映射表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@RestController
@RequestMapping("/api")
public class TUrlMappingController {
    @Autowired
    private ITUrlMappingService itUrlMappingService;

    @PostMapping("/shorten")
    @Limiter(limit = 10,windowSeconds = 1,key = "")
    @NoWrap
    public ShortUrlResponse get_short(@Valid @RequestBody CreateShortUrlRequest shortUrlRequest){
        String shorturl=itUrlMappingService.getShort_Url(shortUrlRequest);
        return ShortUrlResponse.builder()
                .code(shorturl.substring(shorturl.lastIndexOf('/',+1)))
                .shortUrl(shorturl)
                .longUrl(shortUrlRequest.getUrl())
                .build();
    }
}
