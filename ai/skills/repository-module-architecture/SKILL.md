---
name: repository-module-architecture
description: Repository 代码库管理模块架构指南，涵盖 Git/SVN 代码库接入、Webhook 配置、代码库授权、触发器管理。当用户开发代码库功能、配置 Webhook、处理代码库授权或实现触发器逻辑时使用。
---

# Repository 代码库管理模块架构指南

> **模块定位**: Repository 是 BK-CI 的代码库管理模块，负责对接各种代码托管平台（Git、SVN、GitHub、GitLab、TGit、P4），管理代码库的认证、授权、Webhook 等功能。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/repository/
├── api-repository/          # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/repository/
│       ├── api/                 # REST API 接口（30+ 文件）
│       │   ├── github/          # GitHub 专用接口
│       │   └── scm/             # SCM 通用接口
│       ├── constant/            # 常量和消息码
│       ├── pojo/                # 数据对象（40+ 文件）
│       ├── sdk/                 # SDK 定义
│       └── utils/               # 工具类
│
├── biz-repository/          # 业务逻辑层
│   └── src/main/kotlin/com/tencent/devops/repository/
│       ├── dao/                 # 数据访问层（18 文件）
│       ├── resources/           # API 实现
│       │   ├── github/          # GitHub 实现
│       │   ├── scm/             # SCM 实现
│       │   └── tapd/            # TAPD 实现
│       ├── service/             # 业务服务
│       │   ├── code/            # 各代码库类型服务
│       │   ├── github/          # GitHub 服务
│       │   ├── hub/             # SCM Hub 服务
│       │   ├── loader/          # 服务加载器
│       │   └── oauth2/          # OAuth2 服务
│       ├── sdk/                 # SDK 实现
│       └── utils/               # 工具类
│
├── biz-base-scm/            # SCM 基础库（Git/SVN 操作封装）
├── model-repository/        # 数据模型层（JOOQ 生成）
├── boot-repository/         # Spring Boot 启动模块
├── plugin-github/           # GitHub 插件扩展
└── plugin-tapd/             # TAPD 插件扩展
```

### 1.2 支持的代码库类型

| 类型 | 枚举值 | 说明 | 认证方式 |
|------|--------|------|----------|
| **Git（工蜂）** | `CODE_GIT` | 腾讯工蜂 Git | OAuth / SSH / HTTP |
| **TGit** | `CODE_TGIT` | 腾讯 TGit | OAuth / SSH / HTTP |
| **GitHub** | `GITHUB` | GitHub.com | OAuth / GitHub App |
| **GitLab** | `CODE_GITLAB` | GitLab 私有部署 | HTTP / SSH |
| **SVN** | `CODE_SVN` | Subversion | HTTP / SSH |
| **P4** | `CODE_P4` | Perforce | 凭证认证 |

## 二、核心概念

### 2.1 代码库实体模型

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         代码库模型                                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    T_REPOSITORY（主表）                           │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │REPOSITORY_ID│  │ PROJECT_ID  │  │ ALIAS_NAME  │              │   │
│  │  │ (代码库ID)   │  │ (项目ID)    │  │ (别名)       │              │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │    URL      │  │    TYPE     │  │  ENABLE_PAC │              │   │
│  │  │ (代码库地址) │  │ (代码库类型) │  │ (PAC开关)    │              │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│              ┌───────────────┼───────────────┬───────────────┐          │
│              ▼               ▼               ▼               ▼          │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────┐   │
│  │T_REPOSITORY_  │ │T_REPOSITORY_  │ │T_REPOSITORY_  │ │T_REPOSITORY│   │
│  │  CODE_GIT     │ │ CODE_GITLAB   │ │  CODE_SVN     │ │  _GITHUB   │   │
│  │ (Git明细表)    │ │ (GitLab明细)  │ │ (SVN明细表)   │ │(GitHub明细)│   │
│  └───────────────┘ └───────────────┘ └───────────────┘ └───────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心字段说明

> ⚠️ **重要**: 这里的 `PROJECT_ID` 是 `T_PROJECT.english_name`，不是 `T_PROJECT.PROJECT_ID`

| 字段 | 类型 | 说明 |
|------|------|------|
| `REPOSITORY_ID` | Long | 代码库主键（自增） |
| `PROJECT_ID` | String | 项目标识（= T_PROJECT.english_name） |
| `ALIAS_NAME` | String | 代码库别名（用户自定义） |
| `URL` | String | 代码库 URL |
| `TYPE` | String | 代码库类型（CODE_GIT/GITHUB 等） |
| `REPOSITORY_HASH_ID` | String | 代码库 HashId（对外暴露） |
| `ENABLE_PAC` | Boolean | 是否开启 PAC（Pipeline as Code） |
| `SCM_CODE` | String | SCM 配置标识 |

### 2.3 认证类型

```kotlin
enum class RepoAuthType(val value: String) {
    HTTP("HTTP"),           // HTTP 用户名密码
    HTTPS("HTTPS"),         // HTTPS
    SSH("SSH"),             // SSH 密钥
    OAUTH("OAUTH"),         // OAuth 授权
    TOKEN("TOKEN"),         // 个人访问令牌
    GITHUB_APP("GITHUB_APP") // GitHub App
}
```

## 三、核心数据库表

### 3.1 主表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_REPOSITORY` | 代码库主表 | `REPOSITORY_ID`, `PROJECT_ID`, `ALIAS_NAME`, `URL`, `TYPE`, `ENABLE_PAC` |
| `T_REPOSITORY_COMMIT` | 代码提交记录 | `BUILD_ID`, `PIPELINE_ID`, `REPO_ID`, `COMMIT`, `COMMITTER` |

