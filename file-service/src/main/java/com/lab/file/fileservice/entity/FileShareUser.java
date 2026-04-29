package com.lab.file.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享用户权限（私密分享时指定允许访问的用户）
 */
@Data
@TableName("t_file_share_user")
public class FileShareUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shareId;       // 分享ID

    private Long userId;        // 允许访问的用户ID

    private LocalDateTime createTime;
}
