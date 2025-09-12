package com.sparkseries.module.oss.provider.tencent.oss;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.model.*;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;
import com.sparkeries.dto.UploadFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.file.vo.FileInfoVO;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.oss.file.vo.FolderInfoVO;
import com.sparkseries.module.oss.provider.tencent.pool.CosClientPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.net.URLCodec;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.qcloud.cos.http.HttpMethodName.GET;
import static com.sparkeries.constant.Constants.AVATAR_STORAGE_PATH;
import static com.sparkeries.constant.Constants.COS_SIZE_THRESHOLD;
import static com.sparkeries.enums.StorageTypeEnum.COS;
import static com.sparkeries.enums.StorageTypeEnum.LOCAL;
import static com.sparkeries.enums.VisibilityEnum.*;

/**
 * COS 文件管理
 */
@Slf4j
public class CosOssServiceImpl implements OssService {

    private final CosClientPool clientPool;

    private final Map<VisibilityEnum, String> bucketName;

    private final MetadataMapper metadataMapper;

    public CosOssServiceImpl(CosClientPool clientPool, Map<VisibilityEnum, String> bucketName, MetadataMapper metadataMapper) {

        log.info("[初始化COS服务] 开始初始化腾讯云COS存储服务");
        this.clientPool = clientPool;
        this.bucketName = bucketName;
        this.metadataMapper = metadataMapper;
        log.info("COS存储服务初始化完成 - 存储桶: {}, 连接池状态: {}",
                bucketName, clientPool != null ? "已配置" : "未配置");
        log.debug("COS存储服务实例类型: {}, 分片上传阈值: {} MB",
                this.getClass().getSimpleName(), COS_SIZE_THRESHOLD / (1024 * 1024));
        log.info("[初始化COS服务] 腾讯云COS存储服务初始化完成，存储桶: {}", bucketName);
    }

