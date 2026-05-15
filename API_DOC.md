# 面向实验室的智能文件管理与共享平台 - API文档

## 1. 概述

### 1.1 接口协议
- **协议**: HTTP/HTTPS
- **请求格式**: `application/json` 或 `multipart/form-data`
- **响应格式**: `application/json`
- **字符编码**: UTF-8
- **基础路径**: `http://localhost:8080/api`

### 1.2 通用响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 表示成功 |
| message | string | 响应消息 |
| data | object/array | 返回数据 |

### 1.3 通用状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（Token 缺失/无效/过期） |
| 403 | 禁止访问（无权限） |
| 404 | 资源不存在 |
| 410 | 资源已失效（分享链接过期） |
| 500 | 服务器内部错误 |

### 1.4 认证方式

系统采用 **JWT Token + Redis 双重校验**，所有需要鉴权的接口需在 HTTP Header 中携带：

```
Authorization: Bearer <jwt-token>
```

**Token 获取**: 调用 `POST /api/user/login` 接口获取，返回的 token 为 JWT 字符串。  
**Token 有效期**: 24 小时，过期后需重新登录。  
**Token 登出**: 调用 `POST /api/user/logout` 后 Token 立即失效（Redis 中移除），无法再使用。  
**白名单接口**: 登录、注册无需 Token。

### 1.5 网关转发说明

API 网关（端口 8080）接收请求后，会从 JWT 中解析出 `userId` 和 `username`，以 HTTP Header 形式转发给下游微服务：

| 转发 Header | 来源 | 说明 |
|-------------|------|------|
| `userId` | JWT claims | 当前登录用户 ID |
| `username` | JWT subject | 当前登录用户名 |

下游服务（user-service、file-service）的 Controller 通过 `@RequestHeader("userId")` 获取当前用户 ID，**不应从客户端传入的 userId 参数取值**，防止越权操作。

---

## 2. 用户服务 API

### 2.1 用户登录

- **接口**: `POST /api/user/login`
- **鉴权**: 白名单（无需 Token）
- **Content-Type**: `application/json`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/user/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@lab.com",
    "phone": "13800138000",
    "role": 1,
    "avatar": null,
    "status": 1,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**data 字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱 |
| phone | string | 手机号 |
| role | int | 1=管理员, 2=导师, 3=研究生 |
| avatar | string | 头像 URL |
| status | int | 0=禁用, 1=启用 |
| token | string | JWT Token（后续请求需携带） |

---

### 2.2 用户注册

- **接口**: `POST /api/user/register`
- **鉴权**: 白名单（无需 Token）
- **Content-Type**: `application/json`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名（3-50 字符） |
| password | string | 是 | 密码 |
| email | string | 否 | 邮箱 |
| phone | string | 否 | 手机号 |
| role | int | 是 | 角色（2=导师, 3=研究生） |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "password": "123456",
    "email": "student1@lab.com",
    "role": 3
  }'
```

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

---

### 2.3 获取用户信息

- **接口**: `GET /api/user/{id}`
- **鉴权**: 需登录

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 用户 ID |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/user/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@lab.com",
    "phone": "13800138000",
    "role": 1,
    "avatar": null,
    "status": 1,
    "createTime": "2026-03-01T10:00:00",
    "updateTime": "2026-03-01T10:00:00"
  }
}
```

---

### 2.4 更新用户信息

- **接口**: `PUT /api/user/{id}`
- **鉴权**: 需登录（管理员可更新任意用户，普通用户只能更新自己的非角色信息）
- **Content-Type**: `application/json`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 要更新的用户 ID |

**请求参数**（JSON Body）:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| email | string | 否 | 新邮箱 |
| phone | string | 否 | 新手机号 |
| avatar | string | 否 | 新头像 URL |

**请求示例**:
```bash
curl -X PUT "http://localhost:8080/api/user/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newemail@lab.com",
    "phone": "13900139000"
  }'
```

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

---

### 2.5 用户登出

