package com.wcy.shorturl.controller;


import com.wcy.shorturl.config.ContentClint;
import com.wcy.shorturl.service.ITUrlMappingService;
import com.wcy.shorturl.service.VisitStatsRecorder;
import com.wcy.shorturl.util.CurrentHolder;
import com.wcy.shorturl.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    private VisitStatsRecorder statsRecorder;
    private ContentClint contentClint;
    @GetMapping("/code")
    public void getCode(@PathVariable("code") String code , HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        String longUrl=itUrlMappingService.getlongUrl(code);

        if (longUrl == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return ;
        }
        // 旁路统计：异步执行，不阻塞跳转响应
        statsRecorder.record(code, IpUtil.getClientIp(request));
        //或者用用户id
        String userId = CurrentHolder.getCurrentId().toString();
        if (userId != null) {
            statsRecorder.recordByUserId(code, userId);
        }
        response.setStatus(contentClint.getRedirectCode());

        response.setHeader("Location", longUrl);
    }
}
