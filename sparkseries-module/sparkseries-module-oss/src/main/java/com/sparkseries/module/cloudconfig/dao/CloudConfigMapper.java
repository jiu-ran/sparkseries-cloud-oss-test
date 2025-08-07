package com.sparkseries.module.cloudconfig.dao;


import com.sparkseries.module.cloudconfig.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 云服务配置信息 Mapper 接口
 */
@Mapper
public interface CloudConfigMapper {
    /**
     * 添加OSS配置信息
     *
     * @param ossConfigEntity OSS配置信息
     * @return 受影响行数
     */
    Integer insertOssConfig(@Param("ossConfigEntity") OssConfigEntity ossConfigEntity);

    /**
     * 添加COS配置信息
     *
     * @param cosConfigEntity COS配置信息
     * @return 受影响行数
     */
    Integer insertCosConfig(@Param("cosConfigEntity") CosConfigEntity cosConfigEntity);

    /**
     * 添加KODO配置信息
     *
     * @param kodoConfigEntity KODO配置信息
     * @return 受影响行数
     */
    Integer insertKodoConfig(@Param("kodoConfigEntity") KodoConfigEntity kodoConfigEntity);

    /**
     * 添加MinIO配置信息
     *
     * @param minioConfigEntity MinIO配置信息
     * @return 受影响行数
     */
    Integer insertMinioConfig(@Param("minioConfigEntity") MinioConfigEntity minioConfigEntity);

    /**
     * 删除OSS配置信息
     *
     * @param id id
     * @return 受影响行数
     */
    Integer deleteOssConfigById(@Param("id") Long id);

    /**
     * 删除COS配置信息
     *
     * @param id id
     * @return 受影响行数
     */
    Integer deleteCosConfigById(@Param("id") Long id);

    /**
     * 删除KODO配置信息
     *
     * @param id id
     * @return 受影响行数
     */
    Integer deleteKodoConfigById(@Param("id") Long id);

    /**
     * 删除MinIO配置信息
     *
     * @param id id
     * @return 受影响行数
     */
    Integer deleteMinioConfigById(@Param("id") Long id);

    /**
     * 根据id查询OSS配置信息
     *
     * @param id id
     * @return OSS配置信息
     */
    OssConfigEntity getOssConfigById(@Param("id") Long id);

    /**
     * 根据id查询COS配置信息
     *
     * @param id id
     * @return COS配置信息
     */
    CosConfigEntity getCosConfigById(@Param("id") Long id);

    /**
     * 根据id查询KODO配置信息
     *
     * @param id id
     * @return KODO配置信息
     */
    KodoConfigEntity getKodoConfigById(@Param("id") Long id);

    /**
     * 根据id查询MinIO配置信息
     *
     * @param id id
     * @return MinIO配置信息
     */
    MinioConfigEntity getMinioConfigById(@Param("id") Long id);

    /**
     * 查询所有OSS配置信息
     *
     * @return OSS配置信息
     */
    List<OssConfigEntity> listOssConfig();

    /**
     * 查询所有COS配置信息
     *
     * @return COS配置信息
     */
    List<CosConfigEntity> listCosConfig();

    /**
     * 查询所有KODO配置信息
     *
     * @return KODO配置信息
     */
    List<KodoConfigEntity> listKodoConfig();

    /**
     * 查询所有MinIO配置信息
     *
     * @return MinIO配置信息
     */
    List<MinioConfigEntity> listMinioConfig();


}