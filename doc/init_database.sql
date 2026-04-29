-- 实验室文件管理平台数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS lab_file_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lab_file_platform;

-- 用户表
CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role INT DEFAULT 3 COMMENT '角色 1-管理员, 2-导师, 3-研究生',
    avatar VARCHAR(255) COMMENT '头像',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status INT DEFAULT 1 COMMENT '状态 0-禁用, 1-启用'
) COMMENT='用户表';

-- 文件表
CREATE TABLE t_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) COMMENT '存储路径',
    file_size BIGINT COMMENT '文件大小',
    file_type VARCHAR(100) COMMENT '文件类型',
    file_extension VARCHAR(10) COMMENT '文件扩展名',
    file_md5 VARCHAR(32) COMMENT '文件MD5值',
    user_id BIGINT NOT NULL COMMENT '上传用户ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父级目录ID，根目录为0',
    is_directory TINYINT DEFAULT 0 COMMENT '是否是目录 0-否, 1-是',
    thumbnail_path VARCHAR(500) COMMENT '缩略图路径',
    description TEXT COMMENT '文件描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status INT DEFAULT 1 COMMENT '状态 0-删除, 1-正常',
    version VARCHAR(20) DEFAULT '1.0' COMMENT '版本号'
) COMMENT='文件表';

-- 分享用户权限表（私密分享指定允许访问的用户）
CREATE TABLE t_file_share_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    share_id BIGINT NOT NULL COMMENT '分享ID',
    user_id BIGINT NOT NULL COMMENT '允许访问的用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_fsu_share_id (share_id),
    INDEX idx_fsu_user_id (user_id)
) COMMENT='分享用户权限表';

-- 文件分享表
CREATE TABLE t_file_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分享ID',
    file_id BIGINT NOT NULL COMMENT '文件ID',
    share_user_id BIGINT NOT NULL COMMENT '分享用户ID',
    share_code VARCHAR(20) NOT NULL UNIQUE COMMENT '分享码',
    share_type INT DEFAULT 1 COMMENT '分享类型 1-公开分享, 2-私密分享',
    share_url VARCHAR(255) COMMENT '分享链接',
    expire_time DATETIME COMMENT '过期时间',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    max_download_count INT DEFAULT 0 COMMENT '最大下载次数，0表示无限制',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status INT DEFAULT 1 COMMENT '状态 0-失效, 1-有效'
) COMMENT='文件分享表';

-- 操作日志表
CREATE TABLE t_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    user_name VARCHAR(50) COMMENT '操作用户名',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_desc TEXT COMMENT '操作描述',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='操作日志表';

-- 文件分片表
CREATE TABLE t_file_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分片ID',
    chunk_identifier VARCHAR(100) NOT NULL COMMENT '文件唯一标识',
    upload_id VARCHAR(255) COMMENT '分片上传ID',
    etag VARCHAR(255) COMMENT '分片ETag',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    chunk_number INT NOT NULL COMMENT '分片序号',
    total_chunks BIGINT NOT NULL COMMENT '总分片数',
    chunk_size BIGINT COMMENT '分片大小',
    chunk_path VARCHAR(500) COMMENT '分片存储路径',
    user_id BIGINT NOT NULL COMMENT '上传用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status INT DEFAULT 0 COMMENT '状态 0-未完成, 1-已完成'
) COMMENT='文件分片表';

-- 创建索引
CREATE INDEX idx_user_username ON t_user(username);
CREATE INDEX idx_user_status ON t_user(status);
CREATE INDEX idx_file_user_parent ON t_file(user_id, parent_id);
CREATE INDEX idx_file_status ON t_file(status);
CREATE INDEX idx_file_share_code ON t_file_share(share_code);
CREATE INDEX idx_file_share_status ON t_file_share(status);
CREATE INDEX idx_operation_log_user_time ON t_operation_log(user_id, create_time);
CREATE INDEX idx_file_chunk_identifier ON t_file_chunk(chunk_identifier);

-- 分片上传增强字段（在原有表基础上新增）
ALTER TABLE t_file_chunk ADD COLUMN upload_id VARCHAR(255) COMMENT '分片上传ID' AFTER chunk_identifier;
ALTER TABLE t_file_chunk ADD COLUMN etag VARCHAR(255) COMMENT '分片ETag' AFTER upload_id;

-- 初始化默认管理员用户 (密码为明文123456的BCrypt加密结果)
INSERT INTO t_user (username, password, email, role, create_time, update_time, status) VALUES
('admin', '$2a$10$veGcNx45BEzrlZMd/NQumemGZ5jGbYW1AToMNs.s3.lnq1IClNxEy', 'admin@example.com', 1, NOW(), NOW(), 1),
('teacher', '$2a$10$veGcNx45BEzrlZMd/NQumemGZ5jGbYW1AToMNs.s3.lnq1IClNxEy', 'teacher@example.com', 2, NOW(), NOW(), 1),
('student', '$2a$10$veGcNx45BEzrlZMd/NQumemGZ5jGbYW1AToMNs.s3.lnq1IClNxEy', 'student@example.com', 3, NOW(), NOW(), 1);