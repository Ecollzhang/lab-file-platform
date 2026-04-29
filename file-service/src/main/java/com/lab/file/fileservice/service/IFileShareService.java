package com.lab.file.fileservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.file.fileservice.entity.FileEntity;
import com.lab.file.fileservice.entity.FileShare;
import com.lab.file.fileservice.entity.UserInfo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件分享服务接口
 */
public interface IFileShareService extends IService<FileShare> {

    /**
     * 创建文件分享
     *
     * @param fileId           文件或文件夹ID
     * @param shareUserId      分享用户ID
     * @param shareType        分享类型 1-公开分享, 2-私密分享
     * @param maxDownloadCount 最大下载次数（0表示无限制）
     * @param expireHours      过期时间（小时），null表示永不过期
     * @param targetUserIds    私密分享时允许访问的用户ID列表
     * @return 分享记录
     */
    FileShare createShare(Long fileId, Long shareUserId, Integer shareType, Integer maxDownloadCount, Long expireHours, List<Long> targetUserIds);

    /**
     * 根据分享码获取分享信息
     */
    FileShare getShareByCode(String shareCode);

    /**
     * 验证分享链接是否有效
     */
    boolean validateShare(String shareCode);

    /**
     * 检查用户是否有权限访问该分享（私密分享校验）
     */
    void checkShareAccess(String shareCode, Long userId);

    /**
     * 取消分享
     */
    boolean cancelShare(Long shareId, Long userId);

    /**
     * 通过分享码下载文件
     */
    void shareDownload(String shareCode, Long userId, HttpServletResponse response);

    /**
     * 通过分享码预览文件（内联显示）
     */
    void sharePreview(String shareCode, Long userId, HttpServletResponse response);

    /**
     * 通过分享码下载分享目录中的指定文件
     */
    void shareDownloadFile(String shareCode, Long fileId, Long userId, HttpServletResponse response);

    /**
     * 通过分享码预览分享目录中的指定文件
     */
    void sharePreviewFile(String shareCode, Long fileId, Long userId, HttpServletResponse response);

    /**
     * 获取分享的文件/目录列表
     *
     * @param shareCode 分享码
     * @param parentId  父目录ID，0表示分享的根目录
     * @param userId    访问用户ID（私密分享校验用）
     * @return 文件列表
     */
    List<FileEntity> getShareFileList(String shareCode, Long parentId, Long userId);

    /**
     * 通过分享码上传文件到共享目录
     */
    FileEntity shareUpload(String shareCode, MultipartFile file, Long parentId, String description, Long uploadUserId);

    /**
     * 获取用户的所有分享记录
     */
    List<FileShare> getUserShares(Long userId);

    /**
     * 获取所有启用状态的用户（用于分享时选择）
     */
    List<UserInfo> getAllUsers();

    /**
     * 获取私密分享允许访问的用户ID列表
     */
    List<Long> getShareTargetUserIds(Long shareId);
}
