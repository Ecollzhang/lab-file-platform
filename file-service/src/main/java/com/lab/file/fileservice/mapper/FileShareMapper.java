package com.lab.file.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.file.fileservice.entity.FileShare;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分享 Mapper
 */
@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {
}