package com.wcy.shorturl.config;

import com.wcy.shorturl.common.ApiResponse;
import com.wcy.shorturl.domain.vo.Result;
import com.wcy.shorturl.util.NoWrap;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestControllerAdvice
public class ShortResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }

        // 支持自定义 @NoWrap 注解排除
        if (returnType.hasMethodAnnotation(NoWrap.class)) {
            return false;
        }
        if (returnType.getContainingClass().isAnnotationPresent(NoWrap.class)) {
            return false;
        }

        Class<?> type = returnType.getParameterType();

        if (Result.class.isAssignableFrom(type)
                || ResponseEntity.class.isAssignableFrom(type)
                || Resource.class.isAssignableFrom(type)
                || StreamingResponseBody.class.isAssignableFrom(type)
                || byte[].class.isAssignableFrom(type)
                || String.class.isAssignableFrom(type)
                || void.class.equals(type)) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        // 1. 防止重复包装（再次确保安全）
        if (body instanceof Result<?>) {
            return body;
        }

        // 2. 真正的包装动作：把原本的 User 塞进 R.success() 的 data 里
        return Result.success(body);
    }
}
