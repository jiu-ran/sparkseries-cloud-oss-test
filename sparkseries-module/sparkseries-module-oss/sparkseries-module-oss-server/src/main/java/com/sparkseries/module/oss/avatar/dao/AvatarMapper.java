package com.sparkseries.module.oss.avatar.dao;


import com.sparkeries.dto.AvatarDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户头像管理
 */
@Mapper
public interface AvatarMapper {
    /**
     * 添加用户头像
     *
     * @param avatar 头像信息
     * @return 受影响行数
     */
    Integer insertAvatar(@Param("avatar") AvatarDTO avatar);


    /**
     * 修改头像
     *
     * @param avatar 头像信息
     * @return 受影响行数
     */
    Integer updateAvatar(@Param("avatar") AvatarDTO avatar);

    /**
     * 根据用户ID查询头像信息
     *
     * @param userId 用户ID
     * @return 头像信息
     */
    String getAvatarAbsolutePathByUserId(@Param("userId") Long userId);


}