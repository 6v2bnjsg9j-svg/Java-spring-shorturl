package com.wcy.shorturl.util;

import javax.servlet.http.HttpServletRequest;

/**
 * 客户端真实 IP 提取
 *
 * <p>优先级：X-Forwarded-For 首个 IP（多层代理逐跳追加，第一个是原始客户端）
 * -> X-Real-IP -> remoteAddr。
 *
 * <p>注意：XFF 头可被伪造，这里取"第一个"已是最常见折中；
 * 严格防伪造需要在可信的反向代理层覆写该头（Nginx: proxy_set_header）。
 */
public final class IpUtil {

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
