package com.sparkseries.module.oss.avatar.controller;


import com.sparkseries.common.security.util.CurrentUser;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.avatar.service.AvatarService;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 用户头像管理
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user/avatar")
@RequiredArgsConstructor
@Tag(name = "用户头像管理")
public class AvatarController {

    private final AvatarService avatarService;

    /**
     * 上传用户头像
     *
     * @param avatar 用户头像图片
     * @param userId 用户 ID
     * @return 默认响应类
     */
    @PostMapping()
    @Operation(summary = "上传用户头像")
    public Result<String> uploadAvatar(@RequestParam("avatar") @NotNull(message = "请指定上传头像") MultipartFile avatar,
                                       @RequestParam("userId") @NotNull(message = "请指定用户 ID") Long userId) {
        try {
            Tika tika = new Tika();
            String type = tika.detect(avatar.getInputStream(), avatar.getContentType());
            MultipartFileDTO avatarDTO = MultipartFileDTO.builder()
                    .fileName(avatar.getOriginalFilename())
                    .userId(userId)
                    .inputStream(avatar.getInputStream())
                    .size(avatar.getSize())
                    .type(type)
                    .build();
            return avatarService.uploadAvatar(avatarDTO);
        } catch (IOException e) {
            log.warn("Controller层:文件头像上传过程中发生了 IO 异常: {}", e.getMessage(), e);
            return Result.error("上传文件失败,请稍后重试");
        }
    }

    /**
     * 更新用户头像
     *
     * @param avatar 用户头像图片
     * @return 默认响应类
     */
    @PutMapping()
    @Operation(summary = "修改头像")
    public Result<?> updateAvatar(@RequestParam("file") @NotNull(message = "请指定上传头像") MultipartFile avatar) {
        try {
            Long userId = CurrentUser.getId();
            Tika tika = new Tika();
            String type = tika.detect(avatar.getInputStream(), avatar.getContentType());
            MultipartFileDTO file = MultipartFileDTO.builder()
                    .fileName(avatar.getOriginalFilename())
                    .userId(userId)
                    .inputStream(avatar.getInputStream())
                    .size(avatar.getSize())
                    .type(type)
                    .build();
            return avatarService.updateAvatar(file);
        } catch (IOException e) {
            log.warn("Controller层:文件头像修改过程中发生了 IO 异常: {}", e.getMessage(), e);
            return Result.error("上传文件失败,请稍后重试");
        }
    }

    /**
     * 获取用户头像
     *
     * @param userId 用户 ID
     * @return 头像 url
     */
    @GetMapping("{userId}")
    @Operation(summary = "获取用户头像")
    public Result<String> getAvatar(@PathVariable("userId") @NotNull(message = "用户 ID 不能为空") Long userId) {
        return avatarService.getUserAvatar(userId);
    }

    /**
     * 预览本地存储头像
     *
     * @param userId 用户 ID
     * @return 文件预览响应
     */
    @GetMapping("local/{userId}")
    @Operation(summary = "预览本地文件")
    public ResponseEntity<?> getLocalFile(@PathVariable("userId") @NotNull(message = "用户 ID 不能为空") Long userId) {
        return avatarService.getLocalAvatar(userId);
    }
}