package com.sparkseries.module.oss.provider.qiniu.oss;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.sparkeries.dto.UploadFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.file.vo.FileInfoVO;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.oss.file.vo.FolderInfoVO;
import com.sparkseries.module.oss.provider.qiniu.pool.KodoClientPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sparkeries.constant.Constants.AVATAR_STORAGE_PATH;
import static com.sparkeries.constant.Constants.KODO_SIZE_THRESHOLD;
import static com.sparkeries.enums.StorageTypeEnum.KODO;
import static com.sparkeries.enums.StorageTypeEnum.LOCAL;
import static com.sparkeries.enums.VisibilityEnum.*;

/**
 * KODO 文件管理
 */
@Slf4j
public class KodoOssServiceImpl implements OssService {

    private final Map<VisibilityEnum, String> bucketName;
    private final MetadataMapper metadataMapper;
    private final Configuration config;
    private final KodoClientPool clientPool;

    public KodoOssServiceImpl(KodoClientPool clientPool, Map<VisibilityEnum, String> bucketName, MetadataMapper metadataMapper) {

        log.info("[初始化Kodo存储服务] 开始初始化，存储桶: {}", bucketName);
        this.bucketName = bucketName;
        this.clientPool = clientPool;
        this.metadataMapper = metadataMapper;
        this.config = new Configuration(Region.autoRegion());
        config.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        config.resumableUploadMaxConcurrentTaskCount = 20;
        log.info("[初始化Kodo存储服务] 初始化完成，存储桶: {}，分片上传并发数: {}", bucketName, 20);
    }

