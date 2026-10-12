package com.wcy.shorturl.controller;


import com.wcy.shorturl.service.ITUrlMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 号段发号器表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
@RestController
@RequestMapping
public class TIdSegmentController {
    @Autowired
    private ITUrlMappingService itUrlMappingService;
    @GetMapping("/code")
    public String getCode(@PathVariable("code") String code){
        String longUrl=itUrlMappingService.getlongUrl(code);
        if(longUrl==null){
            throw new RuntimeException("短链不存在");
        }

        return longUrl;
    }
}
