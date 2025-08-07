package com.sparkseries.module.file.controller;

import com.sparkseries.common.dto.MultipartFileDTO;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理控制器
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
     * @param files 文件集
     * @param path  文件存储路径
     * @return 上传结果
     */
    @PostMapping("file")
    @Operation(summary = "文件上传")
    public Result<?> uploadFile(
            @RequestParam("files") @NotEmpty(message = "上传文件不能为空") List<MultipartFile> files,
            @RequestParam("path") @NotBlank(message = "请输入文件的存储路径") String path) {

        List<MultipartFileDTO> fileInfos = new ArrayList<>();
        Tika tika = new Tika();
        for (MultipartFile file : files) {
            try {
                String type = tika.detect(file.getInputStream());
                fileInfos.add(
                        new MultipartFileDTO(file.getOriginalFilename(), file.getInputStream(), file.getSize(), type));
            } catch (IOException e) {
                throw new BusinessException("文件上传失败", e);
            }
        }

        return fileServer.uploadFiles(fileInfos, path);
    }

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     * @return 创建结果
     */
    @PostMapping("folder")
    @Operation(summary = "创建文件夹")
    public Result<?> createFolder(@RequestParam @NotBlank(message = "请输入文件夹路径") String path) {

        return fileServer.createFolder(path);
    }

    /**
     * 删除文件
     *
     * @param id 文件Id
     * @return 删除结果
     */
    @DeleteMapping("/file/{id}")
    @Operation(summary = "删除文件")
    public Result<?> deleteFile(@PathVariable("id") @NotNull(message = "请输入文件id") Long id) {

        return fileServer.deleteFile(id);
    }

    /**
     * 删除文件夹
     *
     * @param path 文件夹路径
     * @return 删除结果
     */
    @DeleteMapping("/folder")
    @Operation(summary = "删除文件夹")
    public Result<?> deleteFolder(@RequestParam("path") @NotBlank(message = "请输入文件夹路径") String path) {

        return fileServer.deleteFolder(path);
    }

    /**
     * 移动文件
     *
     * @param id   文件ID
     * @param path 目标路径
     * @return 移动结果
     */
    @PutMapping("movement-file")
    @Operation(summary = "移动文件")
    public Result<?> moveFile(@RequestParam("id") @NotNull(message = "文件id不能为空") Long id,
            @RequestParam("path") @NotBlank(message = "文件路径不能为空") String path) {

        return fileServer.moveFile(id, path);
    }
    // TODO 移动文件夹

    /**
     * 文件重命名
     *
     * @param id   文件ID
     * @param name 新文件名
     * @return 重命名结果
     */
    @PutMapping("/rename/{id}")
    @Operation(summary = "文件重命名")
    public Result<?> rename(@PathVariable("id") @NotNull(message = "请输入文件id") Long id,
            @RequestParam(value = "name") @NotBlank(message = "请输入文件名") String name) {

        return fileServer.rename(id, name);
    }

    // TODO 文件夹重命名

    /**
     * 获取文件的下载URL
     *
     * @param id 文件ID
     * @return 文件的下载URL
     */
    @GetMapping("url/{id}")
    @Operation(summary = "获取文件的下载URL")
    public Result<?> downloadFile(@PathVariable("id") Long id) {
        if (ObjectUtils.isEmpty(id)) {
            return Result.error("请输入文件id");
        }
        return fileServer.downloadFile(id);
    }

    /**
     * 获取文件的预览URL
     *
     * @param id 文件ID
     * @return 文件的预览URL
     */
    @GetMapping("preview-url/{id}")
    @Operation(summary = "获取文件的预览URL")
    public Result<?> previewUrl(@PathVariable("id") @NotNull(message = "请输入文件id") Long id) {

        return fileServer.previewUrl(id);
    }

    /**
     * 获取指定文件夹下的文件及文件夹
     *
     * @param path 文件夹路径
     * @return 文件及文件夹列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取指定文件夹下的文件及文件夹")
    public Result<?> listFiles(@RequestParam("path") @NotBlank(message = "请输入文件路径") String path) {

        return fileServer.listFiles(path);
    }


    /**
     * 本地文件下载
     *
     * @param id 文件ID
     * @return 文件下载响应
     */
    @GetMapping("/localDownLoad/{id}")
    @Operation(summary = "本地文件下载")
    public ResponseEntity<?> localDownLoadFile(@PathVariable("id") @NotNull(message = "文件id不能为空") Long id) {

        return fileServer.downloadLocalFile(id);

    }

    /**
     * 预览本地文件
     *
     * @param id 文件ID
     * @return 文件预览响应
     */
    @GetMapping("/previewLocal/{id}")
    @Operation(summary = "预览本地文件")
    public ResponseEntity<?> previewLocalFile(@PathVariable("id") @NotNull(message = "文件id不能为空") Long id) {

        return fileServer.previewLocalFile(id);
    }

}