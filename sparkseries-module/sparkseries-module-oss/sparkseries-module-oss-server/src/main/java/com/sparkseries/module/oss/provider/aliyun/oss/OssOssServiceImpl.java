package com.sparkseries.module.oss.provider.aliyun.oss;


import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.sparkeries.dto.UploadFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.file.vo.FileInfoVO;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.oss.file.vo.FolderInfoVO;
import com.sparkseries.module.oss.provider.aliyun.pool.OssClientPool;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.sparkeries.constant.Constants.AVATAR_STORAGE_PATH;
import static com.sparkeries.constant.Constants.OSS_SIZE_THRESHOLD;
import static com.sparkeries.enums.StorageTypeEnum.LOCAL;
import static com.sparkeries.enums.StorageTypeEnum.OSS;
import static com.sparkeries.enums.VisibilityEnum.*;

/**
 *  OSS 文件管理
 */
@Slf4j
public class OssOssServiceImpl implements OssService {

    private final Map<VisibilityEnum, String> bucketName;

    private final OssClientPool clientPool;
    private final MetadataMapper metadataMapper;

    public OssOssServiceImpl(OssClientPool clientPool, Map<VisibilityEnum, String> bucketName, MetadataMapper metadataMapper) {

        log.info("[初始化OSS服务] 开始初始化阿里云OSS存储服务");
        int bucketCount = 3;
        if (bucketName.size() != bucketCount) {
            log.warn("[初始化OSS服务] 存储桶数量错误，请检查配置文件");
            throw new OssException("存储桶数量错误，请检查配置文件");
        }
        this.bucketName = bucketName;
        this.clientPool = clientPool;
        this.metadataMapper = metadataMapper;
        log.info("[初始化OSS服务] 阿里云OSS存储服务初始化完成，存储桶: {}", bucketName);
    }

