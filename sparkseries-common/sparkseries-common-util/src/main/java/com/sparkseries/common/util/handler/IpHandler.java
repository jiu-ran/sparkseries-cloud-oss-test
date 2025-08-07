package com.sparkseries.common.util.handler;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

/**
 * IP 处理器
 */
public class IpHandler {
    public static String getClientIp(HttpServletRequest request) {
        String[] headersToCheck = {
                // 通用代理头 (可能包含多个 IP, 如:客户端 IP, 代理 1IP, 代理 2IP)
                "X-Forwarded-For",
                // Apache 代理
                "Proxy-Client-IP",
                // WebLogic 代理
                "WL-Proxy-Client-IP",
                // 部分代理场景
                "HTTP_CLIENT_IP",
                // 部分代理场景
                "HTTP_X_FORWARDED_FOR"
        };

        String ip = null;
        for (String header : headersToCheck) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }

        // 处理多级代理的 X-Forwarded-For (取第一个有效 IP)
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = Arrays.stream(ip.split(","))
                    .map(String::trim)
                    .filter(IpHandler::isValidIp)
                    .findFirst()
                    .orElseGet(() -> {
                        String fallbackIp = request.getRemoteAddr();
                        return isValidIp(fallbackIp) ? fallbackIp : "0.0.0.0";
                    });
        }

        // 确保最终结果不为 null
        if (ip == null) {
            // 默认值
            ip = "0.0.0.0";
        }

        // 本地测试时可能返回 IPv6 地址 "0:0:0:0:0:0:0:1", 需转为 "127.0.0.1"
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    private static boolean isValidIp(String ip) {
        return !StrUtil.isEmpty(ip) && !"unknown".equalsIgnoreCase(ip);
    }
}
