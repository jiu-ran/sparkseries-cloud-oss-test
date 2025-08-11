package com.sparkseries.module.provider.minio.service;

import static com.sparkeries.constant.Constants.MINIO_SIZE_THRESHOLD;
import static com.sparkeries.enums.StorageTypeEnum.COS;
import static com.sparkeries.enums.StorageTypeEnum.MINIO;

import com.sparkseries.common.dto.MultipartFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.file.dao.FileMetadataMapper;
import com.sparkseries.module.file.entity.FileMetadataEntity;
import com.sparkseries.module.file.vo.FileInfoVO;
import com.sparkseries.module.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.file.vo.FolderInfoVO;
import com.sparkseries.module.provider.minio.pool.MinioClientPool;
import com.sparkseries.module.storage.service.oss.OssService;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.net.URLCodec;
import org.springframework.http.ResponseEntity;

/**
 * Minio文件存储服务实现类
 */
@Slf4j
public class MinioOssServiceImpl implements OssService {

    public final MinioClientPool clientPool;
    public final String bucketName;
    private final FileMetadataMapper fileMetadataMapper;

    /**
     * 构造函数
     *
     * @param clientPool Minio客户端连接池
     * @param bucketName 存储桶名称
     */
    public MinioOssServiceImpl(MinioClientPool clientPool, String bucketName, FileMetadataMapper fileMetadataMapper) {

        log.info("[初始化Minio服务] 开始初始化Minio存储服务");
        this.clientPool = clientPool;
        this.bucketName = bucketName;
        this.fileMetadataMapper = fileMetadataMapper;
        log.debug("Minio客户端连接池实例: {}", clientPool.getClass().getSimpleName());
        log.info("[初始化Minio服务] Minio存储服务初始化完成，存储桶: {}", bucketName);
    }

    /**
     * 上传文件到Minio
     *
     * @param fileInfo 文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean upload(MultipartFileDTO fileInfo) {
        log.info("[上传文件操作] 开始上传文件到Minio: {}, 文件名: '{}', 大小: {} bytes, 类型: '{}'",
                fileInfo.getAbsolutePath(), fileInfo.getFilename(), fileInfo.getSize(),
                fileInfo.getType());
        MinioClient client = null;

        try {
            client = clientPool.getClient();

            if (fileInfo.getSize() < MINIO_SIZE_THRESHOLD) {
                // 小文件直接上传
                return uploadSmallFile(client, fileInfo);
            } else {
                // 大文件分片上传
                return uploadLargeFile(client, fileInfo);
            }
        } catch (Exception e) {
            log.error("[上传文件操作] 文件上传失败 - 目标路径: {}, 错误: {}",
                    fileInfo.getAbsolutePath(), e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }
    }


    /**
     * 上传小文件
     *
     * @param client   Minio客户端
     * @param fileInfo 文件信息
     * @return 上传是否成功
     */
    private boolean uploadSmallFile(MinioClient client, MultipartFileDTO fileInfo) {
        long startTime = System.currentTimeMillis();
        try (InputStream inputStream = fileInfo.getInputStream()) {
            log.debug("开始执行小文件上传操作: {}", fileInfo.getAbsolutePath());
            client.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileInfo.getAbsolutePath())
                            .stream(inputStream, fileInfo.getSize(), -1)
                            .contentType(fileInfo.getType())
                            .build());