- **接口**: `POST /api/user/logout`
- **鉴权**: 需登录

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/user/logout" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "退出成功",
  "data": null
}
```

> **说明**: 登出后 Redis 中的 Token 记录被清除，该 Token 立即失效。

---

## 3. 文件管理 API

### 3.1 上传文件

- **接口**: `POST /api/file/upload`
- **鉴权**: 需登录
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | file | 是 | 文件（最大 100MB） |
| parentId | long | 否 | 父目录 ID，0=根目录，默认 0 |
| description | string | 否 | 文件描述 |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/file/upload" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "file=@/path/to/report.pdf" \
  -F "parentId=0" \
  -F "description=实验报告"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "id": 100,
    "fileName": "uuid-storage-name.pdf",
    "originalName": "report.pdf",
    "filePath": "1/2026/05/uuid-storage-name.pdf",
    "fileSize": 1048576,
    "fileType": "application/pdf",
    "fileExtension": "pdf",
    "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
    "userId": 1,
    "parentId": 0,
    "isDirectory": 0,
    "description": "实验报告",
    "createTime": "2026-05-15T10:30:00",
    "updateTime": "2026-05-15T10:30:00",
    "status": 1,
    "version": "1.0"
  }
}
```

---

### 3.2 创建文件夹

- **接口**: `POST /api/file/folder`
- **鉴权**: 需登录
- **Content-Type**: `application/x-www-form-urlencoded`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| folderName | string | 是 | 文件夹名称 |
| parentId | long | 否 | 父目录 ID，0=根目录，默认 0 |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/file/folder" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d "folderName=实验数据&parentId=0"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "文件夹创建成功",
  "data": {
    "id": 101,
    "fileName": "uuid-folder-name",
    "originalName": "实验数据",
    "userId": 1,
    "parentId": 0,
    "isDirectory": 1,
    "createTime": "2026-05-15T10:35:00",
    "status": 1
  }
}
```

---

### 3.3 获取文件列表

- **接口**: `GET /api/file/list`
- **鉴权**: 需登录

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| parentId | long | 否 | 父目录 ID，0=根目录，默认 0 |

> `userId` 由网关从 JWT 自动解析并转发，**无需客户端传入**。

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/list?parentId=0" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取文件列表成功",
  "data": [
    {
      "id": 1,
      "originalName": "实验数据",
      "isDirectory": 1,
      "fileSize": 0,
      "parentId": 0,
      "createTime": "2026-05-15T10:35:00"
    },
    {
      "id": 2,
      "originalName": "年度报告.pdf",
      "fileName": "uuid-name.pdf",
      "isDirectory": 0,
      "fileSize": 1048576,
      "fileExtension": "pdf",
      "fileType": "application/pdf",
      "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
      "parentId": 0,
      "createTime": "2026-05-15T10:30:00",
      "updateTime": "2026-05-15T10:30:00",
      "status": 1
    }
  ]
}
```

---

### 3.4 删除文件或文件夹

- **接口**: `DELETE /api/file/{fileId}`
- **鉴权**: 需登录（只能删除自己的文件）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | long | 是 | 文件/文件夹 ID |

**请求示例**:
```bash
curl -X DELETE "http://localhost:8080/api/file/100" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

> 删除文件夹时，会递归删除其下所有子文件和子文件夹（逻辑删除，`status=0`）。

---

### 3.5 重命名文件或文件夹

- **接口**: `PUT /api/file/{fileId}/rename`
- **鉴权**: 需登录
- **Content-Type**: `application/x-www-form-urlencoded`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | long | 是 | 文件/文件夹 ID |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| newName | string | 是 | 新名称 |

**请求示例**:
```bash
curl -X PUT "http://localhost:8080/api/file/100/rename" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d "newName=新名称"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "重命名成功",
  "data": null
}
```

---

### 3.6 移动文件或文件夹

- **接口**: `PUT /api/file/{fileId}/move`
- **鉴权**: 需登录
- **Content-Type**: `application/x-www-form-urlencoded`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | long | 是 | 文件/文件夹 ID |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| newParentId | long | 是 | 目标父目录 ID |

**请求示例**:
```bash
curl -X PUT "http://localhost:8080/api/file/100/move" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d "newParentId=101"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "移动成功",
  "data": null
}
```

---

### 3.7 下载文件

- **接口**: `GET /api/file/{fileId}/download`
- **鉴权**: 需登录
- **响应格式**: `application/octet-stream`（二进制流）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | long | 是 | 文件 ID |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/100/download" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -o "report.pdf"
```

