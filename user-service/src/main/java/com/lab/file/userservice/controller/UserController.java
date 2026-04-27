package com.lab.file.userservice.controller;

import com.lab.file.common.response.Result;
import com.lab.file.common.vo.LoginUserInfo;
import com.lab.file.userservice.entity.User;
import com.lab.file.userservice.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户登录、注册、信息查询等接口")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户登录获取用户信息和JWT Token")
    @ApiResponse(responseCode = "200", description = "登录成功",
            content = @Content(schema = @Schema(implementation = LoginUserInfo.class)))
    @PostMapping("/login")
    public Result<LoginUserInfo> login(@RequestBody Map<String, Object> requestBody) {

        // 直接从 JSON 里解析
        String username = (String) requestBody.get("username");
        String password = (String) requestBody.get("password");

        LoginUserInfo userInfo = userService.loginWithUserInfo(username, password);
        return Result.success("登录成功", userInfo);
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "新用户注册")
    @ApiResponse(responseCode = "200", description = "注册成功")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody User user) {
        System.out.println(user);
        userService.register(user);
        return Result.success("注册成功", null);
    }

    /**
     * 获取用户信息
     */
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户信息")
    @ApiResponse(responseCode = "200", description = "获取成功",
                 content = @Content(schema = @Schema(implementation = User.class)))
    @GetMapping("/{id}")
    public Result<User> getUserById(@Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新用户信息，管理员可更新所有人，普通用户只能更新自己的信息（角色除外）")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public Result<Void> updateUser(
            @Parameter(description = "要更新的用户ID", required = true) @PathVariable Long id,
            @RequestBody User user,
            @RequestHeader(value = "userId", required = false) Long currentUserId) {  // 从网关传递过来的当前登录用户ID

        userService.updateUserInfo(currentUserId, id, user);
        return Result.success("更新成功", null);
    }

    /**
     * 用户退出登录
     */
    @Operation(summary = "用户退出登录", description = "退出登录，清除Token")
    @ApiResponse(responseCode = "200", description = "退出成功")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        userService.logout(authHeader);
        return Result.success("退出成功", null);
    }

}