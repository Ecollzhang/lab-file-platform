package com.lab.file.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.file.fileservice.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件 Mapper
 */
@Mapper
public interface FileMapper extends BaseMapper<FileEntity> {
}