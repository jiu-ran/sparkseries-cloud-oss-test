package com.sparkseries.module.oss.provider.service.impl.qiniu.oss;

import static com.sparkeries.constant.Constants.KODO_SIZE_THRESHOLD;
import static com.sparkeries.enums.StorageTypeEnum.COS;
import static com.sparkeries.enums.StorageTypeEnum.KODO;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.sparkseries.module.oss.file.dto.MultipartFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.oss.file.dao.FileMetadataMapper;
import com.sparkseries.module.oss.file.entity.FileMetadataEntity;
import com.sparkseries.module.oss.file.vo.FileInfoVO;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.oss.file.vo.FolderInfoVO;
import com.sparkseries.module.oss.provider.service.impl.qiniu.pool.KodoClientPool;
import com.sparkseries.module.oss.provider.service.base.oss.OssService;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.springframework.http.ResponseEntity;

/**
 * Kodo文件存储服务实现类 提供基于七牛云Kodo对象存储的文件操作服务，包括上传、下载、删除、重命名、移动、创建文件夹、列举文件等功能
 */
@Slf4j
public class KodoOssServiceImpl implements OssService {


    private final String bucketName;
    private final FileMetadataMapper fileMetadataMapper;
    /**
     * 七牛云存储配置 用于配置上传、下载等操作的区域、分片上传版本等
     */
    private final Configuration config;
    private final KodoClientPool clientPool;

