package com.wcy.shorturl.service;

import com.wcy.shorturl.domain.dto.CreateShortUrlRequest;
import com.wcy.shorturl.domain.po.TUrlMapping;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wcy.shorturl.domain.pro.Url_lock;

/**
 * <p>
 * 短链映射表 服务类
 * </p>
 *
 * @author author
 * @since 2026-09-28
 */
public interface ITUrlMappingService extends IService<TUrlMapping> {

    String getShort_Url( CreateShortUrlRequest request);

    Url_lock SelectAndGet(String LongUrl, Long hash, CreateShortUrlRequest request);

    String getlongUrl(String code);
}
