package com.lab.file.fileservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.file.fileservice.entity.OperationLog;
import com.lab.file.fileservice.mapper.OperationLogMapper;
import com.lab.file.fileservice.service.IOperationLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements IOperationLogService {

    @Override
    public void recordOperationLog(Long userId, String userName, String operationType, String operationDesc, String ipAddress, String userAgent) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUserName(userName);
        log.setOperationType(operationType);
        log.setOperationDesc(operationDesc);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setCreateTime(java.time.LocalDateTime.now());

        this.save(log);
    }
}