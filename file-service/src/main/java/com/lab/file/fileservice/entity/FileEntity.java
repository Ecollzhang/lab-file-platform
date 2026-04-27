package com.lab.file.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件实体
 */
@Data
@TableName("t_file")
@Schema(description = "文件实体")
public class FileEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "文件名")
    private String fileName;      // 文件名

    @Schema(description = "原始文件名")
    private String originalName;  // 原始文件名

    @Schema(description = "存储路径")
    private String filePath;      // 存储路径

    @Schema(description = "文件大小")
    private Long fileSize;        // 文件大小

    @Schema(description = "文件类型")
    private String fileType;      // 文件类型

    @Schema(description = "文件扩展名")
    private String fileExtension; // 文件扩展名

    @Schema(description = "上传用户ID")
    private Long userId;          // 上传用户ID

    @Schema(description = "父级目录ID，根目录为0")
    private Long parentId;        // 父级目录ID，根目录为0

    @Schema(description = "是否是目录 0-否, 1-是")
    private Integer isDirectory;  // 是否是目录 0-否, 1-是

    @Schema(description = "缩略图路径（如果是图片）")
    private String thumbnailPath; // 缩略图路径（如果是图片）

    @Schema(description = "文件描述")
    private String description;   // 文件描述

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "状态 0-删除, 1-正常")
    private Integer status;       // 状态 0-删除, 1-正常

    @Schema(description = "版本号")
    private String version;       // 版本号

    @Schema(description = "文件MD5值")
    private String fileMd5;  // 文件MD5值
}