package com.lab.file.fileservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.file.fileservice.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 */
public interface IFileService extends IService<FileEntity> {

    /**
     * 上传文件
     */
    FileEntity uploadFile(MultipartFile file, Long userId, Long parentId, String description);

    /**
     * 创建文件夹
     */
    FileEntity createFolder(String folderName, Long userId, Long parentId);

    /**
     * 获取用户文件列表
     */
    List<FileEntity> getFileListByParentId(Long userId, Long parentId);

    /**
     * 删除文件或文件夹
     */
    boolean deleteFile(Long fileId, Long userId);

    /**
     * 重命名文件或文件夹
     */
    boolean renameFile(Long fileId, String newName, Long userId);

    /**
     * 移动文件或文件夹
     */
    boolean moveFile(Long fileId, Long newParentId, Long userId);

    /**
     * 下载文件
     */
    byte[] downloadFile(Long fileId, Long userId);
}