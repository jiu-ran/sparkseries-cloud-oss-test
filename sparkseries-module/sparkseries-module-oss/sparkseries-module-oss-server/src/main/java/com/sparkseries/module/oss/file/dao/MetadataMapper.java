package com.sparkseries.module.oss.file.dao;


import com.sparkeries.enums.StorageTypeEnum;
import com.sparkeries.enums.VisibilityEnum;
import com.sparkseries.module.oss.file.entity.FileMetadataEntity;
import com.sparkseries.module.oss.file.entity.FolderMetadataEntity;
import com.sparkseries.module.oss.file.vo.FileInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件元数据管理
 */
@Mapper
public interface MetadataMapper {

    // -------------------------------文件元数据相关操作----------------------------------

    /**
     * 添加文件元数据
     *
     * @param file 文件元数据
     * @return 添加的文件元数据数量
     */
    Integer insertFile(@Param("file") FileMetadataEntity file);


    /**
     * 根据 ID 删除文件元数据
     *
     * @param id 文件 ID
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 删除的文件元数据行数
     */
    Integer deleteFileById(@Param("id") Long id, @Param("storageType") StorageTypeEnum storageType,
                           @Param("visibility") VisibilityEnum visibility);


    /**
     * 删除指定文件夹下的文件列表
     *
     * @param folderPath 文件夹路径
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 删除的文件元数据行数
     */
    Integer deleteFileByFolderPath(@Param("folderPath") String folderPath, @Param("storageType") StorageTypeEnum storageType,
                                   @Param("visibility") VisibilityEnum visibility);

    /**
     * 修改文件存储路径
     *
     * @param id 文件 ID
     * @param folderPath 文件路径
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 更新的文件元数据数量
     */
    Integer updateFileFolderPath(@Param("id") Long id, @Param("folderPath") String folderPath,
                                 @Param("storageType") StorageTypeEnum storageType, @Param("visibility") VisibilityEnum visibility);

    /**
     * 根据文件名查询文件
     *
     * @param fileName 文件名
     * @param folderPath 文件路径
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 是否存在符合的文件(> 0存在)
     */
    Integer isExistFileByFileName(@Param("fileName") String fileName, @Param("folderPath") String folderPath,
                                  @Param("storageType") StorageTypeEnum storageType, @Param("visibility") VisibilityEnum visibility);


    /**
     * 根据文件 ID 查询文件
     *
     * @param id 文件 ID
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 是否存在符合的文件(> 0存在)
     */
    Integer isExistFileById(@Param("id") Long id, @Param("storageType") StorageTypeEnum storageType,
                            @Param("visibility") VisibilityEnum visibility);

    /**
     * 根据文件 ID 查询文件
     *
     * @param id 文件 ID
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 文件元数据
     */
    FileMetadataEntity getFileMetadataById(@Param("id") Long id, @Param("storageType") StorageTypeEnum storageType,
                                           @Param("visibility") VisibilityEnum visibility);


    /**
     * 查询指定文件夹路径下的文件列表
     *
     * @param folderPath 文件路径
     * @param storageType 存储类型
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 文件元数据列表
     */
    List<FileInfoVO> listFileByFolderPath(@Param("folderPath") String folderPath, @Param("storageType") StorageTypeEnum storageType,
                                          @Param("visibility") VisibilityEnum visibility, @Param("userId") Long userId);

    // -----------------------文件夹元数据相关操作---------------------------

    /**
     * 插入文件夹元数据
     *
     * @param folder 文件夹元数据
     * @return 插入的文件夹元数据数量
     */
    Integer insertFolder(@Param("folder") FolderMetadataEntity folder);

    /**
     * 根据 ID 删除文件夹元数据
     *
     * @param id 文件夹 ID
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 删除的文件夹元数据数量
     */
    Integer deleteFolderById(@Param("id") Long id, @Param("storageType") StorageTypeEnum storageType,
                             @Param("visibility") VisibilityEnum visibility);

    /**
     * 删除指定文件夹的所有子文件夹
     *
     * @param folderPath 文件夹绝对路径
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 删除的文件夹元数据数量
     */
    Integer deleteSubfoldersByFolderPath(@Param("folderPath") String folderPath, @Param("storageType") StorageTypeEnum storageType,
                                         @Param("visibility") VisibilityEnum visibility);

    /**
     * 根据文件夹名查询文件夹
     *
     * @param folderName 文件夹名
     * @param folderPath 文件夹绝对路径
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 是否存在符合的文件夹(> 0存在)
     */
    Integer isExistFolderByFolderPath(@Param("folderName") String folderName,
                                      @Param("folderPath") String folderPath,
                                      @Param("storageType") StorageTypeEnum storageType,
                                      @Param("visibility") VisibilityEnum visibility);

    /**
     * 根据文件路径查询文件夹
     *
     * @param folderPath 文件路径
     * @param storageType 存储类型
     * @param visibility 能见度
     * @return 文件夹列表
     */
    List<String> listFolderPathByFolderName(@Param("folderPath") String folderPath, @Param("storageType") StorageTypeEnum storageType,
                                            @Param("visibility") VisibilityEnum visibility);

    /**
     * 查询指定路径下的文件夹
     *
     * @param folderPath 文件夹路径
     * @param storageType 存储类型
     * @param visibility 能见度
     * @param userId 用户 ID
     * @return 文件夹名称列表
     */
    List<String> listFolderNameByFolderPath(@Param("folderPath") String folderPath, @Param("storageType") StorageTypeEnum storageType,
                                            @Param("visibility") VisibilityEnum visibility, @Param("userId") Long userId);

}