    /**
     * 构造函数，初始化Kodo存储服务
     *
     * @param clientPool Kodo客户端连接池
     * @param bucketName 存储桶名称
     */
    public KodoOssServiceImpl(KodoClientPool clientPool, String bucketName, FileMetadataMapper fileMetadataMapper) {

        log.info("[初始化Kodo存储服务] 开始初始化，存储桶: {}", bucketName);
        this.bucketName = bucketName;
        this.clientPool = clientPool;
        this.fileMetadataMapper = fileMetadataMapper;
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
            }
        }
    }

    /**
     * 上传文件到Kodo存储
     *
     * @param file 要上传的文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean upload(MultipartFileDTO file) {
        String originalFilename = file.getFilename();
        String absolutePath = file.getAbsolutePath();

        Auth client = null;
        try {
            log.debug("[上传文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[上传文件操作] 成功获取Kodo客户端连接，开始上传文件");
            log.info("开始上传文件到Kodo: {}, 大小: {} bytes", originalFilename, file.getSize());

            String uploadToken = client.uploadToken(bucketName, absolutePath);
            UploadManager uploadManager = new UploadManager(config);

            // 按照七牛云最佳实践：4MB作为分片上传阈值
            if (file.getSize() > KODO_SIZE_THRESHOLD) {
                // 大文件分片上传
                return uploadLargeFile(uploadManager, file, uploadToken, absolutePath);
            } else {
                // 小文件直接上传
                return uploadSmallFile(uploadManager, file, uploadToken, absolutePath);
            }
        } catch (Exception e) {
            log.error("Kodo文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("KODO上传失败: " + e.getMessage());
        } finally {
            if (client != null) {
                log.debug("[上传文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 上传小文件（小于分片阈值）
     *
     * @param uploadManager 上传管理器
     * @param file          文件信息
     * @param uploadToken   上传令牌
     * @param absolutePath  文件绝对路径
     * @return 上传是否成功
     */
    private boolean uploadSmallFile(UploadManager uploadManager, MultipartFileDTO file,
            String uploadToken, String absolutePath) {
        try {
            long fileSize = file.getSize();
            log.info("使用直接上传: 文件大小={} KB, 文件路径={}", fileSize / 1024, absolutePath);

            Response response = uploadManager.put(file.getInputStream(), absolutePath, uploadToken,
                    null, null);
            if (!response.isOK()) {
                log.error("直接上传失败: 文件路径={}, 错误信息: {}", absolutePath, response.error);
                throw new BusinessException("直接上传失败: " + response.error);
            }

            log.info("小文件直接上传完成: 文件路径={}, 大小: {} KB", absolutePath, fileSize / 1024);
            return true;
        } catch (Exception e) {
            throw new BusinessException("小文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传大文件（大于分片阈值）
     *
     * @param uploadManager 上传管理器
     * @param file          文件信息
     * @param uploadToken   上传令牌
     * @param absolutePath  文件绝对路径
     * @return 上传是否成功
     */
    private boolean uploadLargeFile(UploadManager uploadManager, MultipartFileDTO file,
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
                log.error("分片上传失败: 文件路径={}, 错误信息: {}", absolutePath, response.error);
                throw new BusinessException("分片上传失败: " + response.error);
            }

            log.info("大文件分片上传完成: 文件路径={}, 大小: {} MB", absolutePath,
                    fileSize / (1024 * 1024));
            return true;
        } catch (Exception e) {
            throw new BusinessException("大文件分片上传失败: " + e.getMessage());
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 上传头像文件
     *
     * @param avatar 头像文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean uploadAvatar(MultipartFileDTO avatar) {

        log.info("开始头像上传到Kodo: 文件名='{}', 大小={} bytes, 目标路径='{}'",
                avatar.getFilename(), avatar.getSize(), avatar.getAbsolutePath());

        // 复用upload方法的完整逻辑（包括分片上传策略、元数据设置等）
        boolean result = upload(avatar);

        if (result) {
            log.info("头像上传到Kodo成功: 文件名='{}', 路径='{}'", avatar.getFilename(),
                    avatar.getAbsolutePath());
        }
        return result;
    }

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     * @return 创建是否成功
     */
    @Override
    public boolean createFolder(String path) {
        log.info("[创建文件夹操作] 开始创建文件夹: {}", path);
        Auth client = null;
        try {
            log.debug("[创建文件夹操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[创建文件夹操作] 成功获取Kodo客户端连接，开始检查文件夹是否存在");
            if (isFolderExists(path)) {
                log.warn("[创建文件夹操作] 文件夹已存在: {}", path);
                throw new BusinessException("文件夹已存在");
            }
            UploadManager uploadManager = new UploadManager(config);
            String uploadToken = client.uploadToken(bucketName, path);

            byte[] emptyData = new byte[0];

            Response response = uploadManager.put(emptyData, path, uploadToken);

            if (!response.isOK()) {
                throw new BusinessException("文件夹创建失败: " + response.error);
            }
            log.info("[创建文件夹操作] 文件夹创建成功: {}", path);

        } catch (BusinessException e) {
            log.error("[创建文件夹操作] 文件夹创建失败: {}, 错误: {}", path, e.getMessage());
            throw new BusinessException("文件夹:" + "创建失败" + e.getMessage());
        } catch (QiniuException e) {
            if (e.code() == 612) {
                log.debug("[创建文件夹操作] 目录 {} 不存在", path);
            }
            log.error("[创建文件夹操作] 七牛云异常: {}, 错误码: {}", e.getMessage(), e.code());
        } catch (Exception e) {
            log.error("[创建文件夹操作] 创建文件夹时发生未知异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[创建文件夹操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
        return true;
    }

    /**
     * 检查文件夹是否存在
     *
     * @param path 文件夹路径
     * @return 文件夹是否存在
     */
    public boolean isFolderExists(String path) {
        log.debug("[检查文件夹存在性] 开始检查文件夹是否存在: {}", path);
        Auth client = null;
        try {
            log.debug("[检查文件夹存在性] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[检查文件夹存在性] 成功获取Kodo客户端连接，开始检查文件夹");
            BucketManager bucketManager = new BucketManager(client, config);
            FileInfo stat = bucketManager.stat(bucketName, path);

            log.debug("[检查文件夹存在性] 目录 {} 存在", path);
            return true;
        } catch (QiniuException e) {
            // 612 表示对象不存在

            if (e.code() == 612) {
                log.debug("[检查文件夹存在性] 目录 {} 不存在", path);
                return false;
            }
            log.error("[检查文件夹存在性] 检查目录 {} 存在性失败: {}", path, e.getMessage());
            throw new RuntimeException("检查目录存在性失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[检查文件夹存在性] 检查文件夹存在性时发生未知异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[检查文件夹存在性] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 删除文件
     *
     * @param absolutePath 文件绝对路径
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFile(String absolutePath) {
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
        } catch (BusinessException | QiniuException e) {
            log.error("[删除文件操作] KODO中删除文件失败: {}, 错误: {}", absolutePath,
                    e.getMessage());
            throw new BusinessException("KODO中删除文件失败");
        } catch (Exception e) {
            log.error("[删除文件操作] 删除文件时发生未知异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
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
     * @param path 文件夹路径
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFolder(String path) {
        log.info("[删除文件夹操作] 开始删除文件夹: {}", path);
        Auth client = null;
        try {
            log.debug("[删除文件夹操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件夹操作] 成功获取Kodo客户端连接，开始删除文件夹");

            BucketManager bucketManager = new BucketManager(client, config);
            // 列出文件夹下的所有对象
            List<String> keysToDelete = listFolderObjects(path);
            if (keysToDelete.isEmpty()) {
                log.info("文件夹 {} 下无内容", path);
                return true;
            } else {
                log.info("文件夹 {} 下找到 {} 个对象，开始删除", path, keysToDelete.size());
            }

            // 添加文件夹本身的占位对象（如果存在）
            keysToDelete.add(path);

            // 批量删除

            BucketManager.BatchOperations batchOps = new BucketManager.BatchOperations();
            keysToDelete.forEach(key -> batchOps.addDeleteOp(bucketName, key));
            bucketManager.batch(batchOps);
            log.info("成功删除文件夹 {} 及其下所有对象，共 {} 个", path, keysToDelete.size());

            return true;

        } catch (QiniuException e) {
            log.error("[删除文件夹操作] 删除文件夹 {} 失败: {}", path, e.getMessage());
            throw new RuntimeException("删除文件夹失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[删除文件夹操作] 删除文件夹时发生未知异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[删除文件夹操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 列出文件夹下的所有对象
     *
     * @param folderPath 文件夹路径
     * @return 对象路径列表
     * @throws QiniuException 七牛云异常
     */
    private List<String> listFolderObjects(String folderPath) throws QiniuException {
        log.debug("[列出文件夹对象] 开始列出文件夹下的所有对象: {}", folderPath);
        Auth client = null;
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
            log.error("[列出文件夹对象] 列出文件夹对象时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[列出文件夹对象] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 生成文件下载链接
     *
     * @param absolutePath     文件绝对路径
     * @param downloadFileName 下载文件名
     * @return 下载链接
     */
    @Override
    public String downLoad(String absolutePath, String downloadFileName) {
        log.info("[下载文件操作] 开始生成下载链接: {}, 下载文件名: {}", absolutePath,
                downloadFileName);
        Auth client = null;
        try {
            log.debug("[下载文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[下载文件操作] 成功获取Kodo客户端连接，开始生成下载链接");
            // 对文件名进行 URL 编码
            URLCodec codec = new URLCodec(CharEncoding.UTF_8);
            String encodedFileName = codec.encode(downloadFileName);
            String encodedAbsolutePath = URLEncoder.encode(absolutePath, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            // 构造基础 URL
            BucketManager bucketManager = new BucketManager(client, config);
            String[] domains = bucketManager.domainList(bucketName);

            if (domains.length == 0) {
                throw new BusinessException("该bucket没有域名 请为该bucket绑定域名");
            }
            String baseUrl = domains[0] + "/" + encodedAbsolutePath + "?attname=" + encodedFileName;

            // 生成带签名的私有下载 URL，有效期 1 小时 (3600 秒)
            String downloadUrl = client.privateDownloadUrl(baseUrl);
            log.info("[下载文件操作] 生成下载 URL 成功: {}", downloadUrl);
            return downloadUrl;
        } catch (Exception e) {
            log.error("[下载文件操作] 生成下载链接时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[下载文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 下载本地文件（此方法已废弃，不实现）
     *
     * @param metadata 文件元数据
     * @return 始终返回null
     */
    @Override
    @Deprecated
    public ResponseEntity<?> downLocalFile(FileMetadataEntity metadata) {
        return null;
    }

    /**
     * 重命名文件
     *
     * @param id          文件ID
     * @param filename    原文件名
     * @param newFilename 新文件名
     * @param path        文件路径
     * @return 重命名是否成功
     */
    @Override
    public boolean rename(Long id, String filename, String newFilename, String path) {
        String sourceAbsolutePath = path + filename;
        String targetAbsolutePath = path + id + newFilename;
        log.info("[重命名文件操作] 开始重命名文件: {} -> {}", sourceAbsolutePath,
                targetAbsolutePath);
        Auth client = null;
        try {
            log.debug("[重命名文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[重命名文件操作] 成功获取Kodo客户端连接，开始重命名文件");
            log.info("尝试重命名 KODO 文件，从 [{}] 到 [{}].", sourceAbsolutePath,
                    targetAbsolutePath);

            BucketManager bucketManager = new BucketManager(client, config);
            Response response = bucketManager.rename(bucketName, sourceAbsolutePath,
                    targetAbsolutePath);
            if (response.isOK()) {
                log.info("[重命名文件操作] 成功重命名 KODO 文件，从 [{}] 到 [{}].",
                        sourceAbsolutePath, targetAbsolutePath);
            } else {
                // 重命名失败，记录错误信息
                log.error(
                        "[重命名文件操作] KODO 重命名文件失败，从 [{}] 到 [{}]. StatusCode: [{}], ErrorMessage: [{}].",
                        sourceAbsolutePath, targetAbsolutePath, response.statusCode,
                        response.getInfo());
            }
            return true;
        } catch (Exception e) {
            log.error("[重命名文件操作] 重命名文件时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[重命名文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param path 要列出的路径
     * @return 包含文件和文件夹信息的VO对象
     */
    @Override
    public FilesAndFoldersVO listFiles(String path) {
        log.info("[列出文件操作] 开始列出路径下的文件和文件夹: {}", path);
        List<FileInfoVO> files = fileMetadataMapper.listFileByPath(path,KODO);
        Set<FolderInfoVO> folders = fileMetadataMapper.listFolderByPath(path,KODO).stream()
                .map(s -> new FolderInfoVO(s.replace(path, "").split("/")[0], path)).collect(Collectors.toSet());
        fileMetadataMapper.listFolderNameByPath(path, COS).stream().map(s -> new FolderInfoVO(s, path)).forEach(folders::add);
        return new FilesAndFoldersVO(files, folders);
    }

    /**
     * 生成文件预览链接
     *
     * @param absolutePath 文件绝对路径
     * @return 预览链接
     */
    @Override
    public String previewFile(String absolutePath) {
        log.info("[预览文件操作] 开始生成预览链接: {}", absolutePath);
        Auth client = null;
        int expiryInSeconds = 300;
        try {
            log.debug("[预览文件操作] 从连接池获取Kodo客户端连接");
            client = clientPool.getClient();
            log.debug("[预览文件操作] 成功获取Kodo客户端连接，开始生成预览链接");
            String baseUrl = getBaseUrl(absolutePath, client);
            log.debug("[预览文件操作] 构建 base URL: {}", baseUrl);

            // 3. 生成带签名的下载 URL (预签名 URL)
            String signedUrl = client.privateDownloadUrl(baseUrl, expiryInSeconds);

            log.info("[预览文件操作] 成功为文件 '{}' 生成预签名 URL，有效期 {} 秒。", absolutePath,
                    expiryInSeconds);
            return signedUrl;

        } catch (Exception e) {
            log.error("[预览文件操作] 生成预签名URL失败: {}", e.getMessage(), e);
            throw new BusinessException("错误");
        } finally {
            if (client != null) {
                log.debug("[预览文件操作] 归还Kodo客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }

    }


    private String getBaseUrl(String absolutePath, Auth client)
            throws EncoderException, QiniuException {
        URLCodec codec = new URLCodec();
        String encode = codec.encode(absolutePath);
        BucketManager bucketManager = new BucketManager(client, config);
        String[] domains = bucketManager.domainList(bucketName);

        if (domains.length == 0) {
            throw new BusinessException("该bucket没有域名 请为该bucket绑定域名");
        }

        // 2. 构建不带签名的 base URL
        // 注意：为了提示浏览器在线预览，可以在 URL 中添加 response-content-disposition=inline 参数

        return String.format("%s/%s?response-content-disposition=inline", domains[0], encode);
    }

    /**
     * 预览本地文件（此方法已废弃，不实现）
     *
     * @param metadata 文件元数据
     * @return 始终返回null
     */
    @Override
    @Deprecated
    public ResponseEntity<?> previewLocalFile(FileMetadataEntity metadata) {
        return null;
    }

    /**
     * 预览本地头像（此方法已废弃，不实现）
     *
     * @param ath 头像路径
     * @return 始终返回null
     */
    @Deprecated
    @Override
    public ResponseEntity<?> previewLocalAvatar(String ath) {
        return null;
    }

    /**
     * 移动文件
     *
     * @param sourceAbsolutePath 源文件的绝对路径
     * @param targetAbsolutePath 目标文件的绝对路径
     * @return 移动是否成功
     */
    @Override
    public boolean moveFile(String sourceAbsolutePath, String targetAbsolutePath) {
        log.info("[移动文件操作] 开始移动文件: {} -> {}", sourceAbsolutePath, targetAbsolutePath);
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
            response = bucketManager.copy(bucketName, sourceAbsolutePath, bucketName,
                    targetAbsolutePath);

            if (response.isOK()) {
                log.info("[移动文件操作] 成功复制 KODO 文件，从 [{}] 到 [{}].", sourceAbsolutePath,
                        targetAbsolutePath);

                // 步骤 2: 删除源文件
                // delete(存储空间, 文件Key)
                response = bucketManager.delete(bucketName, sourceAbsolutePath);

                if (response.isOK()) {
                    log.info("[移动文件操作] 成功删除源 KODO 文件: [{}]", sourceAbsolutePath);
                    log.info("[移动文件操作] 成功移动 KODO 文件，从 [{}] 到 [{}].",
                            sourceAbsolutePath, targetAbsolutePath);
                } else {
                    // 删除失败，记录错误信息
                    log.error(
                            "[移动文件操作] KODO 删除源文件失败，源Key: [{}]. StatusCode: [{}], ErrorMessage: [{}].",
                            sourceAbsolutePath, response.statusCode, response.getInfo());
                    // 注意：此时文件已复制到新位置，但源文件未删除，需要根据业务决定如何处理（例如记录待清理列表）
                }
            } else {
                // 复制失败，记录错误信息
                log.error(
                        "[移动文件操作] KODO 复制文件失败，从 [{}] 到 [{}]. StatusCode: [{}], ErrorMessage: [{}].",
                        sourceAbsolutePath, targetAbsolutePath, response.statusCode,
                        response.getInfo());
            }
            return true;
        } catch (Exception e) {
            log.error("[移动文件操作] 移动文件时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
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

}