### 3.2 各类型明细表

| 表名 | 说明 | 特有字段 |
|------|------|----------|
| `T_REPOSITORY_CODE_GIT` | 工蜂 Git 明细 | `PROJECT_NAME`, `CREDENTIAL_ID`, `AUTH_TYPE`, `GIT_PROJECT_ID` |
| `T_REPOSITORY_CODE_GITLAB` | GitLab 明细 | `PROJECT_NAME`, `CREDENTIAL_ID`, `AUTH_TYPE` |
| `T_REPOSITORY_CODE_SVN` | SVN 明细 | `REGION`, `PROJECT_NAME`, `SVN_TYPE`, `CREDENTIAL_ID` |
| `T_REPOSITORY_GITHUB` | GitHub 明细 | `PROJECT_NAME`, `CREDENTIAL_ID`, `GIT_PROJECT_ID` |
| `T_REPOSITORY_CODE_P4` | P4 明细 | `PROJECT_NAME`, `CREDENTIAL_ID` |

### 3.3 Token 表

| 表名 | 说明 |
|------|------|
| `T_REPOSITORY_GIT_TOKEN` | 工蜂 OAuth Token |
| `T_REPOSITORY_TGIT_TOKEN` | TGit OAuth Token |
| `T_REPOSITORY_GITHUB_TOKEN` | GitHub OAuth Token |

### 3.4 其他表

| 表名 | 说明 |
|------|------|
| `T_REPOSITORY_GIT_CHECK` | Git Commit Check 记录 |
| `T_REPOSITORY_PIPELINE_REF` | 代码库与流水线关联 |
| `T_REPOSITORY_SCM_CONFIG` | SCM 配置 |
| `T_REPOSITORY_SCM_PROVIDER` | SCM 提供商配置 |
| `T_REPOSITORY_WEBHOOK_REQUEST` | Webhook 请求记录 |

