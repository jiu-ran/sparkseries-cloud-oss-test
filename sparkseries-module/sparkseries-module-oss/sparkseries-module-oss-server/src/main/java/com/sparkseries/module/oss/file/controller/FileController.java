package com.sparkseries.module.oss.file.controller;

import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.common.security.util.CurrentUser;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import com.sparkseries.module.oss.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件管理
 */
@Validated
@AllArgsConstructor
@RestController
@RequestMapping("/user")
@Tag(name = "文件管理")
public class FileController {

    private final FileService fileServer;

    /**
     * 文件上传
     *
     * @param files 待上传的文件列表
     * @param path 文件存储路径
     * @param visibility 能见度
     * @return 上传结果
     */
    @PostMapping("file")
    @Operation(summary = "文件上传")
    public Result<?> uploadFile(
            @RequestParam("files") @NotEmpty(message = "上传文件不能为空") List<MultipartFile> files,
            @RequestParam("path") @NotBlank(message = "请输入文件的存储路径") String path,
            @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {
        Long userId = CurrentUser.getId();
        List<MultipartFileDTO> fileInfos = new ArrayList<>();
        Tika tika = new Tika();
        for (MultipartFile file : files) {
            try {
                String type = tika.detect(file.getInputStream());
                fileInfos.add(new MultipartFileDTO(userId, file.getOriginalFilename(), file.getInputStream(), file.getSize(), type));
            } catch (IOException e) {
                throw new OssException("文件上传失败", e);
            }
        }

        return fileServer.uploadFiles(fileInfos, path, visibility);
    }

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     * @param folderName 文件夹名
     * @param visibility 能见度
     * @return 创建结果
     */
    @PostMapping("folder")
    @Operation(summary = "创建文件夹")
    public Result<?> createFolder(@RequestParam String path,
                                  @RequestParam @NotBlank String folderName,
                                  @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {

        return fileServer.createFolder(path, folderName, visibility);
    }

    /**
     * 删除文件
     *
     * @param id 文件Id
     * @param visibility 能见度
     * @return 删除结果
     */
    @DeleteMapping("/file/{id}")
    @Operation(summary = "删除文件")
    public Result<?> deleteFile(@PathVariable("id") @NotNull(message = "请输入文件id") Long id,
                                @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {

        return fileServer.deleteFile(id, visibility);
    }

    /**
     * 删除文件夹及文件夹下的文件
     *
     * @param path 文件夹路径
     * @param visibility 能见度
     * @return 删除结果
     */
    @DeleteMapping("/folder")
    @Operation(summary = "删除文件夹及文件夹下的文件")
    public Result<?> deleteFolder(@RequestParam("path") @NotBlank(message = "请输入文件夹路径") String path,
                                  @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {

        return fileServer.deleteFolder(path, visibility);
    }


    /**
     * 文件重命名
     *
     * @param id 文件 ID
     * @param name 新文件名
     * @param visibility 能见度
     * @return 重命名结果
     */
    @PutMapping("/rename/{id}")
    @Operation(summary = "文件重命名")
    public Result<?> rename(@PathVariable("id") @NotNull(message = "请输入文件id") Long id,
                            @RequestParam(value = "name") @NotBlank(message = "请输入文件名") String name,
                            @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {

        return fileServer.rename(id, name, visibility);
    }

    /**
     * 移动文件
     *
     * @param id 文件ID
     * @param path 目标路径
     * @param visibility 能见度
     * @return 移动结果
     */
    @PutMapping("movement-file")
    @Operation(summary = "移动文件")
    public Result<?> moveFile(@RequestParam("id") @NotNull(message = "文件id不能为空") Long id,
                              @RequestParam("path") @NotBlank(message = "文件路径不能为空") String path,
                              @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {

        return fileServer.moveFile(id, path, visibility);
    }


    /**
     * 获取文件的预览 url
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 预览 url
     */
    @GetMapping("preview-url/{id}")
    @Operation(summary = "获取文件的预览URL")
    public Result<?> previewUrl(@PathVariable("id") @NotNull(message = "请输入文件id") Long id,
                                @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {

        return fileServer.previewUrl(id, visibility);
    }

    /**
     * 获取文件的下载 url
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 下载 url
     */
    @GetMapping("url/{id}")
    @Operation(summary = "获取文件的下载URL")
    public Result<?> downloadFile(@PathVariable("id") Long id,
                                  @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {
        if (ObjectUtils.isEmpty(id)) {
            return Result.error("请输入文件id");
        }
        return fileServer.downloadFile(id, visibility);
    }


    /**
     * 获取指定文件夹下的文件及文件夹
     *
     * @param path 文件夹路径
     * @param visibility 能见度
     * @return 文件及文件夹列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取指定文件夹下的文件及文件夹")
    public Result<?> listFiles(@RequestParam("path") @NotBlank(message = "请输入文件路径") String path,
                               @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {

        return fileServer.listFiles(path, visibility);
    }


    /**
     * 预览本地文件
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 文件预览响应
     */
    @GetMapping("/previewLocal/{id}")
    @Operation(summary = "预览本地文件")
    public ResponseEntity<?> previewLocalFile(@PathVariable("id") @NotNull(message = "文件id不能为空") Long id,
                                              @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {

        return fileServer.previewLocalFile(id, visibility);
    }

    /**
     * 下载本地文件
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 文件下载响应
     */
    @GetMapping("/downloadLocal/{id}/{visibility}")
    @Operation(summary = "本地文件下载")
    public ResponseEntity<?> DownLoadLocalFile(@PathVariable("id") @NotNull(message = "文件id不能为空") Long id,
                                               @RequestParam(defaultValue = "PRIVATE") VisibilityEnum visibility) {
        return fileServer.downloadLocalFile(id, visibility);
    }
}