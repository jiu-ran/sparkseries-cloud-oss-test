package com.sparkseries.module.oss.provider.local.oss;

import com.sparkeries.dto.UploadFileDTO;
import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.common.api.provider.service.OssService;
import com.sparkseries.module.oss.common.exception.OssException;
import com.sparkseries.module.oss.file.dao.MetadataMapper;
import com.sparkseries.module.oss.file.entity.FileMetadataEntity;
import com.sparkseries.module.oss.file.vo.FileInfoVO;
import com.sparkseries.module.oss.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.oss.file.vo.FolderInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sparkeries.constant.Constants.LOCAL_SIZE_THRESHOLD;
import static com.sparkeries.enums.StorageTypeEnum.LOCAL;

/**
 * 本地文件管理
 */
@Slf4j
@Service("local")
public class LocalOssServiceImpl implements OssService {

    /**
     * 头像路径
     */
    public final String avatarPath;
    /**
     * 公共文件路径
     */
    public final String publicPath;
    /**
     * 用户文件路径
     */
    public final String privatePath;

    public final MetadataMapper metadataMapper;


    public LocalOssServiceImpl(@Value("${Local.avatarPath}") String avatarPath,
                               @Value("${Local.publicPath}") String publicPath,
                               @Value("${Local.privatePath}") String privatePath,
                               MetadataMapper metadataMapper) {

        log.info("[初始化本地存储服务] 开始初始化本地文件存储服务");
        this.avatarPath = avatarPath;
        this.publicPath = publicPath;
        this.privatePath = privatePath;
        this.metadataMapper = metadataMapper;
        log.info("本地存储服务初始化成功");
    }

    /**
     * 上传文件
     *
     * @param file 要上传的文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean uploadFile(UploadFileDTO file) {
        String absolutePath = Path.of(file.getFolderPath(), file.getFileName()).toString();

        log.info("[上传文件操作] 开始上传文件到本地存储: {}", absolutePath);

        Path targetPath = getTargetPath(absolutePath, file.getVisibility(), file.getUserId());

        log.info("本地存储开始上传文件 - 文件名: {}, 大小: {}, 目标路径: {}", file.getFileName(), file.getSize(), targetPath);

        try {

            boolean result = upload(file, targetPath);

            log.info("本地存储文件上传完成 - 文件名: {}, 结果: {}", file.getFileName(), result ? "成功" : "失败");
            return result;

        } catch (Exception e) {

            log.warn("[上传文件操作] 文件上传失败: {}", e.getMessage(), e);
            log.warn("本地存储文件上传失败 - 文件: {}", targetPath);
            throw new OssException("文件本地存储失败");
        }
    }

    /**
     * 创建文件夹
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 文件夹能见度
     * @param userId 用户 ID
     * @return 创建是否成功
     */
    @Override
    public boolean createFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = Path.of(folderPath, folderName).toString();
        log.info("[创建文件夹操作] 开始创建文件夹: {}", absolutePath);
        Path targetPath = getTargetPath(absolutePath, visibility, userId);

        log.info("本地存储开始创建目录 - 路径: {}", absolutePath);

