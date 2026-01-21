---
name: ticket-module-architecture
description: Ticket 凭证管理模块架构指南，涵盖凭证类型（密码/SSH/Token）、加密存储、凭证授权、安全访问控制。当用户开发凭证功能、添加新凭证类型、处理凭证加密或配置凭证授权时使用。
---

# Ticket 凭证管理模块架构指南

> **模块定位**: Ticket 是 BK-CI 的凭证管理模块，负责管理各类凭证（Credential）和证书（Cert），为代码库、构建机、部署等场景提供安全的凭证存储和访问服务。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/ticket/
├── api-ticket/              # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/ticket/
│       ├── api/                 # REST API 接口
│       ├── constant/            # 常量定义
│       └── pojo/                # 数据对象
│           ├── enums/           # 枚举
│           └── item/            # 凭证项
│
├── biz-ticket/              # 业务逻辑层
├── model-ticket/            # 数据模型层
└── boot-ticket/             # Spring Boot 启动模块
```

## 二、核心概念

### 2.1 凭证类型

```kotlin
enum class CredentialType(val type: String) {
    PASSWORD("PASSWORD"),                       // 密码
    ACCESSTOKEN("ACCESSTOKEN"),                 // AccessToken
    OAUTHTOKEN("OAUTHTOKEN"),                   // OAuth Token
    USERNAME_PASSWORD("USERNAME_PASSWORD"),     // 用户名+密码
    SECRETKEY("SECRETKEY"),                     // SecretKey
    APPID_SECRETKEY("APPID_SECRETKEY"),         // AppId+SecretKey
    SSH_PRIVATEKEY("SSH_PRIVATEKEY"),           // SSH 私钥
    TOKEN_SSH_PRIVATEKEY("TOKEN_SSH_PRIVATEKEY"), // Token+SSH私钥
    TOKEN_USERNAME_PASSWORD("TOKEN_USERNAME_PASSWORD"), // Token+用户名密码
    COS_APPID_SECRETID_SECRETKEY_REGION("COS_APPID_SECRETID_SECRETKEY_REGION"), // COS凭证
    MULTI_LINE_PASSWORD("MULTI_LINE_PASSWORD"), // 多行密码
}
```

### 2.2 证书类型

```kotlin
enum class CertType(val type: String) {
    IOS("ios"),           // iOS 证书
    ANDROID("android"),   // Android 证书
    TLS("tls"),           // TLS 证书
    ENTERPRISE("enterprise"), // 企业证书
}
```

### 2.3 凭证数据项

| 类名 | 说明 | 字段 |
|------|------|------|
| `PasswordCredentialItem` | 密码凭证 | `v1`(密码) |
| `AccessTokenCredentialItem` | Token 凭证 | `v1`(token) |
| `UserPassCredentialItem` | 用户名密码 | `v1`(用户名), `v2`(密码) |
| `SshPrivateKeyCredentialItem` | SSH 私钥 | `v1`(私钥), `v2`(密码) |
| `TokenSshPrivateKeyCredentialItem` | Token+SSH | `v1`(token), `v2`(私钥), `v3`(密码) |
| `TokenUserPassCredentialItem` | Token+用户名密码 | `v1`(token), `v2`(用户名), `v3`(密码) |

## 三、核心数据库表

### 3.1 凭证表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_CREDENTIAL` | 凭证信息表 | `PROJECT_ID`, `CREDENTIAL_ID`, `CREDENTIAL_NAME`, `CREDENTIAL_TYPE`, `CREDENTIAL_V1`~`V4` |

### 3.2 证书表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_CERT` | 证书主表 | `PROJECT_ID`, `CERT_ID`, `CERT_TYPE`, `CERT_P12_FILE_CONTENT` |
| `T_CERT_ENTERPRISE` | 企业证书表 | `PROJECT_ID`, `CERT_ID`, `CERT_MP_FILE_CONTENT` |
| `T_CERT_TLS` | TLS 证书表 | `PROJECT_ID`, `CERT_ID`, `CERT_SERVER_CRT_FILE`, `CERT_SERVER_KEY_FILE` |

