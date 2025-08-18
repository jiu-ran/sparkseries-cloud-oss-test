package com.sparkseries.module.oss.avatar.service;

import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import com.sparkseries.common.util.entity.Result;
import org.springframework.http.ResponseEntity;

/**
 * 用户头像管理
 */
public interface AvatarService {

    /**
     * 上传用户头像
     *
     * @param avatar 头像图片
     * @param userId 用户ID
     * @return 上传结果信息
     */
    Result<String> uploadAvatar(MultipartFileDTO avatar, Long userId);

    /**
     * 更新用户头像
     *
     * @param avatar 头像文件
     * @return 默认响应类
     */
    Result<?> updateAvatar(MultipartFileDTO avatar);

    /**
     * 获取用户头像
     *
     * @param userId 用户ID
     * @return 用户头像 url 地址
     */
    Result<String> getUserAvatar(Long userId);

    /**
     * 预览本地用户头像
     *
     * @param userId 用户ID
     * @return 文件预览响应
     */
    ResponseEntity<?> previewLocalAvatar(Long userId);

}