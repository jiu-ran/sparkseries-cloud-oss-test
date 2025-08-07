package com.sparkseries.module.file.dao;


import com.sparkseries.module.file.entity.FileMetadataEntity;
import com.sparkseries.module.file.vo.FileInfoVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件元数据数据访问对象接口
 */
@Mapper
public interface FileMetadataMapper {

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
     * @param id 文件id
     * @return 删除的文件元数据数量
     */
    Integer deleteFileById(@Param("id") Long id);

    /**
     * 根据路径删除文件元数据
     *
     * @param path 文件路径
     * @return 删除的文件元数据数量
     */
    Integer deleteFileByPath(@Param("path") String path);

    /**
     * 重命名
     *
     * @param id           文件id
     * @param filename     id+文件名
     * @param originalName 文件名
     * @return 更新的文件元数据数量
     */
    Integer updateFileName(@Param("id") Long id, @Param("fileName") String filename,
            @Param("originalName") String originalName);

    /**
     * 修改路径
     *
     * @param id   文件id
     * @param path 文件路径
     * @return 更新的文件元数据数量
     */
    Integer updatePath(@Param("id") Long id, @Param("path") String path);

    /**
     * 修改文件名和路径
     *
     * @param id       文件id
     * @param filename 文件名
     * @param path     文件路径
     * @return 更新的文件元数据数量
     */
    Integer updateFileNameAndPath(@Param("id") Long id, @Param("fileName") String filename, @Param("path") String path);

    /**
     * 根据文件名查询文件
     *
     * @param name 文件名
     * @param path 文件路径
     * @return 是否存在符合的文件(> 0存在)
     */
    Integer isExistFileByName(@Param("name") String name, @Param("path") String path);

    /**
     * 根据文件id查询文件
     *
     * @param id 文件id
     * @return 是否存在符合的文件(> 0存在)
     */
    Integer isExistFileById(@Param("id") Long id);

    /**
     * 根据文件id查询文件
     *
     * @param id 文件id
     * @return 文件元数据
     */
    FileMetadataEntity getFileMetadataById(@Param("id") Long id);

    /**
     * 根据文件路径查询文件元数据
     *
     * @param path 文件路径
     * @return 文件元数据列表
     */
    List<FileInfoVO> listLocalMetadataByPath(@Param("path") String path);

    /**
     * 根据文件路径查询 OSS 文件元数据
     *
     * @param path 文件路径
     * @return 文件元数据列表
     */
    List<FileInfoVO> listOssMetadataByPath(@Param("path") String path);

    /**
     * 根据文件路径查询 OSS 文件夹
     *
     * @param path 文件路径
     * @return 文件夹列表
     */
    List<String> listOssFolderByPath(@Param("path") String path);

    /**
     * 根据文件路径查询 COS 文件元数据
     *
     * @param path 文件路径
     * @return 文件元数据列表
     */
    List<FileInfoVO> listCosMetadataByPath(@Param("path") String path);

    /**
     * 根据文件路径查询 COS 文件夹
     *
     * @param path 文件路径
     * @return 文件夹列表
     */
    List<String> listCosFolderByPath(@Param("path") String path);

    /**
     * 根据文件路径查询 KODO 文件元数据
     *
     * @param path 文件路径
     * @return 文件元数据列表
     */
    List<FileInfoVO> listKodoMetadataByPath(@Param("path") String path);

    /**
     * 根据文件路径查询 KODO 文件夹
     *
     * @param path 文件路径
     * @return 文件夹列表
     */
    List<String> listKodoFolderByPath(@Param("path") String path);

    /**
     * 根据文件路径查询 MINIO 文件元数据
     *
     * @param path 文件路径
     * @return 文件元数据列表
     */
    List<FileInfoVO> listMinioMetadataByPath(@Param("path") String path);

    /**
     * 根据文件路径查询 MINIO 文件夹
     *
     * @param path 文件路径
     * @return 文件夹列表
     */
    List<String> listMinioFolderByPath(@Param("path") String path);
}