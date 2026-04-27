package com.lab.file.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.file.fileservice.entity.FileChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分片 Mapper
 */
@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunk> {
}