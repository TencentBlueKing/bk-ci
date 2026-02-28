---
name: store-module-architecture
description: Store 研发商店模块架构指南，涵盖插件/模板/镜像管理、版本发布、审核流程、商店市场、扩展点机制。当用户开发研发商店功能、发布插件、管理模板或实现扩展点时使用。
---

# Store 研发商店模块架构指南

> **模块定位**: Store 是 BK-CI 的研发商店模块，负责管理流水线插件（Atom）、流水线模板（Template）、容器镜像（Image）等可复用组件的发布、审核、安装、统计等全生命周期管理。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/store/
├── api-store/               # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/store/
│       ├── api/
│       │   ├── atom/            # 插件相关接口（25+ 文件）
│       │   ├── common/          # 通用接口（40+ 文件）
│       │   ├── container/       # 容器相关接口
│       │   ├── image/           # 镜像相关接口
│       │   └── template/        # 模板相关接口
│       ├── constant/            # 常量和消息码
│       └── pojo/                # 数据对象（100+ 文件）
│           ├── app/             # 应用相关
│           ├── atom/            # 插件相关
│           ├── common/          # 通用对象
│           ├── image/           # 镜像相关
│           └── template/        # 模板相关
│
├── biz-store/               # 业务逻辑层
│   └── src/main/kotlin/com/tencent/devops/store/
│       ├── atom/                # 插件业务
│       │   ├── dao/             # 插件数据访问
│       │   ├── factory/         # 工厂类
│       │   ├── resources/       # API 实现
│       │   └── service/         # 插件服务
│       ├── common/              # 通用业务
│       │   ├── dao/             # 通用数据访问（60+ 文件）
│       │   ├── handler/         # 处理器链
│       │   ├── resources/       # API 实现
│       │   └── service/         # 通用服务
│       ├── image/               # 镜像业务
│       └── template/            # 模板业务
│
├── model-store/             # 数据模型层（JOOQ 生成）
└── boot-store/              # Spring Boot 启动模块
```

### 1.2 Store 组件类型

| 类型 | 枚举值 | 说明 | 核心表 |
|------|--------|------|--------|
| **插件（Atom）** | `ATOM` | 流水线可执行插件 | `T_ATOM` |
| **模板（Template）** | `TEMPLATE` | 流水线模板 | `T_TEMPLATE` |
| **镜像（Image）** | `IMAGE` | 容器构建镜像 | `T_IMAGE` |

## 二、核心概念

### 2.1 插件（Atom）模型

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         插件模型                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      T_ATOM（插件主表）                           │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │  ATOM_CODE  │  │    NAME     │  │   VERSION   │              │   │
│  │  │ (插件标识)   │  │ (插件名称)   │  │ (版本号)     │              │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │ ATOM_STATUS │  │ CLASS_TYPE  │  │ LATEST_FLAG │              │   │
│  │  │ (插件状态)   │  │ (插件大类)   │  │ (最新版本)   │              │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│         ┌────────────────────┼────────────────────┐                     │
│         ▼                    ▼                    ▼                     │
│  ┌───────────────┐   ┌───────────────┐   ┌───────────────┐             │
│  │ T_ATOM_ENV_   │   │ T_ATOM_       │   │ T_ATOM_       │             │
│  │     INFO      │   │   FEATURE     │   │ VERSION_LOG   │             │
│  │ (执行环境信息) │   │ (特性配置)    │   │ (版本日志)     │             │
│  └───────────────┘   └───────────────┘   └───────────────┘             │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 插件核心字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `ID` | String | 插件版本 ID（UUID） |
| `ATOM_CODE` | String | 插件唯一标识（不变） |
| `NAME` | String | 插件名称 |
| `VERSION` | String | 版本号（如 1.0.0） |
| `ATOM_STATUS` | Int | 插件状态 |
| `CLASS_TYPE` | String | 插件大类（marketBuild 等） |
| `JOB_TYPE` | String | 适用 Job 类型（AGENT/AGENT_LESS） |
| `OS` | String | 支持的操作系统 |
| `CLASSIFY_ID` | String | 分类 ID |
| `LATEST_FLAG` | Boolean | 是否最新版本 |
| `DEFAULT_FLAG` | Boolean | 是否默认插件 |
| `PUBLISHER` | String | 发布者 |
| `REPOSITORY_HASH_ID` | String | 代码库 HashId |

### 2.3 插件状态流转

```kotlin
enum class AtomStatusEnum(val status: Int) {
    INIT(0),              // 初始化
    COMMITTING(1),        // 提交中
    BUILDING(2),          // 构建中
    BUILD_FAIL(3),        // 构建失败
    TESTING(4),           // 测试中
    AUDITING(5),          // 审核中
    AUDIT_REJECT(6),      // 审核驳回
    RELEASED(7),          // 已发布
    GROUNDING_SUSPENSION(8), // 上架中止
    UNDERCARRIAGING(9),   // 下架中
    UNDERCARRIAGED(10),   // 已下架
}
```

```
┌─────────────────────────────────────────────────────────────────┐
│                     插件状态流转图                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  INIT ──► COMMITTING ──► BUILDING ──► TESTING ──► AUDITING     │
│                              │                        │          │
│                              ▼                        ▼          │
│                         BUILD_FAIL              AUDIT_REJECT     │
│                                                       │          │
│                                                       ▼          │
│                                                   RELEASED       │
│                                                       │          │
│                                                       ▼          │
│                                              UNDERCARRIAGING     │
│                                                       │          │
│                                                       ▼          │
│                                              UNDERCARRIAGED      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.4 插件分类

