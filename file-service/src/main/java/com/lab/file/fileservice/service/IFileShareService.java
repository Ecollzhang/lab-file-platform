package com.lab.file.fileservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.file.fileservice.entity.FileShare;

/**
 * 文件分享服务接口
 */
public interface IFileShareService extends IService<FileShare> {

    /**
     * 创建文件分享
     */
    FileShare createShare(Long fileId, Long shareUserId, Integer shareType, Integer maxDownloadCount);

    /**
     * 获取分享信息
     */
    FileShare getShareByCode(String shareCode);

    /**
     * 验证分享链接
     */
    boolean validateShare(String shareCode);

    /**
     * 取消分享
     */
    boolean cancelShare(Long shareId, Long userId);
}