package com.sparkseries.module.oss.avatar.controller;


import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.avatar.service.AvatarService;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 用户头像管理
 */
@Validated
@RestController
@RequestMapping("/user/avatar")
@RequiredArgsConstructor
@Tag(name = "头像管理")
public class AvatarController {

    private final AvatarService avatarService;

    /**
     * 上传用户头像
     *
     * @param avatar 头像图片
     * @param userId 用户ID
     * @return 上传结果
     */
    @PostMapping()
    @Operation(summary = "上传用户头像")
    public Result<String> uploadAvatar(@RequestParam("file") @NotNull(message = "请指定上传头像") MultipartFile avatar,
                                       @RequestParam("userId") @NotNull(message = "请指定用户id") Long userId) {
        try {
            Tika tika = new Tika();
            String type = tika.detect(avatar.getInputStream(), avatar.getContentType());
            MultipartFileDTO file = new MultipartFileDTO(avatar.getOriginalFilename(), avatar.getInputStream(), avatar.getSize(), type);
            return avatarService.uploadAvatar(file, userId);
        } catch (IOException e) {
            return Result.error("上传文件失败");
        }
    }

    /**
     * 修改用户头像
     *
     * @param avatar 头像文件
     * @return 修改结果
     */
    @PutMapping()
    @Operation(summary = "修改头像")
    public Result<?> updateAvatar(@RequestParam("file") @NotNull(message = "请指定上传头像") MultipartFile avatar) {
        try {
            Tika tika = new Tika();
            String type = tika.detect(avatar.getInputStream(), avatar.getContentType());
            MultipartFileDTO file = new MultipartFileDTO(avatar.getOriginalFilename(), avatar.getInputStream(), avatar.getSize(), type);
            return avatarService.updateAvatar(file);
        } catch (IOException e) {
            return Result.error("上传文件失败");
        }
    }

    /**
     * 获取用户头像
     *
     * @param userId 用户ID
     * @return 头像URL
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取用户头像")
    public Result<String> getAvatar(@PathVariable("userId") @NotNull(message = "用户id不能为空") Long userId) {
        return avatarService.getUserAvatar(userId);
    }

    /**
     * 预览本地存储头像
     *
     * @param userId 用户ID
     * @return 文件预览响应
     */
    @GetMapping("local/{userId}")
    @Operation(summary = "预览本地文件")
    public ResponseEntity<?> previewLocalFile(@PathVariable("userId") @NotNull(message = "用户id不能为空") Long userId) {
        return avatarService.previewLocalAvatar(userId);
    }
}