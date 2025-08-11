package com.sparkseries.common.util;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.sparkseries.common.util.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文件名和路径校验工具类
 */
@Slf4j
public class FileUtils {

    /**
     * 定义非法字符正则表达式：\ / : * ? " < > |
     */
    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    /**
     * 定义连续点号正则表达式（至少两个连续点）
     */
    private static final Pattern CONSECUTIVE_DOTS_PATTERN = Pattern.compile("\\.{2,}");

    /**
     * 禁止的字符: < > : " \ | ? * （允许正斜杠 `/`，因OSS使用正斜杠）
     */
    private static final Pattern ILLEGAL_CHARS_PATTERN = Pattern.compile("[<>:\"|?*\\\\]");

    /**
     * 校验文件名合法性，包括长度、非法字符和连续点号
     *
     * @param filename 原始文件名（需校验的字符串）
     */
    public static void isValidFileName(String filename) {

        if (ObjectUtils.isEmpty(filename)) {
            log.error("文件名为空");
            throw new BusinessException("文件名为空");
        }

        if (filename.length() > 255) {
            log.error("文件名长度超过255");
            throw new BusinessException("文件名长度超过255");
        }

        if (ILLEGAL_CHAR_PATTERN.matcher(filename).find()) {
            log.error("文件名包含非法字符");
            throw new BusinessException("文件名包含非法字符");
        }

        if (CONSECUTIVE_DOTS_PATTERN.matcher(filename).find()) {
            log.error("文件名包含连续点号");
            throw new BusinessException("文件名包含连续点号");
        }

    }

    /**
     * 规范化文件名
     * 1. 去除首尾空格
     * 2. 去除末尾的所有点（无论数量）
     *
     * @param fileName 文件名
     * @return 规范化的文件名
     */
    public static String normalizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }

        // 去除首尾空格
        fileName = fileName.trim();

        // 去除末尾的所有点（例如 "file..." → "file"）
        fileName = fileName.replaceAll("\\.+$", "");

        return fileName;
    }


    /**
     * 校验路径是否符合规则，包括空路径、Windows盘符格式和非法字符
     *
     * @param path 路径
     */
    public static void isValidPath(String path) {
        if (ObjectUtils.isEmpty(path)) {
            log.error("路径为空");
            throw new BusinessException("路径为空");
        }

        if (path.matches("^[A-Za-z]:/.*")) {
            log.error("路径格式不正确");
            throw new BusinessException("路径格式不正确");
        }

        // 检查非法字符
        if (ILLEGAL_CHARS_PATTERN.matcher(path).find()) {
            log.error("路径包含非法字符");
            throw new BusinessException("路径包含非法字符");
        }
    }

    /**
     * 规范化文件路径，包括替换反斜杠、合并连续斜杠、去除首尾斜杠和处理目录末尾点
     *
     * @param path 原始路径
     * @return 规范化后的路径
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // 1. 替换反斜杠为正斜杠
        String normalized = path.replace('\\', '/');

        // 2. 合并连续斜杠为单个斜杠
        normalized = normalized.replaceAll("/+", "/");

        // 3. 去除首尾斜杠
        normalized = normalized.replaceAll("^/|/$", "");

        // 4. 分割路径，处理每个目录部分的末尾点
        String[] parts = normalized.split("/");
        String processed = Arrays.stream(parts)
                // 过滤空目录
                .filter(part -> !part.isEmpty())
                // 仅去除末尾的点
                .map(part -> part.replaceAll("\\.+$", ""))
                // 过滤处理后可能产生的空目录（如 "..." → ""）
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining("/"));

        return processed.isEmpty() ? "" : processed + "/";
    }

    /**
     * 文件大小转换
     *
     * @param size 文件大小
     * @return 格式化后的文件大小
     */
    public static String conversion(Long size) {

        if (size == null) {
            return null;
        }

        String fileSize = null;

        Long num = 1024L;

        if (size < num) {
            fileSize = String.format("%.2f", size * 1.0) + "B";
        } else if (size < num * num) {
            fileSize = String.format("%.2f", size / (num * 1.0)) + "KB";
        } else if (size < num * num * num) {
            fileSize = String.format("%.2f", size / (num * num * 1.0)) + "MB";
        } else if (size < num * num * num * num) {
            fileSize = String.format("%.2f", size / (num * num * num * 1.0)) + "GB";
        }

        return fileSize;
    }

    /**
     * 获取文件的扩展名
     *
     * @param fileName 文件名
     * @return 文件的扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 规范文件名并检验是否合法
     *
     * @param fileName 文件名
     * @return 规范化的文件名
     */
    public static String normalizeAndValidateFileName(String fileName) {
        String normalized = normalizeFileName(fileName);
        isValidFileName(normalized);
        return normalized;
    }

    /**
     * 规范路径并检验是否合法
     *
     * @param path 路径
     * @return 规范化的路径
     */
    public static String normalizeAndValidatePath(String path) {
        String normalized = normalizePath(path);
        isValidPath(normalized);
        return normalized;
    }
}