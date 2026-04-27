package com.lab.file.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户登录响应信息
 */
@Data
@Schema(description = "用户登录响应信息")
public class LoginUserInfo {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "角色 1-管理员, 2-导师, 3-研究生")
    private Integer role;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "状态 0-禁用, 1-启用")
    private Integer status;
}