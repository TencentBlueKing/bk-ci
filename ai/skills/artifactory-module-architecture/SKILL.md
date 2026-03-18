---
name: artifactory-module-architecture
description: Artifactory 制品库模块架构指南，涵盖制品上传下载、存储后端适配、制品元数据、清理策略、权限控制。当用户开发制品库功能、处理制品存储、配置清理策略或实现制品管理时使用。
---

# Artifactory 制品库模块架构指南

> **模块定位**: Artifactory 是 BK-CI 的制品库模块，负责构建产物的存储、下载、管理，支持对接本地磁盘存储或 BkRepo（蓝鲸制品库）等后端存储系统。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/artifactory/
├── api-artifactory/         # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/artifactory/
│       ├── api/
│       │   ├── builds/          # 构建时接口
│       │   ├── service/         # 服务间接口
│       │   └── user/            # 用户接口
│       ├── constant/            # 常量和消息码
│       └── pojo/                # 数据对象
│           └── enums/           # 枚举定义
│
├── biz-artifactory/         # 业务逻辑层
│   └── src/main/kotlin/com/tencent/devops/artifactory/
│       ├── config/              # 配置类
│       ├── dao/                 # 数据访问层
│       ├── mq/                  # 消息队列监听
│       ├── resources/           # API 实现
│       ├── service/             # 业务服务
│       │   └── impl/            # 服务实现
│       ├── store/               # 研发商店相关
│       │   ├── resources/       # 商店 API 实现
│       │   └── service/         # 商店归档服务
│       └── util/                # 工具类
│
├── model-artifactory/       # 数据模型层（JOOQ 生成）
└── boot-artifactory/        # Spring Boot 启动模块
```

### 1.2 存储后端类型

| 类型 | 说明 | 实现类 |
|------|------|--------|
| **BkRepo** | 蓝鲸制品库（推荐） | `BkRepoArchiveFileServiceImpl` |
| **Disk** | 本地磁盘存储 | `DiskArchiveFileServiceImpl` |

## 二、核心概念

### 2.1 制品类型

```kotlin
enum class ArtifactoryType(val type: String) {
    PIPELINE("PIPELINE"),     // 流水线产物
    CUSTOM_DIR("CUSTOM_DIR"), // 自定义目录
    REPORT("REPORT"),         // 报告文件
}
```

### 2.2 文件类型

```kotlin
enum class FileTypeEnum(val type: String) {
    BK_ARCHIVE("BK_ARCHIVE"),     // 构建归档
    BK_CUSTOM("BK_CUSTOM"),       // 自定义文件
    BK_REPORT("BK_REPORT"),       // 报告文件
    BK_LOG("BK_LOG"),             // 日志文件
    BK_PLUGIN_FE("BK_PLUGIN_FE"), // 插件前端
    BK_STATIC("BK_STATIC"),       // 静态资源
}
```

### 2.3 文件路径规范

```
# 流水线产物路径
/{projectId}/{pipelineId}/{buildId}/{fileName}

# 自定义目录路径
/{projectId}/custom/{customPath}/{fileName}

# 报告文件路径
/{projectId}/report/{pipelineId}/{buildId}/{taskId}/{fileName}
```

## 三、核心数据库表

### 3.1 文件信息表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_FILE_INFO` | 文件信息主表 | `ID`, `PROJECT_CODE`, `FILE_TYPE`, `FILE_PATH`, `FILE_NAME`, `FILE_SIZE` |
| `T_FILE_PROPS_INFO` | 文件元数据表 | `FILE_ID`, `PROPS_KEY`, `PROPS_VALUE` |
| `T_TOKEN` | 下载令牌表 | `USER_ID`, `PROJECT_ID`, `PATH`, `TOKEN`, `EXPIRE_TIME` |
| `T_FILE_TASK` | 文件任务表 | `TASK_ID`, `FILE_TYPE`, `FILE_PATH`, `STATUS`, `BUILD_ID` |

### 3.2 字段说明

> ⚠️ **重要**: `PROJECT_CODE` / `PROJECT_ID` 都是 `T_PROJECT.english_name`

| 字段 | 说明 |
|------|------|
| `PROJECT_CODE` | 项目标识（= T_PROJECT.english_name） |
| `FILE_TYPE` | 文件类型（BK_ARCHIVE/BK_CUSTOM 等） |
| `FILE_PATH` | 文件存储路径 |
| `ARTIFACTORY_TYPE` | 制品库类型（PIPELINE/CUSTOM_DIR） |

