package com.sparkseries.module.storage.service.oss.impl;

import static com.sparkseries.common.constant.Constants.LOCAL_SIZE_THRESHOLD;

import com.sparkseries.common.dto.MultipartFileDTO;
import com.sparkseries.common.enums.StorageTypeEnum;
import com.sparkseries.common.util.exception.BusinessException;
import com.sparkseries.module.file.dao.FileMetadataMapper;
import com.sparkseries.module.file.entity.FileMetadataEntity;
import com.sparkseries.module.file.vo.FileInfoVO;
import com.sparkseries.module.file.vo.FilesAndFoldersVO;
import com.sparkseries.module.file.vo.FolderInfoVO;
import com.sparkseries.module.storage.service.oss.OssService;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
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

/**
 * 本地文件存储服务实现类
 */
@Slf4j
@Service("local")
public class LocalOssServiceImpl implements OssService {


    /**
     * 本地文件存储的基础路径
     */
    public final String basePath;
    private final FileMetadataMapper fileMetadataMapper;

    /**
     * 构造函数，注入本地文件存储的基础路径
     *
     * @param basePath 本地文件存储的基础路径，从配置文件中获取
     */
    public LocalOssServiceImpl(@Value("${Local.basePath}") String basePath, FileMetadataMapper fileMetadataMapper) {

        log.info("[初始化本地存储服务] 开始初始化本地文件存储服务");
        this.basePath = basePath;
        this.fileMetadataMapper = fileMetadataMapper;
        log.info("本地存储服务初始化 - 基础路径: {}, 大文件阈值: {} MB",
                basePath, LOCAL_SIZE_THRESHOLD / (1024 * 1024));
        log.info("[初始化本地存储服务] 本地文件存储服务初始化完成，基础路径: {}", basePath);
    }

