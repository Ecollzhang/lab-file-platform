package com.lab.file.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.file.common.vo.LoginUserInfo;
import com.lab.file.userservice.entity.User;

/**
 * 用户服务接口
 */
public interface IUserService extends IService<User> {

    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 用户登录并返回用户信息
     */
    LoginUserInfo loginWithUserInfo(String username, String password);

    /**
     * 注册用户
     */
    void register(User user);

    /**
     * 更新用户信息（带权限控制）
     * @param currentUserId 当前登录用户ID
     * @param targetUserId 要更新的用户ID
     * @param user 更新数据
     * @throws RuntimeException 权限不足时抛出异常
     */
    void updateUserInfo(Long currentUserId, Long targetUserId, User user);

    /**
     * 用户退出登录
     * @param authHeader Authorization头（包含Bearer token）
     */
    void logout(String authHeader);
}