## 四、分层架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              请求入口                                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API 层 (api-artifactory)                         │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │BuildFile     │ │ServiceArti   │ │UserFile      │ │UserArtifact  │    │
│  │Resource      │ │factoryRes    │ │Resource      │ │oryResource   │    │
│  │(构建时上传)   │ │(服务间调用)   │ │(用户文件)     │ │(用户制品)     │    │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘    │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                     │
│  │BuildArtifact │ │ServiceBkRepo │ │UserReport    │                     │
│  │oryResource   │ │Resource      │ │StorageRes    │                     │
│  │(构建制品)     │ │(BkRepo操作)  │ │(报告存储)     │                     │
│  └──────────────┘ └──────────────┘ └──────────────┘                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       业务层 (biz-artifactory)                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      核心 Service                                 │   │
│  │  ArchiveFileService          - 文件归档服务（接口）               │   │
│  │  BkRepoArchiveFileServiceImpl - BkRepo 实现 (29KB)               │   │
│  │  DiskArchiveFileServiceImpl   - 磁盘存储实现 (28KB)              │   │
│  │  FileTaskService             - 文件任务服务                       │   │
│  │  PipelineBuildArtifactoryService - 流水线构建制品服务             │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      商店归档服务 (store/service/)                │   │
│  │  ArchiveAtomService          - 插件归档服务                       │   │
│  │  ArchiveStorePkgService      - 商店包归档服务                     │   │
│  │  ArchiveAtomToBkRepoServiceImpl - 插件归档到 BkRepo              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      消息监听 (mq/)                               │   │
│  │  PipelineBuildArtifactoryListener - 构建完成后处理制品            │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DAO 层 (biz-artifactory/dao)                     │
│  FileDao (7.5KB) | FileTaskDao (4.8KB)                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      数据层 (model-artifactory + MySQL)                  │
│  数据库：devops_ci_artifactory（共 4 张表）                              │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      存储后端 (BkRepo / 本地磁盘)                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 五、核心类速查

### 5.1 API 接口层

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `BuildFileResource` | `/build/files` | 构建时文件上传下载 |
| `BuildArtifactoryResource` | `/build/artifactories` | 构建时制品操作 |
| `ServiceArtifactoryResource` | `/service/artifactories` | 服务间制品操作 |
| `ServiceFileResource` | `/service/files` | 服务间文件操作 |
| `UserFileResource` | `/user/files` | 用户文件操作 |
| `UserArtifactoryResource` | `/user/artifactories` | 用户制品操作 |
| `ServiceArchiveAtomResource` | `/service/archive/atom` | 插件归档 |

### 5.2 Service 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `BkRepoArchiveFileServiceImpl` | 29KB | BkRepo 文件归档实现 |
| `DiskArchiveFileServiceImpl` | 28KB | 磁盘文件归档实现 |
| `ArchiveAtomServiceImpl` | 16KB | 插件归档服务 |
| `ArchiveStorePkgServiceImpl` | 16KB | 商店包归档服务 |
| `FileTaskServiceImpl` | 10KB | 文件任务服务 |
| `ArchiveFileService` | 8KB | 文件归档接口 |

### 5.3 DAO 层

| 类名 | 职责 |
|------|------|
| `FileDao` | 文件信息访问 |
| `FileTaskDao` | 文件任务访问 |

## 六、核心流程

### 6.1 构建产物上传流程

```
构建插件上传文件
    │
    ▼
BuildFileResource.uploadFile()
    │
    ▼
ArchiveFileService.uploadFile()
    │
    ├─► BkRepoArchiveFileServiceImpl.uploadFile()
    │   └─► 调用 BkRepo API 上传文件
    │
    └─► DiskArchiveFileServiceImpl.uploadFile()
        └─► 写入本地磁盘
    │
    ▼
FileDao.create()
    │
    └─► 记录文件信息到数据库
```

### 6.2 构建产物下载流程

```
用户/插件请求下载
    │
    ▼
UserFileResource.downloadFile() / BuildFileResource.downloadFile()
    │
    ▼
ArchiveFileService.downloadFile()
    │
    ├─► 验证权限
    │   └─► 检查用户是否有项目权限
    │
    ├─► 获取文件
    │   ├─► BkRepo: 调用 BkRepo API 获取
    │   └─► Disk: 从本地磁盘读取
    │
    └─► 返回文件流
```

