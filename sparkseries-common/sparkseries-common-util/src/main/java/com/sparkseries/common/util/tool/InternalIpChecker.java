package com.sparkseries.common.util.tool;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 内网 IP 检查
 */
public class InternalIpChecker {

    /**
     * 判断 IP 是否为内网地址
     */
    public static boolean isInternalIp(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}