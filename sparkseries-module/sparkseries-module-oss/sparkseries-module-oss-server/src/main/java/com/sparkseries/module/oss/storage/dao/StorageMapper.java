package com.sparkseries.module.oss.storage.dao;

import com.sparkseries.module.oss.cloud.entity.CloudActiveEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 存储服务 Mapper
 */
@Mapper
public interface StorageMapper {

    /**
     * 添加指定类型的云服务配置信息
     *
     * @param id   id
     * @param type 云存储类型
     * @return 添加结果
     */
    Integer insertCloudActive(@Param("id") Long id, @Param("type") int type);

    /**
     * 删除指定类型的云服务配置信息
     *
     * @return 删除结果
     */
    Integer deleteCloudActive();

    /**
     * 获取当前激活的云服务配置信息
     *
     * @return 激活的云服务配置信息
     */
    CloudActiveEntity getCloudActive();

}