> **前端注意事项**: 由于浏览器直接发起的请求无法携带自定义 Header，前端应使用 `fetch` + `Authorization` Header 获取 Blob 后创建 Object URL 进行下载，而非直接使用 `<a>` 标签的 `href`。

---

### 3.8 预览文件

- **接口**: `GET /api/file/{fileId}/preview`
- **鉴权**: 需登录
- **响应格式**: 根据文件类型返回对应的 Content-Type（内联显示）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | long | 是 | 文件 ID |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/100/preview" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -O
```

**支持预览的文件类型**:
| 类型 | 格式 |
|------|------|
| 图片 | jpg, jpeg, png, gif, bmp, svg, webp |
| 文本 | txt, md, log, json, xml, yaml, yml, properties, js, ts, py, java, cpp, c, h, html, css, sql, sh, bat |
| PDF | pdf |
| 视频 | mp4, avi, mov, wmv, mkv, flv |
| 音频 | mp3, wav, flac, aac, wma |

> **前端注意事项**: 预览同样需要携带 `Authorization` Header，前端应使用 `fetch` 获取 Blob 后创建 Object URL，再应用于 `<img>`、`<video>`、`<iframe>` 等标签。

---

## 4. 分片上传 API

### 4.1 初始化分片上传

- **接口**: `POST /api/file/chunk/init`
- **鉴权**: 需登录
- **Content-Type**: `application/json`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileName | string | 是 | 原始文件名 |
| md5 | string | 是 | 文件 MD5 值 |
| totalChunks | int | 是 | 总分片数 |

**请求示例**:
```json
{
  "fileName": "large_video.mp4",
  "md5": "d41d8cd98f00b204e9800998ecf8427e",
  "totalChunks": 50
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "初始化分片上传成功",
  "data": "6ab8e1e2-3c4d-5e6f-7a8b-9c0d1e2f3a4b"
}
```

> `data` 字段为该次上传的唯一 `uploadId`（MinIO 的 multipart upload ID），后续步骤需携带。

---

### 4.2 上传单个分片

- **接口**: `POST /api/file/chunk/upload`
- **鉴权**: 需登录
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| md5 | string | 是 | 文件 MD5 值 |
| uploadId | string | 是 | 初始化返回的上传 ID |
| partNumber | int | 是 | 分片序号（从 1 开始） |
| totalChunks | long | 是 | 总分片数 |
| chunk | file | 是 | 分片文件 |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/file/chunk/upload" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "md5=d41d8cd98f00b204e9800998ecf8427e" \
  -F "uploadId=6ab8e1e2-..." \
  -F "partNumber=1" \
  -F "totalChunks=50" \
  -F "chunk=@part1.tmp"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "分片上传成功",
  "data": "etag-value-from-minio"
}
```

---

### 4.3 查询已上传分片

- **接口**: `GET /api/file/chunk/uploaded`
- **鉴权**: 需登录

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| md5 | string | 是 | 文件 MD5 值 |
| uploadId | string | 是 | 上传 ID |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/chunk/uploaded?md5=d41d8cd98f00b204e9800998ecf8427e&uploadId=6ab8e1e2-..." \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询已上传分片成功",
  "data": {
    "1": "etag-1",
    "2": "etag-2",
    "3": "etag-3"
  }
}
```

> `data` 中 key 为已上传的分片序号，value 为该分片的 ETag。

---

### 4.4 查询缺失分片

- **接口**: `GET /api/file/chunk/missing-chunks`
- **鉴权**: 需登录

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| md5 | string | 是 | 文件 MD5 值 |
| uploadId | string | 是 | 上传 ID |
| totalChunks | int | 是 | 总分片数 |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/chunk/missing-chunks?md5=d41d8cd98f00b204e9800998ecf8427e&uploadId=6ab8e1e2-...&totalChunks=50" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "查询未上传分片成功",
  "data": [4, 5, 6, 7, 8]
}
```

