package com.lab.file.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分享实体
 */
@Data
@TableName("t_file_share")
public class FileShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;           // 文件ID

    private Long shareUserId;      // 分享用户ID

    private String shareCode;      // 分享码

    private Integer shareType;     // 分享类型 1-公开分享, 2-私密分享

    private String shareUrl;       // 分享链接

    private LocalDateTime expireTime; // 过期时间

    private Integer downloadCount; // 下载次数

    private Integer maxDownloadCount; // 最大下载次数，0表示无限制

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer status;        // 状态 0-失效, 1-有效
}