    /**
     * 上传文件到本地存储
     *
     * @param file 要上传的文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean upload(MultipartFileDTO file) {
        log.info("[上传文件操作] 开始上传文件到本地存储: {}", file.getAbsolutePath());
        String originalFileName = file.getFilename();
        String absolutePath = file.getAbsolutePath();
        String fullPath = basePath + absolutePath;
        long startTime = System.currentTimeMillis();

        log.info("本地存储开始上传文件 - 文件名: {}, 大小: {} bytes ({} MB), 目标路径: {}",
                originalFileName, file.getSize(), file.getSize() / (1024 * 1024), fullPath);

        try {
            // 验证文件大小（本地存储通常有磁盘空间限制）
            log.debug("开始验证文件大小和磁盘空间");
            validateFileSize(file.getSize());

            Path targetPath = Paths.get(fullPath);
            // 确保目标目录存在
            log.debug("确保目标目录存在: {}", targetPath.getParent());
            createDirectoriesIfNotExists(targetPath.getParent());

            boolean result;
            // 根据文件大小选择上传策略
            if (file.getSize() > LOCAL_SIZE_THRESHOLD) {
                log.debug("文件大小超过阈值，使用大文件分块上传策略");
                // 大文件分块上传
                result = uploadLargeFile(file, targetPath);
            } else {
                log.debug("文件大小未超过阈值，使用小文件直接上传策略");
                // 小文件直接上传
                result = uploadSmallFile(file, targetPath);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("本地存储文件上传完成 - 文件名: {}, 大小: {} MB, 耗时: {} ms, 结果: {}",
                    originalFileName, file.getSize() / (1024 * 1024), duration, result ? "成功" : "失败");
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[上传文件操作] 文件上传失败: {}", e.getMessage(), e);
            log.error("本地存储文件上传失败 - 文件名: {}, 路径: {}, 耗时: {} ms, 错误信息: {}",
                    originalFileName, fullPath, duration, e.getMessage(), e);
            throw new BusinessException("本地保存失败: " + e.getMessage());
        }
    }

    /**
     * 上传小文件（小于分块阈值）
     *
     * @param file       文件信息
     * @param targetPath 目标路径
     * @return 上传是否成功
     */
    private boolean uploadSmallFile(MultipartFileDTO file, Path targetPath) {
        Path tempPath = null;
        long startTime = System.currentTimeMillis();
        log.debug("开始小文件上传 - 文件名: {}, 大小: {} KB", file.getFilename(), file.getSize() / 1024);

        try {
            // 使用临时文件确保原子性操作
            log.debug("创建临时文件");
            tempPath = createTempFile(targetPath);
            log.debug("临时文件创建成功: {}", tempPath);

            // 直接写入临时文件
            log.debug("开始写入文件数据到临时文件");
            try (var inputStream = file.getInputStream();
                    var outputStream = Files.newOutputStream(tempPath, StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE)) {
                inputStream.transferTo(outputStream);
            }
            log.debug("文件数据写入临时文件完成");

            // 验证文件完整性
            log.debug("验证文件完整性");
            validateUploadedFile(tempPath, file.getSize());

            // 原子性移动到目标位置
            log.debug("原子性移动文件到目标位置");
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            long duration = System.currentTimeMillis() - startTime;
            log.info("小文件上传成功 - 文件名: {}, 大小: {} KB, 耗时: {} ms",
                    file.getFilename(), file.getSize() / 1024, duration);
            return true;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("小文件上传失败 - 文件名: {}, 耗时: {} ms, 错误信息: {}",
                    file.getFilename(), duration, e.getMessage(), e);
            // 清理临时文件
            cleanupTempFile(tempPath);
            throw new BusinessException("小文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传大文件（大于分块阈值）
     *
     * @param file       文件信息
     * @param targetPath 目标路径
     * @return 上传是否成功
     */
    private boolean uploadLargeFile(MultipartFileDTO file, Path targetPath) {
        Path tempPath = null;
        long startTime = System.currentTimeMillis();
        log.debug("开始大文件分块上传 - 文件名: {}, 大小: {} MB",
                file.getFilename(), file.getSize() / (1024 * 1024));

        try {
            // 使用临时文件确保原子性操作
            log.debug("创建临时文件");
            tempPath = createTempFile(targetPath);
            log.debug("临时文件创建成功: {}", tempPath);

            // 分块写入大文件（8KB缓冲区）
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesWritten = 0;
            long lastProgressTime = System.currentTimeMillis();

            log.debug("开始分块写入大文件，缓冲区大小: {} KB", buffer.length / 1024);
            try (var inputStream = file.getInputStream();
                    var outputStream = Files.newOutputStream(tempPath, StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE)) {

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesWritten += bytesRead;

                    // 定期记录进度（每10MB或每5秒）
                    long currentTime = System.currentTimeMillis();
                    if (totalBytesWritten % (10 * 1024 * 1024) == 0 ||
                            (currentTime - lastProgressTime) > 5000) {
                        double progress = (double) totalBytesWritten / file.getSize() * 100;
                        log.debug("大文件上传进度: {} MB / {} MB ({:.1f}%)",
                                totalBytesWritten / (1024 * 1024),
                                file.getSize() / (1024 * 1024), progress);
                        lastProgressTime = currentTime;
                    }
                }
            }
            log.debug("大文件数据写入完成，总计: {} MB", totalBytesWritten / (1024 * 1024));

            // 验证文件完整性
            log.debug("验证大文件完整性");
            validateUploadedFile(tempPath, file.getSize());

            // 原子性移动到目标位置
            log.debug("原子性移动大文件到目标位置");
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            long duration = System.currentTimeMillis() - startTime;
            log.info("大文件分块上传成功 - 文件名: {}, 大小: {} MB, 耗时: {} ms",
                    file.getFilename(), file.getSize() / (1024 * 1024), duration);
            return true;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("大文件分块上传失败 - 文件名: {}, 耗时: {} ms, 错误信息: {}",
                    file.getFilename(), duration, e.getMessage(), e);
            // 清理临时文件
            cleanupTempFile(tempPath);
            throw new BusinessException("大文件分块上传失败: " + e.getMessage());
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
            Path basePathObj = Paths.get(basePath);
            long usableSpace = Files.getFileStore(basePathObj).getUsableSpace();

            if (fileSize > usableSpace) {
                throw new BusinessException("磁盘空间不足，无法保存文件");
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
     * @throws IOException 如果创建目录失败
     */
    private void createDirectoriesIfNotExists(Path parentDir) throws IOException {
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("创建目录: {}", parentDir);
        }
    }

    /**
     * 创建临时文件
     *
     * @param targetPath 目标文件路径
     * @return 临时文件路径
     * @throws IOException 如果创建临时文件失败
     */
    private Path createTempFile(Path targetPath) throws IOException {
        Path parentDir = targetPath.getParent();
        String fileName = targetPath.getFileName().toString();
        return Files.createTempFile(parentDir, fileName + ".", ".tmp");
    }

    /**
     * 验证上传的文件
     *
     * @param filePath     文件路径
     * @param expectedSize 期望的文件大小
     * @throws IOException 如果文件大小不匹配
     */
    private void validateUploadedFile(Path filePath, long expectedSize) throws IOException {
        long actualSize = Files.size(filePath);
        if (actualSize != expectedSize) {
            throw new IOException(
                    String.format("文件大小不匹配，期望: %d bytes, 实际: %d bytes", expectedSize, actualSize));
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
     * 上传头像文件
     *
     * @param avatar 头像文件信息
     * @return 上传是否成功
     */
    @Override
    public boolean uploadAvatar(MultipartFileDTO avatar) {
        log.info("[上传头像操作] 开始上传头像到本地存储: {}", avatar.getAbsolutePath());

        log.info("开始头像上传: 文件名='{}', 大小={} bytes, 目标路径='{}'",
                avatar.getFilename(), avatar.getSize(), avatar.getAbsolutePath());

        try {
            // 复用upload方法的完整逻辑（包括文件大小验证、分片上传、原子性操作等）
            boolean result = upload(avatar);

            if (result) {
                log.info("[上传头像操作] 头像上传成功: {}", avatar.getAbsolutePath());
                log.info("头像上传成功: 文件名='{}', 路径='{}'", avatar.getFilename(), avatar.getAbsolutePath());
            } else {
                log.error("[上传头像操作] 头像上传失败: {}", avatar.getAbsolutePath());
            }

            return result;
        } catch (Exception e) {
            log.error("[上传头像操作] 头像上传异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 创建文件夹
     *
     * @param dir 文件夹路径
     * @return 创建是否成功
     */
    @Override
    public boolean createFolder(String dir) {
        log.info("[创建文件夹操作] 开始创建文件夹: {}", dir);
        dir = basePath + dir;
        Path path = Paths.get(dir);
        long startTime = System.currentTimeMillis();
        log.info("本地存储开始创建目录 - 路径: {}", path);

        try {
            log.debug("检查目录是否已存在");
            if (Files.exists(path)) {
                log.info("目录已存在，无需创建: {}", path.toAbsolutePath());
                return true;
            }

            log.debug("开始创建目录（包含父目录）");
            Files.createDirectories(path);

            long duration = System.currentTimeMillis() - startTime;
            log.info("本地存储目录创建成功 - 路径: {}, 耗时: {} ms", path.toAbsolutePath(), duration);
            return true;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[创建文件夹操作] 创建文件夹失败: {}", e.getMessage(), e);
            log.error("本地存储目录创建失败 - 路径: {}, 耗时: {} ms, 错误信息: {}",
                    path, duration, e.getMessage(), e);
            throw new BusinessException("创建目录失败: " + e.getMessage());
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
        absolutePath = basePath + absolutePath;
        long startTime = System.currentTimeMillis();
        log.info("本地存储开始删除文件 - 路径: {}", absolutePath);

        try {
            Path path = Paths.get(absolutePath);
            log.debug("检查文件是否存在");
            boolean exists = Files.exists(path);
            if (!exists) {
                log.warn("文件不存在，无法删除: {}", absolutePath);
                throw new BusinessException("该文件不存在");
            }

            // 获取文件大小用于日志记录
            long fileSize = 0;
            try {
                fileSize = Files.size(path);
            } catch (IOException e) {
                log.debug("无法获取文件大小: {}", e.getMessage());
            }

            log.debug("开始删除文件");
            boolean result = Files.deleteIfExists(path);
            if (!result) {
                log.error("文件删除操作返回false: {}", absolutePath);
                throw new BusinessException("文件删除失败");
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("本地存储文件删除成功 - 路径: {}, 大小: {} bytes, 耗时: {} ms",
                    absolutePath, fileSize, duration);
            return true;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[删除文件操作] 删除文件失败: {}", e.getMessage(), e);
            log.error("本地存储文件删除失败 - 路径: {}, 耗时: {} ms, 错误信息: {}",
                    absolutePath, duration, e.getMessage(), e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件夹及其所有内容
     *
     * @param dir 文件夹路径
     * @return 删除是否成功
     */
    @Override
    public boolean deleteFolder(String dir) {
        log.info("[删除文件夹操作] 开始删除文件夹: {}", dir);
        dir = basePath + dir;
        long startTime = System.currentTimeMillis();
        log.info("本地存储开始删除文件夹 - 路径: {}", dir);

        try {
            Path folder = Paths.get(dir);
            log.debug("检查文件夹是否存在");
            boolean exists = Files.exists(folder);
            if (!exists) {
                log.warn("文件夹不存在，无法删除: {}", dir);
                throw new BusinessException("该文件夹不存在");
            }

            // 统计删除的文件和文件夹数量
            final int[] deletedCount = {0};
            final long[] totalSize = {0};

            log.debug("开始遍历并删除文件夹内容");
            Files.walk(folder)
                    // sorted(Comparator.reverseOrder()) 确保先处理子文件/子目录，再处理父目录
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            // 记录文件大小（如果是文件）
                            if (Files.isRegularFile(path)) {
                                try {
                                    totalSize[0] += Files.size(path);
                                } catch (IOException e) {
                                    log.debug("无法获取文件大小: {}", path);
                                }
                            }

                            Files.delete(path); // 删除文件或空目录
                            deletedCount[0]++;
                            log.debug("成功删除路径: {}", path);
                        } catch (IOException e) {
                            log.error("删除路径失败: {}, 错误信息: {}", path, e.getMessage(), e);
                            // 在 forEach 中抛出异常会中断流，可以考虑在此处记录错误并继续，
                            // 或者使用 AtomicBoolean 标记失败状态
                            throw new RuntimeException("删除文件失败: " + path,
                                    e); // Rethrow as RuntimeException to break walk
                        }
                    });

            long duration = System.currentTimeMillis() - startTime;
            log.info("本地存储文件夹删除成功 - 路径: {}, 删除项目数: {}, 总大小: {} bytes, 耗时: {} ms",
                    dir, deletedCount[0], totalSize[0], duration);
            return true;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[删除文件夹操作] 删除文件夹失败: {}", e.getMessage(), e);
            log.error("本地存储文件夹删除失败 - 路径: {}, 耗时: {} ms, 错误信息: {}",
                    dir, duration, e.getMessage(), e);
            throw new BusinessException("文件夹删除失败: " + e.getMessage());
        }
    }

    /**
     * 生成文件下载链接（本地存储不支持直接下载链接）
     *
     * @param absolutePath     文件绝对路径
     * @param downloadFileName 下载文件名
     * @return 始终返回空字符串
     */
    @Override
    public String downLoad(String absolutePath, String downloadFileName) {
        return "";
    }

    /**
     * 下载本地文件
     *
     * @param fileMetadataEntity 文件元数据
     * @return 文件响应实体
     * @throws RuntimeException 如果文件处理或编码发生错误
     */
    @Override
    public ResponseEntity<?> downLocalFile(FileMetadataEntity fileMetadataEntity) {
        log.info("[下载文件操作] 开始下载本地文件: {}", fileMetadataEntity.getFilename());

        String path = fileMetadataEntity.getStoragePath();

        String originalName = fileMetadataEntity.getOriginalName();

        String filename = fileMetadataEntity.getFilename();

        path = basePath + path;

        try {
            URLCodec codec = new URLCodec();
            Path filePath = Paths.get(path, filename);
            Resource resource = new UrlResource(filePath.toUri());
            ResponseEntity<Resource> body = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(Files.probeContentType(filePath)))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                            + codec.encode(originalName) + "\"")
                    .body(resource);
            log.info("[下载文件操作] 文件下载成功: {}", filename);
            log.info("文件下载url获取成功");
            return body;
        } catch (IOException | EncoderException e) {
            log.error("[下载文件操作] 文件下载失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
        log.info("[重命名文件操作] 开始重命名文件: {} -> {}", filename, newFilename);
        path = basePath + path + filename;
        Path source = Paths.get(path);
        Path target;
        Path parentDir = source.getParent();
        newFilename = id + newFilename;
        long startTime = System.currentTimeMillis();
        log.info("本地存储开始文件重命名操作 - 文件ID: {}, 原文件名: {}, 新文件名: {}, 路径: {}",
                id, filename, newFilename, path);

        if (parentDir == null) {
            target = Paths.get(newFilename);
            log.debug("源路径 '{}' 没有父目录，假设在当前工作目录重命名为 '{}'", path, newFilename);
        } else {
            // 2. 在父目录下构建新的目标路径，使用新的文件名
            target = parentDir.resolve(newFilename);
            log.debug("源路径父目录 '{}'，新文件名称 '{}'，目标路径 '{}'",
                    parentDir.toAbsolutePath(), newFilename, target.toAbsolutePath());
        }

        try {
            // 验证源文件是否存在
            if (!Files.exists(source)) {
                log.warn("文件重命名失败，源文件不存在: {}", path);
                throw new BusinessException("源文件不存在");
            }

            // 获取文件大小用于日志记录
            long fileSize = 0;
            try {
                fileSize = Files.size(source);
            } catch (IOException e) {
                log.debug("无法获取文件大小: {}", e.getMessage());
            }

            log.debug("执行文件重命名操作");
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            long duration = System.currentTimeMillis() - startTime;
            log.info("本地存储文件重命名成功 - 文件ID: {}, 原文件名: {}, 新文件名: {}, 大小: {} bytes, 耗时: {} ms",
                    id, filename, newFilename, fileSize, duration);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[重命名文件操作] 文件重命名失败: {}", e.getMessage(), e);
            log.error("本地存储文件重命名失败 - 文件ID: {}, 原文件名: {}, 新文件名: {}, 耗时: {} ms, 错误信息: {}",
                    id, filename, newFilename, duration, e.getMessage(), e);
            return false;
        }

    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param path 要列出的路径
     * @return 包含文件和文件夹信息的VO对象
     * @throws BusinessException 如果目录不存在
     * @throws RuntimeException  如果读取目录时发生IO错误
     */
    @Override
    public FilesAndFoldersVO listFiles(String path) {
        log.info("[列出文件操作] 开始列出目录文件: {}", path);
        String tempPath = path;
        path = basePath + path;
        Path dir = Paths.get(path);

        List<FolderInfoVO> folders = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        log.info("本地存储开始列举目录内容 - 目录路径: {}", path);

        if (!Files.exists(dir)) {
            log.warn("目录列举失败，目录不存在: {}", path);
            throw new BusinessException("该目录不存在");
        }

        if (!Files.isDirectory(dir)) {
            log.warn("路径不是目录: {}", path);
            throw new BusinessException("指定路径不是目录");
        }


        List<FileInfoVO> files = fileMetadataMapper.listMetadataByPath(tempPath);

        try (var stream = Files.list(dir)) {
            List<Path> list = stream.toList();
            int fileCount = 0;
            int folderCount = 0;
            long totalSize = 0;

            log.debug("开始遍历目录内容，共 {} 个项目", list.size());
            for (Path d : list) {
                if (Files.isDirectory(d)) {
                    folders.add(new FolderInfoVO(d.toAbsolutePath().toString()));
                    folderCount++;
                }
            }

            FilesAndFoldersVO result = new FilesAndFoldersVO(files, folders);
            long duration = System.currentTimeMillis() - startTime;
            log.info("本地存储目录列举完成 - 目录: {}, 文件数: {}, 文件夹数: {}, 总大小: {} bytes, 耗时: {} ms",
                    path, fileCount, folderCount, totalSize, duration);
            return result;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[列出文件操作] 列出文件失败: {}", e.getMessage(), e);
            log.error("本地存储目录列举失败 - 目录: {}, 耗时: {} ms, 错误信息: {}",
                    path, duration, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成文件预览链接（此方法已废弃）
     *
     * @param absolutePath 文件绝对路径
     * @return 始终返回空字符串
     */
    @Override
    @Deprecated
    public String previewFile(String absolutePath) {
        return "";
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
        log.info("[移动文件操作] 开始移动文件: {} -> {}", sourceAbsolutePath, targetAbsolutePath);

        sourceAbsolutePath = basePath + sourceAbsolutePath;
        targetAbsolutePath = basePath + targetAbsolutePath;
        Path source = Paths.get(sourceAbsolutePath);
        Path target = Paths.get(targetAbsolutePath);
        long startTime = System.currentTimeMillis();
        log.info("本地存储开始文件移动操作 - 源路径: {}, 目标路径: {}", sourceAbsolutePath, targetAbsolutePath);

        try {
            log.debug("验证源文件是否存在");
            if (!Files.exists(source)) {
                log.warn("文件移动失败，源文件不存在: {}", sourceAbsolutePath);
                throw new BusinessException("该文件不存在无法移动");
            }

            // 获取文件信息用于日志记录
            long fileSize = 0;
            boolean isDirectory = Files.isDirectory(source);
            if (!isDirectory) {
                try {
                    fileSize = Files.size(source);
                } catch (IOException e) {
                    log.debug("无法获取源文件大小: {}", e.getMessage());
                }
            }

            // 检查目标文件是否已存在
            if (Files.exists(target)) {
                log.warn("目标位置已存在文件，将被覆盖: {}", targetAbsolutePath);
            }

            log.debug("确保目标目录存在");
            Path targetParentDir = target.getParent();
            if (targetParentDir != null && !Files.exists(targetParentDir)) {
                Files.createDirectories(targetParentDir);
                log.debug("创建目标目录: {}", targetParentDir);
            }

            log.debug("执行文件移动操作");
            // 构建移动选项
            CopyOption[] options = new CopyOption[]{StandardCopyOption.REPLACE_EXISTING};
            Files.move(source, target, options);

            long duration = System.currentTimeMillis() - startTime;
            String fileType = isDirectory ? "目录" : "文件";
            log.info("本地存储{}移动成功 - 源路径: {}, 目标路径: {}, 大小: {} bytes, 耗时: {} ms",
                    fileType, sourceAbsolutePath, targetAbsolutePath, fileSize, duration);
            return true;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[移动文件操作] 移动文件失败: {}", e.getMessage(), e);
            log.error("本地存储文件移动失败 - 源路径: {}, 目标路径: {}, 耗时: {} ms, 错误信息: {}",
                    sourceAbsolutePath, targetAbsolutePath, duration, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取当前存储类型
     *
     * @return 存储类型枚举值（LOCAL）
     */
    @Override
    public StorageTypeEnum getCurrStorageType() {
        return StorageTypeEnum.LOCAL;
    }

    /**
     * 预览本地文件
     *
     * @param fileMetadataEntity 文件元数据
     * @return 文件预览响应实体
     */
    @Override
    public ResponseEntity<?> previewLocalFile(FileMetadataEntity fileMetadataEntity) {

        String path = fileMetadataEntity.getStoragePath();

        String originalName = fileMetadataEntity.getOriginalName();

        String filename = fileMetadataEntity.getFilename();

        path = basePath + path;
        try {
            URLCodec codec = new URLCodec();
            Path filePath = Paths.get(path, filename);
            String contentType = Files.probeContentType(filePath);
            String finalContentType = contentType.startsWith("text/") ? contentType + ";charset=UTF-8" : contentType;
            Resource resource = new UrlResource(filePath.toUri());
            ResponseEntity<Resource> body = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(finalContentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''"
                            + codec.encode(originalName))
                    .body(resource);
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
    @Override
    public ResponseEntity<?> previewLocalAvatar(String absolutePath) {
        try {

            Path filePath = Paths.get(basePath, absolutePath);
            String contentType = Files.probeContentType(filePath);
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取用户头像URL
     *
     * @param avatarPath 头像路径
     * @param filename   头像文件名
     * @return 头像访问URL
     */
    public String getUserAvatar(String avatarPath, String filename) {
        long startTime = System.currentTimeMillis();
        log.info("本地存储开始获取用户头像URL - 头像路径: {}, 文件名: {}", avatarPath, filename);

        try {
            String fullPath = basePath + avatarPath;
            Path filePath = Paths.get(fullPath, filename);

            log.debug("检查头像文件是否存在: {}", filePath);
            if (!Files.exists(filePath)) {
                log.warn("头像文件不存在: {}", filePath);
                throw new BusinessException("头像文件不存在");
            }

            // 构建可访问的URL，使用配置的URL映射
            String avatarUrl = "/avatars/" + avatarPath + "/" + filename;

            long duration = System.currentTimeMillis() - startTime;
            log.info("本地存储获取用户头像URL成功 - 头像路径: {}, 文件名: {}, URL: {}, 耗时: {} ms",
                    avatarPath, filename, avatarUrl, duration);
            return avatarUrl;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("本地存储获取用户头像URL失败 - 头像路径: {}, 文件名: {}, 耗时: {} ms, 错误信息: {}",
                    avatarPath, filename, duration, e.getMessage(), e);
            throw e;
        }
    }

}
