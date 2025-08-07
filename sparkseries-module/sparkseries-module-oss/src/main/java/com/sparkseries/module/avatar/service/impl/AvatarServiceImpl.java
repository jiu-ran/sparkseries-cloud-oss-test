package com.sparkseries.module.avatar.service.impl;


import static com.sparkseries.common.constant.Constants.AVATAR_MAX_SIZE;
import static com.sparkseries.common.constant.Constants.AVATAR_PATH_PREFIX;
import static com.sparkseries.common.constant.Constants.IMAGE_MIME_PREFIX;
import static com.sparkseries.common.constant.Constants.SUPPORTED_IMAGE_TYPES;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.sparkseries.common.dto.MultipartFileDTO;
import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.FileUtils;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.avatar.dao.AvatarMapper;
import com.sparkseries.module.avatar.dto.AvatarDTO;
import com.sparkseries.module.avatar.service.AvatarService;
import com.sparkseries.module.storage.provider.StorageStrategyProvider;
import com.sparkseries.module.storage.service.oss.OssService;
import com.sparkseries.module.storage.service.oss.impl.LocalOssServiceImpl;
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
@Transactional(rollbackFor = Exception.class)
public class AvatarServiceImpl implements AvatarService {


    private final AvatarMapper avatarMapper;
    private final StorageStrategyProvider provider;

    /**
     * 获取当前存储服务实例
     *
     * @return 当前激活的存储服务
     */
    private OssService getCurrentStorageService() {
        OssService service = provider.getCurrentStrategy();
        if (service == null) {
            throw new BusinessException("当前没有可用的存储服务");
        }
        return service;
    }

    @Override
    public Result<?> uploadAvatar(MultipartFileDTO avatar, Long userId) {
        // 文件验证
        validateAvatarFile(avatar);

        // 获取文件信息
        String originalFilename = avatar.getFilename();
        String suffix = FileUtils.getFileExtension(originalFilename);

        if (suffix.isEmpty()) {
            throw new BusinessException("请上传带有文件扩展名的图片");
        }

        // 构建存储路径
        String absolutePath = buildAvatarAbsolutePath(userId, suffix);

        avatar.setAbsolutePath(absolutePath);

        // 执行上传
        boolean uploadSuccess = getCurrentStorageService().uploadAvatar(avatar);
        if (!uploadSuccess) {
            throw new BusinessException("上传失败");
        }

        // 保存到数据库
        saveAvatarRecord(avatar, userId, absolutePath);

        return Result.ok("上传成功");
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
            throw new BusinessException("头像大小不能超过10MB");
        }
    }

    /**
     * 验证MIME类型
     *
     * @param contentType 文件MIME类型
     */
    private void validateMimeType(String contentType) {
        if (contentType == null || !contentType.startsWith(IMAGE_MIME_PREFIX)) {
            throw new BusinessException("只支持图片格式的头像文件");
        }

        if (!SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("只支持JPEG、PNG、GIF、WebP格式的图片");
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
     * @param avatar       头像文件
     * @param userId       用户ID
     * @param absolutePath 存储路径
     */
    private void saveAvatarRecord(MultipartFileDTO avatar, Long userId, String absolutePath) {
        long id = IdWorker.getId();
        String conversion = FileUtils.conversion(avatar.getSize());
        StorageTypeEnum currentStorageEnum = provider.getCurrentStorageEnum();

        AvatarDTO avatarDTO = new AvatarDTO(id, userId, absolutePath, conversion,
                currentStorageEnum.getKey());
        Integer row = avatarMapper.insertAvatar(avatarDTO);

        if (row <= 0) {
            throw new BusinessException("上传失败");
        }
    }

    @Override
    public Result<?> updateAvatar(MultipartFileDTO avatar) {

        // 文件验证
        validateAvatarFile(avatar);

        // 获取文件信息
        String originalFilename = avatar.getFilename();
        String suffix = FileUtils.getFileExtension(originalFilename);

        if (suffix.isEmpty()) {
            throw new BusinessException("请上传带有文件扩展名的图片");
        }

        // 构建存储路径
        String absolutePath = buildAvatarAbsolutePath(123L, suffix);

        avatar.setAbsolutePath(absolutePath);

        // 执行上传
        boolean uploadSuccess = getCurrentStorageService().uploadAvatar(avatar);
        if (!uploadSuccess) {
            throw new BusinessException("上传失败");
        }

        // 更新数据库记录
        updateAvatarRecord(avatar, 123L, absolutePath);

        return Result.ok("上传成功");
    }


    /**
     * 更新头像记录到数据库
     *
     * @param avatar       头像文件
     * @param userId       用户ID
     * @param absolutePath 存储路径
     */
    private void updateAvatarRecord(MultipartFileDTO avatar, Long userId, String absolutePath) {
        String conversion = FileUtils.conversion(avatar.getSize());
        StorageTypeEnum currentStorageEnum = provider.getCurrentStorageEnum();

        AvatarDTO avatarDTO = new AvatarDTO(null, userId, absolutePath, conversion,
                currentStorageEnum.getKey());
        Integer row = avatarMapper.updateAvatar(avatarDTO);

        if (row <= 0) {
            throw new BusinessException("上传失败");
        }
    }

    @Override
    public Result<String> getUserAvatar(Long userId) {

        if (getCurrentStorageService() instanceof LocalOssServiceImpl) {
            String avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/avatar/local/{userId}").buildAndExpand(userId).toUriString();
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

}