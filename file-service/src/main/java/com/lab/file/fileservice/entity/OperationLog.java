package com.lab.file.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("t_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;           // 操作用户ID

    private String userName;       // 操作用户名

    private String operationType;  // 操作类型

    private String operationDesc;  // 操作描述

    private String ipAddress;      // IP地址

    private String userAgent;      // 用户代理

    private LocalDateTime createTime;
}