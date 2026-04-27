package com.lab.file.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.file.common.exception.BusinessException;
import com.lab.file.fileservice.entity.FileEntity;
import com.lab.file.fileservice.entity.FileShare;
import com.lab.file.fileservice.entity.OperationLog;
import com.lab.file.fileservice.mapper.FileMapper;
import com.lab.file.fileservice.service.*;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文件服务实现
 */
@Service
@Transactional
public class FileServiceImpl extends ServiceImpl<FileMapper, FileEntity> implements IFileService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Autowired
    private IFileShareService fileShareService;

    @Autowired
    private IOperationLogService operationLogService;

    @Override
    public FileEntity uploadFile(MultipartFile file, Long userId, Long parentId, String description) {
        try {
            // 验证父目录是否存在且属于当前用户
            if (parentId != 0) {
                FileEntity parentDir = this.getById(parentId);
                if (parentDir == null || !parentDir.getUserId().equals(userId) || parentDir.getIsDirectory() != 1) {
                    throw new BusinessException(403, "父目录不存在或无权限");
                }
            }

            // 生成文件存储路径
            String originalName = file.getOriginalFilename();
            String extension = FileUtil.extName(originalName != null ? originalName : "unknown");
            String fileName = UUID.randomUUID().toString() + "." + extension;
            String filePath = userId + "/" + LocalDateTime.now().getYear() + "/" + LocalDateTime.now().getMonthValue() + "/" + fileName;

            // 上传到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 保存文件记录
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setOriginalName(originalName);
            fileEntity.setFilePath(filePath);
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFileType(file.getContentType());
            fileEntity.setFileExtension(extension);
            fileEntity.setUserId(userId);
            fileEntity.setParentId(parentId);
            fileEntity.setIsDirectory(0);
            fileEntity.setDescription(description);
            fileEntity.setCreateTime(LocalDateTime.now());
            fileEntity.setUpdateTime(LocalDateTime.now());
            fileEntity.setStatus(1);
            fileEntity.setVersion("1.0");

            this.save(fileEntity);

            // 记录操作日志
            operationLogService.recordOperationLog(
                    userId,
                    "未知用户名", // 实际应用中应从上下文获取用户名
                    "UPLOAD_FILE",
                    "上传文件: " + originalName,
                    "127.0.0.1", // 实际应用中应从请求中获取IP
                    "Unknown" // 实际应用中应从请求中获取UserAgent
            );

            return fileEntity;
        } catch (Exception e) {
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public FileEntity createFolder(String folderName, Long userId, Long parentId) {
        // 验证父目录是否存在且属于当前用户
        if (parentId != 0) {
            FileEntity parentDir = this.getById(parentId);
            if (parentDir == null || !parentDir.getUserId().equals(userId) || parentDir.getIsDirectory() != 1) {
                throw new BusinessException(403, "父目录不存在或无权限");
            }
        }

        // 检查同名文件夹是否已存在
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId)
                .eq("user_id", userId)
                .eq("file_name", folderName)
                .eq("is_directory", 1)
                .eq("status", 1);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(400, "同名文件夹已存在");
        }

        FileEntity folder = new FileEntity();
        folder.setFileName(folderName);
        folder.setOriginalName(folderName);
        folder.setFilePath("");
        folder.setFileSize(0L);
        folder.setFileType("directory");
        folder.setFileExtension("");
        folder.setUserId(userId);
        folder.setParentId(parentId);
        folder.setIsDirectory(1);
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        folder.setStatus(1);
        folder.setVersion("1.0");

        this.save(folder);

        // 记录操作日志
        operationLogService.recordOperationLog(
                userId,
                "未知用户名",
                "CREATE_FOLDER",
                "创建文件夹: " + folderName,
                "127.0.0.1",
                "Unknown"
        );

        return folder;
    }

    @Override
    public List<FileEntity> getFileListByParentId(Long userId, Long parentId) {
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("parent_id", parentId)
                .eq("status", 1)
                .orderByDesc("is_directory") // 目录排在前面
                .orderByDesc("create_time"); // 按创建时间倒序
        return this.list(wrapper);
    }

    @Override
    public boolean deleteFile(Long fileId, Long userId) {
        FileEntity file = this.getById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(403, "文件不存在或无权限");
        }

        // 如果是目录，需要递归删除子文件
        if (file.getIsDirectory() == 1) {
            deleteDirectoryRecursive(fileId, userId);
        } else {
            // 如果不是目录，则删除MinIO中的文件
            try {
                minioClient.removeObject(
                        io.minio.RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(file.getFilePath())
                                .build()
                );
            } catch (Exception e) {
                // 删除MinIO文件失败时，继续删除数据库记录
            }
        }

        // 软删除：更新状态为0
        file.setStatus(0);
        file.setUpdateTime(LocalDateTime.now());
        this.updateById(file);

        // 记录操作日志
        operationLogService.recordOperationLog(
                userId,
                "未知用户名",
                "DELETE_FILE",
                "删除文件: " + file.getOriginalName(),
                "127.0.0.1",
                "Unknown"
        );

        return true;
    }

    /**
     * 递归删除目录及其子文件
     */
    private void deleteDirectoryRecursive(Long dirId, Long userId) {
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", dirId)
                .eq("user_id", userId)
                .eq("status", 1);

        List<FileEntity> children = this.list(wrapper);
        for (FileEntity child : children) {
            if (child.getIsDirectory() == 1) {
                deleteDirectoryRecursive(child.getId(), userId);
            } else {
                try {
                    minioClient.removeObject(
                            io.minio.RemoveObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(child.getFilePath())
                                    .build()
                    );
                } catch (Exception e) {
                    // 删除MinIO文件失败时，继续删除数据库记录
                }
            }

            child.setStatus(0);
            child.setUpdateTime(LocalDateTime.now());
            this.updateById(child);
        }
    }

    @Override
    public boolean renameFile(Long fileId, String newName, Long userId) {
        FileEntity file = this.getById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(403, "文件不存在或无权限");
        }

        file.setFileName(newName);
        file.setOriginalName(newName);
        file.setUpdateTime(LocalDateTime.now());
        this.updateById(file);

        // 记录操作日志
        operationLogService.recordOperationLog(
                userId,
                "未知用户名",
                "RENAME_FILE",
                "重命名文件: " + file.getOriginalName() + " -> " + newName,
                "127.0.0.1",
                "Unknown"
        );

        return true;
    }

    @Override
    public boolean moveFile(Long fileId, Long newParentId, Long userId) {
        FileEntity file = this.getById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(403, "文件不存在或无权限");
        }

        // 验证目标目录是否存在且属于当前用户
        if (newParentId != 0) {
            FileEntity targetDir = this.getById(newParentId);
            if (targetDir == null || !targetDir.getUserId().equals(userId) || targetDir.getIsDirectory() != 1) {
                throw new BusinessException(403, "目标目录不存在或无权限");
            }
        }

        file.setParentId(newParentId);
        file.setUpdateTime(LocalDateTime.now());
        this.updateById(file);

        // 记录操作日志
        operationLogService.recordOperationLog(
                userId,
                "未知用户名",
                "MOVE_FILE",
                "移动文件: " + file.getOriginalName(),
                "127.0.0.1",
                "Unknown"
        );

        return true;
    }

    @Override
    public byte[] downloadFile(Long fileId, Long userId) {
        FileEntity file = this.getById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(403, "文件不存在或无权限");
        }

        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(file.getFilePath())
                            .build()
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();

            // 记录操作日志
            operationLogService.recordOperationLog(
                    userId,
                    "未知用户名",
                    "DOWNLOAD_FILE",
                    "下载文件: " + file.getOriginalName(),
                    "127.0.0.1",
                    "Unknown"
            );

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(500, "文件下载失败: " + e.getMessage());
        }
    }
}