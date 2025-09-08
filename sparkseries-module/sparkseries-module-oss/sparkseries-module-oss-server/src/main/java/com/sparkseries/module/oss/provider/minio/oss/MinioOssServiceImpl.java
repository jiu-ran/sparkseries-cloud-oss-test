package com.sparkseries.module.oss.provider.minio.oss;

import com.sparkeries.dto.UploadFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.file.vo.FileInfoVO;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.oss.file.vo.FolderInfoVO;
import com.sparkseries.module.oss.provider.minio.pool.MinioClientPool;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.net.URLCodec;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.sparkeries.constant.Constants.AVATAR_STORAGE_PATH;
import static com.sparkeries.constant.Constants.MINIO_SIZE_THRESHOLD;
import static com.sparkeries.enums.StorageTypeEnum.LOCAL;
import static com.sparkeries.enums.StorageTypeEnum.MINIO;
import static com.sparkeries.enums.VisibilityEnum.*;

/**
 * Minio文件存储服务实现类
 */
@Slf4j
public class MinioOssServiceImpl implements OssService {

    public final MinioClientPool clientPool;
    public final Map<VisibilityEnum, String> bucketName;
    private final MetadataMapper metadataMapper;

    /**
     * 构造函数
     *
     * @param clientPool Minio客户端连接池
     * @param bucketName 存储桶名称
     */
    public MinioOssServiceImpl(MinioClientPool clientPool, Map<VisibilityEnum, String> bucketName, MetadataMapper metadataMapper) {

        log.info("[初始化Minio服务] 开始初始化Minio存储服务");
        this.clientPool = clientPool;
        this.bucketName = bucketName;
        this.metadataMapper = metadataMapper;
        log.debug("Minio客户端连接池实例: {}", clientPool.getClass().getSimpleName());
        log.info("[初始化Minio服务] Minio存储服务初始化完成，存储桶: {}", bucketName);
    }