```kotlin
// 插件大类
enum class AtomTypeEnum(val type: Int) {
    SELF_DEVELOPED(0),    // 自研
    THIRD_PARTY(1),       // 第三方
}

// Job 类型
enum class JobTypeEnum(val type: String) {
    AGENT("AGENT"),           // 有构建环境
    AGENT_LESS("AGENT_LESS"), // 无构建环境
}
```

## 三、核心数据库表

### 3.1 插件相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_ATOM` | 插件主表 | `ATOM_CODE`, `NAME`, `VERSION`, `ATOM_STATUS`, `LATEST_FLAG` |
| `T_ATOM_ENV_INFO` | 插件执行环境 | `ATOM_ID`, `PKG_PATH`, `LANGUAGE`, `TARGET` |
| `T_ATOM_FEATURE` | 插件特性 | `ATOM_CODE`, `VISIBILITY_LEVEL`, `YAML_FLAG`, `QUALITY_FLAG` |
| `T_ATOM_BUILD_INFO` | 插件构建信息 | `LANGUAGE`, `SCRIPT`, `SAMPLE_PROJECT_PATH` |
| `T_ATOM_VERSION_LOG` | 版本日志 | `ATOM_ID`, `RELEASE_TYPE`, `CONTENT` |
| `T_ATOM_LABEL_REL` | 插件标签关联 | `ATOM_ID`, `LABEL_ID` |
| `T_ATOM_OFFLINE` | 插件下架记录 | `ATOM_CODE`, `EXPIRE_TIME`, `STATUS` |

### 3.2 模板相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_TEMPLATE` | 模板主表 | `TEMPLATE_CODE`, `TEMPLATE_NAME`, `VERSION`, `TEMPLATE_STATUS` |
| `T_TEMPLATE_CATEGORY_REL` | 模板分类关联 | `TEMPLATE_ID`, `CATEGORY_ID` |
| `T_TEMPLATE_LABEL_REL` | 模板标签关联 | `TEMPLATE_ID`, `LABEL_ID` |

### 3.3 镜像相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_IMAGE` | 镜像主表 | `IMAGE_CODE`, `IMAGE_NAME`, `VERSION`, `IMAGE_STATUS` |
| `T_IMAGE_CATEGORY_REL` | 镜像分类关联 | `IMAGE_ID`, `CATEGORY_ID` |
| `T_IMAGE_LABEL_REL` | 镜像标签关联 | `IMAGE_ID`, `LABEL_ID` |

### 3.4 通用表

| 表名 | 说明 |
|------|------|
| `T_CLASSIFY` | 分类表 |
| `T_CATEGORY` | 范畴表 |
| `T_LABEL` | 标签表 |
| `T_STORE_MEMBER` | 组件成员表 |
| `T_STORE_PROJECT_REL` | 组件项目关联表 |
| `T_STORE_COMMENT` | 评论表 |
| `T_STORE_COMMENT_REPLY` | 评论回复表 |
| `T_STORE_COMMENT_PRAISE` | 评论点赞表 |
| `T_STORE_STATISTICS` | 统计表 |
| `T_STORE_APPROVE` | 审批表 |
| `T_STORE_SENSITIVE_API` | 敏感 API 表 |
| `T_STORE_SENSITIVE_CONF` | 敏感配置表 |

### 3.5 容器编译环境表

| 表名 | 说明 |
|------|------|
| `T_APPS` | 编译环境信息表 |
| `T_APP_ENV` | 编译环境变量表 |
| `T_APP_VERSION` | 编译环境版本表 |
| `T_CONTAINER` | 容器信息表 |
| `T_BUILD_RESOURCE` | 构建资源表 |