        try {
            log.debug("检查目录是否已存在");
            if (Files.exists(targetPath)) {
                log.info("目录已存在，无需创建: {}", targetPath);
                return true;
            }

            log.debug("开始创建目录（包含父目录）");
            Files.createDirectories(targetPath);

            log.info("本地存储目录创建成功 - 路径: {},", targetPath);
            return true;
        } catch (IOException e) {

            log.warn("[创建文件夹操作] 创建文件夹失败: {}", e.getMessage(), e);
            log.warn("本地存储目录创建失败 - 路径: {}, 错误信息: {}", absolutePath, e.getMessage(), e);
            throw new OssException("创建目录失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = Path.of(folderPath, fileName).toString();
        log.info("[删除文件操作] 开始删除文件: {}", absolutePath);
        Path targetPath = getTargetPath(absolutePath, visibility, userId);

        log.info("本地存储开始删除文件 - 路径: {}", targetPath);

        try {
            log.debug("检查文件是否存在");
            boolean exists = Files.exists(targetPath);
            if (!exists) {
                log.warn("文件不存在，无法删除: {}", targetPath);
                throw new OssException("该文件不存在");
            }

            log.debug("开始删除文件");
            boolean result = Files.deleteIfExists(targetPath);
            if (!result) {
                log.warn("文件删除操作返回false: {}", targetPath);
                throw new OssException("文件删除失败");
            }


            log.info("本地存储文件删除成功 - 路径: {}", targetPath);
            return true;
        } catch (IOException e) {

            log.warn("[删除文件操作] 删除文件失败: {}", e.getMessage(), e);
            log.warn("本地存储文件删除失败 - 路径: {}, 错误信息: {}", absolutePath, e.getMessage(), e);
            throw new OssException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件夹及其所有内容
     *
     * @param folderName 文件夹名称
     * @param folderPath 文件夹路径
     * @param visibility 文件夹能见度
     * @param userId 用户 ID
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFolder(String folderName, String folderPath, VisibilityEnum visibility, String userId) {
        String absolutePath = Path.of(folderPath, folderName).toString();
        log.info("[删除文件夹操作] 开始删除文件夹: {}", absolutePath);
        Path targetPath = getTargetPath(absolutePath, visibility, userId);
        log.info("本地存储开始删除文件夹 - 路径: {}", targetPath);

        try {

            log.debug("检查文件夹是否存在");
            boolean exists = Files.exists(targetPath);
            if (!exists) {
                log.warn("文件夹不存在，无法删除: {}", targetPath);
                throw new OssException("该文件夹不存在");
            }

            // 统计删除的文件和文件夹数量
            final int[] deletedCount = {0};
            final long[] totalSize = {0};

            log.debug("开始遍历并删除文件夹内容");

            try (Stream<Path> walk = Files.walk(targetPath)) {
                // sorted(Comparator.reverseOrder()) 确保先处理子文件/子目录，再处理父目录
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        // 记录文件大小（如果是文件）
                        if (Files.isRegularFile(p)) {
                            try {
                                totalSize[0] += Files.size(p);
                            } catch (IOException e) {
                                log.debug("无法获取文件大小: {}", absolutePath);
                            }
                        }

                        Files.delete(p); // 删除文件或空目录
                        deletedCount[0]++;
                        log.debug("成功删除路径: {}", p);
                    } catch (IOException e) {
                        log.warn("删除路径失败: {}, 错误信息: {}", absolutePath, e.getMessage(), e);
                        // 在 forEach 中抛出异常会中断流，可以考虑在此处记录错误并继续，
                        // 或者使用 AtomicBoolean 标记失败状态
                        throw new RuntimeException("删除文件失败: " + absolutePath, e); // Rethrow as RuntimeException to break walk
                    }
                });
            }


            log.info("本地存储文件夹删除成功 - 路径: {}, 删除项目数: {}, 总大小: {} bytes", targetPath, deletedCount[0], totalSize[0]);
            return true;
        } catch (IOException e) {
            log.warn("[删除文件夹操作] 删除文件夹失败: {}", e.getMessage(), e);
            log.warn("本地存储文件夹删除失败 - 路径: {}, 错误信息: {}", targetPath, e.getMessage(), e);
            throw new OssException("文件夹删除失败: " + e.getMessage());
        }
    }