> 返回缺失的分片序号列表，可用于实现断点续传。

---

### 4.5 合并分片

- **接口**: `POST /api/file/chunk/merge`
- **鉴权**: 需登录
- **Content-Type**: `application/x-www-form-urlencoded`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| md5 | string | 是 | 文件 MD5 值 |
| uploadId | string | 是 | 上传 ID |
| fileName | string | 是 | 原始文件名 |
| parentId | long | 否 | 父目录 ID，默认 0 |
| description | string | 否 | 文件描述 |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/file/chunk/merge" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d "md5=d41d8cd98f00b204e9800998ecf8427e" \
  -d "uploadId=6ab8e1e2-..." \
  -d "fileName=large_video.mp4" \
  -d "parentId=0"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "分片合并成功",
  "data": {
    "id": 200,
    "originalName": "large_video.mp4",
    "fileSize": 1073741824,
    "fileExtension": "mp4",
    "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
    "isDirectory": 0,
    "status": 1
  }
}
```

---

### 4.6 取消分片上传

- **接口**: `POST /api/file/chunk/cancel`
- **鉴权**: 需登录
- **Content-Type**: `application/x-www-form-urlencoded`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| md5 | string | 是 | 文件 MD5 值 |
| uploadId | string | 是 | 上传 ID |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/file/chunk/cancel" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d "md5=d41d8cd98f00b204e9800998ecf8427e" \
  -d "uploadId=6ab8e1e2-..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "取消分片上传成功",
  "data": null
}
```

---

### 4.7 文件秒传（检查文件是否存在）

- **接口**: `GET /api/file/chunk/exists`
- **鉴权**: 需登录

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| md5 | string | 是 | 文件 MD5 值 |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/chunk/exists?md5=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**（文件已存在）:
```json
{
  "code": 200,
  "message": "查询完成",
  "data": {
    "id": 200,
    "originalName": "large_video.mp4",
    "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
    "...": "..."
  }
}
```

**响应示例**（文件不存在）:
```json
{
  "code": 200,
  "message": "查询完成",
  "data": null
}
```

> 如果文件已存在（相同 MD5），前端可跳过上传，直接完成"秒传"。

---

## 5. 文件分享 API

### 5.1 获取用户列表（分享选人用）

- **接口**: `GET /api/share/users`
- **鉴权**: 需登录

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/share/users" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取用户列表成功",
  "data": [
    {
      "id": 1,
      "username": "admin",
      "email": "admin@lab.com",
      "role": 1,
      "status": 1
    },
    {
      "id": 2,
      "username": "teacher",
      "email": "teacher@lab.com",
      "role": 2,
      "status": 1
    }
  ]
}
```

---

### 5.2 创建分享

- **接口**: `POST /api/share/create`
- **鉴权**: 需登录
- **Content-Type**: `application/x-www-form-urlencoded`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | long | 是 | 要分享的文件/文件夹 ID |
| shareType | int | 否 | 1=公开, 2=私密，默认 1 |
| maxDownloadCount | int | 否 | 最大下载次数，0=无限制，默认 0 |
| expireHours | long | 否 | 过期时间（小时），空=永不过期 |
| targetUserIds | string | 否 | 私密分享时可选，逗号分隔的用户 ID |

**请求示例**（公开分享）:
```bash
curl -X POST "http://localhost:8080/api/share/create" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d "fileId=100&shareType=1&maxDownloadCount=0&expireHours=24"
```

**请求示例**（私密分享）:
```bash
curl -X POST "http://localhost:8080/api/share/create" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d "fileId=100&shareType=2&targetUserIds=2,3&maxDownloadCount=5"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "分享创建成功",
  "data": {
    "id": 1,
    "fileId": 100,
    "shareUserId": 1,
    "shareCode": "A1b2C3d4",
    "shareType": 1,
    "shareUrl": null,
    "expireTime": "2026-05-16T10:30:00",
    "downloadCount": 0,
    "maxDownloadCount": 0,
    "createTime": "2026-05-15T10:30:00",
    "status": 1
  }
}
```

---

### 5.3 获取分享列表

- **接口**: `GET /api/share/list`
- **鉴权**: 需登录（获取当前用户的分享记录）

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/share/list" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取分享列表成功",
  "data": [
    {
      "id": 1,
      "fileId": 100,
      "shareUserId": 1,
      "shareCode": "A1b2C3d4",
      "shareType": 1,
      "expireTime": "2026-05-16T10:30:00",
      "downloadCount": 3,
      "maxDownloadCount": 0,
      "createTime": "2026-05-15T10:30:00",
      "status": 1
    }
  ]
}
```

