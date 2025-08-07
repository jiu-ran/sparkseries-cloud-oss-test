package com.sparkseries.module.file.vo;

import lombok.Data;

import java.util.List;

/**
 * 文件和文件夹信息VO
 */
@Data
public class FilesAndFoldersVO {
    private List<FileInfoVO> files;
    private List<FolderInfoVO> folders;

    public FilesAndFoldersVO(List<FileInfoVO> files, List<FolderInfoVO> folders) {
        this.files = files;
        this.folders = folders;
    }
}
