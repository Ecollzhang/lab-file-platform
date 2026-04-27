# 面向实验室的智能文件管理与共享平台 - API文档

## 1. 概述

### 1.1 接口协议
- 协议: HTTP/HTTPS
- 请求格式: application/json 或 multipart/form-data
- 响应格式: application/json
- 字符编码: UTF-8

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
| code | int | 状态码，200表示成功 |
| message | string | 响应消息 |
| data | object/array | 返回数据 |

### 1.3 通用状态码
| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 2. 用户服务 API

### 2.1 用户登录
- **接口**: `POST /api/user/login`
- **描述**: 用户登录获取JWT Token
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | username | string | 是 | 用户名 |
  | password | string | 是 | 密码 |

- **请求示例**:
```bash
curl -X POST "http://localhost:8080/api/user/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=123456"
```

- **响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2.2 用户注册
- **接口**: `POST /api/user/register`
- **描述**: 用户注册
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | username | string | 是 | 用户名 |
  | password | string | 是 | 密码 |
  | email | string | 否 | 邮箱 |
  | phone | string | 否 | 手机号 |

- **请求示例**:
```bash
curl -X POST "http://localhost:8080/api/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "newuser@example.com"
  }'
```

### 2.3 获取用户信息
- **接口**: `GET /api/user/{id}`
- **描述**: 根据ID获取用户信息
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | id | long | 是 | 用户ID |

- **请求示例**:
```bash
curl -X GET "http://localhost:8080/api/user/1"
```

## 3. 文件服务 API

### 3.1 上传文件
- **接口**: `POST /api/file/upload`
- **描述**: 上传文件
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | file | file | 是 | 文件 |
  | userId | long | 是 | 用户ID |
  | parentId | long | 否 | 父目录ID，默认为0 |
  | description | string | 否 | 文件描述 |

- **请求示例**:
```bash
curl -X POST "http://localhost:8080/api/file/upload" \
  -H "Authorization: Bearer {token}" \
  -F "file=@/path/to/file.pdf" \
  -F "userId=1" \
  -F "parentId=0" \
  -F "description=测试文件"
```

- **响应示例**:
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "id": 1,
    "fileName": "uuid-filename.pdf",
    "originalName": "test.pdf",
    "filePath": "1/2026/3/uuid-filename.pdf",
    "fileSize": 1024000,
    "fileType": "application/pdf",
    "fileExtension": "pdf",
    "userId": 1,
    "parentId": 0,
    "isDirectory": 0,
    "createTime": "2026-03-01T10:00:00",
    "updateTime": "2026-03-01T10:00:00",
    "status": 1
  }
}
```

### 3.2 创建文件夹
- **接口**: `POST /api/file/folder`
- **描述**: 创建文件夹
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | folderName | string | 是 | 文件夹名称 |
  | userId | long | 是 | 用户ID |
  | parentId | long | 否 | 父目录ID，默认为0 |

- **请求示例**:
```bash
curl -X POST "http://localhost:8080/api/file/folder" \
  -H "Authorization: Bearer {token}" \
  -d "folderName=新文件夹&userId=1&parentId=0"
```

### 3.3 获取文件列表
- **接口**: `GET /api/file/list`
- **描述**: 获取指定目录下的文件列表
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | userId | long | 是 | 用户ID |
  | parentId | long | 否 | 父目录ID，默认为0 |

- **请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/list?userId=1&parentId=0" \
  -H "Authorization: Bearer {token}"
```

- **响应示例**:
```json
{
  "code": 200,
  "message": "获取文件列表成功",
  "data": [
    {
      "id": 1,
      "fileName": "文档文件夹",
      "originalName": "文档文件夹",
      "isDirectory": 1,
      "fileSize": 0,
      "parentId": 0,
      "createTime": "2026-03-01T10:00:00"
    },
    {
      "id": 2,
      "fileName": "report.pdf",
      "originalName": "年度报告.pdf",
      "isDirectory": 0,
      "fileSize": 1024000,
      "parentId": 0,
      "createTime": "2026-03-01T10:30:00"
    }
  ]
}
```

### 3.4 删除文件或文件夹
- **接口**: `DELETE /api/file/{fileId}`
- **描述**: 删除文件或文件夹
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | fileId | long | 是 | 文件ID |
  | userId | long | 是 | 用户ID |

- **请求示例**:
```bash
curl -X DELETE "http://localhost:8080/api/file/1?userId=1" \
  -H "Authorization: Bearer {token}"
```

### 3.5 重命名文件或文件夹
- **接口**: `PUT /api/file/{fileId}/rename`
- **描述**: 重命名文件或文件夹
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | fileId | long | 是 | 文件ID |
  | newName | string | 是 | 新名称 |
  | userId | long | 是 | 用户ID |

- **请求示例**:
```bash
curl -X PUT "http://localhost:8080/api/file/1/rename?newName=新名称&userId=1" \
  -H "Authorization: Bearer {token}"
```

### 3.6 移动文件或文件夹
- **接口**: `PUT /api/file/{fileId}/move`
- **描述**: 移动文件或文件夹
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | fileId | long | 是 | 文件ID |
  | newParentId | long | 是 | 新的父目录ID |
  | userId | long | 是 | 用户ID |

- **请求示例**:
```bash
curl -X PUT "http://localhost:8080/api/file/1/move?newParentId=2&userId=1" \
  -H "Authorization: Bearer {token}"
```

### 3.7 下载文件
- **接口**: `GET /api/file/{fileId}/download`
- **描述**: 下载文件
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  |--------|------|------|------|
  | fileId | long | 是 | 文件ID |
  | userId | long | 是 | 用户ID |

- **请求示例**:
```bash
curl -X GET "http://localhost:8080/api/file/1/download?userId=1" \
  -H "Authorization: Bearer {token}" \
  -O
```

## 4. 认证说明

### 4.1 认证方式
系统采用JWT Token进行认证，在请求头中添加Authorization字段:
```
Authorization: Bearer {jwt-token}
```

### 4.2 Token获取
通过用户登录接口获取Token，后续请求均需携带此Token。

### 4.3 Token刷新
Token有效期为24小时，过期后需重新登录获取新的Token。

## 5. 错误处理

### 5.1 常见错误示例
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

### 5.2 文件相关错误
- 文件大小超过限制
- 文件类型不允许
- 存储空间不足
- 文件不存在或无权限访问

### 5.3 权限相关错误
- 用户无权限操作
- 访问受限资源
- Token过期或无效