    /**
     * 清理临时文件
     *
     * @param tempFile 要清理的临时文件
     */
    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            try {
                Files.delete(tempFile.toPath());
            } catch (IOException e) {
                log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath(), e);
                throw new OssException("临时文件删除失败");
            }
        }
    }

    /**
     * 上传文件
     *
     * @param file 文件信息
     * @return 操作结果
     */
    @Override
    public boolean uploadFile(UploadFileDTO file) {
        String filename = file.getFileName();
        String folderPath = file.getFolderPath();
        String absolutePath = String.join("/",folderPath, filename);
        VisibilityEnum visibility = file.getVisibility();
        Auth client = null;
        try {
            log.debug("[上传文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[上传文件操作] 成功获取Kodo客户端连接，开始上传文件");

            String bucketName = getBucketName(visibility);
            String targetPath = getTargetPath(absolutePath, visibility, file.getUserId());
            String uploadToken = client.uploadToken(bucketName, targetPath);
            UploadManager uploadManager = new UploadManager(config);

            // 按照七牛云最佳实践：4MB作为分片上传阈值
            if (file.getSize() > KODO_SIZE_THRESHOLD) {
                // 大文件分片上传
                return uploadLargeFile(uploadManager, file, uploadToken, targetPath);
            } else {
                // 小文件直接上传
                return uploadSmallFile(uploadManager, file, uploadToken, targetPath);
            }
        } catch (Exception e) {
            log.warn("Kodo文件上传失败: {}", e.getMessage(), e);
            throw new OssException("KODO上传失败: " + e.getMessage());
        } finally {
            if (client != null) {
                log.debug("[上传文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 创建文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 文件夹可见性
     * @param userId 用户 ID
     * @return 操作结果
     */
    @Override
    public boolean createFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = String.join("/",folderPath, folderName);
        String targetPath = getTargetPath(absolutePath, visibility, userId);
        String bucketName = getBucketName(visibility);
        log.info("[创建文件夹操作] 开始创建文件夹: {}", targetPath);
        Auth client = null;
        try {
            log.debug("[创建文件夹操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[创建文件夹操作] 成功获取Kodo客户端连接，开始检查文件夹是否存在");
            if (isFolderExists(targetPath, bucketName)) {
                log.warn("[创建文件夹操作] 文件夹已存在: {}", targetPath);
                throw new OssException("文件夹已存在");
            }
            UploadManager uploadManager = new UploadManager(config);
            String uploadToken = client.uploadToken(bucketName, targetPath);

            byte[] emptyData = new byte[0];

            Response response = uploadManager.put(emptyData, targetPath, uploadToken);

            if (!response.isOK()) {
                log.warn("[创建文件夹操作] 文件夹创建失败: {}, 错误: {}", targetPath, response.error);
                throw new OssException("文件夹创建失败: " + response.error);
            }
            log.info("[创建文件夹操作] 文件夹创建成功: {}", targetPath);

        } catch (OssException e) {
            log.warn("[创建文件夹操作] 文件夹创建失败: {}, 错误: {}", targetPath, e.getMessage());
            throw new OssException("文件夹:" + "创建失败" + e.getMessage());
        } catch (QiniuException e) {
            if (e.code() == 612) {
                log.debug("[创建文件夹操作] 目录 {} 不存在", targetPath);
            }
            log.warn("[创建文件夹操作] 七牛云异常: {}, 错误码: {}", e.getMessage(), e.code());
        } catch (Exception e) {
            log.warn("[创建文件夹操作] 创建文件夹时发生未知异常: {}", e.getMessage(), e);
            throw new OssException("创建文件夹时发生未知异常");
        } finally {
            if (client != null) {
                log.debug("[创建文件夹操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
        return true;
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名称
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 操作结果
     */
    @Override
    public boolean deleteFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = String.join("/",folderPath, fileName);
        String bucketName = getBucketName(visibility);
        log.info("[删除文件操作] 开始删除文件: {}", absolutePath);
        Auth client = null;
        try {
            log.debug("[删除文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件操作] 成功获取Kodo客户端连接，开始删除文件");
            BucketManager bucketManager = new BucketManager(client, config);

            bucketManager.delete(bucketName, absolutePath);
            log.info("[删除文件操作] 文件删除成功: {}", absolutePath);
            return true;
        } catch (OssException | QiniuException e) {
            log.warn("[删除文件操作] KODO中删除文件失败: {}, 错误: {}", absolutePath,
                    e.getMessage());
            throw new OssException("KODO中删除文件失败");
        } catch (Exception e) {
            log.warn("[删除文件操作] 删除文件时发生未知异常: {}", e.getMessage(), e);
            throw new OssException("删除文件时发生未知异常");
        } finally {
            if (client != null) {
                log.debug("[删除文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 删除文件夹及其所有内容
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 操作结果
     */
    @Override
    public boolean deleteFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {

        String absolutePath = String.join("/",folderPath, folderName);
        String targetPath = getTargetPath(absolutePath, visibility, userId);
        String bucketName = getBucketName(visibility);
        log.info("[删除文件夹操作] 开始删除文件夹: {}", targetPath);
        Auth client = null;
        try {
            log.debug("[删除文件夹操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件夹操作] 成功获取Kodo客户端连接，开始删除文件夹");

            BucketManager bucketManager = new BucketManager(client, config);
            // 列出文件夹下的所有对象
            List<String> keysToDelete = listFolderObjects(targetPath, visibility);
            if (keysToDelete.isEmpty()) {
                log.info("文件夹 {} 下无内容", targetPath);
                return true;
            } else {
                log.info("文件夹 {} 下找到 {} 个对象，开始删除", targetPath, keysToDelete.size());
            }

            // 添加文件夹本身的占位对象（如果存在）
            keysToDelete.add(targetPath);

            // 批量删除

            BucketManager.BatchOperations batchOps = new BucketManager.BatchOperations();
            keysToDelete.forEach(key -> batchOps.addDeleteOp(bucketName, key));
            bucketManager.batch(batchOps);
            log.info("成功删除文件夹 {} 及其下所有对象，共 {} 个", targetPath, keysToDelete.size());

            return true;

        } catch (QiniuException e) {
            log.warn("[删除文件夹操作] 删除文件夹 {} 失败: {}", targetPath, e.getMessage());
            throw new OssException("删除文件夹失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("[删除文件夹操作] 删除文件夹时发生未知异常: {}", e.getMessage(), e);
            throw new OssException("删除文件夹时发生未知异常");
        } finally {
            if (client != null) {
                log.debug("[删除文件夹操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 生成文件下载链接
     *
     * @param fileName 文件名称
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 下载链接
     */
    @Override
    public String downLoad(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = String.join("/",folderPath, fileName);
        String targetPath = getTargetPath(absolutePath, visibility, userId);
        String bucketName = getBucketName(visibility);
        log.info("[下载文件操作] 开始生成文件:{}下载链接", targetPath);
        Auth client = null;
        try {
            log.debug("[下载文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[下载文件操作] 成功获取Kodo客户端连接，开始生成下载链接");
            // 对文件名进行 URL 编码
            URLCodec codec = new URLCodec(CharEncoding.UTF_8);
            String encodedFileName = codec.encode(fileName);
            String encodedAbsolutePath = URLEncoder.encode(targetPath, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            // 构造基础 URL
            BucketManager bucketManager = new BucketManager(client, config);
            String[] domains = bucketManager.domainList(bucketName);

            if (domains.length == 0) {
                throw new OssException("该bucket没有域名 请为该bucket绑定域名");
            }
            String baseUrl = domains[0] + "/" + encodedAbsolutePath + "?attname=" + encodedFileName;

            // 生成带签名的私有下载 URL，有效期 1 小时 (3600 秒)
            String downloadUrl = client.privateDownloadUrl(baseUrl);
            log.info("[下载文件操作] 生成下载 URL 成功: {}", downloadUrl);
            return downloadUrl;
        } catch (Exception e) {
            log.warn("[下载文件操作] 生成下载链接时发生异常: {}", e.getMessage(), e);
            throw new OssException("生成下载链接时发生异常");
        } finally {
            if (client != null) {
                log.debug("[下载文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 文件和文件夹信息列表
     */
    @Override
    public FilesAndFoldersVO listFileAndFolder(String folderName, String folderPath, VisibilityEnum visibility, Long userId) {

        String absolutePath = String.join("/",folderPath, folderName);

        log.info("[列出文件操作] 开始列出路径下的文件和文件夹: {}", absolutePath);


        List<FileInfoVO> fileInfos = metadataMapper.listFileByFolderPath(absolutePath, KODO, visibility, userId);

        Set<FolderInfoVO> folders = metadataMapper.listFolderNameByFolderPath(absolutePath, KODO, visibility, userId).stream().map(s -> new FolderInfoVO(s, folderPath)).collect(Collectors.toSet());
        folders.addAll(metadataMapper.listFolderPathByFolderName(folderPath, LOCAL, visibility).stream().map(s -> new FolderInfoVO(s.replace(folderPath, "").split("/")[0], folderPath)).collect(Collectors.toSet()));

        return new FilesAndFoldersVO(fileInfos, folders);
    }

    /**
     * 生成文件预览链接
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 预览链接
     */
    @Override
    public String previewFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = String.join("/",folderPath, fileName);
        String bucketName = getBucketName(visibility);
        String targetPath = getTargetPath(absolutePath, visibility, userId);
        log.info("[预览文件操作] 开始生成预览链接: {}", targetPath);
        Auth client = null;
        int expiryInSeconds = 300;
        try {
            log.debug("[预览文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[预览文件操作] 成功获取Kodo客户端连接，开始生成预览链接");
            String baseUrl = getBaseUrl(targetPath, client, bucketName);
            log.debug("[预览文件操作] 构建 base URL: {}", baseUrl);

            // 3. 生成带签名的下载 URL (预签名 URL)
            String signedUrl = client.privateDownloadUrl(baseUrl, expiryInSeconds);

            log.info("[预览文件操作] 成功为文件 '{}' 生成预签名 URL，有效期 {} 秒。", targetPath,
                    expiryInSeconds);
            return signedUrl;

        } catch (Exception e) {
            log.warn("[预览文件操作] 生成预签名URL失败: {}", e.getMessage(), e);
            throw new OssException("错误");
        } finally {
            if (client != null) {
                log.debug("[预览文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 移动文件
     *
     * @param fileName 文件名
     * @param sourceFolderPath 源文件夹路径
     * @param targetFolderPath 目标文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 操作结果
     */
    @Override
    public boolean moveFile(String fileName, String sourceFolderPath, String targetFolderPath, VisibilityEnum visibility, String userId) {
        String sourceAbsolutePath = String.join("/",sourceFolderPath, fileName);
        String targetAbsolutePath = String.join("/",targetFolderPath, fileName);
        String bucketName = getBucketName(visibility);
        String sourcePath = getTargetPath(sourceAbsolutePath, visibility, userId);
        String targetPath = getTargetPath(targetAbsolutePath, visibility, userId);
        log.info("[移动文件操作] 开始移动文件: {} -> {}", sourcePath, targetPath);
        Auth client = null;
        Response response;

        try {
            log.debug("[移动文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[移动文件操作] 成功获取Kodo客户端连接，开始移动文件");
            BucketManager bucketManager = new BucketManager(client, config);
            // 步骤 1: 复制文件到目标位置
            // copy(源存储空间, 源文件Key, 目标存储空间, 目标文件Key)
            // 如果在同一个存储空间内移动，源存储空间和目标存储空间相同
            response = bucketManager.copy(bucketName, sourcePath, bucketName,
                    targetPath);

            if (response.isOK()) {
                log.info("[移动文件操作] 成功复制 KODO 文件，从 [{}] 到 [{}].", sourcePath,
                        targetPath);

                // 步骤 2: 删除源文件
                // delete(存储空间, 文件Key)
                response = bucketManager.delete(bucketName, sourcePath);

                if (response.isOK()) {
                    log.info("[移动文件操作] 成功删除源 KODO 文件: [{}]", sourcePath);
                    log.info("[移动文件操作] 成功移动 KODO 文件，从 [{}] 到 [{}].",
                            sourcePath, targetPath);
                } else {
                    // 删除失败，记录错误信息
                    log.warn(
                            "[移动文件操作] KODO 删除源文件失败，源Key: [{}]. StatusCode: [{}], warnMessage: [{}].",
                            sourcePath, response.statusCode, response.getInfo());
                    // 注意：此时文件已复制到新位置，但源文件未删除，需要根据业务决定如何处理（例如记录待清理列表）
                }
            } else {
                // 复制失败，记录错误信息
                log.warn(
                        "[移动文件操作] KODO 复制文件失败，从 [{}] 到 [{}]. StatusCode: [{}], warnMessage: [{}].",
                        sourcePath, targetPath, response.statusCode,
                        response.getInfo());
            }
            return true;
        } catch (Exception e) {
            log.warn("[移动文件操作] 移动文件时发生异常: {}", e.getMessage(), e);
            throw new OssException("移动文件时发生异常");
        } finally {
            if (client != null) {
                log.debug("[移动文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 获取当前存储类型
     *
     * @return 存储类型枚举值（KODO）
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return KODO;
    }

    /**
     * 上传小文件（小于分片阈值）
     *
     * @param uploadManager 上传管理器
     * @param file 文件信息
     * @param uploadToken 上传令牌
     * @param targetPath 目标路径
     * @return 操作结果
     */
    private boolean uploadSmallFile(UploadManager uploadManager, UploadFileDTO file,
                                    String uploadToken, String targetPath) {
        try {
            long fileSize = file.getSize();
            log.info("使用直接上传: 文件路径={}", targetPath);

            Response response = uploadManager.put(file.getInputStream(), targetPath, uploadToken,
                    null, null);

            if (!response.isOK()) {
                log.warn("直接上传失败: 文件路径={}, 错误信息: {}", targetPath, response.error);
                throw new OssException("直接上传失败: " + response.error);
            }

            log.info("小文件直接上传完成: 文件路径={}, 大小: {} KB", targetPath, fileSize / 1024);
            return true;
        } catch (Exception e) {
            throw new OssException("小文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传大文件（大于分片阈值）
     *
     * @param uploadManager 上传管理器
     * @param file 文件信息
     * @param uploadToken 上传令牌
     * @param absolutePath 文件绝对路径
     * @return 操作结果
     */
    private boolean uploadLargeFile(UploadManager uploadManager, UploadFileDTO file,
                                    String uploadToken, String absolutePath) {
        File tempFile = null;
        try {
            long fileSize = file.getSize();
            log.info("使用分片上传: 文件大小={} MB, 文件路径={}", fileSize / (1024 * 1024),
                    absolutePath);

            // 将 MultipartFile 转为临时 File
            tempFile = File.createTempFile("qiniu-", ".tmp");
            try (var inputStream = file.getInputStream();
                 var outputStream = Files.newOutputStream(tempFile.toPath())) {
                inputStream.transferTo(outputStream);
            }

            // 分片上传，Kodo SDK会自动处理分片逻辑
            Response response = uploadManager.put(tempFile, absolutePath, uploadToken, null, null,
                    false);
            if (!response.isOK()) {
                log.warn("分片上传失败: 文件路径={}, 错误信息: {}", absolutePath, response.error);
                throw new OssException("分片上传失败: " + response.error);
            }

            log.info("大文件分片上传完成: 文件路径={}, 大小: {} MB", absolutePath,
                    fileSize / (1024 * 1024));
            return true;
        } catch (Exception e) {
            throw new OssException("大文件分片上传失败: " + e.getMessage());
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 检查文件夹是否存在
     *
     * @param folderPath 文件夹路径
     * @param bucketName 存储桶名称
     * @return 文件夹是否存在
     */
    public boolean isFolderExists(String folderPath, String bucketName) {

        log.debug("[检查文件夹存在性] 开始检查文件夹是否存在: {}", folderPath);

        Auth client = null;
        try {
            log.debug("[检查文件夹存在性] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[检查文件夹存在性] 成功获取Kodo客户端连接，开始检查文件夹");
            BucketManager bucketManager = new BucketManager(client, config);
            FileInfo stat = bucketManager.stat(bucketName, folderPath);

            log.debug("[检查文件夹存在性] 目录 {} 存在", folderPath);
            return true;
        } catch (QiniuException e) {
            // 612 表示对象不存在

            if (e.code() == 612) {
                log.debug("[检查文件夹存在性] 目录 {} 不存在", folderPath);
                return false;
            }
            log.warn("[检查文件夹存在性] 检查目录 {} 存在性失败: {}", folderPath, e.getMessage());
            throw new OssException("检查目录存在性失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("[检查文件夹存在性] 检查文件夹存在性时发生未知异常: {}", e.getMessage(), e);
            throw new OssException("检查文件夹存在性时发生未知异常");
        } finally {
            if (client != null) {
                log.debug("[检查文件夹存在性] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 列出文件夹下的所有对象
     *
     * @param folderPath 文件夹路径
     * @return 对象路径列表
     */
    private List<String> listFolderObjects(String folderPath, VisibilityEnum visibility) {
        log.debug("[列出文件夹对象] 开始列出文件夹下的所有对象: {}", folderPath);
        Auth client = null;
        String bucketName = getBucketName(visibility);
        try {
            log.debug("[列出文件夹对象] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[列出文件夹对象] 成功获取Kodo客户端连接，开始列出对象");
            BucketManager bucketManager = new BucketManager(client, config);
            List<String> keys = new ArrayList<>();
            String marker = null;
            final int limit = 1000;
            boolean hasMore = true;

            while (hasMore) {
                // 不使用分隔符，列出所有对象
                FileListing fileListing = bucketManager.listFiles(bucketName, folderPath, marker,
                        limit, null);
                for (FileInfo fileInfo : fileListing.items) {
                    keys.add(fileInfo.key);
                }
                marker = fileListing.marker;
                hasMore = marker != null && !marker.isEmpty();
            }
            log.debug("[列出文件夹对象] 成功列出文件夹下的所有对象: {}, 共 {} 个对象", folderPath,
                    keys.size());
            return keys;
        } catch (Exception e) {
            log.warn("[列出文件夹对象] 列出文件夹对象时发生异常: {}", e.getMessage(), e);
            throw new OssException("列出文件夹对象时发生异常");
        } finally {
            if (client != null) {
                log.debug("[列出文件夹对象] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 获取文件访问地址
     *
     * @param absolutePath 文件绝对路径
     * @param client Kodo 客户端
     * @param bucketName 存储桶名称
     * @return 文件访问地址
     */
    private String getBaseUrl(String absolutePath, Auth client, String bucketName)
            throws EncoderException, QiniuException {
        URLCodec codec = new URLCodec();
        String encode = codec.encode(absolutePath);
        BucketManager bucketManager = new BucketManager(client, config);
        String[] domains = bucketManager.domainList(bucketName);

        if (domains.length == 0) {
            throw new OssException("该bucket没有域名 请为该bucket绑定域名");
        }

        // 2. 构建不带签名的 base URL
        // 注意：为了提示浏览器在线预览，可以在 URL 中添加 response-content-disposition=inline 参数

        return String.format("%s/%s?response-content-disposition=inline", domains[0], encode);
    }

    /**
     * 获取目标路径
     *
     * @param absolutePath 绝对路径
     * @param visibility 访问权限
     * @param userId 用户ID
     * @return 目标路径
     */
    public String getTargetPath(String absolutePath, VisibilityEnum visibility, String userId) {

        if (absolutePath.startsWith("/")) {
            absolutePath = absolutePath.substring(1);
        }
        if (absolutePath.endsWith("/")) {
            absolutePath = absolutePath.substring(0, absolutePath.length() - 1);
        }

        if (visibility == PRIVATE) {
            return String.join("/", userId, absolutePath);
        } else if (visibility == PUBLIC) {
            return String.join("/", absolutePath);
        } else if (visibility == USER_INFO) {
            return String.join("/", AVATAR_STORAGE_PATH, absolutePath);
        }
        log.warn("错误操作");
        throw new OssException("错误操作");
    }

    /**
     * 获取桶名
     *
     * @param visibility 访问权限
     * @return 桶名
     */
    public String getBucketName(VisibilityEnum visibility) {
        if (visibility == PRIVATE) {
            return bucketName.get(PRIVATE);
        } else if (visibility == PUBLIC) {
            return bucketName.get(PUBLIC);
        } else if (visibility == USER_INFO) {
            return bucketName.get(USER_INFO);
        } else {
            log.warn("错误操作");
            throw new OssException("错误操作");
        }
    }

}