### 6.3 插件包归档流程

```
插件发布时归档
    │
    ▼
ServiceArchiveAtomResource.archiveAtom()
    │
    ▼
ArchiveAtomService.archiveAtom()
    │
    ├─► 下载插件包
    │   └─► 从代码库下载构建产物
    │
    ├─► 上传到制品库
    │   └─► ArchiveAtomToBkRepoServiceImpl.archiveAtom()
    │
    └─► 更新插件环境信息
        └─► 记录 PKG_PATH
```

## 七、与其他模块的关系

### 7.1 依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    Artifactory 模块依赖关系                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                  ┌───────────────┐                               │
│                  │  artifactory  │                               │
│                  └───────┬───────┘                               │
│                          │                                       │
│         ┌────────────────┼────────────────┐                     │
│         ▼                ▼                ▼                     │
│  ┌───────────┐    ┌───────────┐    ┌───────────┐               │
│  │  project  │    │   auth    │    │  BkRepo   │               │
│  │ (项目信息) │    │ (权限校验) │    │ (存储后端) │               │
│  └───────────┘    └───────────┘    └───────────┘               │
│                                                                  │
│  被依赖：                                                        │
│  - process（流水线归档产物）                                      │
│  - store（插件包归档）                                           │
│  - worker（构建机上传下载）                                       │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 服务间调用示例

```kotlin
// Process 模块获取构建产物列表
// 注意：projectId 是 T_PROJECT.english_name
client.get(ServiceArtifactoryResource::class).search(
    projectId = projectId,  // english_name
    pipelineId = pipelineId,
    buildId = buildId
)

// 获取文件下载 URL
client.get(ServiceArtifactoryResource::class).getFileDownloadUrls(
    projectId = projectId,
    artifactoryType = ArtifactoryType.PIPELINE,
    filePath = filePath
)

// Worker 上传文件
client.get(BuildFileResource::class).uploadFile(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    file = file
)
```

## 八、BkRepo 集成

### 8.1 BkRepo 配置

```yaml
# application-artifactory.yml
bkrepo:
  # BkRepo 服务地址
  gatewayUrl: http://bkrepo.example.com
  # 是否启用 BkRepo
  enabled: true
  # 仓库名称
  repoName: pipeline
```

### 8.2 BkRepo 工具类

```kotlin
// BkRepoUtils.kt
object BkRepoUtils {
    // 构建 BkRepo 路径
    fun buildPath(projectId: String, pipelineId: String, buildId: String, fileName: String): String
    
    // 上传文件到 BkRepo
    fun uploadFile(path: String, file: File): Boolean
    
    // 从 BkRepo 下载文件
    fun downloadFile(path: String): InputStream
}
```

## 九、开发规范

### 9.1 新增存储后端

1. 实现 `ArchiveFileService` 接口
2. 创建对应的 `*ArchiveFileServiceImpl` 类
3. 在配置中添加存储后端选择逻辑
4. 实现文件上传、下载、删除等方法

### 9.2 文件操作示例

```kotlin
// 上传文件
archiveFileService.uploadFile(
    userId = userId,
    projectId = projectId,  // english_name
    pipelineId = pipelineId,
    buildId = buildId,
    file = file,
    fileType = FileTypeEnum.BK_ARCHIVE
)

// 下载文件
val inputStream = archiveFileService.downloadFile(
    userId = userId,
    projectId = projectId,
    filePath = filePath
)

// 查询文件列表
val files = fileDao.listByPath(
    dslContext = dslContext,
    projectCode = projectId,
    filePath = path
)
```

## 十、常见问题

**Q: 如何切换存储后端？**
A: 通过配置 `bkrepo.enabled` 控制，`true` 使用 BkRepo，`false` 使用本地磁盘。

**Q: 构建产物保留多久？**
A: 根据项目配置的保留策略，默认跟随构建记录保留时间。

**Q: 如何获取文件下载链接？**
A: 调用 `getFileDownloadUrls` 接口获取带 Token 的临时下载链接。

**Q: 大文件上传有限制吗？**
A: 有，默认限制在配置文件中设置，可根据需要调整。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
