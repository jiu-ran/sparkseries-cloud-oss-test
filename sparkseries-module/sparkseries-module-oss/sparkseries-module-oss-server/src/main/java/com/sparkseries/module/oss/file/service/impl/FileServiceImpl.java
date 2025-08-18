package com.sparkseries.module.oss.file.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.common.util.FileUtils;
import com.sparkseries.module.oss.file.dao.FileMetadataMapper;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import com.sparkseries.module.oss.file.entity.FileMetadataEntity;
import com.sparkseries.module.oss.file.entity.FolderMetadataEntity;
import com.sparkseries.module.oss.file.service.FileService;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.oss.switching.DynamicStorageSwitchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.sparkeries.constant.Constants.AVATAR_PATH_PREFIX;

/**
 * 对象存储服务实现类
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FileServiceImpl implements FileService {

    private final FileMetadataMapper metadataMapper;
    private final DynamicStorageSwitchService provider;

    /**
     * 构造函数，注入所需的依赖
     *
     * @param metadataMapper 文件元数据Mapper
     */
    public FileServiceImpl(FileMetadataMapper metadataMapper, DynamicStorageSwitchService provider) {
        this.metadataMapper = metadataMapper;
        this.provider = provider;
        log.info("FileServiceImpl 初始化完成，使用动态存储服务管理器");
    }

    /**
     * 获取当前存储服务实例
     *
     * @return 当前激活的存储服务
     */
    private OssService getCurrentStorageService() {
        OssService service = provider.getCurrentStrategy();
        if (service == null) {
            throw new OssException("当前没有可用的存储服务");
        }
        return service;
    }


    /**
     * 批量上传文件
     *
     * @param files 待上传的文件列表
     * @param path  文件存储路径
     * @return 操作结果
     */
    @Override
    public Result<?> uploadFiles(List<MultipartFileDTO> files, String path) {
        path = FileUtils.normalizePath(path);
        // 判断路径是否合法
        FileUtils.isValidPath(path);
        if (path.startsWith(AVATAR_PATH_PREFIX)) {
            throw new OssException("'avatar/' 文件目录属于系统文件你无权操作");
        }
        log.info("批量文件上传开始，文件数量: {}，目标路径: {}", files.size(), path);

        for (MultipartFileDTO file : files) {
            uploadFile(file, path);
            String originalFilename = file.getFilename();
            log.info("文件:'{}' 存储成功", originalFilename);
        }
        return Result.ok("文件上传成功");
    }

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     * @return 操作结果
     */
    @Override
    public Result<?> createFolder(String path) {
        log.info("开始创建文件夹: {}", path);
        path = FileUtils.normalizePath(path);
        FileUtils.isValidPath(path);

        if (path.startsWith(AVATAR_PATH_PREFIX)) {
            throw new OssException("'avatar/' 文件目录属于系统文件你无权操作");
        }

        log.info("调用文件存储服务创建文件夹: {}", path);
        boolean folder = getCurrentStorageService().createFolder(path);

        if (!folder) {
            return Result.error("创建文件夹失败");
        }

        long id = IdWorker.getId();
        String folderName;
        String[] split = path.split("/");
        if (split.length > 1) {
            folderName = split[split.length - 1];
        } else {
            folderName = split[0];
        }
        metadataMapper.insertFolder(new FolderMetadataEntity(id, folderName, path, LocalDateTime.now(),
                getCurrentStorageService().getStorageType()));
        log.info("文件夹创建成功: {}", path);

        return Result.ok("创建文件夹成功");
    }

    /**
     * 删除文件
     *
     * @param id 文件ID
     * @return 操作结果
     */
    @Override
    public Result<?> deleteFile(Long id) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();
        FileMetadataEntity file = metadataMapper.getFileMetadataById(id, storageType);
        if (ObjectUtils.isEmpty(file)) {
            throw new OssException("文件不存在,删除失败");
        }

        String filename = file.getFileName();

        String path = file.getStoragePath();

        if (path.startsWith(AVATAR_PATH_PREFIX)) {
            throw new OssException("'avatar/' 文件目录属于系统文件你无权操作");
        }

        String absolutePath = path + filename;

        boolean deleteFile = getCurrentStorageService().deleteFile(absolutePath);

        if (!deleteFile) {
            throw new OssException("文件删除失败");
        }

        int rows = metadataMapper.deleteFileById(id, storageType);

        if (rows <= 0) {
            throw new OssException("数据库删除失败");
        }

        log.info("文件删除成功 文件id:{}", id);
        return Result.ok("文件删除成功");

    }

    /**
     * 删除文件夹及其内容
     *
     * @param path 文件夹路径
     * @return 操作结果
     */
    @Override
    public Result<?> deleteFolder(String path) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        path = FileUtils.normalizePath(path);

        FileUtils.isValidPath(path);

        if (path.startsWith(AVATAR_PATH_PREFIX)) {
            throw new OssException("'avatar/' 文件目录属于系统文件你无权操作");
        }

        boolean deleteFolder = getCurrentStorageService().deleteFolder(path);

        if (!deleteFolder) {
            return Result.error("文件夹删除失败");
        }
        Integer row = metadataMapper.deleteFileByPath(path, storageType);
        metadataMapper.deleteFolderByPath(path, storageType);
        if (row < 0) {
            return Result.error("删除失败");
        }

        log.info("文件夹{}删除成功", path);

        return Result.ok("删除成功");
    }

    /**
     * 重命名文件
     *
     * @param id      文件ID
     * @param newName 新的文件名
     * @return 操作结果
     */
    @Override
    public Result<?> rename(Long id, String newName) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();
        FileMetadataEntity metadata = metadataMapper.getFileMetadataById(id, storageType);

        if (ObjectUtils.isEmpty(metadata)) {
            throw new OssException("文件不存在");
        }

        String normalizedFilename = FileUtils.normalizeFileName(newName);

        FileUtils.isValidFileName(normalizedFilename);

        String filename = metadata.getFileName();

        String path = metadata.getStoragePath();

        Integer existFileByName = metadataMapper.isExistFileByName(normalizedFilename, path, storageType);

        if (existFileByName > 0) {
            throw new OssException("文件名已被使用");
        }

        boolean rename = getCurrentStorageService().rename(id, filename, normalizedFilename, path);

        if (!rename) {
            throw new OssException("重命名失败");
        }

        if (metadataMapper.updateFileName(id, id + normalizedFilename, normalizedFilename, storageType) <= 0) {
            throw new OssException("数据库文件重命名失败");
        }

        log.info("重命名成功");

        return Result.ok("文件重命名成功");
    }

    /**
     * 获取文件下载链接
     *
     * @param id 文件ID
     * @return 包含下载链接的操作结果
     */
    @Override
    public Result<?> downloadFile(Long id) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();
        if (metadataMapper.isExistFileById(id, storageType) <= 0) {
            throw new OssException("文件不存在");
        }

        FileMetadataEntity file = metadataMapper.getFileMetadataById(id, storageType);

        if (ObjectUtils.isEmpty(file)) {
            throw new OssException("文件不存在");
        }

        String originalName = file.getOriginalName();

        String filename = file.getFileName();

        String path = file.getStoragePath();

        String absolutePath = path + filename;

        String url = getCurrentStorageService().downLoad(absolutePath, originalName);

        if (ObjectUtils.isEmpty(url)) {
            throw new OssException("获取url路径失败");
        }
        log.info("成功获取文件:{}下载链接", originalName);
        return Result.ok("获取成功", url);
    }

    /**
     * 下载本地文件
     *
     * @param id 文件ID
     * @return ResponseEntity 包含文件内容的响应
     */
    @Override
    public ResponseEntity<?> downloadLocalFile(Long id) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();
        FileMetadataEntity fileMetadataEntity = metadataMapper.getFileMetadataById(id, storageType);

        if (ObjectUtils.isEmpty(fileMetadataEntity)) {
            log.error("[本地存储] 文件不存在");
            throw new OssException("文件不存在");
        }

        return getCurrentStorageService().downLocalFile(fileMetadataEntity);
    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param path 路径
     * @return 包含文件和文件夹列表的操作结果
     */
    @Override
    public Result<?> listFiles(String path) {
        // 规范路径
        path = FileUtils.normalizePath(path);

        if (path.startsWith(AVATAR_PATH_PREFIX)) {
            throw new OssException("'avatar/' 文件目录属于系统文件你无权操作");
        }

        // 判断路径是否合法
        FileUtils.isValidPath(path);

        FilesAndFoldersVO filesAndFoldersVO = getCurrentStorageService().listFiles(path);
        log.info("成功获取{}下的文件及文件夹", path);
        return Result.ok("获取成功", filesAndFoldersVO);
    }

    /**
     * 获取文件预览URL
     *
     * @param id 文件ID
     * @return 包含预览URL的操作结果
     */
    @Override
    public Result<?> previewUrl(Long id) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        Integer row = metadataMapper.isExistFileById(id, storageType);
        if (row <= 0) {
            return Result.error("该文件不存在");
        }

        FileMetadataEntity metadata = metadataMapper.getFileMetadataById(id, storageType);
        String url = getCurrentStorageService().previewFile(metadata.getStoragePath() + metadata.getFileName());
        log.info("获取url成功");
        return Result.ok(url);
    }

    /**
     * 预览本地文件
     *
     * @param id 文件ID
     * @return ResponseEntity 包含文件内容的响应
     */
    @Override
    public ResponseEntity<?> previewLocalFile(Long id) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        FileMetadataEntity fileMetadataEntity = metadataMapper.getFileMetadataById(id, storageType);

        if (ObjectUtils.isEmpty(fileMetadataEntity)) {
            log.error("文件不存在");
            throw new OssException("文件不存在");
        }

        return getCurrentStorageService().previewLocalFile(fileMetadataEntity);
    }

    /**
     * 移动文件
     *
     * @param id      文件ID
     * @param newPath 新的存储路径
     * @return 操作结果
     */
    @Override
    public Result<?> moveFile(Long id, String newPath) {

        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        if (metadataMapper.isExistFileById(id, storageType) <= 0) {
            throw new OssException("该文件不存在");
        }

        newPath = FileUtils.normalizePath(newPath);

        FileUtils.isValidPath(newPath);

        if (newPath.startsWith(AVATAR_PATH_PREFIX)) {
            throw new OssException("'avatar/' 文件目录属于系统文件你无权操作");
        }


        FileMetadataEntity metadata = metadataMapper.getFileMetadataById(id, storageType);
        String filename = metadata.getFileName();
        String storagePath = metadata.getStoragePath();
        String sourcePath = storagePath + filename;
        String targetPath = newPath + filename;

        if (metadataMapper.isExistFileByName(filename, newPath, storageType) > 0) {
            throw new OssException("文件名已被使用");
        }

        boolean moveFile = getCurrentStorageService().moveFile(sourcePath, targetPath);

        if (!moveFile) {
            throw new OssException("文件移动失败");
        }

        Integer row = metadataMapper.updatePath(id, newPath, storageType);
        if (row <= 0) {
            throw new OssException("数据库文件移动失败");
        }
        return Result.ok("文件移动成功");
    }

    /**
     * 文件存储
     *
     * @param file 文件信息
     * @param path 文件存储路径
     */
    public void uploadFile(MultipartFileDTO file, String path) {

        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();
        log.info("文件{}开始上传", file);

        // 获取文件名
        String originalFilename = file.getFilename();

        // 规范文件名
        originalFilename = FileUtils.normalizeFileName(originalFilename);

        // 判断文件名是否合法
        FileUtils.isValidFileName(originalFilename);

        // 查询指定路径下该文件是否存在
        if (metadataMapper.isExistFileByName(originalFilename, path, storageType) != 0) {
            log.error("文件名:{} 路径:{} 文件在该路径下已存在", originalFilename, path);
            throw new OssException(path + "路径下已存在" + originalFilename + "文件");
        }
        file.setFilename(originalFilename);

        // 获取文件大小
        long size = file.getSize();
        String conversion = FileUtils.conversion(size);
        file.setStrSize(conversion);
        // 生成唯一id
        long id = IdWorker.getId();

        String filename = id + originalFilename;

        String absolutePath = path + filename;

        file.setAbsolutePath(absolutePath);

        file.setId(String.valueOf(id));

        boolean upload = getCurrentStorageService().upload(file);
        if (!upload) {
            log.error("文件上传失败");
            throw new RuntimeException("文件上传失败");
        }
        log.info("文件存储服务上传文件成功: {}", absolutePath);
        // 保存元数据到数据库
        FileMetadataEntity metadata = new FileMetadataEntity(id, filename, originalFilename, file.getType(), conversion,
                path, LocalDateTime.now(), getCurrentStorageService().getStorageType());

        Integer rows = metadataMapper.insertFile(metadata);

        if (rows <= 0) {
            log.error("数据库添加文件元数据失败，文件: {}", absolutePath);
            throw new OssException("数据库添加文件失败");
        }
        log.info("文件元数据保存到数据库成功，ID: {}", id);

        log.info("单个文件上传及元数据保存成功，文件名: {}, 绝对路径: {}, ID: {}", originalFilename, absolutePath, id);

    }

}