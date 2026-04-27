package com.lab.file.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.file.userservice.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}