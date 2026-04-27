package com.lab.file.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分片实体
 */
@Data
@TableName("t_file_chunk")
public class FileChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String chunkIdentifier;  // 文件唯一标识

    private String fileName;         // 文件名

    private Integer chunkNumber;     // 分片序号

    private Long totalChunks;        // 总分片数

    private Long chunkSize;          // 分片大小

    private String chunkPath;        // 分片存储路径

    private Long userId;             // 上传用户ID

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer status;          // 状态 0-未完成, 1-已完成
}