    /**
     * 上传文件到Minio
     *
     * @param file 文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean uploadFile(UploadFileDTO file) {
        String absolutePath = Path.of(file.getFolderPath(), file.getFileName()).toString();
        String targetPath = getTargetPath(absolutePath, file.getVisibility(), file.getUserId()).toString();
        log.info("[上传文件操作] 开始上传文件:{}到Minio", targetPath);
        MinioClient client = null;

        try {
            client = clientPool.getClient();

            if (file.getSize() < MINIO_SIZE_THRESHOLD) {
                // 小文件直接上传
                return uploadSmallFile(client, file);
            } else {
                // 大文件分片上传
                return uploadLargeFile(client, file);
            }
        } catch (Exception e) {
            log.error("[上传文件操作] 文件上传失败 - 目标路径: {}, 错误: {}", targetPath, e.getMessage(), e);
            throw new OssException("文件上传失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 在Minio中创建文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 文件夹可见性
     * @param userId 用户ID
     * @return 创建是否成功
     */
    @Override
    public boolean createFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {
        String bucketName = getBucketName(visibility);
        String absolutePath = Path.of(folderPath, folderName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
        log.info("[创建文件夹操作] 开始创建文件夹: {}", targetPath);
        MinioClient client = null;

        try {
            client = clientPool.getClient();
            client.putObject(PutObjectArgs.builder().bucket(bucketName).object(targetPath)
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1).build());

            log.info("[创建文件夹操作] 文件夹创建成功: {}", targetPath);
            return true;
        } catch (Exception e) {

            log.error("[创建文件夹操作] 创建文件夹失败: {},错误: {}", targetPath, e.getMessage(), e);
            throw new OssException("Minio 创建目录:" + targetPath + "失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 从Minio中删除文件
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        String bucketName = getBucketName(visibility);
        String absolutePath = Path.of(folderPath, fileName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
        log.info("[删除文件操作] 开始删除文件: {}", targetPath);
        MinioClient client = null;

        try {
            client = clientPool.getClient();
            client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(targetPath).build());

            log.info("[删除文件操作] 文件删除成功: {}", targetPath);
            return true;
        } catch (Exception e) {

            log.error("[删除文件操作] 删除文件失败: {} 错误: {}", targetPath, e.getMessage(), e);
            throw new OssException("Minio 删除文件:" + targetPath + "失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 删除文件夹及其内容
     *
     * @param folderName 文件夹名称
     * @param visibility 文件夹可见性
     * @param userId 用户 ID
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {
        MinioClient client = null;

        String bucketName = getBucketName(visibility);
        String absolutePath = Path.of(folderPath, folderName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, userId).toString();

        log.info("[删除文件夹操作] 开始删除目录: {}", targetPath);
        try {
            client = clientPool.getClient();
            List<String> files = new ArrayList<>();

            // 列出所有以指定前缀开头的对象
            Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(targetPath).recursive(true).build());

            for (Result<Item> result : results) {
                Item item = result.get();
                files.add(item.objectName());
            }

            log.debug("[删除文件夹操作] 发现目录 {} 下共有 {} 个对象需要删除", targetPath, files.size());

            // 删除所有找到的对象
            int deletedCount = 0;
            for (String objectName : files) {
                client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
                deletedCount++;
                if (deletedCount % 10 == 0) {
                    log.debug("已删除 {}/{} 个对象", deletedCount, files.size());
                }
            }


            log.info("[删除文件夹操作] 目录删除成功: {}, 共删除 {} 个对象", targetPath, deletedCount);
            return true;
        } catch (Exception e) {
            log.error("[删除文件夹操作] 删除目录失败: {}, 错误: {}", targetPath, e.getMessage(), e);
            throw new OssException("Minio 删除目录:" + targetPath + "失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 生成文件下载链接
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @return 下载链接
     */
    @Override
    public String downLoad(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        MinioClient client = null;
        String bucketName = getBucketName(visibility);
        String absolutePath = Path.of(folderPath, fileName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, userId).toString();
        log.info("[下载文件操作] 开始获取下载链接 - 文件路径: {}, 下载文件名: {}", targetPath, fileName);
        try {
            log.debug("开始编码下载文件名: {}", fileName);
            URLCodec codec = new URLCodec("UTF-8");
            String encodedFileName = codec.encode(fileName);
            log.debug("文件名编码完成: {} -> {}", fileName, encodedFileName);

            Map<String, String> reqParams = new HashMap<>(1);
            reqParams.put("response-content-disposition", "attachment; filename=\"" + encodedFileName + "\"");

            client = clientPool.getClient();

            String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucketName).object(targetPath).method(Method.GET).expiry(30, TimeUnit.MINUTES).extraQueryParams(reqParams).build());


            log.info("[下载文件操作] 获取下载链接成功 - 文件路径: {}, 链接有效期: 30分钟", targetPath);
            log.debug("生成的下载链接: {}", url);
            return url;
        } catch (Exception e) {

            log.error("[下载文件操作] 获取下载链接失败 - 文件路径: {}, 错误: {}", targetPath, e.getMessage(), e);
            throw new OssException("Minio 获取" + targetPath + "的下载链接失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 列出Minio指定路径下的文件和文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 文件可见性
     * @return 包含文件和文件夹信息的VO对象
     */
    @Override
    public FilesAndFoldersVO listFileAndFolder(String folderName, String folderPath, VisibilityEnum visibility, Long userId) {
        String absolutePath = Path.of(folderPath, folderName).toString();

        log.info("[列出文件操作] 开始列出路径下的文件和文件夹: {}", absolutePath);


        List<FileInfoVO> fileInfos = metadataMapper.listFileByFolderPath(absolutePath, MINIO, visibility, userId);

        Set<FolderInfoVO> folders = metadataMapper.listFolderNameByFolderPath(absolutePath, MINIO, visibility, userId).stream().map(s -> new FolderInfoVO(s, folderPath)).collect(Collectors.toSet());
        folders.addAll(metadataMapper.listFolderPathByFolderName(folderPath, LOCAL, visibility).stream().map(s -> new FolderInfoVO(s.replace(folderPath, "").split("/")[0], folderPath)).collect(Collectors.toSet()));

        return new FilesAndFoldersVO(fileInfos, folders);
    }

    /**
     * 生成文件预览链接
     *
     * @param fileName 文件绝对路径
     * @param folderPath
     * @param visibility
     * @param userId
     * @return 预览链接
     */
    @Override
    public String previewFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        String bucketName = getBucketName(visibility);
        Map<String, String> extraQueryParams = new HashMap<>(1);
        // 设置 Content-Disposition 为 inline，提示浏览器在线预览
        extraQueryParams.put("response-content-disposition", "inline");
        MinioClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("[预览文件操作] 开始获取预览链接 - 文件路径: {}", fileName);
        try {
            client = clientPool.getClient();
            String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucketName).object(fileName).method(Method.GET).expiry(3, TimeUnit.MINUTES).extraQueryParams(extraQueryParams).build());

            long duration = System.currentTimeMillis() - startTime;
            log.info("[预览文件操作] 获取预览链接成功 - 文件路径: {}, 链接有效期: 3分钟, 耗时: {} ms", fileName, duration);
            return url;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[预览文件操作] 获取预览链接失败 - 文件路径: {}, 耗时: {} ms, 错误: {}", fileName, duration, e.getMessage(), e);
            throw new OssException("Minio 获取预览链接失败: " + e.getMessage());
        } finally {
            if (client != null) {
                log.debug("归还Minio客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 移动文件到新位置
     *
     * @return 移动是否成功
     */
    @Override
    public boolean moveFile(String fileName, String sourceFolderPath, String targetFolderPath, VisibilityEnum visibility, String userId) {
        MinioClient client = null;
        String sourceAbsolutePath = Path.of(sourceFolderPath, fileName).toString();
        String targetAbsolutePath = Path.of(targetFolderPath, fileName).toString();
        String bucketName = getBucketName(visibility);
        String sourcePath = getTargetPath(sourceAbsolutePath, visibility, userId).toString();
        String targetPath = getTargetPath(targetAbsolutePath, visibility, userId).toString();

        log.info("[移动文件操作] 开始移动对象 - 从 {} 到 {}", sourcePath, targetPath);

        try {
            client = clientPool.getClient();

            CopySource source = CopySource.builder().bucket(bucketName).object(sourcePath).build();

            CopyObjectArgs copyArgs = CopyObjectArgs.builder().bucket(bucketName).object(targetPath).source(source)
                    // 如果需要复制源对象的元数据，这里不需要额外设置 metadataDirective，默认就是 COPY
                    // 如果需要修改元数据，可以使用 userMetadata() 和 metadataDirective("REPLACE")
                    .build();

            client.copyObject(copyArgs);

            // 步骤 2: 删除源位置的对象
            RemoveObjectArgs removeArgs = RemoveObjectArgs.builder().bucket(bucketName).object(sourcePath).build();

            client.removeObject(removeArgs);


            log.info("[移动文件操作] 对象移动完成 - 从 {} 到 {}", sourcePath, targetPath);
            return true;
        } catch (Exception e) {

            log.error("[移动文件操作] 对象移动失败 - 从 {} 到 {}, 错误: {}", sourcePath, targetPath, e.getMessage(), e);
            throw new OssException("Minio 对象移动失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 获取当前存储类型
     *
     * @return 存储类型枚举值（MINIO）
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return MINIO;
    }

    /**
     * 上传小文件
     *
     * @param client Minio客户端
     * @param fileInfo 文件信息
     * @return 上传是否成功
     */
    private boolean uploadSmallFile(MinioClient client, UploadFileDTO fileInfo) {
        VisibilityEnum visibility = fileInfo.getVisibility();
        String fileName = fileInfo.getFileName();
        String folderPath = fileInfo.getFolderPath();
        String bucketName = getBucketName(visibility);
        String absolutePath = Path.of(folderPath, fileName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, fileInfo.getUserId()).toString();

        try (InputStream inputStream = fileInfo.getInputStream()) {
            log.debug("开始执行小文件上传操作: {}", targetPath);
            client.putObject(PutObjectArgs.builder().bucket(bucketName).object(targetPath).stream(inputStream, fileInfo.getSize(), -1).build());


            log.info("Minio 小文件上传成功: {}, 大小: {} bytes", targetPath, fileInfo.getSize());
            return true;
        } catch (Exception e) {

            log.error("Minio 小文件上传失败: {}, 大小: {} bytes, 错误信息: {}", targetPath, fileInfo.getSize(), e.getMessage(), e);
            throw new OssException("小文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传大文件（分片上传）
     *
     * @param client Minio客户端
     * @param fileInfo 文件信息
     * @return 上传是否成功
     */
    private boolean uploadLargeFile(MinioClient client, UploadFileDTO fileInfo) {
        VisibilityEnum visibility = fileInfo.getVisibility();
        String fileName = fileInfo.getFileName();
        String folderPath = fileInfo.getFolderPath();
        String bucketName = getBucketName(visibility);
        String absolutePath = Path.of(folderPath, fileName).toString();
        String targetPath = getTargetPath(absolutePath, visibility, fileInfo.getUserId()).toString();
        long startTime = System.currentTimeMillis();
        try (InputStream inputStream = fileInfo.getInputStream()) {
            // Minio会自动处理分片上传，设置合适的分片大小
            long partSize = Math.max(5 * 1024 * 1024L, fileInfo.getSize() / 10000);
            log.debug("开始执行大文件分片上传操作: {}, 计算分片大小: {} bytes", targetPath, partSize);

            client.putObject(PutObjectArgs.builder().bucket(bucketName).object(targetPath).stream(inputStream, fileInfo.getSize(), partSize).build());

            long duration = System.currentTimeMillis() - startTime;
            log.info("Minio 大文件分片上传成功: {}, 大小: {} bytes, 分片大小: {} bytes, 耗时: {} ms", targetPath, fileInfo.getSize(), partSize, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Minio 大文件分片上传失败: {}, 大小: {} bytes, 耗时: {} ms, 错误信息: {}", targetPath, fileInfo.getSize(), duration, e.getMessage(), e);
            throw new OssException("大文件分片上传失败: " + e.getMessage());
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
