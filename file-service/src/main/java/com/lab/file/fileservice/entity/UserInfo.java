package com.lab.file.fileservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息（映射t_user表，仅用于查询）
 */
@Data
@TableName("t_user")
public class UserInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String phone;

    private Integer role;

    private String avatar;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer status;
}
