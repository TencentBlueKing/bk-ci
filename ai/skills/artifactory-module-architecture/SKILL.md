---
name: artifactory-module-architecture
description: Artifactory 制品库模块架构指南，涵盖制品上传下载、归档、存储后端适配（BkRepo/磁盘）、制品元数据、清理策略、权限控制。当用户开发制品库功能、处理构建产物归档、调用 ArchiveFileService、配置 BkRepo 集成、排查文件上传下载问题、配置清理策略或实现制品管理时使用。
---

# Artifactory 制品库模块架构指南

> **模块定位**: BK-CI 制品库模块，负责构建产物的存储、下载、管理，支持 BkRepo（蓝鲸制品库，推荐）和本地磁盘两种存储后端。

## 一、模块结构

代码路径: `src/backend/ci/core/artifactory/`

| 子模块 | 职责 |
|--------|------|
| `api-artifactory/` | API 接口定义（builds/service/user 三类） |
| `biz-artifactory/` | 业务逻辑、DAO、消息监听、商店归档 |
| `model-artifactory/` | JOOQ 生成的数据模型 |
| `boot-artifactory/` | Spring Boot 启动模块 |

> 详细目录结构和完整类参考见 [reference/architecture-details.md](reference/architecture-details.md)

## 二、核心概念

### 制品类型与文件类型

```kotlin
// 制品类型 — 决定存储路径的顶层分类
enum class ArtifactoryType(val type: String) {
    PIPELINE("PIPELINE"),     // 流水线产物
    CUSTOM_DIR("CUSTOM_DIR"), // 自定义目录
    REPORT("REPORT"),         // 报告文件
}

// 文件类型 — 标识文件用途
enum class FileTypeEnum(val type: String) {
    BK_ARCHIVE("BK_ARCHIVE"),     // 构建归档
    BK_CUSTOM("BK_CUSTOM"),       // 自定义文件
    BK_REPORT("BK_REPORT"),       // 报告文件
    BK_LOG("BK_LOG"),             // 日志文件
    BK_PLUGIN_FE("BK_PLUGIN_FE"), // 插件前端
    BK_STATIC("BK_STATIC"),       // 静态资源
}
```

### 文件路径规范

```
/{projectId}/{pipelineId}/{buildId}/{fileName}          # 流水线产物
/{projectId}/custom/{customPath}/{fileName}             # 自定义目录
/{projectId}/report/{pipelineId}/{buildId}/{taskId}/{fileName}  # 报告文件
```

### 存储后端

| 类型 | 实现类 | 切换方式 |
|------|--------|----------|
| **BkRepo**（推荐） | `BkRepoArchiveFileServiceImpl` | `bkrepo.enabled=true` |
| **本地磁盘** | `DiskArchiveFileServiceImpl` | `bkrepo.enabled=false` |

## 三、核心数据库表

> ⚠️ `PROJECT_CODE` / `PROJECT_ID` 都是 `T_PROJECT.english_name`

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_FILE_INFO` | 文件信息主表 | `ID`, `PROJECT_CODE`, `FILE_TYPE`, `FILE_PATH`, `FILE_NAME`, `FILE_SIZE` |
| `T_FILE_PROPS_INFO` | 文件元数据 | `FILE_ID`, `PROPS_KEY`, `PROPS_VALUE` |
| `T_TOKEN` | 下载令牌 | `USER_ID`, `PROJECT_ID`, `PATH`, `TOKEN`, `EXPIRE_TIME` |
| `T_FILE_TASK` | 文件任务 | `TASK_ID`, `FILE_TYPE`, `FILE_PATH`, `STATUS`, `BUILD_ID` |

## 四、核心服务速查

| 类名 | 职责 |
|------|------|
| `ArchiveFileService` | 文件归档接口（策略模式入口） |
| `BkRepoArchiveFileServiceImpl` | BkRepo 归档实现（29KB） |
| `DiskArchiveFileServiceImpl` | 磁盘归档实现（28KB） |
| `FileTaskService` | 文件异步任务管理 |
| `ArchiveAtomService` | 插件包归档 |
| `PipelineBuildArtifactoryListener` | 构建完成后制品处理（MQ） |

## 五、核心流程

### 5.1 构建产物上传

```
BuildFileResource.uploadFile()
    → ArchiveFileService.uploadFile()
        ├─ BkRepo: 调用 BkRepo API 上传
        └─ Disk: 写入本地磁盘
    → FileDao.create()  // 记录文件信息到 DB
