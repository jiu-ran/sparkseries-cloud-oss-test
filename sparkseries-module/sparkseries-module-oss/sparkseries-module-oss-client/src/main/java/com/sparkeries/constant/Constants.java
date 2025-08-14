package com.sparkeries.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 通用常量
 */
public class Constants {

    /**
     * 头像路径前缀
     */
    public final static String AVATAR_PATH_PREFIX = "avatar/";

    /**
     * 头像最大大小 10MB
     */
    public final static Integer AVATAR_MAX_SIZE = 10 * 1024 * 1024;

    /**
     * 支持的图片MIME类型
     */
    public final static List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    /**
     * 图片MIME类型前缀
     */
    public final static String IMAGE_MIME_PREFIX = "image/";

    /**
     * COS 大小文件临界点
     */
    public final static int COS_SIZE_THRESHOLD = 10 * 1024 * 1024;

    /**
     * KODO 大小文件临界点
     */
    public static final int KODO_SIZE_THRESHOLD = 4 * 1024 * 1024;

    /**
     * LOCAL 大小文件临界点
     */
    public static final int LOCAL_SIZE_THRESHOLD = 50 * 1024 * 1024;

    /**
     * MINIO 大小文件临界点
     */
    public static final int MINIO_SIZE_THRESHOLD = 100 * 1024 * 1024;

    /**
     * OSS 大小文件临界点
     */
    public static final int OSS_SIZE_THRESHOLD = 50 * 1024 * 1024;


}
