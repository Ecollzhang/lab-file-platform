package com.lab.file.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.file.fileservice.entity.FileShareUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分享用户权限 Mapper
 */
@Mapper
public interface FileShareUserMapper extends BaseMapper<FileShareUser> {
}
