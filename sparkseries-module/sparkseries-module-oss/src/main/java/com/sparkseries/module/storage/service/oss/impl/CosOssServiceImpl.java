package com.sparkseries.module.storage.service.oss.impl;

import static com.qcloud.cos.http.HttpMethodName.GET;
import static com.sparkseries.common.constant.Constants.COS_SIZE_THRESHOLD;
import static com.sparkseries.common.enums.StorageTypeEnum.COS;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.DeleteObjectsResult;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;
import com.sparkseries.common.dto.MultipartFileDTO;
import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.file.dao.FileMetadataMapper;
import com.sparkseries.module.file.entity.FileMetadataEntity;
import com.sparkseries.module.file.vo.FileInfoVO;
import com.sparkseries.module.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.file.vo.FolderInfoVO;
import com.sparkseries.module.storage.pool.CosClientPool;
import com.sparkseries.module.storage.service.oss.OssService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.net.URLCodec;
import org.springframework.http.ResponseEntity;

/**
 * COS文件存储服务实现
 */
@Slf4j
public class CosOssServiceImpl implements OssService {

    private final CosClientPool clientPool;

    private final String bucketName;

    private final FileMetadataMapper fileMetadataMapper;

    public CosOssServiceImpl(CosClientPool clientPool, String bucketName, FileMetadataMapper fileMetadataMapper) {

        log.info("[初始化COS服务] 开始初始化腾讯云COS存储服务");
        this.clientPool = clientPool;
        this.bucketName = bucketName;
        this.fileMetadataMapper = fileMetadataMapper;
        log.info("COS存储服务初始化完成 - 存储桶: {}, 连接池状态: {}",
                bucketName, clientPool != null ? "已配置" : "未配置");
        log.debug("COS存储服务实例类型: {}, 分片上传阈值: {} MB",
                this.getClass().getSimpleName(), COS_SIZE_THRESHOLD / (1024 * 1024));
        log.info("[初始化COS服务] 腾讯云COS存储服务初始化完成，存储桶: {}", bucketName);
    }

