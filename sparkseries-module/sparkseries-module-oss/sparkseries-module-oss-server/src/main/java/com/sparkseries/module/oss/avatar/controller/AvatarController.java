package com.sparkseries.module.oss.avatar.controller;


import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import com.sparkseries.module.oss.avatar.service.AvatarService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 头像管理控制器
 */
@Validated
@RestController
@RequestMapping("/user/avatar")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    /**
     * 上传用户头像
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
     * 修改头像
     */
    @PutMapping()
    @Operation(summary = "修改头像")
    public Result<?> changeAvatar(@RequestParam("file") @NotNull(message = "请指定上传头像") MultipartFile avatar) {
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
     * 获取云存储用户头像
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