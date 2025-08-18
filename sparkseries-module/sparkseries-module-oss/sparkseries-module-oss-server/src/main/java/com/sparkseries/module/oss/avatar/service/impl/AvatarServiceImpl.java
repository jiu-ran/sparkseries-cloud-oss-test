package com.sparkseries.module.oss.avatar.service.impl;


import static com.sparkeries.constant.Constants.AVATAR_MAX_SIZE;
import static com.sparkeries.constant.Constants.AVATAR_PATH_PREFIX;
import static com.sparkeries.constant.Constants.IMAGE_MIME_PREFIX;
import static com.sparkeries.constant.Constants.SUPPORTED_IMAGE_TYPES;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.sparkseries.common.security.util.CurrentUser;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.common.util.FileUtils;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.avatar.dao.AvatarMapper;
import com.sparkeries.dto.AvatarDTO;
import com.sparkseries.module.oss.avatar.service.AvatarService;
import com.sparkseries.module.oss.switching.DynamicStorageSwitchService;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.provider.local.oss.LocalOssServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 头像服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarServiceImpl implements AvatarService {


    private final AvatarMapper avatarMapper;
    private final DynamicStorageSwitchService provider;

    /**
     * 获取当前存储服务实例
     *
     * @return 当前激活的存储服务
     */
    private OssService getCurrentStorageService() {
        return provider.getCurrentStrategy();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> uploadAvatar(MultipartFileDTO avatar, Long userId) {
        log.info("开始上传用户头像, 用户ID: {}", userId);
        // 文件验证
        validateAvatarFile(avatar);

        // 获取文件信息
        String originalFilename = avatar.getFilename();
        String suffix = FileUtils.getFileExtension(originalFilename);

        if (suffix.isEmpty()) {
            log.warn("请上传带有文件扩展名的图片");
            throw new OssException("请上传带有文件扩展名的图片");
        }

        // 构建存储路径
        String absolutePath = buildAvatarAbsolutePath(userId, suffix);

        avatar.setAbsolutePath(absolutePath);

        // 执行上传
        boolean uploadSuccess = getCurrentStorageService().uploadAvatar(avatar);
        if (!uploadSuccess) {
            log.warn("头像上传失败");
            throw new OssException("上传失败");
        }

        // 保存到数据库
        saveAvatarRecord(avatar, userId, absolutePath);

        return Result.ok("上传成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateAvatar(MultipartFileDTO avatar) {
        log.info("开始修改用户头像");
        // 文件验证
        validateAvatarFile(avatar);

        // 获取文件信息
        String originalFilename = avatar.getFilename();
        String suffix = FileUtils.getFileExtension(originalFilename);

        if (suffix.isEmpty()) {
            log.warn("请上传带有文件扩展名的图片");
            throw new OssException("请上传带有文件扩展名的图片");
        }

        Long userId = CurrentUser.getId();

        // 构建存储路径
        String absolutePath = buildAvatarAbsolutePath(userId, suffix);

        avatar.setAbsolutePath(absolutePath);

        // 执行上传
        boolean uploadSuccess = getCurrentStorageService().uploadAvatar(avatar);
        if (!uploadSuccess) {
            log.warn("头像修改失败");
            throw new OssException("上传失败");
        }

        // 更新数据库记录
        updateAvatarRecord(avatar, userId, absolutePath);

        return Result.ok("上传成功");
    }

    @Override
    public Result<String> getUserAvatar(Long userId) {

        if (getCurrentStorageService() instanceof LocalOssServiceImpl) {
            String avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/avatar/local/{userId}").buildAndExpand(userId).toUriString();
            return Result.ok(avatarUrl);
        }

        String absolutePath = avatarMapper.getAvatarAbsolutePathByUserId(userId);
        String previewUrl = getCurrentStorageService().previewFile(absolutePath);
        return Result.ok(previewUrl);


    }

    @Override
    public ResponseEntity<?> previewLocalAvatar(Long userId) {

        String absolutePath = avatarMapper.getAvatarAbsolutePathByUserId(userId);
        return getCurrentStorageService().previewLocalAvatar(absolutePath);

    }

    /**
     * 验证头像文件
     *
     * @param avatar 头像文件
     */
    private void validateAvatarFile(MultipartFileDTO avatar) {
        validateFileSize(avatar.getSize());
        validateMimeType(avatar.getType());
    }

    /**
     * 验证文件大小
     *
     * @param fileSize 文件大小
     */
    private void validateFileSize(long fileSize) {
        if (fileSize > AVATAR_MAX_SIZE) {
            log.warn("头像大小超出限制: {}MB", fileSize / (1024 * 1024));
            throw new OssException("头像大小不能超过10MB");
        }
    }

    /**
     * 验证MIME类型
     *
     * @param contentType 文件MIME类型
     */
    private void validateMimeType(String contentType) {
        if (contentType == null || !contentType.startsWith(IMAGE_MIME_PREFIX)) {
            log.warn("文件MIME类型不受支持: {}", contentType);
            throw new OssException("只支持图片格式的头像文件");
        }

        if (!SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            log.warn("不支持的图片类型: {}", contentType);
            throw new OssException("只支持JPEG、PNG、GIF、WebP格式的图片");
        }
    }

    /**
     * 构建头像存储路径
     *
     * @param userId 用户ID
     * @param suffix 文件后缀
     * @return 存储路径
     */
    private String buildAvatarAbsolutePath(Long userId, String suffix) {
        return AVATAR_PATH_PREFIX + userId + "." + suffix;
    }

    /**
     * 保存头像记录到数据库
     *
     * @param avatar 头像文件
     * @param userId 用户ID
     * @param absolutePath 存储路径
     */

    private void saveAvatarRecord(MultipartFileDTO avatar, Long userId, String absolutePath) {
        long id = IdWorker.getId();
        String conversion = FileUtils.conversion(avatar.getSize());
        StorageTypeEnum currentStorageEnum = provider.getCurrentStorageEnum();

        AvatarDTO avatarDTO = new AvatarDTO(id, userId, absolutePath, conversion, currentStorageEnum.getKey());
        Integer row = avatarMapper.insertAvatar(avatarDTO);

        if (row <= 0) {
            log.warn("头像信息保存失败 数据库操作失败 头像信息: {}", avatarDTO);
            throw new OssException("上传失败");
        }
    }

    /**
     * 更新头像记录到数据库
     *
     * @param avatar 头像文件
     * @param userId 用户ID
     * @param absolutePath 存储路径
     */
    private void updateAvatarRecord(MultipartFileDTO avatar, Long userId, String absolutePath) {
        String conversion = FileUtils.conversion(avatar.getSize());
        StorageTypeEnum currentStorageEnum = provider.getCurrentStorageEnum();

        AvatarDTO avatarDTO = new AvatarDTO(null, userId, absolutePath, conversion, currentStorageEnum.getKey());
        Integer row = avatarMapper.updateAvatar(avatarDTO);

        if (row <= 0) {
            log.warn("头像信息更新失败 数据库操作失败 头像信息: {}", avatarDTO);
            throw new OssException("上传失败");
        }
    }

}