    /**
     * 上传文件到 OSS
     *
     * @param file MultipartFile对象，包含要上传的文件数据
     * @return 如果文件上传成功，则返回true；否则返回false
     */
    @Override
    public boolean uploadFile(UploadFileDTO file) {
        String userId = file.getUserId();
        VisibilityEnum visibility = file.getVisibility();
        String fileName = file.getFileName();
        String folderPath = file.getFolderPath();
        String absolutePath = Path.of(folderPath, fileName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
        file.setTargetPath(targetPath);
        String currentBucket = getBucketName(visibility);
        log.info("[上传文件操作] 开始上传文件: {}, ", targetPath);

        boolean upload = upload(file, currentBucket);
        if (!upload) {
            log.warn("[上传文件操作] 文件上传失败: {}", targetPath);
            throw new OssException("文件上传失败");
        }
        log.info("[上传文件操作] 文件上传成功: {}", targetPath);
        return true;
    }

    /**
     * 在OSS中创建文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 可见性
     * @param userId 用户ID
     * @return 如果文件夹创建成功，则返回true；否则返回false
     */
    @Override
    public boolean createFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = Path.of(folderPath, folderName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
        log.info("[创建文件夹操作] 开始创建文件夹: {}", targetPath);
        OSS client = null;
        try {
            log.debug("[创建文件夹操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[创建文件夹操作] 成功获取OSS客户端连接，开始检查文件夹是否存在");
            String currentBucket = getBucketName(visibility);
            // 创建目录，实际上是上传一个大小为0的文件
            boolean exists = client.doesObjectExist(currentBucket, targetPath);
            if (exists) {
                log.warn("[创建文件夹操作] 文件夹已存在: {}", targetPath);
                throw new OssException("文件夹已存在 创建失败");
            }

            log.debug("[创建文件夹操作] 文件夹不存在，开始创建文件夹");
            PutObjectRequest putObjectRequest = new PutObjectRequest(currentBucket, targetPath, new ByteArrayInputStream(new byte[0]));
            client.putObject(putObjectRequest);
            log.info("[创建文件夹操作] 文件夹创建成功: {}", targetPath);

            return true;
        } catch (OssException e) {
            log.error("[创建文件夹操作] 业务异常: {}", e.getMessage());
            throw new OssException(e.getMessage());
        } catch (Exception e) {
            log.error("[创建文件夹操作] 创建文件夹时发生异常: {}", e.getMessage(), e);
            throw new OssException("OSS 创建目录失败");
        } finally {
            if (client != null) {
                log.debug("[创建文件夹操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 从OSS中删除文件
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 如果文件删除成功，则返回true；否则返回false
     */
    @Override
    public boolean deleteFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = Path.of(folderPath, fileName).toString();
        log.info("[删除文件操作] 开始删除文件: {}", absolutePath);
        OSS client = null;
        try {
            log.debug("[删除文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件操作] 成功获取OSS客户端连接，开始删除文件");
            String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
            // 删除文件
            client.deleteObject(getBucketName(visibility), targetPath);
            log.info("[删除文件操作] 文件删除成功: {}", targetPath);
            return true;
        } catch (Exception e) {
            log.error("[删除文件操作] 文件删除失败: {}, 错误: {}", absolutePath, e.getMessage(), e);
            throw new OssException("OSS中删除文件失败");
        } finally {
            if (client != null) {
                log.debug("[删除文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 从OSS中删除文件夹及其内容
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 如果文件夹删除成功，则返回true；否则返回false
     */
    @Override
    public boolean deleteFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = Path.of(folderPath, folderName).toString();
        log.info("[删除文件夹操作] 开始删除文件夹: {}", absolutePath);
        OSS client = null;
        try {
            log.debug("[删除文件夹操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件夹操作] 成功获取OSS客户端连接，开始删除文件夹");
            String nextMarker = null;
            ObjectListing objectListing;
            String currentBucket = getBucketName(visibility);
            String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(currentBucket).withPrefix(targetPath).withMarker(nextMarker);

                objectListing = client.listObjects(listObjectsRequest);
                if (!objectListing.getObjectSummaries().isEmpty()) {
                    List<String> keys = new ArrayList<>();
                    for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                        keys.add(s.getKey());
                    }
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(currentBucket).withKeys(keys).withEncodingType("url");
                    DeleteObjectsResult deleteObjectsResult = client.deleteObjects(deleteObjectsRequest);
                    List<String> deletedObjects = deleteObjectsResult.getDeletedObjects();
                    for (String obj : deletedObjects) {
                        URLDecoder.decode(obj, StandardCharsets.UTF_8);
                    }
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
            log.info("[删除文件夹操作] 文件夹删除成功: {}", absolutePath);
            return true;
        } catch (Exception e) {
            log.error("[删除文件夹操作] 文件夹删除失败: {}, 错误: {}", absolutePath, e.getMessage(), e);
            throw new OssException("文件夹删除失败");
        } finally {
            if (client != null) {
                log.debug("[删除文件夹操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }


    }

    /**
     * 生成文件的下载URL
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @return 生成的下载URL字符串
     */
    @Override
    public String downLoad(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = Path.of(folderPath, fileName).toString();
        log.info("[下载文件操作] 开始生成: {} 的下载链接", absolutePath);
        OSS client = null;
        String currentBucket = getBucketName(visibility);
        String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
        try {
            log.debug("[下载文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[下载文件操作] 成功获取OSS客户端连接，开始生成下载链接");
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(currentBucket, targetPath);
            request.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000L));

            String url = client.generatePresignedUrl(request).toString();
            log.info("[下载文件操作] 成功生成下载链接: {}", targetPath);
            return url;
        } catch (Exception e) {
            log.error("[下载文件操作] 生成下载链接失败: {}, 错误: {}", absolutePath, e.getMessage(), e);
            throw new OssException("获取url路径失败");
        } finally {
            if (client != null) {
                log.debug("[下载文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 包含文件和文件夹信息的FilesAndFoldersVO对象
     */
    @Override
    public FilesAndFoldersVO listFileAndFolder(String folderName, String folderPath, VisibilityEnum visibility, Long userId) {

        String absolutePath = Path.of(folderPath, folderName).toString();

        log.info("[列出文件操作] 开始列出路径下的文件和文件夹: {}", absolutePath);


        List<FileInfoVO> fileInfos = metadataMapper.listFileByFolderPath(absolutePath, OSS, visibility, userId);

        Set<FolderInfoVO> folders = metadataMapper.listFolderNameByFolderPath(absolutePath, StorageTypeEnum.OSS, visibility, userId).stream().map(s -> new FolderInfoVO(s, folderPath)).collect(Collectors.toSet());
        folders.addAll(metadataMapper.listFolderPathByFolderName(folderPath, LOCAL, visibility).stream().map(s -> new FolderInfoVO(s.replace(folderPath, "").split("/")[0], folderPath)).collect(Collectors.toSet()));

        return new FilesAndFoldersVO(fileInfos, folders);
    }

    /**
     * 生成文件的预览URL
     *
     * @param fileName 文件的绝对路径
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 生成的预览URL字符串
     */
    @Override
    public String previewFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        log.info("[预览文件操作] 开始生成文件预览链接: {}", fileName);
        OSS client = null;
        String bucketName = getBucketName(visibility);
        log.info("尝试获取阿里云 OSS 文件 [{}] 的预览URL. 存储空间: [{}].", fileName, bucketName);
        String absolutePath = Path.of(folderPath, fileName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
        try {
            int expiryInSeconds = 300;
            log.debug("[预览文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[预览文件操作] 成功获取OSS客户端连接，开始生成预签名URL");
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, targetPath, HttpMethod.GET);
            Date expiration = new Date(System.currentTimeMillis() + expiryInSeconds * 1000);
            request.setExpiration(expiration);
            // 设置响应头，提示浏览器在线预览
            ResponseHeaderOverrides headerOverrides = new ResponseHeaderOverrides();
            // 设置 Content-Disposition 为 inline，提示浏览器在线预览
            headerOverrides.setContentDisposition("inline");
            request.setResponseHeaders(headerOverrides);
            URL url = client.generatePresignedUrl(request);

            log.info("成功为文件 '{}' 生成预签名 URL，有效期 {} 秒。", targetPath, expiryInSeconds);
            log.info("[预览文件操作] 成功生成预览链接: {}", url.toString());
            return url.toString();

        } catch (Exception e) {
            log.error("[预览文件操作] 生成预览链接失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[预览文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 移动OSS中的文件
     *
     * @param fileName 文件名
     * @param sourceFolderPath 源文件夹路径
     * @param targetFolderPath 目标文件夹路径
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 如果文件移动成功，则返回true；否则返回false
     */

    @Override
    public boolean moveFile(String fileName, String sourceFolderPath, String targetFolderPath, VisibilityEnum visibility, String userId) {
        String bucketName = getBucketName(visibility);
        log.info("[移动文件操作] 开始移动文件，从 {} 到 {}", sourceFolderPath, targetFolderPath);
        log.info("尝试移动 OSS 文件，从 [{}] 到 [{}]. 存储空间: [{}].", sourceFolderPath, targetFolderPath, bucketName);
        String sourcePath = getTargetPath(sourceFolderPath, visibility, userId).toString();
        String targetPath = getTargetPath(targetFolderPath, visibility, userId).toString();
        OSS client = null;
        try {
            log.debug("[移动文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[移动文件操作] 成功获取OSS客户端连接，开始移动文件");
            // 步骤 1: 复制文件到目标位置
            // CopyObjectRequest(源存储空间名称, 源文件Key, 目标存储空间名称, 目标文件Key)
            // 如果在同一个存储空间内移动，源存储空间和目标存储空间相同
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, sourcePath, bucketName, targetPath);

            client.copyObject(copyObjectRequest);
            log.info("成功复制 OSS 文件，从 [{}] 到 [{}].", sourcePath, targetPath);
            log.debug("[移动文件操作] 文件复制成功，开始删除源文件");

            // 步骤 2: 删除源文件
            deleteFile(fileName, sourceFolderPath, visibility, userId);
            log.info("成功删除源 OSS 文件: [{}]", sourcePath);
            log.debug("[移动文件操作] 源文件删除成功");

            log.info("成功移动 OSS 文件，从 [{}] 到 [{}].", sourcePath, targetPath);
            log.info("[移动文件操作] 文件移动成功");
            return true;
        } catch (Exception e) {
            log.error("[移动文件操作] 移动文件时发生异常: {}", e.getMessage(), e);
            log.error("OSS 文件移动出现错误", e);
            throw new OssException("文件移动出现错误");
        } finally {
            if (client != null) {
                log.debug("[移动文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.OSS;
    }

    // --------------------------------私有方法--------------------------------

    private boolean upload(UploadFileDTO file, String currentBucket) {

        OSS client = null;
        try {
            log.debug("[上传文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[上传文件操作] 成功获取OSS客户端连接，开始创建文件元数据");

            if (file.getSize() < OSS_SIZE_THRESHOLD) {
                // 小文件直接上传
                return uploadSmallFile(client, file, currentBucket);
            } else {
                // 大文件分片上传
                return uploadLargeFile(client, file, currentBucket);
            }

        } catch (Exception e) {
            log.error("[上传文件操作] 文件上传失败: key={}, size={}, 错误: {}", file.getTargetPath(), file.getSize(), e.getMessage(), e);
            throw new OssException("文件上传失败: " + e.getMessage());
        } finally {
            if (client != null) {
                log.debug("[上传文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    private boolean uploadSmallFile(OSS client, UploadFileDTO file, String currentBucket) {
        try (InputStream inputStream = file.getInputStream()) {
            String targetPath = file.getTargetPath();
            client.putObject(currentBucket, targetPath, inputStream, null);
            log.info("小文件上传成功: key={}, size={}", targetPath, file.getSize());
            return true;
        } catch (Exception e) {
            log.warn("[上传文件操作] 小文件上传失败: key={}, size={}, 错误: {}", file.getTargetPath(), file.getSize(), e.getMessage(), e);
            throw new OssException("小文件上传失败: " + e.getMessage());
        }
    }

    private boolean uploadLargeFile(OSS client, UploadFileDTO file, String currentBucket) {
        try (InputStream inputStream = file.getInputStream()) {
            String targetPath = file.getTargetPath();
            Long fileSize = file.getSize();

            long partSize = calculatePartSize(fileSize);
            int partCount = (int) Math.ceil((double) fileSize / partSize);
            log.info("开始分片上传: 文件大小={}, 分片大小={}, 分片数量={}", fileSize, partSize, partCount);

            InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(currentBucket, targetPath);
            InitiateMultipartUploadResult initiateResult = client.initiateMultipartUpload(initiateRequest);
            String uploadId = initiateResult.getUploadId();

            int corePoolSize = Math.min(partCount, Runtime.getRuntime().availableProcessors());
            int maxPoolSize = corePoolSize * 2;
            BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(partCount);
            ThreadPoolExecutor executorService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, workQueue, new ThreadPoolExecutor.CallerRunsPolicy());

            List<CompletableFuture<PartETag>> futures = new ArrayList<>();
            AtomicInteger uploadedParts = new AtomicInteger(0);

            try (inputStream) {
                byte[] buffer = new byte[(int) partSize];
                for (int i = 0; i < partCount; i++) {
                    int partNumber = i + 1;
                    long offset = i * partSize;
                    int bytesRead = inputStream.read(buffer, 0, (int) Math.min(partSize, fileSize - offset));
                    if (bytesRead <= 0) {
                        break;
                    }

                    ByteArrayInputStream partStream = new ByteArrayInputStream(buffer, 0, bytesRead);
                    CompletableFuture<PartETag> future = CompletableFuture.supplyAsync(() -> {
                        UploadPartRequest uploadPartRequest = new UploadPartRequest(currentBucket, targetPath, uploadId, partNumber, partStream, bytesRead);
                        UploadPartResult uploadPartResult = client.uploadPart(uploadPartRequest);
                        log.info("分片 {} 上传成功，ETag: {}", partNumber, uploadPartResult.getETag());
                        uploadedParts.incrementAndGet();
                        try {
                            partStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return uploadPartResult.getPartETag();
                    }, executorService);
                    futures.add(future);
                }

                // 使用 new ArrayList<> 确保列表可变
                List<PartETag> partETags = new ArrayList<>(CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> futures.stream().map(CompletableFuture::join).toList()).join());

                if (partETags.size() != partCount) {
                    throw new IllegalStateException("分片上传不完整，期望 " + partCount + " 个分片，实际 " + partETags.size());
                }

                partETags.sort(Comparator.comparingInt(PartETag::getPartNumber));
                log.info("所有分片上传完成，总计 {} 个分片", uploadedParts.get());

                CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(currentBucket, targetPath, uploadId, partETags);
                client.completeMultipartUpload(completeRequest);
                log.info("文件上传完成: {}", targetPath);
            } catch (Exception e) {
                log.error("分片上传失败: {}", e.getMessage());
                client.abortMultipartUpload(new AbortMultipartUploadRequest(currentBucket, targetPath, uploadId));
                throw e;
            } finally {
                executorService.shutdown();
                if (!executorService.isTerminated()) {
                    executorService.shutdownNow();
                    log.warn("线程池未在 60 秒内关闭，强制终止");
                }
            }
            log.info("大文件上传成功: key={}, size={}", targetPath, file.getSize());
            return true;
        } catch (Exception e) {
            log.warn("[上传文件操作] 大文件上传失败: key={}, size={}, 错误: {}", file.getTargetPath(), file.getSize(), e.getMessage(), e);
            throw new OssException("大文件上传失败: " + e.getMessage());
        }
    }


    /**
     * 计算分片大小
     *
     * @param fileSize 文件大小
     * @return 分片大小
     */
    private long calculatePartSize(long fileSize) {
        long minPartSize = 5 * 1024 * 1024;
        long maxPartSize = 100 * 1024 * 1024;
        int maxParts = 10000;
        long idealPartSize = (long) Math.ceil((double) fileSize / maxParts);
        return Math.max(minPartSize, Math.min(maxPartSize, idealPartSize));
    }


    /**
     * 获取目标路径
     *
     * @param absolutePath 绝对路径
     * @param visibility 访问权限
     * @param userId 用户ID
     * @return 目标路径
     */
    public Path getTargetPath(String absolutePath, VisibilityEnum visibility, String userId) {
        if (visibility == PRIVATE) {
            return Path.of(userId, absolutePath);
        } else if (visibility == PUBLIC) {
            return Path.of(absolutePath);
        } else if (visibility == USER_AVATAR) {
            return Path.of(AVATAR_STORAGE_PATH, absolutePath);
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
        } else if (visibility == USER_AVATAR) {
            return bucketName.get(USER_AVATAR);
        } else {
            log.warn("错误操作");
            throw new OssException("错误操作");
        }
    }
}
