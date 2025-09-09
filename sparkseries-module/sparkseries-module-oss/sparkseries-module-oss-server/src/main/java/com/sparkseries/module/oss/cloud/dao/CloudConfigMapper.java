package com.sparkseries.module.oss.cloud.dao;


import com.sparkseries.module.oss.cloud.entity.CosConfigEntity;
import com.sparkseries.module.oss.cloud.entity.KodoConfigEntity;
import com.sparkseries.module.oss.cloud.entity.MinioConfigEntity;
import com.sparkseries.module.oss.cloud.entity.OssConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 云服务配置信息 Mapper 接口
 */
@Mapper
public interface CloudConfigMapper {
    /**
     * 添加 OSS 配置信息
     *
     * @param ossConfigEntity OSS 配置信息
     * @return 受影响行数
     */
    Integer insertOssConfig(@Param("ossConfigEntity") OssConfigEntity ossConfigEntity);

    /**
     * 添加 COS 配置信息
     *
     * @param cosConfigEntity COS 配置信息
     * @return 受影响行数
     */
    Integer insertCosConfig(@Param("cosConfigEntity") CosConfigEntity cosConfigEntity);

    /**
     * 添加 KODO 配置信息
     *
     * @param kodoConfigEntity KODO 配置信息
     * @return 受影响行数
     */
    Integer insertKodoConfig(@Param("kodoConfigEntity") KodoConfigEntity kodoConfigEntity);

    /**
     * 添加 Minio 配置信息
     *
     * @param minioConfigEntity Minio 配置信息
     * @return 受影响行数
     */
    Integer insertMinioConfig(@Param("minioConfigEntity") MinioConfigEntity minioConfigEntity);

    /**
     * 删除 OSS 配置信息
     *
     * @param id id
     * @return 受影响行数
     */
    Integer deleteOssConfigById(@Param("id") Long id);

    /**
     * 删除 COS 配置信息
     *
     * @param id id
     * @return 受影响行数
     */
    Integer deleteCosConfigById(@Param("id") Long id);

    /**
     * 删除 KODO 配置信息
     *
     * @param id id
     * @return 受影响行数
     */
    Integer deleteKodoConfigById(@Param("id") Long id);

    /**
     * 删除 Minio 配置信息
     *
     * @param id id
     * @return 受影响行数
     */
    Integer deleteMinioConfigById(@Param("id") Long id);

    /**
     * 根据 id 查询 OSS 配置信息
     *
     * @param id id
     * @return OSS 配置信息
     */
    OssConfigEntity getOssConfigById(@Param("id") Long id);

    /**
     * 根据 id 查询 COS 配置信息
     *
     * @param id id
     * @return COS 配置信息
     */
    CosConfigEntity getCosConfigById(@Param("id") Long id);

    /**
     * 根据 id 查询 KODO 配置信息
     *
     * @param id id
     * @return KODO 配置信息
     */
    KodoConfigEntity getKodoConfigById(@Param("id") Long id);

    /**
     * 根据 id 查询 Minio 配置信息
     *
     * @param id id
     * @return MinIO 配置信息
     */
    MinioConfigEntity getMinioConfigById(@Param("id") Long id);

    /**
     * 查询所有 OSS 配置信息
     *
     * @return OSS 配置信息
     */
    List<OssConfigEntity> listOssConfig();

    /**
     * 查询所有 COS 配置信息
     *
     * @return COS 配置信息
     */
    List<CosConfigEntity> listCosConfig();

    /**
     * 查询所有 KODO 配置信息
     *
     * @return KODO 配置信息
     */
    List<KodoConfigEntity> listKodoConfig();

    /**
     * 查询所有 Minio 配置信息
     *
     * @return Minio 配置信息
     */
    List<MinioConfigEntity> listMinioConfig();

}