### 3.3 字段说明

> ⚠️ **重要**: `PROJECT_ID` 是 `T_PROJECT.english_name`

| 字段 | 说明 |
|------|------|
| `CREDENTIAL_V1` ~ `V4` | 加密后的凭证内容，根据类型使用不同字段 |
| `ALLOW_ACROSS_PROJECT` | 是否允许跨项目使用 |

## 四、API 接口速查

### 4.1 凭证接口

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `UserCredentialResource` | `/user/credentials` | 用户凭证管理 |
| `ServiceCredentialResource` | `/service/credentials` | 服务间凭证查询 |
| `BuildCredentialResource` | `/build/credentials` | 构建时凭证获取 |
| `BuildAgentCredentialResource` | `/build/agent/credentials` | Agent 凭证获取 |

### 4.2 证书接口

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `UserCertResource` | `/user/certs` | 用户证书管理 |
| `ServiceCertResource` | `/service/certs` | 服务间证书查询 |
| `BuildCertResource` | `/build/certs` | 构建时证书获取 |
| `BuildAgentCertResource` | `/build/agent/certs` | Agent 证书获取 |

## 五、核心流程

### 5.1 凭证创建流程

```
用户创建凭证
    │
    ▼
UserCredentialResource.create()
    │
    ├─► 参数校验
    │   └─► 校验凭证 ID 唯一性
    │
    ├─► 加密凭证内容
    │   └─► 使用 AES 加密 v1~v4 字段
    │
    └─► 保存到数据库
```

### 5.2 凭证获取流程

```
构建时获取凭证
    │
    ▼
BuildCredentialResource.get()
    │
    ├─► 权限校验
    │   └─► 检查构建是否有权限使用该凭证
    │
    ├─► 查询凭证
    │   └─► credentialDao.get()
    │
    ├─► 解密凭证内容
    │   └─► 使用 AES 解密
    │
    └─► 返回凭证信息
```

## 六、安全机制

### 6.1 加密存储

- 凭证内容使用 AES 加密后存储
- 密钥由系统配置管理
- 数据库中不存储明文

### 6.2 访问控制

- 凭证按项目隔离
- 支持跨项目共享（需配置）
- 构建时通过 Build ID 验证权限

## 七、与其他模块的关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    Ticket 模块关系                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐                   │
│  │repository │  │  worker   │  │   store   │                   │
│  │(代码库认证)│  │(构建凭证)  │  │(插件凭证)  │                   │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘                   │
│        │              │              │                          │
│        └──────────────┼──────────────┘                          │
│                       ▼                                          │
│               ┌───────────────┐                                  │
│               │    ticket     │                                  │
│               └───────┬───────┘                                  │
│                       │                                          │
│               ┌───────┴───────┐                                  │
│               ▼               ▼                                  │
│        ┌───────────┐   ┌───────────┐                            │
│        │  project  │   │   auth    │                            │
│        └───────────┘   └───────────┘                            │
└─────────────────────────────────────────────────────────────────┘
```

## 八、服务间调用示例

```kotlin
// Repository 模块获取凭证
client.get(ServiceCredentialResource::class).get(
    projectId = projectId,  // english_name
    credentialId = credentialId
)

// Worker 获取构建凭证
client.get(BuildCredentialResource::class).get(
    projectId = projectId,
    buildId = buildId,
    credentialId = credentialId,
    publicKey = publicKey  // 用于加密返回的凭证
)
```

## 九、常见问题

**Q: 凭证如何加密存储？**
A: 使用 AES 对称加密，密钥配置在系统配置中。

**Q: 如何跨项目使用凭证？**
A: 设置 `ALLOW_ACROSS_PROJECT = true`，并在目标项目中引用。

**Q: 构建时如何安全获取凭证？**
A: 构建时传入公钥，服务端用公钥加密凭证返回，构建机用私钥解密。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