---

### 5.4 获取分享信息（通过分享码）

- **接口**: `GET /api/share/{shareCode}`
- **鉴权**: 需登录

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| shareCode | string | 是 | 8 位分享码 |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/share/A1b2C3d4" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取分享信息成功",
  "data": {
    "id": 1,
    "fileId": 100,
    "shareUserId": 1,
    "shareCode": "A1b2C3d4",
    "shareType": 1,
    "expireTime": "2026-05-16T10:30:00",
    "downloadCount": 3,
    "maxDownloadCount": 0,
    "status": 1
  }
}
```

**错误响应**（分享码不存在）:
```json
{
  "code": 404,
  "message": "分享码不存在",
  "data": null
}
```

**错误响应**（分享已失效）:
```json
{
  "code": 410,
  "message": "分享链接已失效",
  "data": null
}
```

---

### 5.5 获取私密分享目标用户

- **接口**: `GET /api/share/{shareCode}/target-users`
- **鉴权**: 需登录

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| shareCode | string | 是 | 分享码 |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/share/A1b2C3d4/target-users" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": [2, 3]
}
```

---

### 5.6 通过分享码下载

- **接口**: `GET /api/share/{shareCode}/download`
- **鉴权**: 需登录
- **响应格式**: `application/octet-stream`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| shareCode | string | 是 | 分享码 |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | long | 否 | 共享目录中的指定文件 ID（可选） |

- 不传 `fileId`：下载分享根文件（当分享的是单文件时）
- 传 `fileId`：下载共享目录中的指定文件

**请求示例**:
```bash
# 下载根文件
curl -X GET "http://localhost:8080/api/share/A1b2C3d4/download" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -o "report.pdf"

# 下载共享目录中的特定文件
curl -X GET "http://localhost:8080/api/share/A1b2C3d4/download?fileId=105" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -o "data.xlsx"
```

---

### 5.7 通过分享码预览

- **接口**: `GET /api/share/{shareCode}/preview`
- **鉴权**: 需登录
- **响应格式**: 根据文件类型返回对应的 Content-Type（内联显示）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| shareCode | string | 是 | 分享码 |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | long | 否 | 共享目录中的指定文件 ID（可选） |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/share/A1b2C3d4/preview" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 5.8 获取分享目录文件列表

- **接口**: `GET /api/share/{shareCode}/list`
- **鉴权**: 需登录

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| shareCode | string | 是 | 分享码 |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| parentId | long | 否 | 父目录 ID，0=分享根目录，默认 0 |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/share/A1b2C3d4/list" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取文件列表成功",
  "data": [
    {
      "id": 100,
      "originalName": "共享文件夹",
      "isDirectory": 1,
      "createTime": "2026-05-15T10:30:00"
    },
    {
      "id": 102,
      "originalName": "共享文档.pdf",
      "fileSize": 2048000,
      "fileExtension": "pdf",
      "isDirectory": 0,
      "createTime": "2026-05-15T10:35:00"
    }
  ]
}
```

---

### 5.9 上传文件到分享目录

- **接口**: `POST /api/share/{shareCode}/upload`
- **鉴权**: 需登录
- **Content-Type**: `multipart/form-data`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| shareCode | string | 是 | 分享码 |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | file | 是 | 文件 |
| parentId | long | 否 | 父目录 ID，0=分享根目录，默认 0 |
| description | string | 否 | 文件描述 |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/share/A1b2C3d4/upload" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "file=@/path/to/report.pdf" \
  -F "parentId=0"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "id": 300,
    "originalName": "report.pdf",
    "fileSize": 1048576,
    "fileExtension": "pdf",
    "isDirectory": 0,
    "status": 1
  }
}
```

