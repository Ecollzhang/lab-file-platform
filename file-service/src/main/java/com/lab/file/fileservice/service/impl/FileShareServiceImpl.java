package com.lab.file.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.file.common.exception.BusinessException;
import com.lab.file.fileservice.entity.FileEntity;
import com.lab.file.fileservice.entity.FileShare;
import com.lab.file.fileservice.entity.FileShareUser;
import com.lab.file.fileservice.entity.UserInfo;
import com.lab.file.fileservice.mapper.FileShareMapper;
import com.lab.file.fileservice.mapper.FileShareUserMapper;
import com.lab.file.fileservice.mapper.UserInfoMapper;
import com.lab.file.fileservice.service.IFileService;
import com.lab.file.fileservice.service.IFileShareService;
import com.lab.file.fileservice.service.IOperationLogService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件分享服务实现
 */
@Service
@Transactional
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements IFileShareService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private IFileService fileService;

    @Autowired
    private IOperationLogService operationLogService;

    @Autowired
    private FileShareUserMapper fileShareUserMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Value("${minio.bucket}")
    private String bucket;

    private static final String SHARE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHARE_CODE_LENGTH = 8;
    private static final Random RANDOM = new Random();

    @Override
    public FileShare createShare(Long fileId, Long shareUserId, Integer shareType, Integer maxDownloadCount, Long expireHours, List<Long> targetUserIds) {
        // 验证文件/文件夹是否存在且属于当前用户
        FileEntity file = fileService.getById(fileId);
        if (file == null) {
            throw new BusinessException(404, "文件或文件夹不存在");
        }
        if (!file.getUserId().equals(shareUserId)) {
            throw new BusinessException(403, "无权限分享该文件");
        }
        if (file.getStatus() != 1) {
            throw new BusinessException(400, "文件或文件夹已删除");
        }

        // 生成唯一分享码
        String shareCode = generateUniqueShareCode();

        // 构建分享URL
        String shareUrl = "/share/" + shareCode;

        // 设置过期时间
        LocalDateTime expireTime = null;
        if (expireHours != null && expireHours > 0) {
            expireTime = LocalDateTime.now().plusHours(expireHours);
        }

        // 创建分享记录
        FileShare fileShare = new FileShare();
        fileShare.setFileId(fileId);
        fileShare.setShareUserId(shareUserId);
        fileShare.setShareCode(shareCode);
        fileShare.setShareType(shareType != null ? shareType : 1);
        fileShare.setShareUrl(shareUrl);
        fileShare.setExpireTime(expireTime);
        fileShare.setDownloadCount(0);
        fileShare.setMaxDownloadCount(maxDownloadCount != null ? maxDownloadCount : 0);
        fileShare.setCreateTime(LocalDateTime.now());
        fileShare.setUpdateTime(LocalDateTime.now());
        fileShare.setStatus(1);

        this.save(fileShare);

        // 私密分享：存储允许访问的用户
        if (shareType != null && shareType == 2 && targetUserIds != null && !targetUserIds.isEmpty()) {
            for (Long uid : targetUserIds) {
                FileShareUser su = new FileShareUser();
                su.setShareId(fileShare.getId());
                su.setUserId(uid);
                su.setCreateTime(LocalDateTime.now());
                fileShareUserMapper.insert(su);
            }
        }

        // 记录操作日志
        operationLogService.recordOperationLog(
                shareUserId,
                "未知用户名",
                "CREATE_SHARE",
                "创建分享: " + file.getOriginalName() + ", 分享码: " + shareCode + ", 类型: " + (shareType == 2 ? "私密" : "公开"),
                "127.0.0.1",
                "Unknown"
        );

        return fileShare;
    }

    @Override
    public FileShare getShareByCode(String shareCode) {
        QueryWrapper<FileShare> wrapper = new QueryWrapper<>();
        wrapper.eq("share_code", shareCode);
        return this.getOne(wrapper);
    }

    @Override
    public boolean validateShare(String shareCode) {
        FileShare share = getShareByCode(shareCode);
        return validateShare(share);
    }

    /**
     * 验证分享是否有效
     */
    private boolean validateShare(FileShare share) {
        if (share == null) {
            return false;
        }
        if (share.getStatus() != 1) {
            return false;
        }
        // 检查过期时间
        if (share.getExpireTime() != null && LocalDateTime.now().isAfter(share.getExpireTime())) {
            share.setStatus(0);
            share.setUpdateTime(LocalDateTime.now());
            this.updateById(share);
            return false;
        }
        // 检查下载次数
        if (share.getMaxDownloadCount() > 0 && share.getDownloadCount() >= share.getMaxDownloadCount()) {
            return false;
        }
        return true;
    }

    /**
     * 验证并获取有效分享，无效则抛出异常
     */
    private FileShare validateAndGetShare(String shareCode) {
        FileShare share = getShareByCode(shareCode);
        if (!validateShare(share)) {
            throw new BusinessException(400, "分享链接已失效或不存在");
        }
        return share;
    }

    @Override
    public void checkShareAccess(String shareCode, Long userId) {
        FileShare share = getShareByCode(shareCode);
        if (share == null) {
            throw new BusinessException(404, "分享码不存在");
        }
        if (!validateShare(share)) {
            throw new BusinessException(400, "分享链接已失效");
        }
        // 私密分享：校验用户是否有权限
        if (share.getShareType() == 2) {
            if (userId == null) {
                throw new BusinessException(403, "私密分享需要提供用户ID");
            }
            long count = fileShareUserMapper.selectCount(
                    new QueryWrapper<FileShareUser>()
                            .eq("share_id", share.getId())
                            .eq("user_id", userId)
            );
            if (count == 0) {
                throw new BusinessException(403, "您没有权限访问该分享");
            }
        }
    }

    @Override
    public boolean cancelShare(Long shareId, Long userId) {
        FileShare share = this.getById(shareId);
        if (share == null) {
            throw new BusinessException(404, "分享记录不存在");
        }
        if (!share.getShareUserId().equals(userId)) {
            throw new BusinessException(403, "无权限取消该分享");
        }

        share.setStatus(0);
        share.setUpdateTime(LocalDateTime.now());
        this.updateById(share);

        // 记录操作日志
        operationLogService.recordOperationLog(
                userId,
                "未知用户名",
                "CANCEL_SHARE",
                "取消分享: " + share.getShareCode(),
                "127.0.0.1",
                "Unknown"
        );

        return true;
    }

    @Override
    public void shareDownload(String shareCode, Long userId, HttpServletResponse response) {
        FileShare share = validateAndGetShare(shareCode);
        checkShareAccess(shareCode, userId);

        FileEntity file = fileService.getById(share.getFileId());
        if (file == null || file.getStatus() != 1) {
            throw new BusinessException(404, "分享的文件不存在或已删除");
        }
        if (file.getIsDirectory() == 1) {
            throw new BusinessException(400, "无法下载文件夹，请指定 fileId 参数下载文件夹中的具体文件");
        }

        writeFileToResponse(file, response, true);

        // 更新下载次数
        share.setDownloadCount(share.getDownloadCount() + 1);
        share.setUpdateTime(LocalDateTime.now());
        this.updateById(share);

        // 记录操作日志
        operationLogService.recordOperationLog(
                share.getShareUserId(),
                "未知用户名",
                "SHARE_DOWNLOAD",
                "通过分享码下载文件: " + file.getOriginalName() + ", 分享码: " + shareCode,
                "127.0.0.1",
                "Unknown"
        );
    }

    @Override
    public void sharePreview(String shareCode, Long userId, HttpServletResponse response) {
        FileShare share = validateAndGetShare(shareCode);
        checkShareAccess(shareCode, userId);

        FileEntity file = fileService.getById(share.getFileId());
        if (file == null || file.getStatus() != 1) {
            throw new BusinessException(404, "分享的文件不存在或已删除");
        }
        if (file.getIsDirectory() == 1) {
            throw new BusinessException(400, "无法预览文件夹，请指定 fileId 参数预览文件夹中的具体文件");
        }

        writeFileToResponse(file, response, false);

        // 更新下载次数
        share.setDownloadCount(share.getDownloadCount() + 1);
        share.setUpdateTime(LocalDateTime.now());
        this.updateById(share);
    }

    @Override
    public void shareDownloadFile(String shareCode, Long fileId, Long userId, HttpServletResponse response) {
        FileShare share = validateAndGetShare(shareCode);
        checkShareAccess(shareCode, userId);

        // 验证请求的文件在分享范围内
        FileEntity file = validateFileInShare(share, fileId);

        writeFileToResponse(file, response, true);

        // 更新下载次数
        share.setDownloadCount(share.getDownloadCount() + 1);
        share.setUpdateTime(LocalDateTime.now());
        this.updateById(share);

        // 记录操作日志
        operationLogService.recordOperationLog(
                share.getShareUserId(),
                "未知用户名",
                "SHARE_DOWNLOAD",
                "通过分享码下载文件: " + file.getOriginalName() + ", 分享码: " + shareCode,
                "127.0.0.1",
                "Unknown"
        );
    }

    @Override
    public void sharePreviewFile(String shareCode, Long fileId, Long userId, HttpServletResponse response) {
        FileShare share = validateAndGetShare(shareCode);
        checkShareAccess(shareCode, userId);

        // 验证请求的文件在分享范围内
        FileEntity file = validateFileInShare(share, fileId);

        writeFileToResponse(file, response, false);

        // 更新下载次数
        share.setDownloadCount(share.getDownloadCount() + 1);
        share.setUpdateTime(LocalDateTime.now());
        this.updateById(share);
    }

    /**
     * 验证文件是否在分享范围内
     */
    private FileEntity validateFileInShare(FileShare share, Long fileId) {
        FileEntity targetFile = fileService.getById(fileId);
        if (targetFile == null || targetFile.getStatus() != 1) {
            throw new BusinessException(404, "文件不存在或已删除");
        }
        if (targetFile.getIsDirectory() == 1) {
            throw new BusinessException(400, "无法下载或预览文件夹");
        }

        // 如果分享的就是这个文件，直接返回
        if (share.getFileId().equals(fileId)) {
            return targetFile;
        }

        // 验证分享的文件/目录
        FileEntity sharedEntity = fileService.getById(share.getFileId());
        if (sharedEntity == null || sharedEntity.getStatus() != 1) {
            throw new BusinessException(404, "分享的文件或目录不存在");
        }

        // 如果分享的是目录，验证文件是否在目录下
        if (sharedEntity.getIsDirectory() == 1) {
            if (!isDescendantOf(targetFile, sharedEntity)) {
                throw new BusinessException(403, "该文件不在分享范围内");
            }
            return targetFile;
        }

        // 分享的是单个文件但 fileId 不匹配
        throw new BusinessException(403, "该文件不在分享范围内");
    }

    /**
     * 将文件写入HTTP响应
     */
    private void writeFileToResponse(FileEntity file, HttpServletResponse response, boolean asAttachment) {
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(file.getFilePath())
                            .build()
            );

            if (asAttachment) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=\"" +
                        new String(file.getOriginalName().getBytes("UTF-8"), "ISO-8859-1") + "\"");
            } else {
                String contentType = getContentType(file.getFileExtension());
                response.setContentType(contentType);
                response.setHeader("Content-Disposition", "inline; filename=\"" + file.getOriginalName() + "\"");
            }

            if (file.getFileSize() != null) {
                response.setHeader("Content-Length", String.valueOf(file.getFileSize()));
            }

            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.flush();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, (asAttachment ? "下载" : "预览") + "失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileEntity> getShareFileList(String shareCode, Long parentId, Long userId) {
        FileShare share = validateAndGetShare(shareCode);
        checkShareAccess(shareCode, userId);

        // 获取分享的文件/目录
        FileEntity sharedEntity = fileService.getById(share.getFileId());
        if (sharedEntity == null || sharedEntity.getStatus() != 1) {
            throw new BusinessException(404, "分享的文件或目录不存在");
        }

        // 如果分享的是单个文件，直接返回该文件
        if (sharedEntity.getIsDirectory() != 1) {
            return List.of(sharedEntity);
        }

        // parentId为0或null时，从分享的根目录开始
        Long queryParentId;
        if (parentId == null || parentId == 0) {
            queryParentId = sharedEntity.getId();
        } else {
            FileEntity targetDir = fileService.getById(parentId);
            if (targetDir == null || targetDir.getStatus() != 1) {
                throw new BusinessException(404, "目标目录不存在");
            }
            if (!isDescendantOf(targetDir, sharedEntity)) {
                throw new BusinessException(403, "目标目录不在分享范围内");
            }
            queryParentId = parentId;
        }

        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", queryParentId)
                .eq("status", 1)
                .orderByDesc("is_directory")
                .orderByDesc("create_time");

        return fileService.list(wrapper);
    }

    @Override
    @Transactional
    public FileEntity shareUpload(String shareCode, MultipartFile file, Long parentId, String description, Long uploadUserId) {
        FileShare share = validateAndGetShare(shareCode);

        // 获取分享的目录
        FileEntity sharedEntity = fileService.getById(share.getFileId());
        if (sharedEntity == null || sharedEntity.getStatus() != 1) {
            throw new BusinessException(404, "分享的目录不存在或已删除");
        }
        if (sharedEntity.getIsDirectory() != 1) {
            throw new BusinessException(400, "该分享不是文件夹，无法上传文件");
        }

        // 确定上传的目标目录
        Long targetParentId;
        if (parentId == null || parentId == 0) {
            targetParentId = sharedEntity.getId();
        } else {
            FileEntity targetDir = fileService.getById(parentId);
            if (targetDir == null || targetDir.getStatus() != 1 || targetDir.getIsDirectory() != 1) {
                throw new BusinessException(400, "目标目录不存在");
            }
            if (!isDescendantOf(targetDir, sharedEntity)) {
                throw new BusinessException(403, "目标目录不在分享范围内");
            }
            targetParentId = parentId;
        }

        try {
            String originalName = file.getOriginalFilename();
            String extension = FileUtil.extName(originalName != null ? originalName : "unknown");
            String fileName = UUID.randomUUID().toString() + "." + extension;
            String filePath = "user/" + sharedEntity.getUserId() + "/" +
                    LocalDateTime.now().getYear() + "/" +
                    LocalDateTime.now().getMonthValue() + "/" + fileName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setOriginalName(originalName);
            fileEntity.setFilePath(filePath);
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFileType(file.getContentType());
            fileEntity.setFileExtension(extension);
            fileEntity.setUserId(sharedEntity.getUserId());
            fileEntity.setParentId(targetParentId);
            fileEntity.setIsDirectory(0);
            fileEntity.setDescription(description != null ? description :
                    "通过分享码上传" + (uploadUserId != null ? " (上传用户ID: " + uploadUserId + ")" : ""));
            fileEntity.setCreateTime(LocalDateTime.now());
            fileEntity.setUpdateTime(LocalDateTime.now());
            fileEntity.setStatus(1);
            fileEntity.setVersion("1.0");

            fileService.save(fileEntity);

            return fileEntity;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileShare> getUserShares(Long userId) {
        QueryWrapper<FileShare> wrapper = new QueryWrapper<>();
        wrapper.eq("share_user_id", userId)
                .orderByDesc("create_time");
        return this.list(wrapper);
    }

    @Override
    public List<UserInfo> getAllUsers() {
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .orderByAsc("username");
        return userInfoMapper.selectList(wrapper);
    }

    @Override
    public List<Long> getShareTargetUserIds(Long shareId) {
        List<FileShareUser> list = fileShareUserMapper.selectList(
                new QueryWrapper<FileShareUser>().eq("share_id", shareId)
        );
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(FileShareUser::getUserId).collect(Collectors.toList());
    }

    /**
     * 检查目标文件/目录是否是分享根目录的后代
     */
    private boolean isDescendantOf(FileEntity target, FileEntity root) {
        if (target.getParentId().equals(root.getId())) {
            return true;
        }
        if (target.getParentId() == null || target.getParentId() == 0) {
            return false;
        }
        FileEntity parent = fileService.getById(target.getParentId());
        if (parent == null) {
            return false;
        }
        return isDescendantOf(parent, root);
    }

    /**
     * 生成唯一的分享码
     */
    private String generateUniqueShareCode() {
        String code;
        do {
            code = generateShareCode();
        } while (this.count(new QueryWrapper<FileShare>().eq("share_code", code)) > 0);
        return code;
    }

    /**
     * 生成随机分享码（8位字母数字组合）
     */
    private String generateShareCode() {
        StringBuilder sb = new StringBuilder(SHARE_CODE_LENGTH);
        for (int i = 0; i < SHARE_CODE_LENGTH; i++) {
            sb.append(SHARE_CODE_CHARS.charAt(RANDOM.nextInt(SHARE_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 根据文件扩展名获取MIME类型
     */
    private String getContentType(String extension) {
        if (extension == null) return "application/octet-stream";
        switch (extension.toLowerCase()) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "bmp": return "image/bmp";
            case "svg": return "image/svg+xml";
            case "ico": return "image/x-icon";
            case "pdf": return "application/pdf";
            case "txt": return "text/plain; charset=UTF-8";
            case "html": case "htm": return "text/html; charset=UTF-8";
            case "json": return "application/json; charset=UTF-8";
            case "xml": return "application/xml; charset=UTF-8";
            case "css": return "text/css; charset=UTF-8";
            case "js": return "application/javascript; charset=UTF-8";
            case "md": return "text/markdown; charset=UTF-8";
            case "yaml": case "yml": return "text/yaml; charset=UTF-8";
            case "java": case "py": case "c": case "cpp": case "h": case "hpp":
            case "cs": case "php": case "rb": case "go": case "rs": case "swift":
            case "kt": case "scala": case "sql": case "sh": case "bat":
                return "text/plain; charset=UTF-8";
            case "mp4": return "video/mp4";
            case "webm": return "video/webm";
            case "avi": return "video/x-msvideo";
            case "mov": return "video/quicktime";
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "flac": return "audio/flac";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "zip": return "application/zip";
            case "rar": return "application/vnd.rar";
            default: return "application/octet-stream";
        }
    }
}
