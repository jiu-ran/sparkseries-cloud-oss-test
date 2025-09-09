package com.sparkseries.module.oss.file.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.sparkeries.dto.UploadFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.common.security.util.CurrentUser;
import com.sparkseries.common.util.entity.Result;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.common.util.FileUtil;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import com.sparkseries.module.oss.file.entity.FileMetadataEntity;
import com.sparkseries.module.oss.file.entity.FolderMetadataEntity;
import com.sparkseries.module.oss.file.service.FileService;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.oss.provider.local.oss.LocalOssServiceImpl;
import com.sparkseries.module.oss.switching.DynamicStorageSwitchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对象存储管理
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FileServiceImpl implements FileService {

    private final MetadataMapper metadataMapper;

    private final DynamicStorageSwitchService provider;

    public FileServiceImpl(MetadataMapper metadataMapper, DynamicStorageSwitchService provider) {
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
        return provider.getCurrentStrategy();
    }


    /**
     * 文件列表上传
     *
     * @param files 待上传的文件列表
     * @param folderPath 文件存储的文件夹路径
     * @param visibility 能见度
     * @return 默认响应类
     */
    @Override
    public Result<?> uploadFiles(List<MultipartFileDTO> files, String folderPath, VisibilityEnum visibility) {

        folderPath = FileUtil.normalizeAndValidateFolderPath(folderPath);

        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        log.info("批量文件上传开始，文件数量: {}，目标路径: {}", files.size(), folderPath);

        // 检验上传的文件名是否存在
        checkFileExist(files, visibility, folderPath, storageType);

        for (MultipartFileDTO file : files) {
            uploadFile(file, folderPath, visibility);
        }
        return Result.ok("文件上传成功");
    }

    /**
     * 创建文件夹
     *
     * @param folderName 文件夹名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @return 默认响应类
     */
    @Override
    public Result<?> createFolder(String folderName, String folderPath, VisibilityEnum visibility) {
        log.info("开始创建文件夹: {}", folderPath);
        Long userId = CurrentUser.getId();
        folderPath = FileUtil.normalizeAndValidateFolderPath(folderPath);
        folderName = FileUtil.normalizeAndValidateFolderName(folderName);
        log.info("调用文件存储服务创建文件夹: {}", folderPath);

        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();
        int count = metadataMapper.isExistFolderByFolderPath(folderName, folderPath, storageType, visibility);
        if (count > 0) {
            return Result.error("该文件夹已存在");
        }

        boolean folder = getCurrentStorageService().createFolder(folderName, folderPath, visibility, userId.toString());

        if (!folder) {
            return Result.error("创建文件夹失败");
        }
        long id = IdWorker.getId();

        FolderMetadataEntity folderMetadataEntity = new FolderMetadataEntity(id, userId, folderName, folderPath, LocalDateTime.now(), storageType, visibility);


        int row = metadataMapper.insertFolder(folderMetadataEntity);

        if (row <= 0) {
            log.warn("数据库中添加文件夹相关信息失败");
            throw new OssException("数据库中添加 文件夹相关信息失败");
        }
        log.info("文件夹创建成功: {}", folderPath + folderName);

        return Result.ok("创建文件夹成功");
    }

    /**
     * 删除文件
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 默认响应类
     */
    @Override
    public Result<?> deleteFile(Long id, VisibilityEnum visibility) {
        Long userId = CurrentUser.getId();
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        FileMetadataEntity file = metadataMapper.getFileMetadataById(id, storageType, visibility);

        if (ObjectUtils.isEmpty(file)) {
            log.warn("用户:{} 进行文件删除操作 文件:{} 不存在 ", userId, id);
            throw new OssException("文件不存在,删除失败");
        }

        if (!file.getUserId().equals(userId) && file.getVisibility() == VisibilityEnum.PRIVATE) {
            log.warn("用户:{} 进行文件删除操作 文件:{} 不存在 ", userId, id);
            throw new OssException("您没有权限删除该文件");
        }

        String fileName = file.getFileName();

        String folderPath = file.getFolderPath();

        boolean deleteFile = getCurrentStorageService().deleteFile(fileName, folderPath, visibility, userId.toString());

        if (!deleteFile) {
            log.warn("用户:{} 删除云存储文件:{} 删除失败", userId, folderPath + fileName);
            throw new OssException("存储文件删除失败");
        }

        int row = metadataMapper.deleteFileById(id, storageType, visibility);

        if (row <= 0) {
            log.warn("用户:{} 删除数据库元数据:{} 删除失败", userId, folderPath + fileName);
            throw new OssException("数据库删除失败");
        }

        log.info("文件删除成功 文件id:{}", id);
        return Result.ok("文件删除成功");

    }

    /**
     * 删除文件夹及其内容
     *
     * @param folderName 文件夹名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @return 默认响应类
     */
    @Override
    public Result<?> deleteFolder(String folderName, String folderPath, VisibilityEnum visibility) {

        Long userId = CurrentUser.getId();

        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        folderName = FileUtil.normalizeAndValidateFolderName(folderName);

        folderPath = FileUtil.normalizeAndValidateFolderPath(folderPath);

        boolean deleteFolder = getCurrentStorageService().deleteFolder(folderName, folderPath, visibility, userId.toString());

        if (!deleteFolder) {
            return Result.error("文件夹删除失败");
        }

        int row = metadataMapper.deleteFileByFolderPath(folderPath, storageType, visibility);

        if (row < 0) {
            return Result.error("删除失败");
        }

        log.info("文件夹{}删除成功", folderPath + folderName);

        return Result.ok("删除成功");
    }

    /**
     * 移动文件到指定文件夹中
     *
     * @param id 文件ID
     * @param folderName 文件夹名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @return 默认响应类
     */
    @Override
    public Result<?> moveFile(Long id, String folderName, String folderPath, VisibilityEnum visibility) {

        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        String targetPath = Path.of(FileUtil.normalizeAndValidateFolderPath(folderPath), FileUtil.normalizeAndValidateFileName(folderName)).toString();

        FileMetadataEntity metadata = getFileMetadataById(id, visibility, storageType);
        String filename = metadata.getFileName();
        String sourcePath = metadata.getFolderPath();

        int row;

        row = metadataMapper.isExistFileByFileName(filename, folderPath, storageType, visibility);

        if (row > 0) {
            throw new OssException("文件名已被使用");
        }

        boolean moveFile = getCurrentStorageService().moveFile(filename, sourcePath, targetPath, visibility, CurrentUser.getId().toString());

        if (!moveFile) {
            throw new OssException("文件移动失败");
        }

        row = metadataMapper.updateFileFolderPath(id, folderPath, storageType, visibility);
        if (row <= 0) {
            log.warn("数据库文件移动失败");
            throw new OssException("数据库文件移动失败");
        }
        return Result.ok("文件移动成功");
    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param folderName 文件夹名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @return 文件和文件夹列表
     */
    @Override
    public Result<?> listFileAndFolder(String folderName, String folderPath, VisibilityEnum visibility) {
        // 规范路径
        Long userId = CurrentUser.getId();

        folderPath = FileUtil.normalizeAndValidateFolderPath(folderPath);

        folderName = FileUtil.normalizeAndValidateFolderName(folderName);

        FilesAndFoldersVO filesAndFoldersVO = getCurrentStorageService().listFileAndFolder(folderName, folderPath, visibility, userId);
        log.info("成功获取{}下的文件及文件夹", folderPath + folderName);
        return Result.ok("获取成功", filesAndFoldersVO);
    }

    /**
     * 获取文件预览 url
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 文件预览链接
     */
    @Override
    public Result<?> previewUrl(Long id, VisibilityEnum visibility) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        FileMetadataEntity metadata = getFileMetadataById(id, visibility, storageType);

        if (getCurrentStorageService() instanceof LocalOssServiceImpl) {

            try {
                // 从请求上下文中获取 HttpServletRequest
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes == null) {
                    throw new OssException("无法获取当前请求上下文");
                }
                HttpServletRequest request = attributes.getRequest();

                // 动态构建URL
                String host = InetAddress.getLocalHost().getHostAddress();
                String avatarUrl = UriComponentsBuilder.newInstance().scheme(request.getScheme()).host(host).port(request.getServerPort()).path("/user/previewLocal/{id}").queryParam("visibility", visibility).buildAndExpand(id).toUriString();
                return Result.ok(avatarUrl);
            } catch (UnknownHostException e) {
                log.warn("获取服务器主机地址失败", e);
                throw new OssException("无法生成头像URL，获取主机地址失败");
            }
        }

        String url = getCurrentStorageService().previewFile(metadata.getFileName(), metadata.getFolderPath(), visibility, metadata.getUserId().toString());

        log.info("获取url成功");
        return Result.ok(url);
    }

    /**
     * 获取文件下载链接
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 文件下载链接
     */
    @Override
    public Result<?> downloadFile(Long id, VisibilityEnum visibility) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        FileMetadataEntity file = getFileMetadataById(id, visibility, storageType);

        String fileName = file.getFileName();

        String folderPath = file.getFolderPath();

        String userId = CurrentUser.getId().toString();

        if (getCurrentStorageService() instanceof LocalOssServiceImpl) {

            try {
                // 从请求上下文中获取 HttpServletRequest
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes == null) {
                    throw new OssException("无法获取当前请求上下文");
                }
                HttpServletRequest request = attributes.getRequest();

                // 动态构建URL
                String host = InetAddress.getLocalHost().getHostAddress();
                String avatarUrl = UriComponentsBuilder.newInstance().scheme(request.getScheme()).host(host).port(request.getServerPort()).path("/user/downloadLocal/{id}").queryParam("visibility", visibility).buildAndExpand(id).toUriString();
                return Result.ok(avatarUrl);
            } catch (UnknownHostException e) {
                log.warn("获取服务器主机地址失败", e);
                throw new OssException("无法生成头像URL，获取主机地址失败");
            }
        }

        String url = getCurrentStorageService().downLoad(fileName, folderPath, visibility, userId);

        if (ObjectUtils.isEmpty(url)) {
            throw new OssException("获取url路径失败");
        }
        log.info("成功获取文件:{}下载链接", fileName);
        return Result.ok("获取成功", url);
    }

    /**
     * 预览本地文件
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 文件预览响应
     */
    @Override
    public ResponseEntity<?> previewLocalFile(Long id, VisibilityEnum visibility) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();
        Long userId = CurrentUser.getId();
        FileMetadataEntity fileMetadataEntity = getFileMetadataById(id, visibility, storageType);

        if (getCurrentStorageService() instanceof LocalOssServiceImpl) {

            return ((LocalOssServiceImpl) getCurrentStorageService()).previewLocalFile(fileMetadataEntity, visibility, userId.toString());
        }

        throw new OssException("错误操作");
    }

    /**
     * 下载本地文件
     *
     * @param id 文件 ID
     * @param visibility 能见度
     * @return 文件下载响应
     */
    @Override
    public ResponseEntity<?> downloadLocalFile(Long id, VisibilityEnum visibility) {
        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();
        FileMetadataEntity fileMetadataEntity = getFileMetadataById(id, visibility, storageType);

        Long userId = CurrentUser.getId();

        if (ObjectUtils.isEmpty(fileMetadataEntity)) {
            log.error("[本地存储] 文件不存在");
            throw new OssException("文件不存在");
        }
        if (getCurrentStorageService() instanceof LocalOssServiceImpl) {

            return ((LocalOssServiceImpl) getCurrentStorageService()).downLocalFile(fileMetadataEntity, visibility, userId.toString());
        }

        throw new OssException("当前存储类型不支持下载");
    }

    // 私有方法

    /**
     * 文件存储
     *
     * @param file 文件信息
     * @param folderPath 文件存储的文件夹路径
     * @param visibility 能见度
     */
    private void uploadFile(MultipartFileDTO file, String folderPath, VisibilityEnum visibility) {

        log.info("文件{}开始上传", file);

        StorageTypeEnum storageType = getCurrentStorageService().getStorageType();

        String filename = file.getFileName();

        // 获取文件大小
        long size = file.getSize();
        String conversion = FileUtil.conversion(size);
        // 生成文件 Id
        long id = IdWorker.getId();

        UploadFileDTO fileDTO = UploadFileDTO.builder().userId(file.getUserId().toString()).inputStream(file.getInputStream()).fileName(filename).size(size).folderPath(folderPath).visibility(visibility).build();
        // 上传文件
        boolean upload = getCurrentStorageService().uploadFile(fileDTO);
        if (!upload) {
            log.warn("文件上传失败");
            throw new OssException("文件上传失败");
        }
        log.info("文件存储服务上传文件成功: {}", folderPath);
        // 保存元数据到数据库
        FileMetadataEntity metadata = new FileMetadataEntity(id, file.getUserId(), filename, file.getType(), conversion, folderPath, null, storageType, visibility);

        Integer row = metadataMapper.insertFile(metadata);

        if (row <= 0) {
            log.warn("数据库添加文件元数据失败，文件: {}", filename);
            throw new OssException("数据库添加文件失败");
        }
        log.info("文件元数据保存到数据库成功，ID: {}", id);

        log.info("文件上传 元数据保存 成功，文件名: {}, 存储文件夹: {}, ID: {}", filename, folderPath, id);

    }

    /**
     * 获取文件元数据
     *
     * @param id 文件 ID
     * @param storageType 存储类型
     * @return 文件元数据实体
     */
    private FileMetadataEntity getFileMetadataById(Long id, VisibilityEnum visibility, StorageTypeEnum storageType) {

        FileMetadataEntity file = metadataMapper.getFileMetadataById(id, storageType, visibility);

        if (ObjectUtils.isEmpty(file)) {
            log.warn("文件不存在");
            throw new OssException("文件不存在");
        }
        return file;
    }

    /**
     * 检验指定文件夹下是否存在相同文件名的文件
     *
     * @param lists 文件列表
     * @param visibility 能见度
     * @param folderPath 文件夹路径
     * @param storageType 存储类型
     */
    private void checkFileExist(List<MultipartFileDTO> lists, VisibilityEnum visibility, String folderPath, StorageTypeEnum storageType) {
        StringBuilder filenameList = new StringBuilder();
        for (MultipartFileDTO file : lists) {
            file.setFileName(FileUtil.normalizeAndValidateFileName(file.getFileName()));
            int row = metadataMapper.isExistFileByFileName(file.getFileName(), folderPath, storageType, visibility);
            if (row > 0) {
                filenameList.append(file.getFileName()).append(",");
            }
        }
        if (!filenameList.isEmpty()) {
            filenameList.deleteCharAt(filenameList.lastIndexOf(","));
            throw new OssException("文件名已存在: " + filenameList);
        }
    }

}