package com.sparkseries.module.oss.avatar.service;

import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import org.springframework.http.ResponseEntity;

/**
 * 用户头像管理
 */
public interface AvatarService {

    /**
     * 上传用户头像
     *
     * @param avatar 用户头像相关信息
     * @return 默认响应类
     */
    Result<String> uploadAvatar(MultipartFileDTO avatar);

    /**
     * 更新用户头像
     *
     * @param avatar 用户头像相关信息
     * @return 默认响应类
     */
    Result<?> updateAvatar(MultipartFileDTO avatar);

    /**
     * 获取用户头像
     *
     * @param userId 用户 ID
     * @return 头像 url
     */
    Result<String> getUserAvatar(Long userId);

    /**
     * 预览本地用户头像
     *
     * @param userId 用户 ID
     * @return 文件预览响应
     */
    ResponseEntity<?> getLocalAvatar(Long userId);

}