package com.lab.file.fileservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.file.fileservice.entity.OperationLog;

/**
 * 操作日志服务接口
 */
public interface IOperationLogService extends IService<OperationLog> {

    /**
     * 记录操作日志
     */
    void recordOperationLog(Long userId, String userName, String operationType, String operationDesc, String ipAddress, String userAgent);
}