            long duration = System.currentTimeMillis() - startTime;
            log.info("Minio 小文件上传成功: {}, 大小: {} bytes, 耗时: {} ms",
                    fileInfo.getAbsolutePath(), fileInfo.getSize(), duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Minio 小文件上传失败: {}, 大小: {} bytes, 耗时: {} ms, 错误信息: {}",
                    fileInfo.getAbsolutePath(), fileInfo.getSize(), duration, e.getMessage(), e);
            throw new BusinessException("小文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传大文件（分片上传）
     *
     * @param client   Minio客户端
     * @param fileInfo 文件信息
     * @return 上传是否成功
     */
    private boolean uploadLargeFile(MinioClient client, MultipartFileDTO fileInfo) {
        long startTime = System.currentTimeMillis();
        try (InputStream inputStream = fileInfo.getInputStream()) {
            // Minio会自动处理分片上传，设置合适的分片大小
            long partSize = Math.max(5 * 1024 * 1024L, fileInfo.getSize() / 10000);
            log.debug("开始执行大文件分片上传操作: {}, 计算分片大小: {} bytes",
                    fileInfo.getAbsolutePath(), partSize);

            client.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileInfo.getAbsolutePath())
                            .stream(inputStream, fileInfo.getSize(), partSize)
                            .contentType(fileInfo.getType()).build());

            long duration = System.currentTimeMillis() - startTime;
            log.info(
                    "Minio 大文件分片上传成功: {}, 大小: {} bytes, 分片大小: {} bytes, 耗时: {} ms",
                    fileInfo.getAbsolutePath(), fileInfo.getSize(), partSize, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Minio 大文件分片上传失败: {}, 大小: {} bytes, 耗时: {} ms, 错误信息: {}",
                    fileInfo.getAbsolutePath(), fileInfo.getSize(), duration, e.getMessage(), e);
            throw new BusinessException("大文件分片上传失败: " + e.getMessage());
        }
    }


    /**
     * 上传头像文件
     *
     * @param avatarInfo 头像文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean uploadAvatar(MultipartFileDTO avatarInfo) {
        log.info("[头像上传操作] 开始上传头像到Minio: 文件名='{}', 大小={} bytes, 目标路径='{}'",
                avatarInfo.getFilename(), avatarInfo.getSize(), avatarInfo.getAbsolutePath());

        try {
            // 复用upload方法的完整逻辑（包括分片上传策略、元数据设置等）
            boolean result = upload(avatarInfo);

            if (result) {
                log.info("[头像上传操作] 头像上传成功: 文件名='{}', 路径='{}'",
                        avatarInfo.getFilename(), avatarInfo.getAbsolutePath());
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
     * 在Minio中创建文件夹
     *
     * @param path 文件夹路径
     * @return 创建是否成功
     */
    @Override
    public boolean createFolder(String path) {
        log.info("[创建文件夹操作] 开始创建文件夹: {}", path);
        MinioClient client = null;
        long startTime = System.currentTimeMillis();
        try {
            client = clientPool.getClient();
            client.putObject(PutObjectArgs.builder().bucket(bucketName).object(path)
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1).build());
            long duration = System.currentTimeMillis() - startTime;
            log.info("[创建文件夹操作] 文件夹创建成功: {}, 耗时: {} ms", path, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[创建文件夹操作] 创建文件夹失败: {}, 耗时: {} ms, 错误: {}", path, duration,
                    e.getMessage(), e);
            throw new BusinessException("Minio 创建目录:" + path + "失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 从Minio中删除文件
     *
     * @param absolutePath 文件绝对路径
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFile(String absolutePath) {
        log.info("[删除文件操作] 开始删除文件: {}", absolutePath);
        MinioClient client = null;
        long startTime = System.currentTimeMillis();
        try {
            client = clientPool.getClient();
            client.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(absolutePath).build());
            long duration = System.currentTimeMillis() - startTime;
            log.info("[删除文件操作] 文件删除成功: {}, 耗时: {} ms", absolutePath, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[删除文件操作] 删除文件失败: {}, 耗时: {} ms, 错误: {}", absolutePath,
                    duration, e.getMessage(), e);
            throw new BusinessException(
                    "Minio 删除文件:" + absolutePath + "失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
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
        MinioClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("[删除文件夹操作] 开始删除目录: {}", path);
        try {
            client = clientPool.getClient();
            List<String> files = new ArrayList<>();

            // 列出所有以指定前缀开头的对象
            Iterable<Result<Item>> results = client.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(path).recursive(true)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                files.add(item.objectName());
            }

            log.debug("[删除文件夹操作] 发现目录 {} 下共有 {} 个对象需要删除", path, files.size());

            // 删除所有找到的对象
            int deletedCount = 0;
            for (String objectName : files) {
                client.removeObject(
                        RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
                deletedCount++;
                if (deletedCount % 10 == 0) {
                    log.debug("已删除 {}/{} 个对象", deletedCount, files.size());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[删除文件夹操作] 目录删除成功: {}, 共删除 {} 个对象, 耗时: {} ms", path,
                    deletedCount, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[删除文件夹操作] 删除目录失败: {}, 耗时: {} ms, 错误: {}", path, duration,
                    e.getMessage(), e);
            throw new BusinessException("Minio 删除目录:" + path + "失败: " + e.getMessage());
        } finally {
            if (client != null) {
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
        MinioClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("[下载文件操作] 开始获取下载链接 - 文件路径: {}, 下载文件名: {}", absolutePath,
                downloadFileName);
        try {
            log.debug("开始编码下载文件名: {}", downloadFileName);
            URLCodec codec = new URLCodec("UTF-8");
            String encodedFileName = codec.encode(downloadFileName);
            log.debug("文件名编码完成: {} -> {}", downloadFileName, encodedFileName);

            Map<String, String> reqParams = new HashMap<>(1);
            reqParams.put("response-content-disposition",
                    "attachment; filename=\"" + encodedFileName + "\"");

            client = clientPool.getClient();

            String url = client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder().bucket(bucketName).object(absolutePath)
                            .method(Method.GET).expiry(30, TimeUnit.MINUTES)
                            .extraQueryParams(reqParams).build());

            long duration = System.currentTimeMillis() - startTime;
            log.info(
                    "[下载文件操作] 获取下载链接成功 - 文件路径: {}, 链接有效期: 30分钟, 耗时: {} ms",
                    absolutePath, duration);
            log.debug("生成的下载链接: {}", url);
            return url;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[下载文件操作] 获取下载链接失败 - 文件路径: {}, 耗时: {} ms, 错误: {}",
                    absolutePath, duration, e.getMessage(), e);
            throw new BusinessException(
                    "Minio 获取" + absolutePath + "的下载链接失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }

    }

    /**
     * 下载本地文件（Minio服务不实现此方法）
     *
     * @param metadata 文件元数据
     * @return 始终返回null
     */
    @Override
    public ResponseEntity<?> downLocalFile(FileMetadataEntity metadata) {
        // 本地文件下载逻辑，Minio服务不实现
        return null;
    }

    /**
     * 重命名Minio中的文件
     *
     * @param id          文件ID (在Minio中通常不直接使用，但可能用于构建新文件名)
     * @param oldFilename 旧文件名
     * @param newFilename 新文件名
     * @param path        文件所在路径
     * @return 重命名是否成功
     */
    @Override
    public boolean rename(Long id, String oldFilename, String newFilename, String path) {
        // Minio没有直接的重命名操作，通过复制到新名称再删除旧名称实现
        // 注意：这里使用id和newFilename构建新的objectName，
        // 这意味着重命名会改变文件的实际存储路径，如果只是想改变显示名称，
        // 应该更新元数据而不是移动文件。
        // 但根据当前实现，是进行文件移动。
        MinioClient client = null;
        String sourceAbsolutePath = path + oldFilename;
        String targetAbsolutePath = path + id + newFilename;
        long startTime = System.currentTimeMillis();
        log.info("[重命名文件操作] 开始重命名文件 - 原路径: {}, 新路径: {}", sourceAbsolutePath,
                targetAbsolutePath);
        try {
            client = clientPool.getClient();
            CopySource source = CopySource.builder().bucket(bucketName).object(sourceAbsolutePath)
                    .build();
            Map<String, String> metadata = new HashMap<>(1);
            metadata.put("original-filename", newFilename);
            // 构建复制目标信息
            CopyObjectArgs copyArgs = CopyObjectArgs.builder().bucket(bucketName)
                    .object(targetAbsolutePath).source(source).userMetadata(metadata).build();

            log.debug("[重命名文件操作] 开始复制对象 - 从 {} 到 {}", sourceAbsolutePath,
                    targetAbsolutePath);
            client.copyObject(copyArgs);
            log.debug("[重命名文件操作] 复制对象成功，开始删除原文件: {}", sourceAbsolutePath);
            deleteFile(sourceAbsolutePath);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[重命名文件操作] 文件重命名完成 - 原路径: {}, 新路径: {}, 耗时: {} ms",
                    sourceAbsolutePath, targetAbsolutePath, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error(
                    "[重命名文件操作] 文件重命名失败 - 原路径: {}, 新路径: {}, 耗时: {} ms, 错误: {}",
                    sourceAbsolutePath, targetAbsolutePath, duration, e.getMessage(), e);
            throw new BusinessException(
                    "Minio 文件:" + sourceAbsolutePath + "重命名失败: " + e.getMessage());
        } finally {
            if (client != null) {
                clientPool.returnClient(client);
            }
        }
    }

    /**
     * 列出Minio指定路径下的文件和文件夹
     *
     * @param path 要列出的路径
     * @return 包含文件和文件夹信息的VO对象
     */
    @Override
    public FilesAndFoldersVO listFiles(String path) {
        log.info("[列出文件操作] 开始获取目录列表 - 路径: {} (非递归)", path);

        List<FileInfoVO> fileInfos = fileMetadataMapper.listFileByPath(path,MINIO);

        Set<FolderInfoVO> folders = fileMetadataMapper.listFolderByPath(path,MINIO).stream()
                .map(s -> new FolderInfoVO(s.replace(path, "").split("/")[0], path)).collect(Collectors.toSet());
        fileMetadataMapper.listFolderNameByPath(path, COS).stream().map(s -> new FolderInfoVO(s, path)).forEach(folders::add);
        return new FilesAndFoldersVO(fileInfos, folders);
    }

    /**
     * 生成文件预览链接
     *
     * @param absolutePath 文件绝对路径
     * @return 预览链接
     */
    @Override
    public String previewFile(String absolutePath) {
        Map<String, String> extraQueryParams = new HashMap<>(1);
        // 设置 Content-Disposition 为 inline，提示浏览器在线预览
        extraQueryParams.put("response-content-disposition", "inline");
        MinioClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("[预览文件操作] 开始获取预览链接 - 文件路径: {}", absolutePath);
        try {
            client = clientPool.getClient();
            String url = client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder().bucket(bucketName).object(absolutePath)
                            .method(Method.GET).expiry(3, TimeUnit.MINUTES)
                            .extraQueryParams(extraQueryParams).build());

            long duration = System.currentTimeMillis() - startTime;
            log.info(
                    "[预览文件操作] 获取预览链接成功 - 文件路径: {}, 链接有效期: 3分钟, 耗时: {} ms",
                    absolutePath, duration);
            return url;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[预览文件操作] 获取预览链接失败 - 文件路径: {}, 耗时: {} ms, 错误: {}",
                    absolutePath, duration, e.getMessage(), e);
            throw new BusinessException("Minio 获取预览链接失败: " + e.getMessage());
        } finally {
            if (client != null) {
                log.debug("归还Minio客户端连接到连接池");
                clientPool.returnClient(client);
            }
        }
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
        // 本地文件预览逻辑，Minio服务不实现
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
     * 移动文件到新位置
     *
     * @param sourceAbsolutePath 源文件绝对路径
     * @param targetAbsolutePath 目标文件绝对路径
     * @return 移动是否成功
     */
    @Override
    public boolean moveFile(String sourceAbsolutePath, String targetAbsolutePath) {
        MinioClient client = null;
        long startTime = System.currentTimeMillis();
        log.info("[移动文件操作] 开始移动对象 - 从 {} 到 {}", sourceAbsolutePath,
                targetAbsolutePath);

        try {
            client = clientPool.getClient();

            CopySource source = CopySource.builder().bucket(bucketName).object(sourceAbsolutePath)
                    .build();

            CopyObjectArgs copyArgs = CopyObjectArgs.builder().bucket(bucketName)
                    .object(targetAbsolutePath).source(source)
                    // 如果需要复制源对象的元数据，这里不需要额外设置 metadataDirective，默认就是 COPY
                    // 如果需要修改元数据，可以使用 userMetadata() 和 metadataDirective("REPLACE")
                    .build();

            client.copyObject(copyArgs);

            // 步骤 2: 删除源位置的对象
            RemoveObjectArgs removeArgs = RemoveObjectArgs.builder().bucket(bucketName)
                    .object(sourceAbsolutePath).build();

            client.removeObject(removeArgs);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[移动文件操作] 对象移动完成 - 从 {} 到 {}, 耗时: {} ms", sourceAbsolutePath,
                    targetAbsolutePath, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[移动文件操作] 对象移动失败 - 从 {} 到 {}, 耗时: {} ms, 错误: {}",
                    sourceAbsolutePath, targetAbsolutePath, duration, e.getMessage(), e);
            throw new BusinessException("Minio 对象移动失败: " + e.getMessage());
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
    public StorageTypeEnum getCurrStorageType() {
        return MINIO;
    }
}
