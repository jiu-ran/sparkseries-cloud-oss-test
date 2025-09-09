package com.sparkseries.module.oss.avatar.dao;


import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.avatar.entity.AvatarEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户头像管理
 */
@Mapper
public interface AvatarMapper {
    /**
     * 添加用户头像元数据
     *
     * @param avatar 头像信息
     * @return 受影响行数
     */
    Integer insertAvatar(@Param("avatar") AvatarEntity avatar);


    /**
     * 修改头像
     *
     * @param avatar 头像信息
     * @return 受影响行数
     */
    Integer updateAvatar(@Param("avatar") AvatarEntity avatar);

    /**
     * 根据用户 ID 查询头像信息
     *
     * @param userId 用户 ID
     * @param storageType 存储类型
     * @return 头像信息
     */
    AvatarEntity getAvatarByUserId(@Param("userId") Long userId, @Param("storageType") StorageTypeEnum storageType);


}