## 四、分层架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              请求入口                                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API 层 (api-store)                               │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │UserMarket    │ │ServiceAtom   │ │UserTemplate  │ │UserImage     │    │
│  │AtomResource  │ │Resource      │ │Resource      │ │Resource      │    │
│  │(用户插件管理) │ │(服务间调用)   │ │(模板管理)     │ │(镜像管理)    │    │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘    │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                     │
│  │UserAtom      │ │OpAtom        │ │UserStore     │                     │
│  │ReleaseRes    │ │Resource      │ │MemberRes     │                     │
│  │(插件发布)     │ │(运维管理)     │ │(成员管理)     │                     │
│  └──────────────┘ └──────────────┘ └──────────────┘                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       业务层 (biz-store)                                 │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      插件服务 (atom/service/)                     │   │
│  │  MarketAtomService       - 插件市场服务                           │   │
│  │  AtomReleaseService      - 插件发布服务                           │   │
│  │  AtomService             - 插件基础服务                           │   │
│  │  MarketAtomEnvService    - 插件环境服务                           │   │
│  │  MarketAtomArchiveService - 插件归档服务                          │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      通用服务 (common/service/)                   │   │
│  │  StoreCommentService     - 评论服务                               │   │
│  │  StoreMemberService      - 成员管理服务                           │   │
│  │  StoreProjectService     - 项目关联服务                           │   │
│  │  StoreStatisticService   - 统计服务                               │   │
│  │  StoreApproveService     - 审批服务                               │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      处理器链 (common/handler/)                   │   │
│  │  StoreCreateHandlerChain   - 创建处理器链                         │   │
│  │  StoreUpdateHandlerChain   - 更新处理器链                         │   │
│  │  StoreDeleteHandlerChain   - 删除处理器链                         │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DAO 层 (biz-store/dao)                           │
│  AtomDao (59KB) | MarketAtomDao (31KB) | StoreProjectRelDao (25KB)       │
│  StoreBaseQueryDao (20KB) | MarketAtomEnvInfoDao | ...                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      数据层 (model-store + MySQL)                        │
│  数据库：devops_ci_store（共 50+ 张表）                                  │
└─────────────────────────────────────────────────────────────────────────┘
```

## 五、核心类速查

### 5.1 API 接口层

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `UserMarketAtomResource` | `/user/market/atom` | 用户插件市场操作 |
| `UserAtomReleaseResource` | `/user/market/atom/release` | 插件发布 |
| `ServiceAtomResource` | `/service/atoms` | 服务间插件查询 |
| `ServiceMarketAtomResource` | `/service/market/atom` | 服务间市场插件 |
| `OpAtomResource` | `/op/market/atom` | 运维插件管理 |
| `UserTemplateResource` | `/user/market/template` | 模板管理 |
| `UserMarketImageResource` | `/user/market/image` | 镜像管理 |
| `UserStoreMemberResource` | `/user/store/member` | 成员管理 |

### 5.2 Service 层

| 类名 | 职责 |
|------|------|
| `MarketAtomService` | 插件市场核心服务 |
| `AtomReleaseService` | 插件发布流程 |
| `AtomService` | 插件基础操作 |
| `MarketAtomEnvService` | 插件执行环境 |
| `MarketAtomArchiveService` | 插件归档 |
| `AtomCooperationService` | 插件协作 |
| `AtomNotifyService` | 插件通知 |

### 5.3 DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `AtomDao` | 59KB | 插件主表访问（最大） |
| `MarketAtomDao` | 31KB | 市场插件访问 |
| `StoreProjectRelDao` | 25KB | 项目关联访问 |
| `StoreBaseQueryDao` | 20KB | 基础查询 |
| `MarketAtomEnvInfoDao` | 13KB | 插件环境访问 |

## 六、核心流程

### 6.1 插件发布流程

```
开发者提交发布请求
    │
    ▼
UserAtomReleaseResource.createAtom()
    │
    ▼
AtomReleaseService.handleAtomRelease()
    │
    ├─► 参数校验
    │   ├─► 校验插件代码唯一性
    │   ├─► 校验版本号格式
    │   └─► 校验代码库权限
    │
    ├─► 创建插件记录
    │   ├─► atomDao.create()
    │   └─► 状态设为 INIT
    │
    ├─► 触发构建流水线
    │   └─► 调用 Process 模块构建插件包
    │
    ├─► 构建完成回调
    │   ├─► 更新状态为 TESTING
    │   └─► 上传插件包到制品库
    │
    ├─► 提交审核
    │   └─► 状态设为 AUDITING
    │
    └─► 审核通过
        ├─► 状态设为 RELEASED
        └─► 更新 LATEST_FLAG
```

### 6.2 插件安装流程

```
用户安装插件到项目
    │
    ▼
UserMarketAtomResource.installAtom()
    │
    ▼