## 四、分层架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              请求入口                                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API 层 (api-repository)                          │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │UserRepository│ │ServiceRepo   │ │ServiceScm    │ │ServiceGithub │    │
│  │Resource      │ │Resource      │ │Resource      │ │Resource      │    │
│  │(用户代码库)   │ │(服务间调用)   │ │(SCM操作)     │ │(GitHub操作)  │    │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       业务层 (biz-repository)                            │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      核心 Service                                 │   │
│  │  RepositoryService (70KB)    - 代码库 CRUD 核心服务               │   │
│  │  RepositoryOauthService      - OAuth 授权服务                    │   │
│  │  RepositoryPacService        - PAC 服务                          │   │
│  │  RepositoryScmConfigService  - SCM 配置服务                      │   │
│  │  CommitService               - 提交记录服务                       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    代码库类型服务 (service/code/)                  │   │
│  │  CodeGitRepositoryService    - 工蜂 Git 服务                     │   │
│  │  CodeTGitRepositoryService   - TGit 服务                         │   │
│  │  CodeGithubRepositoryService - GitHub 服务                       │   │
│  │  CodeGitlabRepositoryService - GitLab 服务                       │   │
│  │  CodeSvnRepositoryService    - SVN 服务                          │   │
│  │  CodeP4RepositoryService     - P4 服务                           │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    SCM Hub 服务 (service/hub/)                    │   │
│  │  ScmApiComposer              - SCM API 组合器                    │   │
│  │  ScmRepositoryApiService     - 仓库 API 服务                     │   │
│  │  ScmFileApiService           - 文件 API 服务                     │   │
│  │  ScmWebhookApiService        - Webhook API 服务                  │   │
│  │  ScmProviderAuthFactory      - 认证工厂                          │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DAO 层 (biz-repository/dao)                      │
│  RepositoryDao (25KB) | RepositoryCodeGitDao | RepositoryGithubDao      │
│  CommitDao | GitTokenDao | RepositoryScmConfigDao | ...                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      数据层 (model-repository + MySQL)                   │
│  数据库：devops_ci_repository（共 15+ 张表）                             │
└─────────────────────────────────────────────────────────────────────────┘
```

## 五、核心类速查

### 5.1 API 接口层

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `UserRepositoryResource` | `/user/repositories` | 用户代码库管理（创建/更新/删除） |
| `ServiceRepositoryResource` | `/service/repositories` | 服务间代码库查询 |
| `ServiceScmResource` | `/service/scm` | SCM 通用操作 |
| `ServiceGitResource` | `/service/git` | Git 操作（分支/Tag/文件） |
| `ServiceGithubResource` | `/service/github` | GitHub 操作 |
| `ServiceOauthResource` | `/service/oauth` | OAuth 授权 |
| `ServiceRepositoryPacResource` | `/service/pac` | PAC 相关 |

### 5.2 Service 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `RepositoryService` | 70KB | 代码库核心服务（最大） |
| `OPRepositoryService` | 32KB | 运维代码库服务 |
| `RepositoryScmConfigService` | 30KB | SCM 配置服务 |
| `CodeGitRepositoryService` | 25KB | 工蜂 Git 服务 |
| `ScmRepositoryApiService` | 23KB | SCM 仓库 API |
| `RepoFileService` | 21KB | 文件服务 |
| `GithubService` | 21KB | GitHub 服务 |

### 5.3 DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `RepositoryDao` | 25KB | 代码库主表访问 |
| `RepositoryCodeGitDao` | 13KB | Git 明细表访问 |
| `RepoPipelineRefDao` | 12KB | 流水线关联访问 |
| `RepositoryGithubDao` | 11KB | GitHub 明细表访问 |
| `RepositoryScmConfigDao` | 10KB | SCM 配置访问 |

## 六、核心流程

### 6.1 代码库创建流程

```
用户请求
    │
    ▼
UserRepositoryResource.create()
    │
    ▼
RepositoryService.userCreate()
    │
    ├─► 权限校验
    │   └─► repositoryPermissionService.validatePermission()
    │
    ├─► 参数校验
    │   ├─► 校验 URL 格式
    │   ├─► 校验别名唯一性
    │   └─► 校验凭证有效性
    │
    ├─► 根据类型创建
    │   └─► CodeRepositoryServiceRegistrar.getService(type).create()
    │       ├─► CodeGitRepositoryService.create()
    │       ├─► CodeGithubRepositoryService.create()
    │       └─► ...
    │
    ├─► 写入数据库
    │   ├─► repositoryDao.create()        # 主表
    │   └─► repositoryCodeXxxDao.create() # 明细表
    │
    └─► 注册到权限中心
        └─► authPermissionApi.createResource()
```

### 6.2 OAuth 授权流程

```
┌─────────────────────────────────────────────────────────────────┐
│                     OAuth 授权流程                               │
├─────────────────────────────────────────────────────────────────┤
│  1. 用户发起授权                                                 │
│     UserRepositoryOauthResource.getOauthUrl()                   │
│                          │                                       │
│                          ▼                                       │
│  2. 重定向到代码托管平台                                          │
│     返回 OAuth 授权 URL                                          │
│                          │                                       │
│                          ▼                                       │
│  3. 用户在平台授权                                               │
│     用户登录并授权 BK-CI 访问                                     │
│                          │                                       │
│                          ▼                                       │
│  4. 回调处理                                                     │
│     ExternalGithubResource.callback() / ExternalRepoResource    │
│                          │                                       │
│                          ▼                                       │
│  5. 获取并存储 Token                                             │
│     RepositoryOauthService.saveToken()                          │
│     存入 T_REPOSITORY_XXX_TOKEN 表                              │
└─────────────────────────────────────────────────────────────────┘
```

### 6.3 代码库类型服务加载机制

```kotlin
// 服务注册器模式
@Service
class CodeRepositoryServiceRegistrar {
    
    private val serviceMap = mutableMapOf<ScmType, CodeRepositoryService>()
    
    fun register(type: ScmType, service: CodeRepositoryService) {
        serviceMap[type] = service
    }
    
    fun getService(type: ScmType): CodeRepositoryService {
        return serviceMap[type] ?: throw NotFoundException("...")
    }
}

