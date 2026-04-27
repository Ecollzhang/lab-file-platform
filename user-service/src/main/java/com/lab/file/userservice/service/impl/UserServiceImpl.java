package com.lab.file.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.file.common.exception.BusinessException;
import com.lab.file.common.service.TokenRedisService;
import com.lab.file.common.utils.JwtUtil;
import com.lab.file.common.vo.LoginUserInfo;
import com.lab.file.userservice.entity.User;
import com.lab.file.userservice.mapper.UserMapper;
import com.lab.file.userservice.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil = new JwtUtil();

    @Autowired
    private TokenRedisService tokenRedisService;

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(
                new QueryWrapper<User>().eq("username", username)
        );
    }

    @Override
    public void updateUserInfo(Long currentUserId, Long targetUserId, User user) {
        // 1. 获取当前登录用户信息
        System.out.println("currentId:" + currentUserId);
        User currentUser = getById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("当前登录用户不存在");
        }

        // 2. 获取要更新的用户信息
        User targetUser = getById(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("要更新的用户不存在");
        }

        // 3. 权限验证
        boolean isAdmin = currentUser.getRole() == 1;
        boolean isSelf = currentUserId.equals(targetUserId);

        // 不是管理员且不是更新自己的信息 -> 无权限
        if (!isAdmin && !isSelf) {
            throw new RuntimeException("权限不足，只能修改自己的信息");
        }

        // 4. 非管理员不能修改角色（即使传了也忽略）
        if (!isAdmin) {
            user.setRole(null);  // 忽略角色字段
        }

        // 5. 防止修改密码（密码应该通过单独的接口修改）
        user.setPassword(null);

        // 6. 设置要更新的用户ID
        user.setId(targetUserId);

        // 7. 执行更新
        boolean updated = updateById(user);
        if (!updated) {
            throw new RuntimeException("更新用户信息失败");
        }
    }
/**
     * 用户登录并返回用户信息
     */
    public LoginUserInfo loginWithUserInfo(String username, String password) {
        User user = findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(401, "账户已被禁用");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        // 保存到Redis上
        tokenRedisService.saveToken(user.getId(), token);
        // 构建用户信息响应对象
        LoginUserInfo userInfo = new LoginUserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setRole(user.getRole());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setToken(token);
        userInfo.setStatus(user.getStatus());

        return userInfo;
    }

    @Override
    public void register(User user) {
        User existingUser = findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(null);
        user.setStatus(1);
        user.setCreateTime(java.time.LocalDateTime.now());
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(400, "无效的Token");
        }

        // 提取token
        String token = authHeader.substring(7);

        // 从Redis中删除token
        tokenRedisService.removeToken(token);

        System.out.println("用户退出登录，Token已清除: " + token);
    }
}