    /**
     * 上传文件
     *
     * @param file 文件信息
     * @return 操作结果
     */
    @Override
    public boolean uploadFile(UploadFileDTO file) {
        COSClient client = null;
        ExecutorService threadPool = null;
        TransferManager transferManager = null;

        VisibilityEnum visibility = file.getVisibility();
        String folderPath = file.getFolderPath();
        String fileName = file.getFileName();
        String absolutePath = String.join("/", folderPath, fileName);

        String targetPath = getTargetPath(absolutePath, visibility, file.getUserId());
        file.setTargetPath(targetPath);
        log.info("COS 开始上传文件 - 文件名: {}, 大小: {} bytes, 路径: {}",
                file.getFileName(), file.getSize(), targetPath);

        try {
            log.debug("[上传文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[上传文件操作] 成功获取COS客户端连接，开始创建对象元数据");

            if (file.getSize() < COS_SIZE_THRESHOLD) {
                log.debug("文件大小 {} bytes 小于阈值 {} bytes，使用小文件上传策略",
                        file.getSize(), COS_SIZE_THRESHOLD);
                return uploadSmallFile(client, file);
            } else {
                log.debug("文件大小 {} bytes 大于阈值 {} bytes，使用分片上传策略",
                        file.getSize(), COS_SIZE_THRESHOLD);
                threadPool = createThreadPool();
                transferManager = new TransferManager(client, threadPool);
                return uploadLargeFile(file, transferManager);
            }

        } catch (Exception e) {
            log.warn(
                    "COS 文件上传失败 - 文件名: {}, 大小: {} bytes, 路径: {}, 错误信息: {}",
                    file.getFileName(), file.getSize(), targetPath,
                    e.getMessage(), e);
            throw new OssException("文件上传失败: " + e.getMessage());
        } finally {
            // 确保资源正确释放
            closeResources(transferManager, threadPool, client);
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
        COSClient client = null;
        String absolutePath = String.join("/", folderPath, folderName);
        String targetPath = getTargetPath(absolutePath, visibility, userId);
        String bucketName = getBucketName(visibility);
        log.info("COS 开始创建文件夹 - 路径: {}", targetPath);

        try {
            log.debug("[创建文件夹操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[创建文件夹操作] 成功获取COS客户端连接，检查文件夹是否存在");

            // 检查文件夹是否已存在（可选）
            if (client.doesObjectExist(bucketName, targetPath)) {
                log.warn("COS 文件夹已存在 - 路径: {}", targetPath);
                return false;
            }

            log.debug("文件夹不存在，开始创建空对象表示文件夹");
            // 创建空对象表示文件夹
            byte[] emptyContent = new byte[0];
            PutObjectRequest request = new PutObjectRequest(bucketName, targetPath,
                    new ByteArrayInputStream(emptyContent), null);
            PutObjectResult result = client.putObject(request);


            log.info("COS 文件夹创建成功 - 路径: {}, eTag: {}", targetPath, result.getETag());
            return true;
        } catch (Exception e) {

            log.warn("COS 创建文件夹失败 - 路径: {}, 错误信息: {}", targetPath,
                    e.getMessage(), e);
            throw new OssException("创建文件夹失败: " + e.getMessage());
        } finally {
            // 确保客户端归还到池中
            if (client != null) {
                try {
                    log.debug("[创建文件夹操作] 归还COS客户端连接到连接池");
                    clientPool.returnClient(client);
                } catch (Exception e) {
                    log.warn("归还COSClient到池中失败", e);
                }
            }
        }

    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 操作结果
     */
    @Override
    public boolean deleteFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        COSClient client = null;
        String absolutePath = String.join("/", folderPath, fileName);
        String targetPath = getTargetPath(absolutePath, visibility, userId);
        String bucketName = getBucketName(visibility);
        log.info("COS 开始删除文件 - 路径: {}", targetPath);

        try {
            log.debug("[删除文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件操作] 成功获取COS客户端连接，检查文件是否存在");

            if (!client.doesObjectExist(bucketName, absolutePath)) {
                log.warn("COS 文件不存在 - 路径: {}", absolutePath);
                return false;
            }

            log.debug("文件存在，开始执行删除操作");
            // 删除文件
            client.deleteObject(bucketName, absolutePath);


            log.info("COS 文件删除成功 - 路径: {}", absolutePath);
            return true;
        } catch (Exception e) {

            log.warn("COS 删除文件失败 - 路径: {},错误信息: {}", absolutePath, e.getMessage(), e);
            throw new OssException("删除文件失败: " + e.getMessage());
        } finally {
            // 确保客户端归还到池中
            if (client != null) {
                try {
                    log.debug("[删除文件操作] 归还COS客户端连接到连接池");
                    clientPool.returnClient(client);
                } catch (Exception e) {
                    log.warn("归还COSClient到池中失败", e);
                }
            }
        }
    }

    /**
     * 删除文件夹及其内容
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {
        COSClient client = null;
        String absolutePath = String.join("/", folderPath, folderName);
        String bucketName = getBucketName(visibility);
        String targetPath = getTargetPath(absolutePath, visibility, userId);
        log.info("COS 开始删除文件夹 - 路径: {}", targetPath);

        try {
            log.debug("[删除文件夹操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件夹操作] 成功获取COS客户端连接，开始列出文件夹下的所有对象");

            // 列出所有对象
            List<String> keysToDelete = new ArrayList<>();
            ListObjectsRequest listRequest = new ListObjectsRequest();
            listRequest.setBucketName(bucketName);
            listRequest.setPrefix(targetPath);
            // 不设置 delimiter，确保列出所有子对象（包括嵌套文件）

            ObjectListing objectListing;
            do {
                objectListing = client.listObjects(listRequest);
                for (COSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    keysToDelete.add(objectSummary.getKey());
                }
                listRequest.setMarker(objectListing.getNextMarker());
            } while (objectListing.isTruncated());

            if (keysToDelete.isEmpty()) {
                log.warn("COS 文件夹下未找到对象 - 路径: {}", targetPath);
                return false;
            }

            log.debug("找到 {} 个对象需要删除", keysToDelete.size());
            // 批量删除
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName);
            deleteRequest.setKeys(keysToDelete.stream()
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .toList());
            DeleteObjectsResult result = client.deleteObjects(deleteRequest);


            log.info("COS 文件夹及其内容删除成功 - 路径: {}, 删除对象数: {}",
                    targetPath, result.getDeletedObjects().size());
            return true;
        } catch (Exception e) {

            log.warn("COS 删除文件夹失败 - 路径: {}, 错误信息: {}", targetPath,
                    e.getMessage(), e);
            throw new OssException("删除文件夹失败: " + e.getMessage());
        } finally {
            // 确保客户端归还到池中
            if (client != null) {
                try {
                    log.debug("[删除文件夹操作] 归还COS客户端连接到连接池");
                    clientPool.returnClient(client);
                } catch (Exception e) {
                    log.warn("归还COSClient到池中失败", e);
                }
            }
        }

    }

    /**
     * 生成文件下载链接
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param userId 用户 ID
     * @param visibility 文件可见性
     * @return 下载链接
     */
    @Override
    public String downLoad(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        COSClient client = null;
        String bucketName = getBucketName(visibility);
        String absolutePath = String.join("/", folderPath, fileName);
        String targetPath = getTargetPath(absolutePath, visibility, userId);
        log.info("COS 开始生成下载链接 - 文件路径: {}", targetPath);

        try {
            log.debug("[下载文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[下载文件操作] 成功获取COS客户端连接，开始生成预签名URL");

            // 设置签名过期时间
            Date expiration = new Date(System.currentTimeMillis() + 30 * 1000L);

            // 创建签名 URL 请求
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, targetPath)
                    .withExpiration(expiration);

            // 设置强制下载并指定文件名（URL 编码）
            URLCodec codec = new URLCodec(CharEncoding.UTF_8);
            String encodedFileName = codec.encode(fileName);
            log.debug("文件名编码: {} -> {}", fileName, encodedFileName);
            request.addRequestParameter("response-content-disposition",
                    "attachment; filename=\"" + encodedFileName + "\"");

            URL url = client.generatePresignedUrl(request);
            String downloadUrl = url.toString();

            log.info(
                    "COS 生成下载链接成功 - 文件路径: {}, 下载文件名: {}, 链接有效期: 30秒",
                    absolutePath, fileName);
            log.debug("生成的下载链接: {}", downloadUrl);
            return downloadUrl;
        } catch (Exception e) {

            log.warn(
                    "COS 生成下载链接失败 - 文件路径: {}, 下载文件名: {},错误信息: {}",
                    absolutePath, fileName, e.getMessage(), e);
            throw new OssException("生成下载URL失败: " + e.getMessage());
        } finally {
            // 确保客户端归还到池中
            if (client != null) {
                try {
                    log.debug("[下载文件操作] 归还COS客户端连接到连接池");
                    clientPool.returnClient(client);
                } catch (Exception e) {
                    log.warn("归还COSClient到池中失败", e);
                }
            }
        }
    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param userId 用户 ID
     * @param visibility 文件可见性
     * @return 文件和文件夹信息
     */
    @Override
    public FilesAndFoldersVO listFileAndFolder(String folderName, String folderPath, VisibilityEnum visibility, Long userId) {

        String absolutePath = String.join("/", folderPath, folderName);

        log.info("[列出文件操作] 开始列出路径下的文件和文件夹: {}", absolutePath);


        List<FileInfoVO> fileInfos = metadataMapper.listFileByFolderPath(absolutePath, COS, visibility, userId);

        Set<FolderInfoVO> folders = metadataMapper.listFolderNameByFolderPath(absolutePath, COS, visibility, userId).stream().map(s -> new FolderInfoVO(s, folderPath)).collect(Collectors.toSet());
        folders.addAll(metadataMapper.listFolderPathByFolderName(folderPath, LOCAL, visibility).stream().map(s -> new FolderInfoVO(s.replace(folderPath, "").split("/")[0], folderPath)).collect(Collectors.toSet()));

        return new FilesAndFoldersVO(fileInfos, folders);
    }

    /**
     * 生成文件的预览链接
     *
     * @param fileName 文件的绝对路径
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @param userId 用户 ID
     * @return 文件预览链接
     */
    @Override
    public String previewFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        COSClient client = null;

        log.info("COS 开始生成文件预览URL - 路径: {}", fileName);
        String bucketName = getBucketName(visibility);
        try {
            log.debug("[预览文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[预览文件操作] 成功获取COS客户端连接，开始生成预签名URL");

            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucketName, fileName, GET);
            // 设置过期时间
            long expirationTimeMillis = System.currentTimeMillis() + 3 * 60 * 1000;
            Date expirationDate = new Date(expirationTimeMillis);
            req.setExpiration(expirationDate);
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            responseHeaders.setContentDisposition("inline");
            req.setResponseHeaders(responseHeaders);

            log.debug("开始生成预签名URL，过期时间: {}", expirationDate);
            // 生成预签名URL
            URL url = client.generatePresignedUrl(req);


            log.info("COS 文件预览URL生成成功 - 路径: {}, 过期时间: {}", fileName,
                    expirationDate);
            log.debug("生成的预览URL: {}", url.toString());

            return url.toString();

        } catch (Exception e) {

            log.warn("COS 文件预览URL生成失败 - 路径: {},  错误信息: {}", fileName, e.getMessage(), e);
            throw new OssException("获取对应url失败: " + e.getMessage());
        } finally {
            if (client != null) {
                try {
                    log.debug("[预览文件操作] 归还COS客户端连接到连接池");
                    clientPool.returnClient(client);
                } catch (Exception e) {
                    log.warn("归还COSClient到池中失败", e);
                }
            }
        }

    }

    /**
     * 移动文件
     *
     * @param fileName 文件名
     * @param sourceFolderPath 源文件夹路径
     * @param targetFolderPath 目标文件夹路径
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 操作结果
     */
    @Override
    public boolean moveFile(String fileName, String sourceFolderPath, String targetFolderPath, VisibilityEnum visibility, String userId) {

        COSClient client = null;
        String sourceAbsolutePath = String.join("/", sourceFolderPath, fileName);
        String targetAbsolutePath = String.join("/", targetFolderPath, fileName);
        String sourcePath = getTargetPath(sourceAbsolutePath, visibility, userId);
        String targetPath = getTargetPath(targetAbsolutePath, visibility, userId);
        String bucketName = getBucketName(visibility);
        log.info("COS 开始移动文件 - 源路径: {}, 目标路径: {}", sourcePath,
                targetPath);

        try {
            log.debug("[移动文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[移动文件操作] 成功获取COS客户端连接，开始构建复制请求");

            log.info("尝试移动 COS 文件，从 [{}] 到 [{}]. 存储桶: [{}].",
                    sourcePath, targetPath, bucketName);
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName,
                    sourcePath, bucketName,
                    targetPath);

            log.debug("开始复制文件到目标路径");
            client.copyObject(copyObjectRequest);
            log.info("成功复制 COS 文件，从 [{}] 到 [{}].", sourcePath, targetPath);

            // 步骤 2: 删除源文件
            log.debug("开始删除源文件");
            deleteFile(fileName, sourceFolderPath, visibility, userId);
            log.info("成功删除源 COS 文件: [{}]", sourcePath);


            log.info("COS 文件移动完成 - 源路径: {}, 目标路径: {}", sourcePath,
                    targetPath);

            return true;
        } catch (Exception e) {

            log.warn("COS 文件移动失败 - 源路径: {}, 目标路径: {}, 错误信息: {}",
                    sourcePath, targetPath, e.getMessage(), e);
            throw new OssException("文件移动失败: " + e.getMessage());
        } finally {
            // 确保客户端归还到池中
            if (client != null) {
                try {
                    log.debug("[移动文件操作] 归还COS客户端连接到连接池");
                    clientPool.returnClient(client);
                } catch (Exception e) {
                    log.warn("归还COSClient到池中失败", e);
                }
            }
        }

    }

    /**
     * 获取当前存储类型
     *
     * @return 存储类型枚举
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return COS;
    }

    /**
     * 上传小文件
     *
     * @param client COS 客户端
     * @param file 文件信息
     * @return 上传是否成功
     */
    private boolean uploadSmallFile(COSClient client, UploadFileDTO file) {
        String bucketName = getBucketName(file.getVisibility());
        String absolutePath = String.join("/", file.getFolderPath(), file.getFileName());
        String targetPath = getTargetPath(absolutePath, file.getVisibility(), file.getUserId());
        log.debug("COS 开始小文件上传 - 文件: {}, 大小: {} bytes", targetPath,
                file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            client.putObject(bucketName, targetPath, inputStream, null);


            log.info("COS 小文件上传成功 - 文件: {}, 大小: {} bytes",
                    targetPath, file.getSize());
            return true;
        } catch (IOException e) {

            log.warn("COS 小文件上传失败 - 文件: {}, 大小: {} bytes,  错误信息: {}",
                    targetPath, file.getSize(), e.getMessage(), e);
            throw new OssException("小文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传大文件（分片上传）
     *
     * @param file 文件信息
     * @param transferManager 传输管理器
     * @return 操作结果
     */
    private boolean uploadLargeFile(UploadFileDTO file,
                                    TransferManager transferManager) {
        File tempFile = null;
        String absolutePath = String.join("/", file.getFolderPath(), file.getFileName());
        String bucketName = getBucketName(file.getVisibility());
        String targetPath = getTargetPath(absolutePath, file.getVisibility(), file.getUserId());
        log.debug("COS 开始大文件分片上传 - 文件: {}, 大小: {} bytes", targetPath,
                file.getSize());

        try {
            // 创建临时文件
            log.debug("创建临时文件用于分片上传");
            tempFile = createTempFile(file);
            log.debug("临时文件创建成功: {}", tempFile.getAbsolutePath());

            // 配置分片上传请求
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, targetPath,
                    tempFile);

            // 执行上传并监控进度
            log.debug("开始执行分片上传");
            Upload upload = transferManager.upload(putRequest);
            monitorUploadProgress(upload, file);

            upload.waitForCompletion();

            log.info("COS 大文件分片上传成功 - 文件: {}, 大小: {} bytes",
                    targetPath, file.getSize());
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            log.warn("COS 大文件上传被中断 - 文件: {}, 大小: {} bytes",
                    targetPath, file.getSize());
            throw new OssException("大文件上传被中断");
        } catch (Exception e) {

            log.warn(
                    "COS 大文件分片上传失败 - 文件: {}, 大小: {} bytes,错误信息: {}",
                    targetPath, file.getSize(), e.getMessage(), e);
            throw new OssException("大文件上传失败: " + e.getMessage());
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 创建临时文件
     *
     * @param file 文件信息
     * @return 临时文件
     */
    private File createTempFile(UploadFileDTO file) {
        File tempFile;

        try (InputStream inputStream = file.getInputStream()) {
            tempFile = File.createTempFile("cos_upload_", "_" + file.getFileName());
            FileOutputStream fos = new FileOutputStream(tempFile);
            inputStream.transferTo(fos);
        } catch (IOException e) {
            log.warn("创建临时文件失败", e);
            throw new OssException("创建临时文件失败");
        }

        return tempFile;
    }

    /**
     * 监控上传进度
     *
     * @param upload 上传对象
     * @param file 文件信息
     */
    private void monitorUploadProgress(Upload upload, UploadFileDTO file) {

        String fileName = file.getFileName();
        String folderPath = file.getFolderPath();
        String absolutePath = String.join("/", folderPath, fileName);
        String targetPath = getTargetPath(absolutePath, file.getVisibility(), file.getUserId());

        upload.addProgressListener(progressEvent -> {
            if (progressEvent.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                log.info("上传完成: key={}", targetPath);
            } else if (progressEvent.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT) {
                log.warn("上传失败: key={}", targetPath);
            }

            // 记录进度（可选）
            double percentage = (double) progressEvent.getBytesTransferred() / file.getSize() * 100;
            if (percentage % 10 == 0) { // 每10%记录一次
                log.debug("上传进度: {}% for key={}", percentage, targetPath);
            }
        });
    }

    /**
     * 创建线程池
     *
     * @return 线程池
     */
    private ExecutorService createThreadPool() {
        int corePoolSize = Math.min(10, Runtime.getRuntime().availableProcessors() * 2);
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(100);
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "cos-uploadFile-thread");
            t.setDaemon(true);
            return t;
        };
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler
        );
    }

    /**
     * 清理临时文件
     *
     * @param tempFile 临时文件
     */
    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            if (!tempFile.delete()) {
                log.warn("删除临时文件失败: {}", tempFile.getAbsolutePath());
                tempFile.deleteOnExit();
            }
        }
    }

    /**
     * 关闭资源
     *
     * @param transferManager 传输管理器
     * @param threadPool 线程池
     * @param client COS 客户端
     */
    private void closeResources(TransferManager transferManager, ExecutorService threadPool,
                                COSClient client) {
        // 关闭TransferManager
        if (transferManager != null) {
            try {
                log.debug("关闭TransferManager");
                transferManager.shutdownNow();
                log.debug("TransferManager关闭成功");
            } catch (Exception e) {
                log.warn("关闭TransferManager时出错", e);
            }
        }

        // 关闭线程池
        if (threadPool != null) {
            try {
                log.debug("关闭线程池");
                threadPool.shutdown();
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.debug("线程池未在5秒内关闭，强制关闭");
                    threadPool.shutdownNow();
                }
                log.debug("线程池关闭成功");
            } catch (InterruptedException e) {
                log.debug("线程池关闭被中断，强制关闭");
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 归还客户端
        if (client != null) {
            try {
                log.debug("[上传文件操作] 归还COS客户端连接到连接池");
                clientPool.returnClient(client);
                log.debug("COS客户端连接归还成功");
            } catch (Exception e) {
                log.warn("归还 COSClient 到池中失败", e);
            }
        }
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