    @Override
    @Deprecated
    public String downLoad(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        return "";
    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param folderPath 文件夹路径
     * @param visibility 能见度
     * @return 包含文件和文件夹信息的VO对象
     * @throws OssException 如果目录不存在
     * @throws RuntimeException 如果读取目录时发生IO错误
     */
    @Override
    public FilesAndFoldersVO listFileAndFolder(String folderName, String folderPath, VisibilityEnum visibility, Long userId) {
        String absolutePath = Path.of(folderPath, folderName).toString();
        log.info("[列出文件操作] 开始列出目录文件: {}", folderPath);

        List<FileInfoVO> fileInfos = metadataMapper.listFileByFolderPath(absolutePath, LOCAL, visibility, userId);

        Set<FolderInfoVO> folders = metadataMapper.listFolderNameByFolderPath(absolutePath, LOCAL, visibility, userId)
                .stream()
                .map(s -> new FolderInfoVO(s, folderPath))
                .collect(Collectors.toSet());
        folders.addAll(metadataMapper.listFolderPathByFolderName(folderPath, LOCAL, visibility)
                .stream()
                .map(s -> new FolderInfoVO(s.replace(folderPath, "").split("/")[0], folderPath))
                .collect(Collectors.toSet()));
        return new FilesAndFoldersVO(fileInfos, folders);
    }

    @Override
    @Deprecated
    public String previewFile(String fileName, String folderPath, VisibilityEnum visibility, String userId) {
        return "";
    }

    /**
     * 移动文件
     *
     * @param fileName 文件名
     * @param sourceFolderPath 源文件夹路径
     * @param targetFolderPath 目标文件夹路径
     * @return 移动是否成功
     */
    @Override
    public boolean moveFile(String fileName, String sourceFolderPath, String targetFolderPath, VisibilityEnum visibility, String userId) {
        log.info("[移动文件操作] 开始移动文件: {} -> {}", sourceFolderPath, targetFolderPath);
        Path sourcePath = getTargetPath(sourceFolderPath, visibility, userId);
        Path targetPath = getTargetPath(targetFolderPath, visibility, userId);

        long startTime = System.currentTimeMillis();
        log.info("本地存储开始文件移动操作 - 源路径: {}, 目标路径: {}", sourcePath, targetPath);

        try {
            log.debug("验证源文件是否存在");
            if (!Files.exists(sourcePath)) {
                log.warn("文件移动失败，源文件不存在: {}", sourceFolderPath);
                throw new OssException("该文件不存在无法移动");
            }

            // 获取文件信息用于日志记录
            long fileSize = 0;
            boolean isDirectory = Files.isDirectory(sourcePath);
            if (!isDirectory) {
                try {
                    fileSize = Files.size(sourcePath);
                } catch (IOException e) {
                    log.debug("无法获取源文件大小: {}", e.getMessage());
                }
            }

            // 检查目标文件是否已存在
            if (Files.exists(targetPath)) {
                log.warn("目标位置已存在文件，将被覆盖: {}", targetFolderPath);
            }

            log.debug("确保目标目录存在");
            Path targetParentDir = targetPath.getParent();
            if (targetParentDir != null && !Files.exists(targetParentDir)) {
                Files.createDirectories(targetParentDir);
                log.debug("创建目标目录: {}", targetParentDir);
            }

            log.debug("执行文件移动操作");
            // 构建移动选项
            CopyOption[] options = new CopyOption[]{StandardCopyOption.REPLACE_EXISTING};
            Files.move(sourcePath, targetPath, options);

            long duration = System.currentTimeMillis() - startTime;
            String fileType = isDirectory ? "目录" : "文件";
            log.info("本地存储{}移动成功 - 源路径: {}, 目标路径: {}, 大小: {} bytes, 耗时: {} ms", fileType, sourceFolderPath, targetFolderPath, fileSize, duration);
            return true;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("[移动文件操作] 移动文件失败: {}", e.getMessage(), e);
            log.warn("本地存储文件移动失败 - 源路径: {}, 目标路径: {}, 耗时: {} ms, 错误信息: {}", sourceFolderPath, targetFolderPath, duration, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取当前存储类型
     *
     * @return 存储类型枚举值（LOCAL）
     */
    @Override
    public StorageTypeEnum getStorageType() {
        return LOCAL;
    }

    /**
     * 下载本地文件
     *
     * @param fileMetadataEntity 文件元数据
     * @param visibility 文件能见度
     * @return 文件响应实体
     * @throws RuntimeException 如果文件处理或编码发生错误
     */
    public ResponseEntity<?> downLocalFile(FileMetadataEntity fileMetadataEntity, VisibilityEnum visibility, String userId) {
        log.info("[下载文件操作] 开始下载本地文件: {}", fileMetadataEntity.getFileName());

        String path = fileMetadataEntity.getFolderPath();

        String filename = fileMetadataEntity.getFileName();

        String absolutePath = path + filename;

        Path targetPath = getTargetPath(absolutePath, visibility, userId);

        try {
            URLCodec codec = new URLCodec();
            Resource resource = new UrlResource(targetPath.toUri());
            ResponseEntity<Resource> body = ResponseEntity.ok().contentType(
                            MediaType.parseMediaType(Files.probeContentType(targetPath))).
                    header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + codec.encode(filename) + "\"").body(resource);
            log.info("[下载文件操作] 文件下载成功: {}", filename);
            log.info("文件下载url获取成功");
            return body;
        } catch (IOException | EncoderException e) {
            log.warn("[下载文件操作] 文件下载失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 预览本地文件
     *
     * @param fileMetadataEntity 文件元数据
     * @param visibility 文件能见度
     * @param userId 用户 ID
     * @return 文件预览响应实体
     */
    public ResponseEntity<?> previewLocalFile(FileMetadataEntity fileMetadataEntity, VisibilityEnum visibility, String userId) {

        String folderPath = fileMetadataEntity.getFolderPath();

        String filename = fileMetadataEntity.getFileName();

        Path targetPath = getTargetPath(folderPath + filename, visibility, userId);

        try {
            URLCodec codec = new URLCodec();

            String contentType = Files.probeContentType(targetPath);

            String finalContentType = contentType.startsWith("text/") ? contentType + ";charset=UTF-8" : contentType;

            Resource resource = new UrlResource(targetPath.toUri());

            ResponseEntity<Resource> body = ResponseEntity.ok().contentType(MediaType.parseMediaType(finalContentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + codec.encode(filename)).body(resource);
            log.info("文件预览url获取成功");
            return body;
        } catch (IOException | EncoderException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 预览本地头像文件
     *
     * @param absolutePath 头像文件的绝对路径
     * @return 包含头像资源的ResponseEntity
     * @throws RuntimeException 如果文件处理发生错误
     */
    public ResponseEntity<?> previewLocalAvatar(String absolutePath) {
        try {
            Path filePath = Paths.get(avatarPath, absolutePath);
            String contentType = Files.probeContentType(filePath);
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传文件
     *
     * @param file 文件信息
     * @return 上传是否成功
     */
    private boolean upload(UploadFileDTO file, Path targetPath) {
        log.info("[上传文件操作] 开始上传文件: {}", file.getFileName());

        // 验证文件大小（本地存储通常有磁盘空间限制）
        log.debug("开始验证文件大小和磁盘空间");
        validateFileSize(file.getSize());

        // 确保目标目录存在
        createDirectoriesIfNotExists(targetPath.getParent());

        if (file.getSize() > LOCAL_SIZE_THRESHOLD) {
            log.debug("文件大小超过阈值，使用大文件分块上传策略");
            // 大文件分块上传
            return uploadLargeFile(file.getInputStream(), file.getFileName(), file.getSize(), targetPath);
        } else {
            log.debug("文件大小未超过阈值，使用小文件直接上传策略");
            // 小文件直接上传
            return uploadSmallFile(file.getInputStream(), file.getFileName(), file.getSize(), targetPath);
        }
    }

    /**
     * 上传小文件
     *
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param size 文件大小
     * @param absolutePath 目标路径
     */
    private boolean uploadSmallFile(InputStream inputStream, String filename, long size, Path absolutePath) {
        Path tempPath = null;
        long startTime = System.currentTimeMillis();
        log.info("开始小文件上传 - 文件名: {}", filename);

        try {
            // 使用临时文件确保原子性操作
            tempPath = createTempFile(absolutePath);

            // 直接写入临时文件

            try (var outputStream = Files.newOutputStream(tempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                inputStream.transferTo(outputStream);
            }
            log.debug("文件数据写入临时文件完成");

            // 验证文件完整性
            log.debug("验证文件完整性");
            validateUploadedFile(tempPath, size);

            // 原子性移动到目标位置
            log.debug("原子性移动文件到目标位置");
            Files.move(tempPath, absolutePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            long duration = System.currentTimeMillis() - startTime;
            log.info("小文件上传成功 - 文件名: {}, 耗时: {} ms", filename, duration);
            return true;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("小文件上传失败 - 文件名: {}, 耗时: {} ms, 错误信息: {}", filename, duration, e.getMessage(), e);
            // 清理临时文件
            cleanupTempFile(tempPath);
            throw new OssException("小文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传大文件
     *
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param size 文件大小
     * @param absolutePath 目标路径
     * @return 上传是否成功
     */
    private boolean uploadLargeFile(InputStream inputStream, String filename, long size, Path absolutePath) {
        Path tempPath = null;
        long startTime = System.currentTimeMillis();
        log.debug("开始大文件分块上传 - 文件名: {}, 大小: {} MB", filename, size / (1024 * 1024));

        try {
            // 使用临时文件确保原子性操作
            log.debug("创建临时文件");
            tempPath = createTempFile(absolutePath);
            log.debug("临时文件创建成功: {}", tempPath);

            // 分块写入大文件（8KB缓冲区）
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesWritten = 0;
            long lastProgressTime = System.currentTimeMillis();

            log.debug("开始分块写入大文件，缓冲区大小: {} KB", buffer.length / 1024);
            try (var outputStream = Files.newOutputStream(tempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesWritten += bytesRead;

                    // 定期记录进度（每10MB或每5秒）
                    long currentTime = System.currentTimeMillis();
                    if (totalBytesWritten % (10 * 1024 * 1024) == 0 || (currentTime - lastProgressTime) > 5000) {
                        lastProgressTime = currentTime;
                    }
                }
            }
            log.debug("大文件数据写入完成，总计: {} MB", totalBytesWritten / (1024 * 1024));

            // 验证文件完整性
            log.debug("验证大文件完整性");
            validateUploadedFile(tempPath, size);

            // 原子性移动到目标位置
            log.debug("原子性移动大文件到目标位置");
            Files.move(tempPath, absolutePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            long duration = System.currentTimeMillis() - startTime;
            log.info("大文件分块上传成功 - 文件名: {}, 大小: {} MB, 耗时: {} ms", filename, size / (1024 * 1024), duration);
            return true;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("大文件分块上传失败 - 文件名: {}, 耗时: {} ms, 错误信息: {}", filename, duration, e.getMessage(), e);
            // 清理临时文件
            cleanupTempFile(tempPath);
            throw new OssException("大文件分块上传失败: " + e.getMessage());
        }
    }

    /**
     * 验证文件大小
     *
     * @param fileSize 文件大小
     */
    private void validateFileSize(long fileSize) {
        // 检查可用磁盘空间
        try {
            Path basePathObj = Paths.get(privatePath);
            long usableSpace = Files.getFileStore(basePathObj).getUsableSpace();

            if (fileSize > usableSpace) {
                throw new OssException("磁盘空间不足，无法保存文件");
            }

            // 预留一些空间（至少保留100MB）
            long reservedSpace = 100 * 1024 * 1024;
            if (fileSize > (usableSpace - reservedSpace)) {
                log.warn("磁盘空间即将不足，当前可用空间: {} MB", usableSpace / (1024 * 1024));
            }

        } catch (IOException e) {
            log.warn("无法检查磁盘空间: {}", e.getMessage());
        }
    }

    /**
     * 创建目录（如果不存在）
     *
     * @param parentDir 父目录路径
     */
    private void createDirectoriesIfNotExists(Path parentDir) {
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                log.warn("文件夹创建失败: {}", e.getMessage());
                throw new OssException("文件夹创建失败", e);
            }
        }
    }

    /**
     * 创建临时文件
     *
     * @param targetPath 目标文件路径
     * @return 临时文件路径
     */
    private Path createTempFile(Path targetPath) {
        Path parentDir = targetPath.getParent();
        String fileName = targetPath.getFileName().toString();
        try {
            return Files.createTempFile(parentDir, fileName + ".", ".tmp");
        } catch (IOException e) {
            throw new OssException("临时文件创建失败", e);
        }
    }

    /**
     * 验证上传的文件
     *
     * @param filePath 文件路径
     * @param expectedSize 期望的文件大小
     */
    private void validateUploadedFile(Path filePath, long expectedSize) {
        long actualSize;
        try {
            actualSize = Files.size(filePath);
        } catch (IOException e) {
            throw new OssException("无法获取文件大小进行验证", e);
        }
        if (actualSize != expectedSize) {
            throw new OssException(String.format("文件大小不匹配，期望: %d bytes, 实际: %d bytes", expectedSize, actualSize));
        }
    }

    /**
     * 清理临时文件
     *
     * @param tempPath 临时文件路径
     */
    private void cleanupTempFile(Path tempPath) {
        if (tempPath != null && Files.exists(tempPath)) {
            try {
                Files.delete(tempPath);
                log.debug("清理临时文件: {}", tempPath);
            } catch (IOException e) {
                log.warn("清理临时文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取完整路径
     *
     * @param absolutePath 绝对路径
     * @param visibility 访问权限
     * @param userId 用户 Id
     * @return 目标路径
     */
    public Path getTargetPath(String absolutePath, VisibilityEnum visibility, String userId) {
        Path targetPath;
        if (visibility == VisibilityEnum.PRIVATE) {
            targetPath = Path.of(privatePath, userId, absolutePath);
        } else if (visibility == VisibilityEnum.PUBLIC) {
            targetPath = Path.of(publicPath, absolutePath);
        } else if (visibility == VisibilityEnum.USER_INFO) {
            targetPath = Path.of(avatarPath, absolutePath);
        } else {
            log.warn("未知的访问权限");
            throw new OssException("未知的访问权限");
        }
        return targetPath.normalize();
    }

}
