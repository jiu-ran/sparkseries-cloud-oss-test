package com.sparkseries.module.oss.file.dao;


import com.sparkeries.enums.StorageTypeEnum;
import com.sparkseries.module.oss.file.entity.FileMetadataEntity;
import com.sparkseries.module.oss.file.entity.FolderMetadataEntity;
import com.sparkseries.module.oss.file.vo.FileInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件元数据数据访问对象接口
 */
@Mapper
public interface FileMetadataMapper {

    // 文件元数据相关操作

    /**
     * 插入文件元数据
     *
     * @param file 文件元数据
     * @return 插入的文件元数据数量
     */
    Integer insertFile(@Param("file") FileMetadataEntity file);


    /**
     * 根据id删除文件元数据
     *
     * @param id 文件 id
     * @param storageType 存储类型
     * @return 删除的文件元数据数量
     */
    Integer deleteFileById(@Param("id") Long id, @Param("storageType") StorageTypeEnum storageType);


    /**
     * 根据路径删除文件元数据
     *
     * @param path 文件路径
     * @param storageType 存储类型
     * @return 删除的文件元数据数量
     */
    Integer deleteFileByPath(@Param("path") String path, @Param("storageType") StorageTypeEnum storageType);


    /**
     * 重命名
     *
     * @param id 文件 id
     * @param filename 文件名
     * @param storageType 存储类型
     * @return 更新的文件元数据数量
     */
    Integer updateFileName(@Param("id") Long id, @Param("fileName") String filename,
                           @Param("storageType") StorageTypeEnum storageType);

    /**
     * 修改路径
     *
     * @param id 文件id
     * @param path 文件路径
     * @param storageType 存储类型
     * @return 更新的文件元数据数量
     */
    Integer updatePath(@Param("id") Long id, @Param("path") String path,
                       @Param("storageType") StorageTypeEnum storageType);

    /**
     * 修改文件名和路径
     *
     * @param id 文件id
     * @param filename 文件名
     * @param path 文件路径
     * @param storageType 存储类型
     * @return 更新的文件元数据数量
     */
    Integer updateFileNameAndPath(@Param("id") Long id, @Param("fileName") String filename,
                                  @Param("path") String path, @Param("storageType") StorageTypeEnum storageType);

    /**
     * 根据文件名查询文件
     *
     * @param name 文件名
     * @param path 文件路径
     * @return 是否存在符合的文件(> 0存在)
     */
    Integer isExistFileByName(@Param("name") String name, @Param("path") String path,
                              @Param("storageType") StorageTypeEnum storageType);


    /**
     * 根据文件id查询文件
     *
     * @param id 文件id
     * @param storageType 存储类型
     * @return 是否存在符合的文件(> 0存在)
     */
    Integer isExistFileById(@Param("id") Long id, @Param("storageType") StorageTypeEnum storageType);

    /**
     * 根据文件id查询文件
     *
     * @param id 文件id
     * @param storageType 存储类型
     * @return 文件元数据
     */
    FileMetadataEntity getFileMetadataById(@Param("id") Long id, @Param("storageType") StorageTypeEnum storageType);


    /**
     * 根据文件路径查询文件元数据
     *
     * @param path 文件路径
     * @param storageType 存储类型
     * @return 文件元数据列表
     */
    List<FileInfoVO> listFileByPath(@Param("path") String path, @Param("storageType") StorageTypeEnum storageType);

    // 文件夹元数据相关操作

    /**
     * 插入文件夹元数据
     *
     * @param folder 文件夹元数据
     * @return 插入的文件夹元数据数量
     */
    Integer insertFolder(@Param("folder") FolderMetadataEntity folder);

    /**
     * 根据id删除文件夹元数据
     *
     * @param id 文件夹 id
     * @param storageType 存储类型
     * @return 删除的文件夹元数据数量
     */
    Integer deleteFolderById(@Param("id") Long id, @Param("storageType") StorageTypeEnum storageType);

    /**
     * 根据路径删除文件夹元数据
     *
     * @param path 文件夹路径
     * @param storageType 存储类型
     * @return 删除的文件夹元数据数量
     */
    Integer deleteFolderByPath(@Param("path") String path, @Param("storageType") StorageTypeEnum storageType);

    /**
     * 根据文件夹名查询文件夹
     *
     * @param name 文件夹名
     * @param path 文件夹路径
     * @return 是否存在符合的文件夹(> 0存在)
     */
    Integer isExistFolderByName(@Param("name") String name, @Param("path") String path);

    /**
     * 根据文件路径查询文件夹
     *
     * @param path 文件路径
     * @param storageType 存储类型
     * @return 文件夹列表
     */
    List<String> listFolderByPath(@Param("path") String path, @Param("storageType") StorageTypeEnum storageType);

    /**
     * 查询指定路径下的文件夹
     *
     * @param absolutePath 绝对路径
     * @param storageType 存储类型
     * @return 文件夹名称列表
     */
    List<String> listFolderNameByPath(@Param("absolutePath") String absolutePath, @Param("storageType") StorageTypeEnum storageType);

}