package com.lab.file.fileservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.file.fileservice.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息 Mapper（用于获取用户列表）
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}