MarketAtomService.installAtom()
    │
    ├─► 权限校验
    │   └─► 检查用户是否有项目权限
    │
    ├─► 检查插件可见性
    │   └─► 检查项目是否在可见范围内
    │
    ├─► 创建关联记录
    │   └─► storeProjectRelDao.create()
    │
    └─► 更新统计数据
        └─► 增加安装量
```

### 6.3 处理器链模式

Store 模块使用责任链模式处理组件的创建、更新、删除：

```kotlin
// 创建处理器链
class StoreCreateHandlerChain {
    private val handlers = listOf(
        StoreCreateParamCheckHandler,    // 参数校验
        StoreCreatePreBusHandler,        // 前置业务处理
        StoreCreateDataPersistHandler,   // 数据持久化
        StoreCreatePostBusHandler        // 后置业务处理
    )
    
    fun handle(context: StoreContext) {
        handlers.forEach { it.handle(context) }
    }
}
```

## 七、与其他模块的关系

### 7.1 依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    Store 模块依赖关系                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                  ┌───────────────┐                               │
│                  │     store     │                               │
│                  └───────┬───────┘                               │
│                          │                                       │
│       ┌──────────────────┼──────────────────┐                   │
│       ▼                  ▼                  ▼                   │
│  ┌───────────┐    ┌───────────┐    ┌───────────┐               │
│  │  project  │    │repository │    │artifactory│               │
│  │ (项目信息) │    │ (代码库)   │    │ (制品库)   │               │
│  └───────────┘    └───────────┘    └───────────┘               │
│                                                                  │
│  被依赖：                                                        │
│  - process（流水线使用插件）                                      │
│  - worker（构建机执行插件）                                       │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 服务间调用示例

```kotlin
// Process 模块获取插件信息
// 注意：projectCode 是 T_PROJECT.english_name
client.get(ServiceAtomResource::class).getAtomByCode(
    atomCode = atomCode,
    username = userId
)

// 获取插件执行环境
client.get(ServiceMarketAtomEnvResource::class).getAtomEnv(
    projectCode = projectId,  // english_name
    atomCode = atomCode,
    atomVersion = version
)

// 获取项目可用的插件列表
client.get(ServiceMarketAtomResource::class).getProjectElements(
    projectCode = projectId
)
```

## 八、插件开发规范

### 8.1 插件目录结构

```
my-atom/
├── task.json           # 插件配置文件
├── README.md           # 插件说明
├── src/                # 源代码
│   └── main.py         # 入口文件
├── requirements.txt    # Python 依赖
└── logo.png            # 插件图标
```

### 8.2 task.json 配置

```json
{
  "atomCode": "myAtom",
  "execution": {
    "language": "python",
    "packagePath": "src/",
    "target": "main.py"
  },
  "input": {
    "param1": {
      "label": "参数1",
      "type": "vuex-input",
      "required": true
    }
  },
  "output": {
    "output1": {
      "type": "string",
      "description": "输出参数"
    }
  }
}
```

### 8.3 插件开发语言支持

| 语言 | 说明 |
|------|------|
| Python | 推荐，有完善的 SDK |
| NodeJS | 支持 |
| Java | 支持 |
| Golang | 支持 |

## 九、开发规范

### 9.1 新增组件类型

1. 在 `StoreTypeEnum` 添加新类型
2. 创建对应的主表和关联表
3. 创建 DAO、Service、Resource 层代码
4. 在处理器链中注册新类型的处理器

### 9.2 插件查询示例

```kotlin
// 根据插件代码查询最新版本
val atom = atomDao.getLatestAtomByCode(
    dslContext = dslContext,
    atomCode = atomCode
)

// 查询项目可用的插件
val atoms = atomDao.getProjectAtoms(
    dslContext = dslContext,
    projectCode = projectId,  // english_name
    classifyCode = classifyCode
)

// 查询插件执行环境
val envInfo = marketAtomEnvInfoDao.getMarketAtomEnvInfo(
    dslContext = dslContext,
    atomId = atomId
)
```

## 十、常见问题

**Q: atomCode 和 atomId 的区别？**
A: `atomCode` 是插件唯一标识（不变），`atomId` 是具体版本的 ID（每个版本不同）。

**Q: 如何判断插件是否可用？**
A: 检查 `ATOM_STATUS = 7`（RELEASED）且 `LATEST_FLAG = true`。

**Q: 插件如何关联到项目？**
A: 通过 `T_STORE_PROJECT_REL` 表关联，`STORE_CODE` 存储 `atomCode`。

**Q: 如何获取插件的执行环境？**
A: 查询 `T_ATOM_ENV_INFO` 表，根据 `ATOM_ID` 获取 `PKG_PATH`、`TARGET` 等信息。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
