package com.sparkseries.module.storage.service.oss.impl;

import static com.sparkseries.common.constant.Constants.DATA_FORMAT;
import static com.sparkseries.common.constant.Constants.OSS_SIZE_THRESHOLD;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.sparkseries.common.dto.MultipartFileDTO;
import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.file.entity.FileMetadataEntity;
import com.sparkseries.module.file.vo.FileInfoVO;
import com.sparkseries.module.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.file.vo.FolderInfoVO;
import com.sparkseries.module.storage.pool.OssClientPool;
import com.sparkseries.module.storage.service.oss.OssService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

/**
 * 阿里云文件管理
 */
@Slf4j
public class OssOssServiceImpl implements OssService {

    private final String bucketName;

    private final OssClientPool clientPool;

    public OssOssServiceImpl(OssClientPool clientPool, String bucketName) {
        log.info("[初始化OSS服务] 开始初始化阿里云OSS存储服务");
        this.bucketName = bucketName;
        this.clientPool = clientPool;
        log.info("[初始化OSS服务] 阿里云OSS存储服务初始化完成，存储桶: {}", bucketName);
    }

    /**
     * 上传文件到OSS
     *
     * @param file MultipartFile对象，包含要上传的文件数据
     * @return 如果文件上传成功，则返回true；否则返回false
     */
    @Override
    public boolean upload(MultipartFileDTO file) {
        log.info("[上传文件操作] 开始上传文件: {}, 大小: {} bytes", file.getAbsolutePath(),
                file.getSize());
        OSS client = null;
        try {
            log.debug("[上传文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[上传文件操作] 成功获取OSS客户端连接，开始创建文件元数据");
            ObjectMetadata metadata = createMetadata(file);

            if (file.getSize() < OSS_SIZE_THRESHOLD) {
                // 小文件直接上传
                return uploadSmallFile(client, file, metadata);
            } else {
                // 大文件分片上传
                return uploadLargeFile(client, file, metadata);
            }

        } catch (Exception e) {
            log.error("[上传文件操作] 文件上传失败: key={}, size={}, 错误: {}",
                    file.getAbsolutePath(), file.getSize(), e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        } finally {
            if (client != null) {
                log.debug("[上传文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    private ObjectMetadata createMetadata(MultipartFileDTO file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getType());
        metadata.addUserMetadata("original-filename", file.getFilename());
        metadata.addUserMetadata("id", file.getId());
        metadata.addUserMetadata("size", file.getStrSize());
        return metadata;
    }

    private boolean uploadSmallFile(OSS client, MultipartFileDTO file, ObjectMetadata metadata) {
        try (InputStream inputStream = file.getInputStream()) {
            client.putObject(bucketName, file.getAbsolutePath(), inputStream, metadata);
            log.info("小文件上传成功: key={}, size={}", file.getAbsolutePath(), file.getSize());
            return true;
        } catch (Exception e) {
            throw new BusinessException("小文件上传失败: " + e.getMessage());
        }
    }

    private boolean uploadLargeFile(OSS client, MultipartFileDTO file, ObjectMetadata metadata) {
        try {
            uploadFileByMultipart(client, file.getInputStream(), file.getSize(), bucketName,
                    file.getAbsolutePath(), metadata);
            log.info("大文件上传成功: key={}, size={}", file.getAbsolutePath(), file.getSize());
            return true;
        } catch (Exception e) {
            throw new BusinessException("大文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean uploadAvatar(MultipartFileDTO avatar) {
        log.info("[上传头像操作] 开始头像上传到OSS: 文件名='{}', 大小={} bytes, 目标路径='{}'",
                avatar.getFilename(), avatar.getSize(), avatar.getAbsolutePath());

        // 复用upload方法的完整逻辑（包括分片上传策略、元数据设置等）
        boolean result = upload(avatar);

        if (result) {
            log.info("[上传头像操作] 头像上传到OSS成功: 文件名='{}', 路径='{}'",
                    avatar.getFilename(), avatar.getAbsolutePath());
        } else {
            log.error("[上传头像操作] 头像上传到OSS失败: 文件名='{}', 路径='{}'",
                    avatar.getFilename(), avatar.getAbsolutePath());
        }

        return result;
    }


    /**
     * 在OSS中创建文件夹
     *
     * @param path 要创建的文件夹的路径
     * @return 如果文件夹创建成功，则返回true；否则返回false
     */
    @Override
    public boolean createFolder(String path) {
        log.info("[创建文件夹操作] 开始创建文件夹: {}", path);
        OSS client = null;

        try {
            log.debug("[创建文件夹操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[创建文件夹操作] 成功获取OSS客户端连接，开始检查文件夹是否存在");

            // 创建目录，实际上是上传一个大小为0的文件。
            boolean exists = client.doesObjectExist(bucketName, path);
            if (exists) {
                log.warn("[创建文件夹操作] 文件夹已存在: {}", path);
                throw new BusinessException("文件夹已存在 创建失败");
            }

            log.debug("[创建文件夹操作] 文件夹不存在，开始创建文件夹");
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path,
                    new ByteArrayInputStream(new byte[0]));
            client.putObject(putObjectRequest);
            log.info("[创建文件夹操作] 文件夹创建成功: {}", path);

            return true;
        } catch (BusinessException e) {
            log.error("[创建文件夹操作] 业务异常: {}", e.getMessage());
            throw new BusinessException(e.getMessage());
        } catch (Exception e) {
            log.error("[创建文件夹操作] 创建文件夹时发生异常: {}", e.getMessage(), e);
            throw new BusinessException("OSS 创建目录失败");
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
     * @param absolutePath 要删除的文件的绝对路径
     * @return 如果文件删除成功，则返回true；否则返回false
     */
    @Override
    public boolean deleteFile(String absolutePath) {
        log.info("[删除文件操作] 开始删除文件: {}", absolutePath);
        OSS client = null;
        try {
            log.debug("[删除文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件操作] 成功获取OSS客户端连接，开始删除文件");

            // 删除文件
            client.deleteObject(bucketName, absolutePath);
            log.info("[删除文件操作] 文件删除成功: {}", absolutePath);
            return true;
        } catch (Exception e) {
            log.error("[删除文件操作] 文件删除失败: {}, 错误: {}", absolutePath, e.getMessage(), e);
            throw new BusinessException("OSS中删除文件失败");
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
     * @param path 要删除的文件夹的路径
     * @return 如果文件夹删除成功，则返回true；否则返回false
     */
    @Override
    public boolean deleteFolder(String path) {
        log.info("[删除文件夹操作] 开始删除文件夹: {}", path);
        OSS client = null;
        try {
            log.debug("[删除文件夹操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[删除文件夹操作] 成功获取OSS客户端连接，开始删除文件夹");
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(
                        bucketName).withPrefix(path).withMarker(nextMarker);

                objectListing = client.listObjects(listObjectsRequest);
                if (!objectListing.getObjectSummaries().isEmpty()) {
                    List<String> keys = new ArrayList<>();
                    for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                        keys.add(s.getKey());
                    }
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(
                            bucketName).withKeys(keys).withEncodingType("url");
                    DeleteObjectsResult deleteObjectsResult = client.deleteObjects(
                            deleteObjectsRequest);
                    List<String> deletedObjects = deleteObjectsResult.getDeletedObjects();
                    for (String obj : deletedObjects) {
                        URLDecoder.decode(obj, StandardCharsets.UTF_8);
                    }
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
            log.info("[删除文件夹操作] 文件夹删除成功: {}", path);
            return true;
        } catch (Exception e) {
            log.error("[删除文件夹操作] 文件夹删除失败: {}, 错误: {}", path, e.getMessage(), e);
            throw new BusinessException("文件夹删除失败");
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
     * @param absolutePath     文件的绝对路径
     * @param downloadFileName 下载时显示的文件名
     * @return 生成的下载URL字符串
     */
    @Override
    public String downLoad(String absolutePath, String downloadFileName) {
        log.info("[下载文件操作] 开始生成下载链接: {}, 下载文件名: {}", absolutePath,
                downloadFileName);
        OSS client = null;
        try {
            log.debug("[下载文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[下载文件操作] 成功获取OSS客户端连接，开始生成下载链接");
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName,
                    absolutePath);
            request.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000L));

            String encodedFileName = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 使用 RFC 5987 标准格式设置文件名
            String disposition = "attachment; filename*=UTF-8''" + encodedFileName;
            request.addQueryParameter("response-content-disposition", disposition);

            // 生成预签名 URL（自动包含签名)
            String url = client.generatePresignedUrl(request).toString();
            log.info("[下载文件操作] 成功生成下载链接: {}", absolutePath);
            return url;
        } catch (Exception e) {
            log.error("[下载文件操作] 生成下载链接失败: {}, 错误: {}", absolutePath, e.getMessage(),
                    e);
            throw new BusinessException("获取url路径失败");
        } finally {
            if (client != null) {
                log.debug("[下载文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 下载本地文件（此方法在OSS服务中不适用，返回null）
     *
     * @param metadata 文件的元数据实体
     * @return 始终返回null
     */
    @Override
    public ResponseEntity<?> downLocalFile(FileMetadataEntity metadata) {
        return null;
    }

    /**
     * 重命名OSS中的文件
     *
     * @param id          文件ID
     * @param filename    文件的当前名称
     * @param newFilename 文件的新名称
     * @param path        文件所在的路径
     * @return 如果文件重命名成功，则返回true；否则返回false
     */
    @Override
    public boolean rename(Long id, String filename, String newFilename, String path) {
        OSS client = null;
        String sourceAbsolutePath = path + id + filename;
        String targetAbsolutePath = path + id + newFilename;
        log.info("[重命名文件操作] 开始重命名文件: {} -> {}", sourceAbsolutePath,
                targetAbsolutePath);
        try {
            log.debug("[重命名文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[重命名文件操作] 成功获取OSS客户端连接，开始重命名文件");

            // 步骤 1: 复制文件到新名称
            // CopyObjectRequest(源存储空间名称, 源文件Key, 目标存储空间名称, 目标文件Key)
            // 重命名通常在同一个存储空间内进行
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.addUserMetadata("original-filename", newFilename);
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName,
                    sourceAbsolutePath, bucketName, targetAbsolutePath);
            copyObjectRequest.setNewObjectMetadata(metadata);

            client.copyObject(copyObjectRequest);
            log.info("[重命名文件操作] 成功复制文件，从 [{}] 到 [{}].", sourceAbsolutePath,
                    targetAbsolutePath);

            // 步骤 2: 删除源文件
            deleteFile(sourceAbsolutePath);
            log.info("[重命名文件操作] 成功删除源文件: [{}]", sourceAbsolutePath);

            log.info("[重命名文件操作] 文件重命名成功，从 [{}] 到 [{}].", sourceAbsolutePath,
                    targetAbsolutePath);

            return true;
        } catch (Exception e) {
            log.error("[重命名文件操作] 文件重命名失败: {} -> {}, 错误: {}", sourceAbsolutePath,
                    targetAbsolutePath, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[重命名文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param path 要列出内容的路径
     * @return 包含文件和文件夹信息的FilesAndFoldersVO对象
     */
    @Override
    public FilesAndFoldersVO listFiles(String path) {
        log.info("[列出文件操作] 开始列出路径下的文件和文件夹: {}", path);
        OSS client = null;
        try {
            log.debug("[列出文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[列出文件操作] 成功获取OSS客户端连接，开始列出文件");
            // 构造 ListObjectsRequest，设置 prefix(指定目录) 和 delimiter(指定分隔符)
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                    .withPrefix(path)
                    .withDelimiter("/");
            ObjectListing objectListing = client.listObjects(listObjectsRequest);
            // 获取目录下的文件
            List<FileInfoVO> files = new ArrayList<>();
            for (OSSObjectSummary file : objectListing.getObjectSummaries()) {
                if (file.getKey().endsWith("/")) {
                    continue; // 跳过目录
                }
                ObjectMetadata metadata = client.getObjectMetadata(bucketName, file.getKey());
                Map<String, String> userMetadata = metadata.getUserMetadata();

                String filename = userMetadata.get("original-filename");
                String id = userMetadata.get("id");
                String size = userMetadata.get("size");

                Date lastModified = metadata.getLastModified();
                SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT);
                String formattedDate = sdf.format(lastModified);

                files.add(new FileInfoVO(id, filename, size, formattedDate));
            }

            // 获取目录下的子目录（CommonPrefixes）
            List<String> folders = objectListing.getCommonPrefixes();
            ArrayList<FolderInfoVO> folderInfos = new ArrayList<>();
            for (String folder : folders) {
                folderInfos.add(new FolderInfoVO(folder));
            }
            log.info("[列出文件操作] 成功列出路径 {} 下的 {} 个文件和 {} 个文件夹", path,
                    files.size(), folderInfos.size());
            return new FilesAndFoldersVO(files, folderInfos);
        } catch (BusinessException e) {
            log.error("[列出文件操作] 业务异常，获取路径 {} 下的文件及文件夹失败: {}", path,
                    e.getMessage());
            throw new BusinessException("获取路径下的文件及文件夹失败");
        } catch (Exception e) {
            log.error("[列出文件操作] 列出文件时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                log.debug("[列出文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }


    /**
     * 生成文件的预览URL
     *
     * @param absolutePath 文件的绝对路径
     * @return 生成的预览URL字符串
     */
    @Override
    public String previewFile(String absolutePath) {
        log.info("[预览文件操作] 开始生成文件预览链接: {}", absolutePath);
        OSS client = null;
        log.info("尝试获取阿里云 OSS 文件 [{}] 的预览URL. 存储空间: [{}].",
                absolutePath, bucketName);

        try {
            int expiryInSeconds = 300;
            log.debug("[预览文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[预览文件操作] 成功获取OSS客户端连接，开始生成预签名URL");
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName,
                    absolutePath, HttpMethod.GET);
            Date expiration = new Date(System.currentTimeMillis() + expiryInSeconds * 1000);
            request.setExpiration(expiration);
            // 设置响应头，提示浏览器在线预览
            ResponseHeaderOverrides headerOverrides = new ResponseHeaderOverrides();
            // 设置 Content-Disposition 为 inline，提示浏览器在线预览
            headerOverrides.setContentDisposition("inline");
            request.setResponseHeaders(headerOverrides);
            URL url = client.generatePresignedUrl(request);

            log.info("成功为文件 '{}' 生成预签名 URL，有效期 {} 秒。", absolutePath, expiryInSeconds);
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
     * 预览本地文件（此方法在OSS服务中不适用，返回null）
     *
     * @param metadata 文件的元数据实体
     * @return 始终返回null
     */
    @Override
    @Deprecated
    public ResponseEntity<?> previewLocalFile(FileMetadataEntity metadata) {
        return null;
    }

    @Deprecated
    @Override
    public ResponseEntity<?> previewLocalAvatar(String ath) {
        return null;
    }

    /**
     * 移动OSS中的文件
     *
     * @param sourceAbsolutePath 源文件的绝对路径
     * @param targetAbsolutePath 目标文件的绝对路径
     * @return 如果文件移动成功，则返回true；否则返回false
     */

    @Override
    public boolean moveFile(String sourceAbsolutePath, String targetAbsolutePath) {
        log.info("[移动文件操作] 开始移动文件，从 {} 到 {}", sourceAbsolutePath, targetAbsolutePath);
        log.info("尝试移动 OSS 文件，从 [{}] 到 [{}]. 存储空间: [{}].",
                sourceAbsolutePath, targetAbsolutePath, bucketName);
        OSS client = null;
        try {
            log.debug("[移动文件操作] 从连接池获取OSS客户端连接");
            client = clientPool.getClient();
            log.debug("[移动文件操作] 成功获取OSS客户端连接，开始移动文件");
            // 步骤 1: 复制文件到目标位置
            // CopyObjectRequest(源存储空间名称, 源文件Key, 目标存储空间名称, 目标文件Key)
            // 如果在同一个存储空间内移动，源存储空间和目标存储空间相同
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName,
                    sourceAbsolutePath, bucketName, targetAbsolutePath);

            client.copyObject(copyObjectRequest);
            log.info("成功复制 OSS 文件，从 [{}] 到 [{}].", sourceAbsolutePath, targetAbsolutePath);
            log.debug("[移动文件操作] 文件复制成功，开始删除源文件");

            // 步骤 2: 删除源文件
            deleteFile(sourceAbsolutePath);
            log.info("成功删除源 OSS 文件: [{}]", sourceAbsolutePath);
            log.debug("[移动文件操作] 源文件删除成功");

            log.info("成功移动 OSS 文件，从 [{}] 到 [{}].", sourceAbsolutePath, targetAbsolutePath);
            log.info("[移动文件操作] 文件移动成功");
            return true;
        } catch (Exception e) {
            log.error("[移动文件操作] 移动文件时发生异常: {}", e.getMessage(), e);
            log.error("OSS 文件移动出现错误", e);
            throw new BusinessException("文件移动出现错误");
        } finally {
            if (client != null) {
                log.debug("[移动文件操作] 归还OSS客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    @Override
    public StorageTypeEnum getCurrStorageType() {
        return StorageTypeEnum.OSS;
    }


    /**
     * 分片上传文件到 OSS
     *
     * @param client      OSS 客户端
     * @param inputStream 文件输入流
     * @param fileSize    文件大小（字节）
     * @param bucketName  Bucket 名称
     * @param objectKey   对象键
     * @param metadata    元数据
     * @throws Exception 如果上传失败
     */
    public void uploadFileByMultipart(OSS client, InputStream inputStream, long fileSize,
            String bucketName,
            String objectKey, ObjectMetadata metadata) throws Exception {
        long partSize = calculatePartSize(fileSize);
        int partCount = (int) Math.ceil((double) fileSize / partSize);
        log.info("开始分片上传: 文件大小={}, 分片大小={}, 分片数量={}", fileSize, partSize,
                partCount);

        InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(
                bucketName, objectKey);
        initiateRequest.setObjectMetadata(metadata);
        InitiateMultipartUploadResult initiateResult = client.initiateMultipartUpload(
                initiateRequest);
        String uploadId = initiateResult.getUploadId();

        int corePoolSize = Math.min(partCount, Runtime.getRuntime().availableProcessors());
        int maxPoolSize = corePoolSize * 2;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(partCount);
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, workQueue,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        List<CompletableFuture<PartETag>> futures = new ArrayList<>();
        AtomicInteger uploadedParts = new AtomicInteger(0);

        try (inputStream) {
            byte[] buffer = new byte[(int) partSize];
            for (int i = 0; i < partCount; i++) {
                int partNumber = i + 1;
                long offset = i * partSize;
                int bytesRead = inputStream.read(buffer, 0,
                        (int) Math.min(partSize, fileSize - offset));
                if (bytesRead <= 0) {
                    break;
                }

                ByteArrayInputStream partStream = new ByteArrayInputStream(buffer, 0, bytesRead);
                CompletableFuture<PartETag> future = CompletableFuture.supplyAsync(() -> {
                    UploadPartRequest uploadPartRequest = new UploadPartRequest(bucketName,
                            objectKey, uploadId, partNumber, partStream, bytesRead);
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
            List<PartETag> partETags = new ArrayList<>(
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> futures.stream().map(CompletableFuture::join).toList())
                            .join());

            if (partETags.size() != partCount) {
                throw new IllegalStateException(
                        "分片上传不完整，期望 " + partCount + " 个分片，实际 " + partETags.size());
            }

            partETags.sort(Comparator.comparingInt(PartETag::getPartNumber));
            log.info("所有分片上传完成，总计 {} 个分片", uploadedParts.get());

            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                    bucketName, objectKey, uploadId, partETags);
            client.completeMultipartUpload(completeRequest);
            log.info("文件上传完成: {}", objectKey);
        } catch (Exception e) {
            log.error("分片上传失败: {}", e.getMessage());
            client.abortMultipartUpload(
                    new AbortMultipartUploadRequest(bucketName, objectKey, uploadId));
            throw e;
        } finally {
            executorService.shutdown();
            boolean b = executorService.awaitTermination(60, TimeUnit.SECONDS);
            if (!executorService.isTerminated()) {
                executorService.shutdownNow();
                log.warn("线程池未在 60 秒内关闭，强制终止");
            }
        }
    }

    private long calculatePartSize(long fileSize) {
        long minPartSize = 5 * 1024 * 1024;
        long maxPartSize = 100 * 1024 * 1024;
        int maxParts = 10000;
        long idealPartSize = (long) Math.ceil((double) fileSize / maxParts);
        return Math.max(minPartSize, Math.min(maxPartSize, idealPartSize));
    }

}