```

**验证检查点**:
1. 上传前 — 校验 projectId 有效性、文件大小限制、用户权限
2. 上传后 — 确认 FileDao 记录已创建，BkRepo 返回成功状态码
3. 失败恢复 — 若 DB 写入失败，需清理已上传的文件避免孤立制品

```kotlin
// 上传文件示例
archiveFileService.uploadFile(
    userId = userId,
    projectId = projectId,  // english_name
    pipelineId = pipelineId,
    buildId = buildId,
    file = file,
    fileType = FileTypeEnum.BK_ARCHIVE
)
// 验证：检查返回值确认上传成功，异常时捕获 RemoteServiceException
```

### 5.2 构建产物下载

```
UserFileResource.downloadFile() / BuildFileResource.downloadFile()
    → ArchiveFileService.downloadFile()
        ├─ 验证权限（检查用户项目权限）
        ├─ 获取文件（BkRepo API / 本地磁盘读取）
        └─ 返回文件流
```

**验证检查点**:
1. 权限校验 — 用户必须拥有项目访问权限
2. 文件存在性 — 检查 FileDao 记录存在且路径有效
3. Token 过期 — 临时下载链接需验证 `T_TOKEN.EXPIRE_TIME`

```kotlin
// 下载文件示例
val inputStream = archiveFileService.downloadFile(
    userId = userId,
    projectId = projectId,
    filePath = filePath
)
// 验证：确认 inputStream 非空，处理 NotFoundException
```

### 5.3 插件包归档

```
ServiceArchiveAtomResource.archiveAtom()
    → ArchiveAtomService.archiveAtom()
        ├─ 下载插件包（从代码库获取构建产物）
        ├─ 上传到制品库（ArchiveAtomToBkRepoServiceImpl）
        └─ 更新插件环境信息（记录 PKG_PATH）
```

**验证检查点**:
1. 下载完整性 — 校验下载文件的 MD5/SHA256
2. 归档确认 — BkRepo 返回成功后再更新 PKG_PATH
3. 回滚策略 — 归档失败时清理已上传的部分文件

## 六、模块依赖

| 方向 | 模块 | 用途 |
|------|------|------|
| 依赖 | `project` | 项目信息查询 |
| 依赖 | `auth` | 权限校验 |
| 依赖 | `BkRepo` | 外部存储后端 |
| 被依赖 | `process` | 流水线归档产物 |
| 被依赖 | `store` | 插件包归档 |
| 被依赖 | `worker` | 构建机上传下载 |

### 服务间调用示例

```kotlin
// Process 模块获取构建产物列表（projectId 是 english_name）
client.get(ServiceArtifactoryResource::class).search(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId
)

// 获取带 Token 的临时下载 URL
client.get(ServiceArtifactoryResource::class).getFileDownloadUrls(
    projectId = projectId,
    artifactoryType = ArtifactoryType.PIPELINE,
    filePath = filePath
)

// Worker 构建机上传文件
client.get(BuildFileResource::class).uploadFile(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    file = file
)
```

## 七、BkRepo 配置

```yaml
# application-artifactory.yml
bkrepo:
  gatewayUrl: http://bkrepo.example.com
  enabled: true       # false 则回退到本地磁盘
  repoName: pipeline
```

## 八、新增存储后端检查清单

1. 实现 `ArchiveFileService` 接口的所有方法
2. 创建 `*ArchiveFileServiceImpl` 实现类
3. 在配置中添加存储后端选择逻辑
4. **验证**: 单元测试覆盖上传/下载/删除，确认大文件和并发场景
5. **验证**: 集成测试确认与 FileDao 记录一致性

## 九、常见问题

| 问题 | 答案 |
|------|------|
| 如何切换存储后端？ | 配置 `bkrepo.enabled`，`true` 用 BkRepo，`false` 用磁盘 |
| 构建产物保留多久？ | 跟随项目配置的保留策略，默认与构建记录保留时间一致 |
| 如何获取下载链接？ | 调用 `getFileDownloadUrls` 获取带 Token 的临时链接 |
| 大文件上传限制？ | 配置文件中设置，可按需调整 |