// 各类型服务自动注册
@Service
class CodeGitRepositoryService : CodeRepositoryService {
    @PostConstruct
    fun init() {
        registrar.register(ScmType.CODE_GIT, this)
    }
}
```

## 七、与其他模块的关系

### 7.1 依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    Repository 模块依赖关系                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                  ┌───────────────┐                               │
│                  │  repository   │                               │
│                  └───────┬───────┘                               │
│                          │                                       │
│         ┌────────────────┼────────────────┐                     │
│         ▼                ▼                ▼                     │
│  ┌───────────┐    ┌───────────┐    ┌───────────┐               │
│  │  project  │    │  ticket   │    │   auth    │               │
│  │ (项目信息) │    │ (凭证管理) │    │ (权限校验) │               │
│  └───────────┘    └───────────┘    └───────────┘               │
│                                                                  │
│  被依赖：                                                        │
│  - process（流水线拉取代码）                                      │
│  - store（插件代码库）                                           │
│  - openapi（开放接口）                                           │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 服务间调用示例

```kotlin
// Process 模块调用 Repository 获取代码库信息
// 注意：projectId 是 T_PROJECT.english_name
client.get(ServiceRepositoryResource::class).get(
    projectId = projectId,  // english_name
    repositoryId = repositoryHashId,
    repositoryType = RepositoryType.ID
)

// 获取代码库列表
client.get(ServiceRepositoryResource::class).list(
    projectId = projectId,
    repositoryType = ScmType.CODE_GIT.name
)

// 获取文件内容
client.get(ServiceScmResource::class).getFileContent(
    projectName = repoProjectName,
    ref = branch,
    filePath = filePath,
    token = token,
    authType = RepoAuthType.OAUTH,
    scmType = ScmType.CODE_GIT
)
```

## 八、PAC（Pipeline as Code）支持

### 8.1 PAC 概念

PAC 允许用户在代码库中通过 YAML 文件定义流水线，代码库变更时自动同步流水线配置。

### 8.2 PAC 相关字段

```kotlin
// T_REPOSITORY 表
ENABLE_PAC: Boolean      // 是否开启 PAC
YAML_SYNC_STATUS: String // PAC 同步状态（SYNC/SYNCING/FAILED）
```

### 8.3 PAC 服务

```kotlin
@Service
class RepositoryPacService {
    // 开启 PAC
    fun enablePac(projectId: String, repositoryHashId: String)
    
    // 关闭 PAC
    fun disablePac(projectId: String, repositoryHashId: String)
    
    // 同步 YAML 流水线
    fun syncYamlPipeline(projectId: String, repositoryHashId: String)
}
```

## 九、开发规范

### 9.1 新增代码库类型

1. 在 `ScmType` 枚举中添加新类型
2. 创建明细表 `T_REPOSITORY_CODE_XXX`
3. 创建 DAO 类 `RepositoryCodeXxxDao`
4. 创建服务类 `CodeXxxRepositoryService` 实现 `CodeRepositoryService`
5. 在 `CodeRepositoryServiceRegistrar` 中注册
6. 添加对应的 POJO 类（如 `CodeXxxRepository`）

### 9.2 代码库查询示例

```kotlin
// 根据 HashId 查询
val repository = repositoryDao.getByHashId(
    dslContext = dslContext,
    projectId = projectId,  // english_name
    repositoryHashId = hashId
)

// 根据 ID 查询
val repository = repositoryDao.get(
    dslContext = dslContext,
    projectId = projectId,
    repositoryId = repositoryId
)

// 列表查询
val list = repositoryDao.listByProject(
    dslContext = dslContext,
    projectId = projectId,
    repositoryType = ScmType.CODE_GIT.name,
    offset = offset,
    limit = limit
)
```

### 9.3 凭证使用

代码库的凭证存储在 Ticket 模块，通过 `CREDENTIAL_ID` 关联：

```kotlin
// 获取凭证
val credential = client.get(ServiceCredentialResource::class).get(
    projectId = projectId,
    credentialId = credentialId
)
```

## 十、常见问题

**Q: 如何判断代码库类型？**
A: 查看 `T_REPOSITORY.TYPE` 字段，值为 `CODE_GIT`、`GITHUB`、`CODE_GITLAB` 等。

**Q: repositoryId 和 repositoryHashId 的区别？**
A: `repositoryId` 是数据库自增 ID，`repositoryHashId` 是对外暴露的 Hash 编码 ID。

**Q: 如何获取代码库的认证信息？**
A: 通过 `CREDENTIAL_ID` 从 Ticket 模块获取凭证，或通过 OAuth Token 表获取授权信息。

**Q: PAC 开启后代码库能否删除？**
A: 不能，需要先关闭 PAC 才能删除代码库。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
