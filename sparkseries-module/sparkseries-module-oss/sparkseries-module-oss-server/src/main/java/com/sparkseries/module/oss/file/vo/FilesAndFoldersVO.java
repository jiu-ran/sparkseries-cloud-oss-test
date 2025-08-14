package com.sparkseries.module.oss.file.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 文件和文件夹信息VO
 */
@Data
public class FilesAndFoldersVO {
    private List<FileInfoVO> files;
    private Set<FolderInfoVO> folders;

    public FilesAndFoldersVO(List<FileInfoVO> files, Set<FolderInfoVO> folders) {
        this.files = files;
        this.folders = folders;
    }
}