    /**
     * 上传文件到COS
     *
     * @param file 要上传的文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean upload(MultipartFileDTO file) {
        COSClient client = null;
        ExecutorService threadPool = null;
        TransferManager transferManager = null;
        long startTime = System.currentTimeMillis();

        log.info("COS 开始上传文件 - 文件名: {}, 大小: {} bytes, 路径: {}",
                file.getFilename(), file.getSize(), file.getAbsolutePath());

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
            long duration = System.currentTimeMillis() - startTime;
            log.error(
                    "COS 文件上传失败 - 文件名: {}, 大小: {} bytes, 路径: {}, 耗时: {} ms, 错误信息: {}",
                    file.getFilename(), file.getSize(), file.getAbsolutePath(), duration,
                    e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        } finally {
            // 确保资源正确释放
            closeResources(transferManager, threadPool, client);
        }
    }

    /**
     * 上传小文件
     *
     * @param client COS客户端
     * @param file   文件信息
     * @return 上传是否成功
     */
    private boolean uploadSmallFile(COSClient client, MultipartFileDTO file) {
        long startTime = System.currentTimeMillis();
        log.debug("COS 开始小文件上传 - 文件: {}, 大小: {} bytes", file.getAbsolutePath(),
                file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            client.putObject(bucketName, file.getAbsolutePath(), inputStream, null);

            long duration = System.currentTimeMillis() - startTime;
            log.info("COS 小文件上传成功 - 文件: {}, 大小: {} bytes, 耗时: {} ms",
                    file.getAbsolutePath(), file.getSize(), duration);
            return true;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("COS 小文件上传失败 - 文件: {}, 大小: {} bytes, 耗时: {} ms, 错误信息: {}",
                    file.getAbsolutePath(), file.getSize(), duration, e.getMessage(), e);
            throw new BusinessException("小文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传大文件（分片上传）
     *
     * @param file            文件信息
     * @param transferManager 传输管理器
     * @return 上传是否成功
     */
    private boolean uploadLargeFile(MultipartFileDTO file,
            TransferManager transferManager) {
        File tempFile = null;
        long startTime = System.currentTimeMillis();
        log.debug("COS 开始大文件分片上传 - 文件: {}, 大小: {} bytes", file.getAbsolutePath(),
                file.getSize());

        try {
            // 创建临时文件
            log.debug("创建临时文件用于分片上传");
            tempFile = createTempFile(file);
            log.debug("临时文件创建成功: {}", tempFile.getAbsolutePath());

            // 配置分片上传请求
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, file.getAbsolutePath(),
                    tempFile);

            // 执行上传并监控进度
            log.debug("开始执行分片上传");
            Upload upload = transferManager.upload(putRequest);
            monitorUploadProgress(upload, file);

            upload.waitForCompletion();

            long duration = System.currentTimeMillis() - startTime;
            log.info("COS 大文件分片上传成功 - 文件: {}, 大小: {} bytes, 耗时: {} ms",
                    file.getAbsolutePath(), file.getSize(), duration);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            log.error("COS 大文件上传被中断 - 文件: {}, 大小: {} bytes, 耗时: {} ms",
                    file.getAbsolutePath(), file.getSize(), duration);
            throw new BusinessException("大文件上传被中断");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error(
                    "COS 大文件分片上传失败 - 文件: {}, 大小: {} bytes, 耗时: {} ms, 错误信息: {}",
                    file.getAbsolutePath(), file.getSize(), duration, e.getMessage(), e);
            throw new BusinessException("大文件上传失败: " + e.getMessage());
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 创建临时文件
     *
     * @param file 文件信息
     * @return 临时文件
     * @throws IOException IO异常
     */
    private File createTempFile(MultipartFileDTO file) throws IOException {
        File tempFile = File.createTempFile("cos_upload_", "_" + file.getFilename());

        try (InputStream inputStream = file.getInputStream();
                FileOutputStream fos = new FileOutputStream(tempFile)) {
            inputStream.transferTo(fos);
        }

        return tempFile;
    }

    /**
     * 监控上传进度
     *
     * @param upload 上传对象
     * @param file   文件信息
     */
    private void monitorUploadProgress(Upload upload, MultipartFileDTO file) {
        upload.addProgressListener(progressEvent -> {
            if (progressEvent.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                log.info("上传完成: key={}", file.getAbsolutePath());
            } else if (progressEvent.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT) {
                log.error("上传失败: key={}", file.getAbsolutePath());
            }

            // 记录进度（可选）
            double percentage = (double) progressEvent.getBytesTransferred() / file.getSize() * 100;
            if (percentage % 10 == 0) { // 每10%记录一次
                log.debug("上传进度: {}% for key={}", percentage, file.getAbsolutePath());
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
            Thread t = new Thread(r, "cos-upload-thread");
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
     * @param threadPool      线程池
     * @param client          COS客户端
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
     * 上传头像文件
     *
     * @param avatar 头像文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean uploadAvatar(MultipartFileDTO avatar) {
        log.info("[头像上传操作] 开始上传头像到COS");
        log.info("开始头像上传到COS: 文件名='{}', 大小={} bytes, 目标路径='{}'",
                avatar.getFilename(), avatar.getSize(), avatar.getAbsolutePath());

        try {
            // 复用upload方法的完整逻辑（包括分片上传策略、元数据设置等）
            boolean result = upload(avatar);

            if (result) {
                log.info("头像上传到COS成功: 文件名='{}', 路径='{}'", avatar.getFilename(),
                        avatar.getAbsolutePath());
                log.info("[头像上传操作] 头像上传成功");
            } else {
                log.error("[头像上传操作] 头像上传失败");
            }

            return result;
        } catch (Exception e) {
            log.error("[头像上传操作] 头像上传时发生异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     * @return 创建是否成功
     */
    @Override
    public boolean createFolder(String path) {
        COSClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("COS 开始创建文件夹 - 路径: {}", path);

        try {
            log.debug("[创建文件夹操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[创建文件夹操作] 成功获取COS客户端连接，检查文件夹是否存在");

            // 检查文件夹是否已存在（可选）
            if (client.doesObjectExist(bucketName, path)) {
                log.warn("COS 文件夹已存在 - 路径: {}", path);
                return false;
            }

            log.debug("文件夹不存在，开始创建空对象表示文件夹");
            // 创建空对象表示文件夹
            byte[] emptyContent = new byte[0];
            PutObjectRequest request = new PutObjectRequest(bucketName, path,
                    new ByteArrayInputStream(emptyContent), null);
            PutObjectResult result = client.putObject(request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("COS 文件夹创建成功 - 路径: {}, eTag: {}, 耗时: {} ms", path, result.getETag(),
                    duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("COS 创建文件夹失败 - 路径: {}, 耗时: {} ms, 错误信息: {}", path, duration,
                    e.getMessage(), e);
            throw new BusinessException("创建文件夹失败: " + e.getMessage());
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
     * @param absolutePath 文件绝对路径
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFile(String absolutePath) {
        COSClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("COS 开始删除文件 - 路径: {}", absolutePath);

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

            long duration = System.currentTimeMillis() - startTime;
            log.info("COS 文件删除成功 - 路径: {}, 耗时: {} ms", absolutePath, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("COS 删除文件失败 - 路径: {}, 耗时: {} ms, 错误信息: {}", absolutePath,
                    duration, e.getMessage(), e);
            throw new BusinessException("删除文件失败: " + e.getMessage());
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
     * @param path 文件夹路径
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFolder(String path) {
        COSClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("COS 开始删除文件夹 - 路径: {}", path);

        try {
            log.debug("[删除文件夹操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件夹操作] 成功获取COS客户端连接，开始列出文件夹下的所有对象");

            // 列出所有对象
            List<String> keysToDelete = new ArrayList<>();
            ListObjectsRequest listRequest = new ListObjectsRequest();
            listRequest.setBucketName(bucketName);
            listRequest.setPrefix(path);
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
                log.warn("COS 文件夹下未找到对象 - 路径: {}", path);
                return false;
            }

            log.debug("找到 {} 个对象需要删除", keysToDelete.size());
            // 批量删除
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName);
            deleteRequest.setKeys(keysToDelete.stream()
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .toList());
            DeleteObjectsResult result = client.deleteObjects(deleteRequest);

            long duration = System.currentTimeMillis() - startTime;
            log.info("COS 文件夹及其内容删除成功 - 路径: {}, 删除对象数: {}, 耗时: {} ms",
                    path, result.getDeletedObjects().size(), duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("COS 删除文件夹失败 - 路径: {}, 耗时: {} ms, 错误信息: {}", path, duration,
                    e.getMessage(), e);
            throw new BusinessException("删除文件夹失败: " + e.getMessage());
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
     * @param absolutePath     文件绝对路径
     * @param downloadFileName 下载文件名
     * @return 下载链接
     */
    @Override
    public String downLoad(String absolutePath, String downloadFileName) {
        COSClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("COS 开始生成下载链接 - 文件路径: {}, 下载文件名: {}", absolutePath,
                downloadFileName);

        try {
            log.debug("[下载文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[下载文件操作] 成功获取COS客户端连接，开始生成预签名URL");

            // 设置签名过期时间
            Date expiration = new Date(System.currentTimeMillis() + 30 * 1000L);

            // 创建签名 URL 请求
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName,
                    absolutePath)
                    .withExpiration(expiration);

            // 设置强制下载并指定文件名（URL 编码）
            URLCodec codec = new URLCodec(CharEncoding.UTF_8);
            String encodedFileName = codec.encode(downloadFileName);
            log.debug("文件名编码: {} -> {}", downloadFileName, encodedFileName);
            request.addRequestParameter("response-content-disposition",
                    "attachment; filename=\"" + encodedFileName + "\"");

            URL url = client.generatePresignedUrl(request);
            String downloadUrl = url.toString();

            long duration = System.currentTimeMillis() - startTime;
            log.info(
                    "COS 生成下载链接成功 - 文件路径: {}, 下载文件名: {}, 链接有效期: 30秒, 耗时: {} ms",
                    absolutePath, downloadFileName, duration);
            log.debug("生成的下载链接: {}", downloadUrl);
            return downloadUrl;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error(
                    "COS 生成下载链接失败 - 文件路径: {}, 下载文件名: {}, 耗时: {} ms, 错误信息: {}",
                    absolutePath, downloadFileName, duration, e.getMessage(), e);
            throw new BusinessException("生成下载URL失败: " + e.getMessage());
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
     * 下载本地文件
     *
     * @param metadata 文件元数据
     * @return 响应实体
     */
    @Override
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
        COSClient client = null;
        long startTime = System.currentTimeMillis();
        String sourceAbsolutePath = path + filename;
        String targetAbsolutePath = path + id + newFilename;
        log.info("COS 开始重命名文件 - 原路径: {}, 新路径: {}", sourceAbsolutePath,
                targetAbsolutePath);

        try {
            log.debug("[重命名文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[重命名文件操作] 成功获取COS客户端连接，开始构建复制请求");

            ObjectMetadata metadata = new ObjectMetadata();
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName,
                    sourceAbsolutePath, bucketName,
                    targetAbsolutePath);
            // 如果需要，可以设置存储类型、访问权限等元数据
            metadata.addUserMetadata("originalFilename", newFilename);
            copyObjectRequest.setNewObjectMetadata(metadata);

            log.debug("开始复制文件到新路径");
            client.copyObject(copyObjectRequest);
            log.info("COS 文件复制成功 - 从 {} 到 {}", sourceAbsolutePath, targetAbsolutePath);

            // 步骤 2: 删除原文件
            log.debug("开始删除原文件");
            boolean deleteFile = deleteFile(sourceAbsolutePath);

            if (!deleteFile) {
                throw new BusinessException("文件重命名失败: 原文件删除失败");
            }

            log.info("COS 原文件删除成功: {}", sourceAbsolutePath);

            long duration = System.currentTimeMillis() - startTime;
            log.info("COS 文件重命名完成 - 原路径: {}, 新路径: {}, 耗时: {} ms", sourceAbsolutePath,
                    targetAbsolutePath, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("COS 文件重命名失败 - 原路径: {}, 新路径: {}, 耗时: {} ms, 错误信息: {}",
                    sourceAbsolutePath, targetAbsolutePath, duration, e.getMessage(), e);
            throw new BusinessException("文件重命名失败: " + e.getMessage());
        } finally {
            // 确保客户端归还到池中
            if (client != null) {
                try {
                    log.debug("[重命名文件操作] 归还COS客户端连接到连接池");
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
     * @param path 路径
     * @return 文件和文件夹信息
     */
    @Override
    public FilesAndFoldersVO listFiles(String path) {

        log.info("COS 开始列出文件和文件夹 - 路径: {}", path);

        List<FileInfoVO> fileInfos = fileMetadataMapper.listFileByPath(path,COS);

        Set<FolderInfoVO> folders = fileMetadataMapper.listFolderByPath(path,COS).stream()
                .map(s -> new FolderInfoVO(s.replace(path, "").split("/")[0], path)).collect(Collectors.toSet());
        fileMetadataMapper.listFolderNameByPath(path, COS).stream().map(s -> new FolderInfoVO(s, path)).forEach(folders::add);

        return new FilesAndFoldersVO(fileInfos, folders);
    }

    /**
     * 预览文件，生成预签名URL
     *
     * @param absolutePath 文件的绝对路径
     * @return 文件的预签名URL
     */
    @Override
    public String previewFile(String absolutePath) {
        COSClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("COS 开始生成文件预览URL - 路径: {}", absolutePath);

        try {
            log.debug("[预览文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[预览文件操作] 成功获取COS客户端连接，开始生成预签名URL");

            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucketName,
                    absolutePath, GET);
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

            long duration = System.currentTimeMillis() - startTime;
            log.info("COS 文件预览URL生成成功 - 路径: {}, 过期时间: {}, 耗时: {} ms", absolutePath,
                    expirationDate, duration);
            log.debug("生成的预览URL: {}", url.toString());

            return url.toString();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("COS 文件预览URL生成失败 - 路径: {}, 耗时: {} ms, 错误信息: {}", absolutePath,
                    duration, e.getMessage(), e);
            throw new BusinessException("获取对应url失败: " + e.getMessage());
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
     * 预览本地文件（此方法在COS存储服务中不实现，始终返回null）
     *
     * @param metadata 文件元数据实体
     * @return 始终返回null
     */
    @Override
    @Deprecated
    public ResponseEntity<?> previewLocalFile(FileMetadataEntity metadata) {
        return null;
    }

    /**
     * 预览本地头像（此方法在COS存储服务中不实现，始终返回null）
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
     * @param sourceAbsolutePath 源文件绝对路径
     * @param targetAbsolutePath 目标文件绝对路径
     * @return 移动是否成功
     */
    @Override
    public boolean moveFile(String sourceAbsolutePath, String targetAbsolutePath) {

        COSClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("COS 开始移动文件 - 源路径: {}, 目标路径: {}", sourceAbsolutePath,
                targetAbsolutePath);

        try {
            log.debug("[移动文件操作] 从连接池获取COS客户端连接");
            client = clientPool.getClient();
            log.debug("[移动文件操作] 成功获取COS客户端连接，开始构建复制请求");

            log.info("尝试移动 COS 文件，从 [{}] 到 [{}]. 存储桶: [{}].",
                    sourceAbsolutePath, targetAbsolutePath, bucketName);
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName,
                    sourceAbsolutePath, bucketName,
                    targetAbsolutePath);

            log.debug("开始复制文件到目标路径");
            client.copyObject(copyObjectRequest);
            log.info("成功复制 COS 文件，从 [{}] 到 [{}].", sourceAbsolutePath, targetAbsolutePath);

            // 步骤 2: 删除源文件
            log.debug("开始删除源文件");
            deleteFile(sourceAbsolutePath);
            log.info("成功删除源 COS 文件: [{}]", sourceAbsolutePath);

            long duration = System.currentTimeMillis() - startTime;
            log.info("COS 文件移动完成 - 源路径: {}, 目标路径: {}, 耗时: {} ms", sourceAbsolutePath,
                    targetAbsolutePath, duration);

            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("COS 文件移动失败 - 源路径: {}, 目标路径: {}, 耗时: {} ms, 错误信息: {}",
                    sourceAbsolutePath, targetAbsolutePath, duration, e.getMessage(), e);
            throw new BusinessException("文件移动失败: " + e.getMessage());
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
    public StorageTypeEnum getCurrStorageType() {
        return COS;
    }
}
