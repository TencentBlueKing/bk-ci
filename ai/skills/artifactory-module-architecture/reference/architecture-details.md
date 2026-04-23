# Artifactory 模块架构详细参考

## 完整子模块目录结构

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

## 分层架构图

```
请求入口
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API 层 (api-artifactory)                         │
│  BuildFileResource (构建时上传)    ServiceArtifactoryResource (服务间)    │
│  UserFileResource (用户文件)       UserArtifactoryResource (用户制品)     │
│  BuildArtifactoryResource (构建制品) ServiceBkRepoResource (BkRepo操作)  │
│  UserReportStorageResource (报告存储)                                     │
└─────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       业务层 (biz-artifactory)                           │
│  核心: ArchiveFileService → BkRepoArchiveFileServiceImpl (29KB)         │
│                            → DiskArchiveFileServiceImpl (28KB)          │
│  商店: ArchiveAtomService / ArchiveStorePkgService                       │
│  消息: PipelineBuildArtifactoryListener (构建完成后处理)                   │
└─────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  DAO 层: FileDao (7.5KB) | FileTaskDao (4.8KB)                          │
│  数据层: devops_ci_artifactory（4 张表）                                  │
│  存储后端: BkRepo（推荐）/ 本地磁盘                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

## 模块依赖关系

```
                  ┌───────────────┐
                  │  artifactory  │
                  └───────┬───────┘
                          │
         ┌────────────────┼────────────────┐
         ▼                ▼                ▼
  ┌───────────┐    ┌───────────┐    ┌───────────┐
  │  project  │    │   auth    │    │  BkRepo   │
  │ (项目信息) │    │ (权限校验) │    │ (存储后端) │
  └───────────┘    └───────────┘    └───────────┘

  被依赖：process（流水线归档）、store（插件包归档）、worker（构建机上传下载）
```

## 完整 API 接口层参考

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `BuildFileResource` | `/build/files` | 构建时文件上传下载 |
| `BuildArtifactoryResource` | `/build/artifactories` | 构建时制品操作 |
| `ServiceArtifactoryResource` | `/service/artifactories` | 服务间制品操作 |
| `ServiceFileResource` | `/service/files` | 服务间文件操作 |
| `UserFileResource` | `/user/files` | 用户文件操作 |
| `UserArtifactoryResource` | `/user/artifactories` | 用户制品操作 |
| `ServiceArchiveAtomResource` | `/service/archive/atom` | 插件归档 |

## 完整 Service 层参考

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `BkRepoArchiveFileServiceImpl` | 29KB | BkRepo 文件归档实现 |
| `DiskArchiveFileServiceImpl` | 28KB | 磁盘文件归档实现 |
| `ArchiveAtomServiceImpl` | 16KB | 插件归档服务 |
| `ArchiveStorePkgServiceImpl` | 16KB | 商店包归档服务 |
| `FileTaskServiceImpl` | 10KB | 文件任务服务 |
| `ArchiveFileService` | 8KB | 文件归档接口 |

## DAO 层参考

| 类名 | 职责 |
|------|------|
| `FileDao` | 文件信息 CRUD（T_FILE_INFO） |
| `FileTaskDao` | 文件任务 CRUD（T_FILE_TASK） |
