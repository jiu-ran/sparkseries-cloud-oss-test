package com.sparkeries.enums;

import com.sparkseries.common.util.exception.BusinessException;
import lombok.Getter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 存储类型枚举
 */
@Getter
public enum StorageTypeEnum {

    /**
     * 阿里云
     */
    OSS("oss", 1),

    /**
     * 腾讯云
     */
    COS("cos", 2),

    /**
     * 七牛云
     */
    KODO("kodo", 3),

    /**
     * MinIO
     */
    MINIO("minio", 4),

    /**
     * 本地储存
     */
    LOCAL("local", 5);


    private static final Map<Integer, StorageTypeEnum> VALUE_MAP =
            Stream.of(values()).collect(Collectors.toMap(StorageTypeEnum::getValue, Function.identity()));
    private final Integer value;
    private final String key;


    StorageTypeEnum(String key, int value) {
        this.key = key;
        this.value = value;
    }


    /**
     * 根据整数类型获取枚举常量
     *
     * @param type 整数类型值
     * @return 对应的枚举常量
     */
    public static StorageTypeEnum getStorageEnum(int type) {
        StorageTypeEnum storageType = VALUE_MAP.get(type);
        if (storageType == null) {
            throw new BusinessException("未发现该存储类型: " + type);
        }
        return storageType;
    }
}