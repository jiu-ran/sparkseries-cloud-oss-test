package com.sparkseries.common.util.tool;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import ua_parser.Parser;

/**
 * 设备信息解析器
 */
public class DeviceInfoParser {
    private static final Parser UA_PARSER = new Parser();
    // 添加盐值防止伪造
    private static final String SALT = "spark";

    /**
     * 解析设备信息
     */
    public static String parseDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return UA_PARSER.parse(userAgent).os.family;
    }

    /**
     * 生成设备指纹
     */
    public static String generateAdvancedFingerprint(HttpServletRequest request) {
        String deviceMemory = request.getHeader("deviceMemory");
        String hardwareConcurrency = request.getHeader("hardwareConcurrency");
        String screen = request.getHeader("screen");
        String timezone = request.getHeader("timezone");
        String acceptLanguage = request.getHeader("Accept-Language");
        String userAgent = request.getHeader("User-Agent");

        return DigestUtils.sha256Hex(userAgent + acceptLanguage + SALT);
    }
}