---

### 5.10 取消分享

- **接口**: `DELETE /api/share/{shareId}`
- **鉴权**: 需登录（只能取消自己的分享）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| shareId | long | 是 | 分享记录 ID（非分享码） |

**请求示例**:
```bash
curl -X DELETE "http://localhost:8080/api/share/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例**:
```json
{
  "code": 200,
  "message": "取消分享成功",
  "data": null
}
```

---

## 6. 完整调用流程

### 6.1 普通文件上传流程

```
1. 用户登录 → POST /api/user/login → 获取 JWT Token
2. 创建文件夹 → POST /api/file/folder → 获取 folderId
3. 上传文件 → POST /api/file/upload → 返回 FileEntity
4. 查看列表 → GET  /api/file/list    → 查看目录下所有文件
5. 下载文件 → GET  /api/file/{id}/download
```

### 6.2 大文件分片上传流程

```
1. 计算 MD5 → 前端 SparkMD5 计算文件 MD5
2. 检查秒传 → GET  /api/file/chunk/exists?md5=xxx
   ├─ 已存在 → 结束（秒传成功）
   └─ 不存在 → 继续
3. 初始化   → POST /api/file/chunk/init → 获取 uploadId
4. 上传分片 → POST /api/file/chunk/upload（循环每个分片）
   ├─ 失败 → 重试（最多 3 次）
   └─ 成功 → 继续下一个
5. 合并分片 → POST /api/file/chunk/merge → 完成
```

### 6.3 文件分享与访问流程

```
1. 创建分享 → POST /api/share/create → 获取 shareCode
2. 分享者查看列表 → GET /api/share/list
3. 访问者查看信息 → GET /api/share/{shareCode}
4. 访问者浏览文件 → GET /api/share/{shareCode}/list
5. 访问者下载文件 → GET /api/share/{shareCode}/download
6. 分享者取消分享 → DELETE /api/share/{shareId}
```

---

## 7. 错误处理

### 7.1 常见错误示例

**参数错误**:
```json
{
  "code": 400,
  "message": "用户名或密码错误",
  "data": null
}
```

**未授权**:
```json
{
  "code": 401,
  "message": "无权访问",
  "data": null
}
```

**权限拒绝**:
```json
{
  "code": 500,
  "message": "无权限删除此文件",
  "data": null
}
```

### 7.2 文件相关错误说明

| 场景 | 说明 |
|------|------|
| 文件大小超限 | 单文件最大 100MB（Spring 配置），总请求最大 200MB |
| 文件类型不允许 | 后端未做硬性类型过滤 |
| 文件不存在 | fileId 不存在或已被删除（status=0） |
| 无权限访问 | 操作非自己拥有的文件 |

### 7.3 分享相关错误说明

| 场景 | 说明 |
|------|------|
| 分享码不存在 | 返回 code=404 |
| 分享已失效 | 已过期/被取消/超下载次数，返回 code=410 |
| 私密分享无权限 | 非目标用户访问私密分享 |

---

## 8. 数据字典

### 8.1 角色 (role)

| 值 | 说明 |
|----|------|
| 1 | 管理员 |
| 2 | 导师 |
| 3 | 研究生 |

### 8.2 用户状态 (status)

| 值 | 说明 |
|----|------|
| 0 | 禁用 |
| 1 | 启用 |

### 8.3 文件/文件夹类型 (is_directory)

| 值 | 说明 |
|----|------|
| 0 | 文件 |
| 1 | 文件夹 |

### 8.4 文件状态 (status)

| 值 | 说明 |
|----|------|
| 0 | 已删除（逻辑删除） |
| 1 | 正常 |

### 8.5 分享类型 (share_type)

| 值 | 说明 |
|----|------|
| 1 | 公开分享 |
| 2 | 私密分享 |

### 8.6 分享状态 (status)

| 值 | 说明 |
|----|------|
| 0 | 已失效 |
| 1 | 有效 |
