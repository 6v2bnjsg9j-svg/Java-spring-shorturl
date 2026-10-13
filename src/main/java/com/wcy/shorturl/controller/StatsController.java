package com.wcy.shorturl.controller;


import com.wcy.shorturl.common.ApiResponse;
import com.wcy.shorturl.common.BizException;
import com.wcy.shorturl.common.ErrorCode;
import com.wcy.shorturl.domain.po.TUrlMapping;
import com.wcy.shorturl.domain.vo.StatsResponse;
import com.wcy.shorturl.service.impl.TUrlMappingServiceImpl;
import com.wcy.shorturl.util.NoWrap;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访问统计接口
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final TUrlMappingServiceImpl shortUrlService;

    /**
     * 查询短链累计访问统计
     *
     * <pre>
     * GET /api/stats/{code}
     * -> { "code": "aB3x9Z", "pv": 10234, "uv": 5678 }
     * </pre>
     *
     * <p>PV 有最多 1 分钟的 Redis 蓄水延迟（见 VisitStatsRecorder）。
     */
    @NoWrap
    @GetMapping("/{code}")
    public ApiResponse<StatsResponse> stats(@PathVariable("code") String code) {
        TUrlMapping mapping = shortUrlService.getByCode(code);
        if (mapping == null) {
            throw new BizException(ErrorCode.URL_NOT_FOUND);
        }
        return ApiResponse.ok(StatsResponse.builder()
                .code(code)
                .pv(mapping.getPv() == null ? 0 : mapping.getPv())
                .uv(mapping.getUv() == null ? 0 : mapping.getUv())
                .build());
    }
}
