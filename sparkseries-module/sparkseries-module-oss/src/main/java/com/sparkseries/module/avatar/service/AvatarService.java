package com.sparkseries.module.avatar.service;

import com.sparkseries.common.dto.MultipartFileDTO;
import com.sparkseries.common.util.entity.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * 头像服务接口
 */
public interface AvatarService {
    /**
     * 上传用户头像
     *
     * @param avatar 头像文件
     * @param userId 用户ID
     * @return 包含操作结果的Result对象
     */
    Result<?> uploadAvatar(MultipartFileDTO avatar, Long userId);

    /**
     * 更新用户头像
     *
     * @param avatar 头像文件
     * @return 包含操作结果的Result对象
     */
    Result<?> updateAvatar(MultipartFileDTO avatar);

    /**
     * 获取用户头像
     *
     * @param userId 用户ID
     * @return 包含用户头像的Result对象
     */
    Result<String> getUserAvatar(Long userId);

    /**
     * 预览本地用户头像
     *
     * @param userId 用户ID
     * @return 包含头像内容的ResponseEntity对象
     */
    ResponseEntity<?> previewLocalAvatar(Long userId);

}