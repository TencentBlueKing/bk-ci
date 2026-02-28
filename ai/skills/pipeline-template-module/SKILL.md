---
name: pipeline-template-module
description: 流水线模板模块架构指南，涵盖模板创建、版本管理、实例化、权限控制、研发商店集成。当用户需要开发模板功能、实现模板复用、管理模板版本或进行批量实例化时使用。
---

# 流水线模板模块架构指南

## 概述

流水线模板（Pipeline Template）是 BK-CI 的核心功能模块之一，允许用户将流水线配置抽象为可复用的模板，支持模板的创建、版本管理、实例化、权限控制以及与研发商店的集成。

### 核心价值

- **配置复用**：将通用的流水线配置抽象为模板，避免重复配置
- **标准化管理**：通过模板统一管理流水线标准，确保一致性
- **版本控制**：支持模板版本管理，可追溯历史变更
- **批量操作**：支持批量实例化和更新，提高效率
- **商店集成**：支持模板发布到研发商店，实现跨项目共享

### 系统架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           流水线模板模块                                   │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│  │   API 接口层     │  │   业务服务层     │  │   数据访问层     │         │
│  │  (V1 + V2)      │  │  (Service)      │  │    (DAO)        │         │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘         │
│           │                    │                    │                   │
│           ▼                    ▼                    ▼                   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                        数据库表                                   │   │
│  │  T_TEMPLATE | T_TEMPLATE_PIPELINE | T_PIPELINE_TEMPLATE_INFO    │   │
│  │  T_PIPELINE_TEMPLATE_RESOURCE_VERSION | T_TEMPLATE_INSTANCE_*   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│  │   权限控制       │  │   研发商店集成   │  │   事件驱动       │         │
│  │  (RBAC)         │  │   (Store)       │  │   (MQ)          │         │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 一、版本架构

系统存在两个版本的实现，V2 版本是新架构，功能更完善：

| 版本 | 特点 | 适用场景 |
|------|------|----------|
| **V1** | 原始实现，功能基础 | 兼容老版本，简单场景 |
| **V2** | 新架构，支持草稿、PAC、完善的版本管理 | 新功能开发，复杂场景 |

### 模板类型

```kotlin
// 文件: process/api-process/.../pojo/template/TemplateType.kt
enum class TemplateType(val value: String) {
    CUSTOMIZE("customize"),  // 自定义模板 - 项目内创建
    CONSTRAINT("constraint"), // 约束模板 - 来自研发商店
    PUBLIC("public")         // 公共模板 - 系统级公共模板
}
```

---

## 二、目录结构

### 2.1 API 接口层

```
src/backend/ci/core/process/api-process/src/main/kotlin/com/tencent/devops/process/
├── api/template/                          # V1 版本 API
│   ├── UserPTemplateResource.kt           # 用户模板接口
│   ├── UserTemplateInstanceResource.kt    # 模板实例化接口
│   ├── UserPipelineTemplateResource.kt    # 流水线模板接口
│   ├── UserTemplateAtomResource.kt        # 模板插件接口
│   ├── ServicePTemplateResource.kt        # 服务间调用接口
│   ├── ServiceTemplateInstanceResource.kt # 服务间实例接口
│   └── v2/                                # V2 版本 API
│       ├── UserPipelineTemplateV2Resource.kt         # V2 用户模板接口
│       ├── UserPipelineTemplateInstanceV2Resource.kt # V2 实例化接口
│       ├── ServicePipelineTemplateV2Resource.kt      # V2 服务间接口
│       ├── OpPipelineTemplateResource.kt             # V2 运营管理接口
│       └── UserTemplateAtomV2Resource.kt             # V2 插件检查接口
├── api/op/
│   └── OpPipelineTemplateResource.kt      # 运营管理模板接口
├── api/builds/
│   └── BuildTemplateAcrossResource.kt     # 跨项目模板访问
└── api/service/
    └── ServiceTemplateAcrossResource.kt   # 服务间跨项目模板
```

### 2.2 POJO 数据模型

```
src/backend/ci/core/process/api-process/src/main/kotlin/com/tencent/devops/process/pojo/template/
├── TemplateType.kt                # 模板类型枚举
├── TemplateModel.kt               # 模板模型
├── TemplateModelDetail.kt         # 模板模型详情
├── TemplateVersion.kt             # 模板版本信息
├── TemplateId.kt                  # 模板ID
├── TemplateInstanceCreate.kt      # 实例创建请求
├── TemplateInstanceUpdate.kt      # 实例更新请求
├── TemplateInstanceParams.kt      # 实例参数
├── TemplatePipeline.kt            # 模板流水线
├── TemplateWithPermission.kt      # 带权限的模板
├── TemplateCompareModel.kt        # 模板对比模型
├── CopyTemplateReq.kt             # 复制模板请求
├── SaveAsTemplateReq.kt           # 另存为模板请求
├── MarketTemplateRequest.kt       # 商店模板请求
└── v2/                            # V2 版本 POJO
    ├── PipelineTemplateInfoV2.kt           # V2 模板基础信息
    ├── PipelineTemplateResource.kt         # V2 模板资源(版本)
    ├── PipelineTemplateDetailsResponse.kt  # V2 模板详情响应
    ├── PipelineTemplateVersionInfo.kt      # V2 版本信息
    ├── PipelineTemplateCustomCreateReq.kt  # V2 自定义创建请求
    ├── PipelineTemplateCopyCreateReq.kt    # V2 复制创建请求
    ├── PipelineTemplateMarketCreateReq.kt  # V2 商店导入请求
    ├── PipelineTemplateDraftSaveReq.kt     # V2 草稿保存请求
    ├── PipelineTemplateDraftReleaseReq.kt  # V2 草稿发布请求
    ├── PipelineTemplateInstancesRequest.kt # V2 批量实例化请求
    ├── PipelineTemplateCompareResponse.kt  # V2 版本对比响应
    └── TemplateInstanceType.kt             # 实例类型枚举
```

### 2.3 业务服务层

```
src/backend/ci/core/process/biz-process/src/main/kotlin/com/tencent/devops/process/service/template/
├── TemplateFacadeService.kt       # V1 核心门面服务 (120KB)
├── TemplateCommonService.kt       # 模板通用服务
├── TemplateSettingService.kt      # 模板设置服务
├── TemplateAtomService.kt         # 模板插件服务
├── TemplatePACService.kt          # 模板 PAC 服务
└── v2/                            # V2 版本服务
    ├── PipelineTemplateFacadeService.kt        # V2 核心门面服务 (71KB)
    ├── PipelineTemplateInstanceService.kt      # V2 实例化服务 (41KB)
    ├── PipelineTemplatePersistenceService.kt   # V2 持久化服务 (38KB)
    ├── PipelineTemplateMigrateService.kt       # V2 迁移服务 (38KB)
    ├── PipelineTemplateGenerator.kt            # 模板生成器
    ├── PipelineTemplateMarketFacadeService.kt  # 商店模板服务
    ├── PipelineTemplateInfoService.kt          # 模板信息服务
    ├── PipelineTemplateResourceService.kt      # 模板资源服务
    ├── PipelineTemplateRelatedService.kt       # 模板关联服务
    ├── PipelineTemplateSettingService.kt       # V2 模板设置服务
    ├── PipelineTemplateCommonService.kt        # V2 通用服务
    ├── PipelineTemplateAtomService.kt          # V2 插件服务
    ├── PipelineTemplateVersionValidator.kt     # 版本校验器
    ├── PipelineTemplateModelInitializer.kt     # 模型初始化器
    ├── PipelineTemplateInstanceListener.kt     # 实例化事件监听器
    └── version/                                # 版本管理
        ├── PipelineTemplateVersionManager.kt   # 版本管理器
        ├── PipelineTemplateVersionCreateContext.kt  # 版本创建上下文
        ├── PipelineTemplateVersionDeleteContext.kt  # 版本删除上下文
        ├── convert/                            # 请求转换器
        │   ├── PipelineTemplateCustomCreateReqConverter.kt
        │   ├── PipelineTemplateCopyCreateReqConverter.kt
        │   ├── PipelineTemplateMarketCreateReqConverter.kt
        │   ├── PipelineTemplateDraftSaveReqConverter.kt
        │   ├── PipelineTemplateDraftReleaseReqConverter.kt
        │   └── PipelineTemplateDraftRollbackReqConverter.kt
        ├── hander/                             # 版本处理器
        │   ├── PipelineTemplateVersionCreateHandler.kt
        │   ├── PipelineTemplateDraftSaveHandler.kt
        │   ├── PipelineTemplateDraftReleaseHandler.kt
        │   ├── PipelineTemplateReleaseCreateHandler.kt
        │   ├── PipelineTemplateBranchCreateHandler.kt
        │   └── PipelineTemplateVersionDeleteHandler.kt
        └── processor/                          # 版本后处理器
            ├── PTemplateVersionCreatePostProcessor.kt
            ├── PTemplateVersionDeletePostProcessor.kt
            ├── PTemplateCompatibilityVersionPostProcessor.kt
            ├── PTemplateMarketInstallVersionPostProcessor.kt
            └── PTemplateOperationLogVersionPostProcessor.kt
```

### 2.4 数据访问层

```
src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/
├── engine/dao/template/           # V1 版本 DAO
│   ├── TemplateDao.kt             # 核心模板 DAO (25KB)
│   └── TemplatePipelineDao.kt     # 模板流水线 DAO (16KB)
└── dao/template/                  # V2 版本 DAO
    ├── PipelineTemplateInfoDao.kt      # V2 模板信息 DAO (16KB)
    ├── PipelineTemplateResourceDao.kt  # V2 模板资源 DAO (23KB)
    ├── PipelineTemplateSettingDao.kt   # V2 模板设置 DAO (13KB)
    ├── PipelineTemplateRelatedDao.kt   # V2 模板关联 DAO (15KB)
    └── PipelineTemplateMigrationDao.kt # V2 迁移 DAO
```

### 2.5 权限控制

```
src/backend/ci/core/process/biz-process/src/main/kotlin/com/tencent/devops/process/permission/template/
├── PipelineTemplatePermissionService.kt          # 权限服务接口
├── AbstractPipelineTemplatePermissionService.kt  # 抽象实现
├── RbacPipelineTemplatePermissionService.kt      # RBAC 权限实现
├── MockPipelineTemplatePermissionService.kt      # Mock 实现
└── config/
    └── PipelineTemplatePermConfiguration.kt      # 权限配置
```

---

## 三、数据库表结构

### 3.1 V1 版本表

#### T_TEMPLATE - 模板信息表

```sql
CREATE TABLE IF NOT EXISTS `T_TEMPLATE` (
    `VERSION` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '版本号',
    `ID` varchar(32) NOT NULL COMMENT '模板ID',
    `TEMPLATE_NAME` varchar(64) NOT NULL COMMENT '模板名称',
    `PROJECT_ID` varchar(34) NOT NULL COMMENT '项目ID',
    `VERSION_NAME` varchar(64) NOT NULL COMMENT '版本名称',
    `CREATOR` varchar(64) NOT NULL COMMENT '创建者',
    `CREATED_TIME` datetime(3) DEFAULT NULL COMMENT '创建时间',
    `TEMPLATE` mediumtext COMMENT '模板内容(JSON)',
    `TYPE` varchar(32) NOT NULL DEFAULT 'CUSTOMIZE' COMMENT '模板类型',
    `CATEGORY` varchar(128) DEFAULT NULL COMMENT '分类',
    `LOGO_URL` varchar(512) DEFAULT NULL COMMENT 'Logo地址',
    `SRC_TEMPLATE_ID` varchar(32) DEFAULT NULL COMMENT '源模板ID',
    `STORE_FLAG` bit(1) DEFAULT b'0' COMMENT '是否来自商店',
    `WEIGHT` int(11) DEFAULT '0' COMMENT '权重',
    `LATEST_FLAG` bit(1) DEFAULT b'0' COMMENT '是否最新版本',
    `DRAFT_FLAG` bit(1) DEFAULT b'0' COMMENT '是否草稿',
    PRIMARY KEY (`VERSION`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`),
    KEY `IDX_ID` (`ID`),
    KEY `IDX_SRC_TEMPLATE_ID` (`SRC_TEMPLATE_ID`)
);
```

#### T_TEMPLATE_PIPELINE - 模板流水线关联表

```sql
CREATE TABLE IF NOT EXISTS `T_TEMPLATE_PIPELINE` (
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `INSTANCE_TYPE` varchar(32) NOT NULL DEFAULT 'CONSTRAINT' COMMENT '实例类型',
    `ROOT_TEMPLATE_ID` varchar(32) DEFAULT NULL COMMENT '根模板ID',
    `VERSION` bigint(20) NOT NULL COMMENT '模板版本',
    `VERSION_NAME` varchar(64) NOT NULL COMMENT '版本名称',
    `TEMPLATE_ID` varchar(32) NOT NULL COMMENT '模板ID',
    `CREATOR` varchar(64) NOT NULL COMMENT '创建者',
    `UPDATOR` varchar(64) NOT NULL COMMENT '更新者',
    `CREATED_TIME` datetime NOT NULL COMMENT '创建时间',
    `UPDATED_TIME` datetime NOT NULL COMMENT '更新时间',
    `BUILD_NO` text COMMENT '构建号信息',
    `PARAM` mediumtext COMMENT '参数',
    `DELETED` bit(1) DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`PIPELINE_ID`),
    KEY `IDX_TEMPLATE_ID` (`TEMPLATE_ID`),
    KEY `IDX_ROOT_TEMPLATE_ID` (`ROOT_TEMPLATE_ID`)
);
```

### 3.2 V2 版本表

#### T_PIPELINE_TEMPLATE_INFO - 模板基础信息表

```sql
CREATE TABLE IF NOT EXISTS `T_PIPELINE_TEMPLATE_INFO` (
    `TEMPLATE_ID` varchar(34) NOT NULL COMMENT '模板ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `TEMPLATE_NAME` varchar(255) NOT NULL COMMENT '模板名称',
    `TEMPLATE_TYPE` varchar(32) NOT NULL DEFAULT 'CUSTOMIZE' COMMENT '模板类型',
    `CREATOR` varchar(64) NOT NULL COMMENT '创建者',
    `CREATE_TIME` datetime(3) NOT NULL COMMENT '创建时间',
    `LATEST_VERSION` bigint(20) DEFAULT NULL COMMENT '最新版本号',
    `RELEASE_VERSION` bigint(20) DEFAULT NULL COMMENT '发布版本号',
    `DRAFT_VERSION` bigint(20) DEFAULT NULL COMMENT '草稿版本号',
    `DELETE` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`TEMPLATE_ID`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`)
);
```

#### T_PIPELINE_TEMPLATE_RESOURCE_VERSION - 模板资源版本表

```sql
CREATE TABLE IF NOT EXISTS `T_PIPELINE_TEMPLATE_RESOURCE_VERSION` (
    `TEMPLATE_ID` varchar(34) NOT NULL COMMENT '模板ID',
    `VERSION` bigint(20) NOT NULL COMMENT '版本号',
    `VERSION_NAME` varchar(64) NOT NULL COMMENT '版本名称',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `CREATOR` varchar(64) NOT NULL COMMENT '创建者',
    `CREATE_TIME` datetime(3) NOT NULL COMMENT '创建时间',
    `MODEL` mediumtext COMMENT '模型内容(JSON)',
    `STATUS` varchar(32) DEFAULT NULL COMMENT '版本状态',
    `BRANCH_ACTION` varchar(32) DEFAULT NULL COMMENT '分支操作',
    `DESCRIPTION` varchar(1024) DEFAULT NULL COMMENT '版本描述',
    `YAML` mediumtext COMMENT 'YAML内容',
    `YAML_VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML版本',
    `BASE_VERSION` bigint(20) DEFAULT NULL COMMENT '基础版本',
    `DEBUG_BUILD_ID` varchar(64) DEFAULT NULL COMMENT '调试构建ID',
    `REFER_FLAG` bit(1) DEFAULT NULL COMMENT '引用标志',
    `REFER_TEMPLATE_ID` varchar(34) DEFAULT NULL COMMENT '引用模板ID',
    PRIMARY KEY (`TEMPLATE_ID`, `VERSION`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`)
);
```

#### T_TEMPLATE_INSTANCE_BASE - 实例化基础表

```sql
CREATE TABLE IF NOT EXISTS `T_TEMPLATE_INSTANCE_BASE` (
    `ID` varchar(32) NOT NULL COMMENT '主键ID',
    `TEMPLATE_ID` varchar(32) DEFAULT NULL COMMENT '模板ID',
    `TEMPLATE_VERSION` bigint(20) DEFAULT NULL COMMENT '模板版本',
    `USE_TEMPLATE_SETTINGS_FLAG` bit(1) DEFAULT b'1' COMMENT '使用模板设置',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `TOTAL_ITEM_NUM` int(11) NOT NULL COMMENT '总实例数',
    `SUCCESS_ITEM_NUM` int(11) NOT NULL DEFAULT '0' COMMENT '成功实例数',
    `FAIL_ITEM_NUM` int(11) NOT NULL DEFAULT '0' COMMENT '失败实例数',
    `STATUS` varchar(32) NOT NULL COMMENT '状态',
    `CREATOR` varchar(50) NOT NULL COMMENT '创建者',
    `MODIFIER` varchar(50) NOT NULL COMMENT '修改者',
    `UPDATE_TIME` datetime(3) NOT NULL COMMENT '更新时间',
    `CREATE_TIME` datetime(3) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    KEY `IDX_TEMPLATE_ID` (`TEMPLATE_ID`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`)
);
```

#### T_TEMPLATE_INSTANCE_ITEM - 实例化项表

```sql
CREATE TABLE IF NOT EXISTS `T_TEMPLATE_INSTANCE_ITEM` (
    `ID` varchar(32) NOT NULL COMMENT '主键ID',
    `BASE_ID` varchar(32) NOT NULL COMMENT '基础ID',
    `PIPELINE_ID` varchar(34) DEFAULT NULL COMMENT '流水线ID',
    `PIPELINE_NAME` varchar(255) NOT NULL COMMENT '流水线名称',
    `BUILD_NO_INFO` varchar(512) DEFAULT NULL COMMENT '构建号信息',
    `STATUS` varchar(32) NOT NULL COMMENT '状态',
    `PARAM` mediumtext COMMENT '参数',
    `CREATE_TIME` datetime(3) NOT NULL COMMENT '创建时间',
    `UPDATE_TIME` datetime(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `IDX_BASE_ID` (`BASE_ID`),
    KEY `IDX_PIPELINE_ID` (`PIPELINE_ID`)
);
```

---

## 四、核心功能实现

### 4.1 模板创建

#### V1 版本创建流程

```kotlin
// 文件: process/biz-process/.../service/template/TemplateFacadeService.kt

@Service
class TemplateFacadeService {
    
    fun createTemplate(
        projectId: String,
        userId: String,
        template: Model,
        templateType: TemplateType = TemplateType.CUSTOMIZE
    ): String {
        // 1. 权限校验
        pipelineTemplatePermissionService.checkPipelineTemplatePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        
        // 2. 模板名称校验
        if (templateDao.countByName(dslContext, projectId, template.name) > 0) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_TEMPLATE_NAME_IS_EXISTS)
        }
        
        // 3. 生成模板ID
        val templateId = UUIDUtil.generate()
        
        // 4. 保存模板
        templateDao.create(
            dslContext = dslContext,
            templateId = templateId,
            projectId = projectId,
            templateName = template.name,
            versionName = INIT_TEMPLATE_VERSION_NAME,
            userId = userId,
            template = JsonUtil.toJson(template),
            type = templateType.name
        )
        
        // 5. 注册权限资源
        pipelineTemplatePermissionService.createResource(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            templateName = template.name
        )
        
        return templateId
    }
}
```

#### V2 版本创建流程

```kotlin
// 文件: process/biz-process/.../service/template/v2/PipelineTemplateFacadeService.kt

@Service
class PipelineTemplateFacadeService {
    
    fun create(
        userId: String,
        projectId: String,
        request: PipelineTemplateCustomCreateReq
    ): PipelineTemplateCreateResp {
        // 1. 权限校验
        pipelineTemplatePermissionService.checkPipelineTemplatePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        
        // 2. 调用版本管理器
        val result = pipelineTemplateVersionManager.deployTemplate(
            userId = userId,
            projectId = projectId,
            request = request
        )
        
        // 3. 注册权限资源
        pipelineTemplatePermissionService.createResource(
            userId = userId,
            projectId = projectId,
            templateId = result.templateId,
            templateName = request.name
        )
        
        return PipelineTemplateCreateResp(
            templateId = result.templateId,
            version = result.version
        )
    }
}
```

### 4.2 模板实例化

模板实例化是将模板转换为具体流水线的过程：

```kotlin
// 文件: process/biz-process/.../service/template/v2/PipelineTemplateInstanceService.kt

@Service
class PipelineTemplateInstanceService {
    
    /**
     * 批量实例化模板
     */
    fun createTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        request: PipelineTemplateInstancesRequest
    ): List<PipelineTemplateInstanceItem> {
        // 1. 校验创建流水线权限
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        
        // 2. 获取模板详情
        val templateDetail = pipelineTemplateFacadeService.getTemplateDetail(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        
        // 3. 遍历创建流水线实例
        return request.instances.map { instance ->
            createInstance(
                userId = userId,
                projectId = projectId,
                templateDetail = templateDetail,
                instance = instance
            )
        }
    }
    
    private fun createInstance(
        userId: String,
        projectId: String,
        templateDetail: PipelineTemplateDetailsResponse,
        instance: PipelineTemplateInstanceItem
    ): PipelineTemplateInstanceItem {
        // 1. 合并模板参数和实例参数
        val model = mergeTemplateAndInstanceParams(
            templateModel = templateDetail.model,
            instanceParams = instance.params
        )
        
        // 2. 创建流水线
        val pipelineId = pipelineInfoFacadeService.create(
            userId = userId,
            projectId = projectId,
            model = model,
            channelCode = ChannelCode.BS
        )
        
        // 3. 建立模板-流水线关联
        templatePipelineDao.create(
            dslContext = dslContext,
            pipelineId = pipelineId,
            templateId = templateDetail.templateId,
            version = templateDetail.version,
            versionName = templateDetail.versionName,
            userId = userId
        )
        
        return instance.copy(pipelineId = pipelineId)
    }
}
```

### 4.3 版本管理

V2 版本采用策略模式实现版本管理：

```kotlin
// 文件: process/biz-process/.../service/template/v2/version/PipelineTemplateVersionManager.kt

@Service
class PipelineTemplateVersionManager(
    private val versionReqConverters: List<PipelineTemplateVersionReqConverter>,
    private val versionCreateHandlers: List<PipelineTemplateVersionCreateHandler>,
    private val pipelineTemplateVersionValidator: PipelineTemplateVersionValidator,
    private val versionDeleteHandler: PipelineTemplateVersionDeleteHandler
) {
    
    /**
     * 部署模板（创建/更新版本）
     */
    fun deployTemplate(
        userId: String,
        projectId: String,
        templateId: String? = null,
        version: Long? = null,
        request: PipelineTemplateVersionReq
    ): DeployTemplateResult {
        // 1. 转换请求为上下文
        val context = getConverter(request).convert(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version,
            request = request
        )
        
        // 2. 校验版本
        pipelineTemplateVersionValidator.validate(context = context)
        
        // 3. 调用对应处理器
        return getHandler(context).handle(context = context)
    }
    
    /**
     * 删除版本
     */
    fun deleteVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?,
        versionName: String? = null
    ) {
        val context = PipelineTemplateVersionDeleteContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version,
            versionAction = PipelineVersionAction.DELETE_VERSION
        )
        versionDeleteHandler.handle(context = context)
    }
    
    private fun getHandler(context: PipelineTemplateVersionCreateContext): PipelineTemplateVersionCreateHandler {
        return versionCreateHandlers.find { it.support(context) }
            ?: throw IllegalArgumentException("Unsupported version event")
    }
    
    private fun getConverter(request: PipelineTemplateVersionReq): PipelineTemplateVersionReqConverter {
        return versionReqConverters.find { it.support(request) }
            ?: throw IllegalArgumentException("Unsupported version request")
    }
}
```

#### 版本处理器类型

| 处理器 | 功能 | 触发场景 |
|--------|------|----------|
| `PipelineTemplateDraftSaveHandler` | 保存草稿 | 编辑模板后保存草稿 |
| `PipelineTemplateDraftReleaseHandler` | 发布草稿 | 将草稿发布为正式版本 |
| `PipelineTemplateReleaseCreateHandler` | 创建发布版本 | 直接创建正式版本 |
| `PipelineTemplateBranchCreateHandler` | 创建分支版本 | PAC 场景下的分支版本 |
| `PipelineTemplateVersionDeleteHandler` | 删除版本 | 删除指定版本 |

### 4.4 权限控制

```kotlin
// 文件: process/biz-process/.../permission/template/PipelineTemplatePermissionService.kt

interface PipelineTemplatePermissionService {
    
    /**
     * 校验模板权限
     */
    fun checkPipelineTemplatePermission(
        userId: String,
        projectId: String,
        templateId: String? = null,
        permission: AuthPermission,
        message: String? = null
    )
    
    /**
     * 创建权限资源
     */
    fun createResource(
        userId: String,
        projectId: String,
        templateId: String,
        templateName: String
    )
    
    /**
     * 删除权限资源
     */
    fun deleteResource(
        projectId: String,
        templateId: String
    )
    
    /**
     * 获取有权限的模板列表
     */
    fun filterTemplates(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        templateIds: List<String>
    ): List<String>
    
    /**
     * 是否启用模板权限管理
     */
    fun enableTemplatePermissionManage(projectId: String): Boolean
}
```

#### RBAC 权限实现

```kotlin
// 文件: process/biz-process/.../permission/template/RbacPipelineTemplatePermissionService.kt

@Service
class RbacPipelineTemplatePermissionService(
    private val client: Client,
    private val authPermissionApi: AuthPermissionApi,
    private val authResourceApi: AuthResourceApi
) : AbstractPipelineTemplatePermissionService() {
    
    override fun checkPipelineTemplatePermission(
        userId: String,
        projectId: String,
        templateId: String?,
        permission: AuthPermission,
        message: String?
    ) {
        // 调用权限中心校验
        val hasPermission = authPermissionApi.validateUserResourcePermission(
            userId = userId,
            serviceCode = AuthServiceCode.PIPELINE,
            resourceType = AuthResourceType.PIPELINE_TEMPLATE,
            projectCode = projectId,
            resourceCode = templateId ?: "*",
            permission = permission
        )
        
        if (!hasPermission) {
            throw PermissionForbiddenException(message ?: "无模板操作权限")
        }
    }
    
    override fun createResource(
        userId: String,
        projectId: String,
        templateId: String,
        templateName: String
    ) {
        authResourceApi.createResource(
            userId = userId,
            serviceCode = AuthServiceCode.PIPELINE,
            resourceType = AuthResourceType.PIPELINE_TEMPLATE,
            projectCode = projectId,
            resourceCode = templateId,
            resourceName = templateName
        )
    }
}
```

---

## 五、PAC (Pipeline as Code) 模板

### 5.1 PAC 概述

PAC（Pipeline as Code）是 BK-CI 的核心特性，允许用户通过 YAML 文件定义和管理流水线模板。模板 PAC 支持：

- **YAML 定义模板**：使用 YAML 格式定义模板配置
- **Git 仓库托管**：模板 YAML 文件存储在 Git 仓库中
- **Webhook 自动同步**：代码推送自动触发模板更新
- **分支版本管理**：支持基于 Git 分支的版本管理
- **双向转换**：Model ↔ YAML 双向转换

### 5.2 模板类型（PAC 视角）

```kotlin
// 文件: common/common-pipeline/.../template/PipelineTemplateType.kt
enum class PipelineTemplateType(val value: String) {
    PIPELINE("pipeline"),  // 完整流水线模板
    STAGE("stage"),        // Stage 级别模板
    JOB("job"),            // Job 级别模板
    STEP("step"),          // Step 级别模板
    VARIABLE("variable")   // 变量模板
}
```

### 5.3 PAC 核心服务

#### 5.3.1 TemplatePACService（V1 PAC 服务）

```kotlin
// 文件: process/biz-process/.../service/template/TemplatePACService.kt

@Service
class TemplatePACService(
    private val templateDao: TemplateDao,
    private val transferYamlService: PipelineTransferYamlService,
    private val templateCommonService: TemplateCommonService
) {
    /**
     * 预览模板（返回 Model + YAML + 高亮标记）
     */
    fun previewTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        highlightType: HighlightType?
    ): TemplatePreviewDetail {
        // 1. 获取模板
        val template = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        val model: Model = objectMapper.readValue(template.template)
        
        // 2. 获取设置
        val setting = pipelineRepositoryService.getSetting(projectId, templateId)
        
        // 3. Model 转 YAML
        val yaml = transferYamlService.transfer(
            userId = userId,
            projectId = projectId,
            actionType = TransferActionType.FULL_MODEL2YAML,
            data = TransferBody(PipelineModelAndSetting(model, setting))
        ).yamlWithVersion?.yamlStr
        
        // 4. 生成高亮标记（用于 UI 展示）
        val highlightMarkList = buildHighlightMarks(yaml, highlightType)
        
        return TemplatePreviewDetail(
            template = model,
            templateYaml = yaml,
            setting = setting,
            hasPermission = hasPermission,
            highlightMarkList = highlightMarkList
        )
    }
}
```

#### 5.3.2 PTemplateYamlResourceService（YAML 资源服务）

```kotlin
// 文件: process/biz-process/.../yaml/resource/PTemplateYamlResourceService.kt

@Service
class PTemplateYamlResourceService(
    private val pipelineTemplateFacadeService: PipelineTemplateFacadeService,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService
) : IPipelineYamlResourceService {
    
    /**
     * 通过 YAML 创建模板（Git Webhook 触发）
     */
    override fun createYamlPipeline(
        userId: String,
        projectId: String,
        yaml: String,
        event: PipelineYamlFileEvent
    ): DeployPipelineResult {
        with(event) {
            val isDefaultBranch = ref == defaultBranch
            val yamlFileName = GitActionCommon.getCiTemplateName(filePath)
            
            // 调用 V2 服务创建 YAML 模板
            val result = pipelineTemplateFacadeService.createYamlTemplate(
                userId = userId,
                projectId = projectId,
                yaml = yaml,
                yamlFileName = yamlFileName,
                branchName = ref,
                isDefaultBranch = isDefaultBranch,
                description = commit!!.commitMsg,
                yamlFileInfo = PipelineYamlFileInfo(repoHashId, filePath)
            )
            
            return DeployPipelineResult(
                pipelineId = result.templateId,
                pipelineName = result.templateName,
                version = result.version.toInt(),
                versionName = result.versionName
            )
        }
    }
    
    /**
     * 通过 YAML 更新模板
     */
    override fun updateYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: String,
        event: PipelineYamlFileEvent
    ): DeployPipelineResult {
        // 类似创建逻辑，调用 updateYamlTemplate
    }
    
    /**
     * 分支失活（分支删除时调用）
     */
    override fun updateBranchAction(
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String,
        branchVersionAction: BranchVersionAction
    ) {
        pipelineTemplateFacadeService.inactiveBranch(
            userId = userId,
            projectId = projectId,
            templateId = pipelineId,
            branch = branchName
        )
    }
}
```

#### 5.3.3 PipelineYamlResourceManager（统一管理器）

```kotlin
// 文件: process/biz-process/.../yaml/resource/PipelineYamlResourceManager.kt

@Service
class PipelineYamlResourceManager(
    private val pipelineYamlResourceService: PipelineYamlResourceService,
    @Lazy private val pTemplateYamlResourceService: PTemplateYamlResourceService
) {
    /**
     * 根据 isTemplate 标志选择对应的服务
     */
    fun getService(isTemplate: Boolean): IPipelineYamlResourceService {
        return if (isTemplate) {
            pTemplateYamlResourceService  // 模板 YAML 服务
        } else {
            pipelineYamlResourceService   // 流水线 YAML 服务
        }
    }
    
    fun createYamlPipeline(userId: String, projectId: String, yaml: String, event: PipelineYamlFileEvent) =
        getService(event.isTemplate).createYamlPipeline(userId, projectId, yaml, event)
    
    fun updateYamlPipeline(userId: String, projectId: String, pipelineId: String, yaml: String, event: PipelineYamlFileEvent) =
        getService(event.isTemplate).updateYamlPipeline(userId, projectId, pipelineId, yaml, event)
}
```

### 5.4 YAML Webhook 请求处理

#### 5.4.1 PipelineTemplateYamlWebhookReq（请求对象）

```kotlin
// 文件: process/api-process/.../pojo/template/v2/PipelineTemplateYamlWebhookReq.kt

@Schema(title = "模版yaml文件推送请求")
data class PipelineTemplateYamlWebhookReq(
    @get:Schema(title = "模板YAML", required = true)
    val yaml: String,
    
    @get:Schema(title = "yaml文件名", required = true)
    val yamlFileName: String,
    
    @get:Schema(title = "分支名", required = true)
    val branchName: String,
    
    @get:Schema(title = "是否默认分支", required = true)
    val isDefaultBranch: Boolean,
    
    @get:Schema(title = "描述", required = true)
    val description: String? = null,
    
    @get:Schema(title = "yaml文件信息", required = true)
    val yamlFileInfo: PipelineYamlFileInfo? = null
) : PipelineTemplateVersionReq  // 继承版本请求接口
```

#### 5.4.2 PipelineTemplateYamlWebhookReqConverter（请求转换器）

```kotlin
// 文件: process/biz-process/.../version/convert/PipelineTemplateYamlWebhookReqConverter.kt

@Service
class PipelineTemplateYamlWebhookReqConverter(
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService
) : PipelineTemplateVersionReqConverter {
    
    override fun support(request: PipelineTemplateVersionReq) = 
        request is PipelineTemplateYamlWebhookReq
    
    override fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext {
        request as PipelineTemplateYamlWebhookReq
        
        // 1. YAML 转 Model
        val transferResult = pipelineTemplateGenerator.transfer(
            userId = userId,
            projectId = projectId,
            storageType = PipelineStorageType.YAML,
            yaml = request.yaml
        )
        
        // 2. 根据是否默认分支决定版本状态
        val (status, versionAction) = if (request.isDefaultBranch) {
            Pair(VersionStatus.RELEASED, PipelineVersionAction.CREATE_RELEASE)
        } else {
            Pair(VersionStatus.BRANCH, PipelineVersionAction.CREATE_BRANCH)
        }
        
        // 3. 模板名称优先级：setting > model > fileName
        val templateName = transferResult.templateSetting.pipelineName
            .takeIf { it.isNotBlank() }
            ?: (transferResult.templateModel as? Model)?.name?.ifBlank { request.yamlFileName }
            ?: request.yamlFileName
        
        // 4. 构建上下文
        return PipelineTemplateVersionCreateContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId ?: pipelineTemplateGenerator.generateTemplateId(),
            versionAction = versionAction,
            newTemplate = templateId == null,
            enablePac = true,  // 标记为 PAC 模板
            yamlFileInfo = request.yamlFileInfo,
            branchName = request.branchName
        )
    }
}
```

### 5.5 Model ↔ YAML 转换

#### 5.5.1 PipelineTemplateGenerator（模板生成器）

```kotlin
// 文件: process/biz-process/.../service/template/v2/PipelineTemplateGenerator.kt

@Service
class PipelineTemplateGenerator(
    private val transferService: PipelineTransferYamlService
) {
    /**
     * 双向转换：Model ↔ YAML
     */
    fun transfer(
        userId: String,
        projectId: String,
        storageType: PipelineStorageType,
        templateType: PipelineTemplateType?,
        templateModel: ITemplateModel?,
        templateSetting: PipelineSetting?,
        params: List<BuildFormProperty>?,
        yaml: String?,
        fallbackOnError: Boolean = false
    ): PTemplateModelTransferResult {
        return if (storageType == PipelineStorageType.YAML) {
            // YAML → Model
            transferYamlToModel(userId, projectId, templateType, params, yaml)
        } else {
            // Model → YAML
            transferModelToYamlWithFallback(
                userId, projectId, templateType, 
                templateModel, templateSetting, params, fallbackOnError
            )
        }
    }
    
    /**
     * YAML → Model 转换
     */
    private fun transferYamlToModel(...): PTemplateModelTransferResult {
        val transferResult = transferService.transfer(
            userId = userId,
            projectId = projectId,
            actionType = TransferActionType.TEMPLATE_YAML2MODEL_PIPELINE,
            data = TransferBody(oldYaml = yaml)
        )
        return PTemplateModelTransferResult(
            templateType = templateType ?: PipelineTemplateType.PIPELINE,
            templateModel = transferResult.templateModelAndSetting?.templateModel,
            templateSetting = transferResult.templateModelAndSetting?.setting,
            yamlWithVersion = transferResult.yamlWithVersion
        )
    }
    
    /**
     * Model → YAML 转换（带异常兜底）
     */
    private fun transferModelToYamlWithFallback(...): PTemplateModelTransferResult {
        val actionType = getTransferActionType(templateType)
        // TransferActionType 根据模板类型选择：
        // PIPELINE → TEMPLATE_MODEL2YAML_PIPELINE
        // STAGE    → TEMPLATE_MODEL2YAML_STAGE
        // JOB      → TEMPLATE_MODEL2YAML_JOB
        // STEP     → TEMPLATE_MODEL2YAML_STEP
        
        return try {
            val result = transferService.transfer(
                actionType = actionType,
                data = TransferBody(
                    templateModelAndSetting = TemplateModelAndSetting(templateModel, templateSetting)
                )
            )
            PTemplateModelTransferResult(yamlWithVersion = result.yamlWithVersion, ...)
        } catch (ex: Exception) {
            if (fallbackOnError) {
                // 兜底：返回原始 Model，YAML 为空
                PTemplateModelTransferResult(yamlWithVersion = null, ...)
            } else {
                throw ex
            }
        }
    }
}
```

#### 5.5.2 TransferActionType（转换动作类型）

```kotlin
// 转换动作类型枚举
enum class TransferActionType {
    // 模板 YAML → Model
    TEMPLATE_YAML2MODEL_PIPELINE,
    
    // 模板 Model → YAML（按模板类型）
    TEMPLATE_MODEL2YAML_PIPELINE,
    TEMPLATE_MODEL2YAML_STAGE,
    TEMPLATE_MODEL2YAML_JOB,
    TEMPLATE_MODEL2YAML_STEP,
    
    // 完整转换（包含设置）
    FULL_MODEL2YAML,
    FULL_YAML2MODEL
}
```

### 5.6 PAC 版本状态

```kotlin
// 版本状态枚举
enum class VersionStatus {
    COMMITTING,  // 草稿状态
    RELEASED,    // 已发布（默认分支）
    BRANCH       // 分支版本（非默认分支）
}
```

| 状态 | 触发场景 | 说明 |
|------|----------|------|
| `RELEASED` | 推送到默认分支 | 正式发布版本 |
| `BRANCH` | 推送到非默认分支 | 分支版本，可用于测试 |
| `COMMITTING` | UI 保存草稿 | 草稿状态，未发布 |

### 5.7 PAC 版本生成

```kotlin
// 文件: PipelineTemplateGenerator.kt

/**
 * 生成分支版本
 */
fun generateBranchVersion(
    projectId: String,
    templateId: String,
    branchName: String
): PTemplateResourceOnlyVersion {
    val latestResource = pipelineTemplateResourceService.getLatestVersionResource(projectId, templateId)
    
    // 如果已存在同名分支版本，基于分支版本创建
    val branchResource = pipelineTemplateResourceService.getLatestBranchResource(
        projectId, templateId, branchName
    )
    
    return PTemplateResourceOnlyVersion(
        version = generateTemplateVersion(),
        number = latestResource.number + 1,
        versionName = branchName,  // 分支版本名 = 分支名
        baseVersion = branchResource?.version ?: latestResource.version
    )
}

/**
 * 草稿发布时的 PAC 版本生成
 */
fun generateDraftReleaseVersionWithPac(
    projectId: String,
    templateId: String,
    draftResource: PipelineTemplateResource,
    repoHashId: String,
    targetAction: CodeTargetAction?,
    targetBranch: String?
): Pair<VersionStatus, PTemplateResourceOnlyVersion> {
    return when (targetAction) {
        // 直接提交到主分支 → 发布版本
        CodeTargetAction.COMMIT_TO_MASTER -> {
            Pair(VersionStatus.RELEASED, generateReleaseVersion(...))
        }
        
        // 提交到源分支 → 分支版本
        CodeTargetAction.COMMIT_TO_SOURCE_BRANCH -> {
            Pair(VersionStatus.BRANCH, ...)
        }
        
        // 新建分支并提交 → 分支版本
        CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE -> {
            val versionName = "bk-ci-template-$templateId-${draftResource.number}"
            Pair(VersionStatus.BRANCH, ...)
        }
        
        // 提交到指定分支
        CodeTargetAction.COMMIT_TO_BRANCH -> {
            // 如果是默认分支 → 发布版本
            // 否则 → 分支版本
        }
    }
}
```

### 5.8 PAC 目录结构

```
src/backend/ci/core/process/biz-process/src/main/kotlin/com/tencent/devops/process/
├── yaml/
│   ├── resource/
│   │   ├── IPipelineYamlResourceService.kt      # YAML 资源服务接口
│   │   ├── PipelineYamlResourceService.kt       # 流水线 YAML 服务
│   │   ├── PTemplateYamlResourceService.kt      # 模板 YAML 服务
│   │   └── PipelineYamlResourceManager.kt       # 统一管理器
│   ├── mq/
│   │   └── PipelineYamlFileEvent.kt             # YAML 文件事件
│   ├── PipelineYamlFileManager.kt               # YAML 文件管理
│   ├── PipelineYamlFacadeService.kt             # YAML 门面服务
│   └── PipelineYamlSyncService.kt               # YAML 同步服务
├── service/template/
│   ├── TemplatePACService.kt                    # V1 PAC 服务
│   └── v2/
│       ├── PipelineTemplateGenerator.kt         # 模板生成器（含转换）
│       └── version/convert/
│           └── PipelineTemplateYamlWebhookReqConverter.kt  # Webhook 转换器
└── common/common-pipeline-yaml/                 # YAML 解析公共库
    └── src/main/kotlin/.../yaml/
        ├── v2/parsers/template/
        │   ├── TemplateYamlMapper.kt            # YAML 映射
        │   └── TemplateYamlUtil.kt              # YAML 工具
        └── v3/parsers/template/
            ├── TemplateYamlMapper.kt
            └── TemplateYamlUtil.kt
```

### 5.9 PAC 完整流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PAC 模板创建/更新流程                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌──────────────────┐    ┌─────────────────────────┐    │
│  │  Git Push   │───▶│  Webhook Event   │───▶│  PipelineYamlFileEvent  │    │
│  │  (YAML文件) │    │  (代码仓库触发)   │    │  (isTemplate=true)      │    │
│  └─────────────┘    └──────────────────┘    └───────────┬─────────────┘    │
│                                                         │                   │
│                                                         ▼                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    PipelineYamlResourceManager                       │   │
│  │                    (根据 isTemplate 路由)                            │   │
│  └───────────────────────────────┬─────────────────────────────────────┘   │
│                                  │                                         │
│                                  ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    PTemplateYamlResourceService                      │   │
│  │                    (模板 YAML 资源服务)                              │   │
│  └───────────────────────────────┬─────────────────────────────────────┘   │
│                                  │                                         │
│                                  ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │              PipelineTemplateFacadeService.createYamlTemplate()      │   │
│  │              构建 PipelineTemplateYamlWebhookReq                     │   │
│  └───────────────────────────────┬─────────────────────────────────────┘   │
│                                  │                                         │
│                                  ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    PipelineTemplateVersionManager                    │   │
│  │                    deployTemplate()                                  │   │
│  └───────────────────────────────┬─────────────────────────────────────┘   │
│                                  │                                         │
│         ┌────────────────────────┼────────────────────────┐                │
│         ▼                        ▼                        ▼                │
│  ┌─────────────┐    ┌────────────────────┐    ┌───────────────────┐       │
│  │  Converter  │───▶│    Validator       │───▶│     Handler       │       │
│  │ (YAML→Model)│    │  (校验模板)        │    │ (创建/更新版本)   │       │
│  └─────────────┘    └────────────────────┘    └─────────┬─────────┘       │
│                                                         │                  │
│         ┌───────────────────────────────────────────────┘                  │
│         │                                                                  │
│         ▼                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  默认分支？                                                          │   │
│  │  ├── YES → VersionStatus.RELEASED → 正式版本                        │   │
│  │  └── NO  → VersionStatus.BRANCH   → 分支版本                        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.10 PAC 最佳实践

#### 5.10.1 模板 YAML 文件命名

- 文件路径：`.ci/templates/xxx.yml` 或 `.ci/templates/xxx.yaml`
- 模板名称默认从 YAML 中的 `name` 字段获取，若无则使用文件名

#### 5.10.2 分支策略

| 分支类型 | 版本状态 | 使用场景 |
|----------|----------|----------|
| 默认分支（main/master） | `RELEASED` | 正式发布，可被实例化 |
| 特性分支 | `BRANCH` | 开发测试，不影响正式版本 |
| PR 分支 | `BRANCH` | 代码审查，合并后自动发布 |

#### 5.10.3 enablePac 标志

```kotlin
// 模板信息中的 PAC 标志
data class PipelineTemplateInfoV2(
    val id: String,
    val projectId: String,
    val name: String,
    val enablePac: Boolean = false,  // 是否启用 PAC
    // ...
)
```

- `enablePac = true`：模板由 YAML 文件管理，UI 编辑会同步到代码仓库
- `enablePac = false`：传统模板，仅在 BK-CI 内管理

---

## 六、研发商店集成

### 6.1 Store 模块模板服务

```
src/backend/ci/core/store/biz-store/src/main/kotlin/com/tencent/devops/store/template/
├── service/
│   ├── MarketTemplateService.kt           # 商店模板服务接口
│   ├── MarketTemplateServiceImpl.kt       # 商店模板服务实现
│   ├── TemplateReleaseService.kt          # 模板发布服务接口
│   ├── TemplateReleaseServiceImpl.kt      # 模板发布服务实现
│   ├── OpTemplateService.kt               # 运营模板服务接口
│   └── OpTemplateServiceImpl.kt           # 运营模板服务实现
└── dao/
    ├── MarketTemplateDao.kt               # 商店模板 DAO
    ├── TemplateCategoryRelDao.kt          # 模板分类关联 DAO
    └── TemplateLabelRelDao.kt             # 模板标签关联 DAO
```

### 6.2 商店模板数据库表

```sql
-- Store 模块的模板表
CREATE TABLE IF NOT EXISTS `T_TEMPLATE` (
    `ID` varchar(32) NOT NULL COMMENT '主键ID',
    `TEMPLATE_NAME` varchar(200) NOT NULL COMMENT '模板名称',
    `TEMPLATE_CODE` varchar(64) NOT NULL COMMENT '模板代码',
    `CLASSIFY_ID` varchar(32) NOT NULL COMMENT '分类ID',
    `VERSION` varchar(20) NOT NULL COMMENT '版本号',
    `TEMPLATE_TYPE` tinyint(4) NOT NULL DEFAULT '1' COMMENT '模板类型',
    `TEMPLATE_STATUS` tinyint(4) NOT NULL COMMENT '模板状态',
    `TEMPLATE_STATUS_MSG` varchar(1024) DEFAULT NULL COMMENT '状态信息',
    `LOGO_URL` varchar(256) DEFAULT NULL COMMENT 'Logo地址',
    `SUMMARY` varchar(256) DEFAULT NULL COMMENT '简介',
    `DESCRIPTION` text COMMENT '描述',
    `PUBLISHER` varchar(50) NOT NULL COMMENT '发布者',
    `PUB_DESCRIPTION` text COMMENT '发布描述',
    `PUBLIC_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
    `LATEST_FLAG` bit(1) NOT NULL COMMENT '是否最新',
    `CREATOR` varchar(50) NOT NULL COMMENT '创建者',
    `MODIFIER` varchar(50) NOT NULL COMMENT '修改者',
    `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
    `UPDATE_TIME` datetime NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_TEMPLATE_CODE_VERSION` (`TEMPLATE_CODE`, `VERSION`)
);
```

### 6.3 模板安装流程

```kotlin
// 从商店安装模板到项目
fun installTemplateFromStore(
    userId: String,
    projectId: String,
    templateCode: String,
    version: String
): String {
    // 1. 获取商店模板信息
    val storeTemplate = marketTemplateService.getTemplateByCode(templateCode, version)
    
    // 2. 获取模板模型
    val templateModel = storeTemplate.templateModel
    
    // 3. 创建项目模板（类型为 CONSTRAINT）
    val templateId = templateFacadeService.createTemplate(
        projectId = projectId,
        userId = userId,
        template = templateModel,
        templateType = TemplateType.CONSTRAINT
    )
    
    // 4. 记录安装历史
    templateVersionInstallHistoryDao.create(
        projectId = projectId,
        templateId = templateId,
        templateCode = templateCode,
        version = version,
        userId = userId
    )
    
    return templateId
}
```

---

## 七、事件驱动机制

### 7.1 模板相关事件

```kotlin
// 文件: process/biz-base/.../engine/pojo/event/

// 模板实例化事件
data class PipelineTemplateInstanceEvent(
    val source: String,
    val projectId: String,
    val templateId: String,
    val version: Long,
    val instanceBaseId: String,
    val userId: String
) : IEvent

// 模板迁移事件
data class PipelineTemplateMigrateEvent(
    val source: String,
    val projectId: String,
    val templateId: String,
    val userId: String
) : IEvent

// 模板触发器升级事件
data class PipelineTemplateTriggerUpgradesEvent(
    val source: String,
    val projectId: String,
    val templateId: String,
    val version: Long,
    val userId: String
) : IEvent
```

### 7.2 事件监听器

```kotlin
// 文件: process/biz-process/.../service/template/v2/PipelineTemplateInstanceListener.kt

@Service
class PipelineTemplateInstanceListener(
    private val pipelineTemplateInstanceService: PipelineTemplateInstanceService
) {
    
    @StreamListener(PipelineTemplateInstanceEvent.TOPIC)
    fun onTemplateInstance(event: PipelineTemplateInstanceEvent) {
        // 处理异步批量实例化
        pipelineTemplateInstanceService.processAsyncInstance(
            projectId = event.projectId,
            templateId = event.templateId,
            version = event.version,
            instanceBaseId = event.instanceBaseId,
            userId = event.userId
        )
    }
}
```

---

## 八、分布式锁

### 8.1 模板相关锁

```kotlin
// 文件: process/biz-base/.../engine/control/lock/

// 实例计数锁 - 防止并发实例化时计数错误
class PipelineTemplateInstanceCountLock(
    private val redisOperation: RedisOperation,
    private val templateId: String
) : BaseLock(redisOperation, "template:instance:count:$templateId")

// 触发器升级锁 - 防止并发升级触发器
class PipelineTemplateTriggerUpgradesLock(
    private val redisOperation: RedisOperation,
    private val templateId: String
) : BaseLock(redisOperation, "template:trigger:upgrade:$templateId")
```

```kotlin
// 文件: process/biz-process/.../service/template/v2/

// V2 实例化锁
class PipelineTemplateInstanceLock(
    private val redisOperation: RedisOperation,
    private val templateId: String,
    private val pipelineId: String
) : BaseLock(redisOperation, "template:v2:instance:$templateId:$pipelineId")

// V2 模型锁
class PipelineTemplateModelLock(
    private val redisOperation: RedisOperation,
    private val templateId: String
) : BaseLock(redisOperation, "template:v2:model:$templateId")
```

---

## 九、OpenAPI 接口

### 9.1 API 网关接口

```
src/backend/ci/core/openapi/api-openapi/src/main/kotlin/com/tencent/devops/openapi/api/apigw/
├── v3/
│   ├── ApigwTemplateResourceV3.kt          # V3 模板接口
│   ├── ApigwTemplateInstanceResourceV3.kt  # V3 实例化接口
│   └── ApigwMarketTemplateResourceV3.kt    # V3 商店模板接口
└── v4/
    ├── ApigwTemplateResourceV4.kt          # V4 模板接口
    ├── ApigwTemplateInstanceResourceV4.kt  # V4 实例化接口
    └── ApigwMarketTemplateResourceV4.kt    # V4 商店模板接口
```

### 9.2 主要 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v4/projects/{projectId}/templates` | 创建模板 |
| GET | `/v4/projects/{projectId}/templates` | 获取模板列表 |
| GET | `/v4/projects/{projectId}/templates/{templateId}` | 获取模板详情 |
| PUT | `/v4/projects/{projectId}/templates/{templateId}` | 更新模板 |
| DELETE | `/v4/projects/{projectId}/templates/{templateId}` | 删除模板 |
| POST | `/v4/projects/{projectId}/templates/{templateId}/instances` | 实例化模板 |
| GET | `/v4/projects/{projectId}/templates/{templateId}/instances` | 获取实例列表 |
| GET | `/v4/projects/{projectId}/templates/{templateId}/versions` | 获取版本列表 |

---

## 十、最佳实践

### 10.1 新增模板功能

1. **API 层**：在 `api/template/v2/` 下定义接口
2. **POJO 层**：在 `pojo/template/v2/` 下定义请求/响应对象
3. **Service 层**：在 `service/template/v2/` 下实现业务逻辑
4. **DAO 层**：在 `dao/template/` 下实现数据访问
5. **权限控制**：调用 `PipelineTemplatePermissionService` 进行权限校验

### 10.2 版本管理扩展

如需新增版本操作类型：

1. 在 `PipelineVersionAction` 枚举中添加新操作
2. 实现 `PipelineTemplateVersionReqConverter` 转换器
3. 实现 `PipelineTemplateVersionCreateHandler` 处理器
4. 可选：实现 `PTemplateVersionPostProcessor` 后处理器

### 10.3 商店集成扩展

1. 在 Store 模块的 `template/service/` 下实现服务
2. 调用 Process 模块的模板服务进行模板操作
3. 维护商店特有的元数据（分类、标签、评论等）

---

## 十一、常见问题

### Q1: V1 和 V2 版本如何选择？

- 新功能开发优先使用 V2 版本
- V1 版本用于兼容老数据和简单场景
- V2 版本支持草稿、PAC、完善的版本管理

### Q2: 模板实例化失败如何排查？

1. 检查模板是否存在且版本正确
2. 检查用户是否有创建流水线权限
3. 检查模板参数是否完整
4. 查看 `T_TEMPLATE_INSTANCE_ITEM` 表中的错误信息

### Q3: 如何实现模板的批量更新？

使用异步实例化接口：
1. 调用 `createAsyncTemplateInstances` 创建异步任务
2. 系统通过 MQ 事件异步处理每个实例
3. 通过 `getTemplateInstanceStatus` 查询进度

### Q4: 模板权限如何配置？

1. 项目级别开关：`enableTemplatePermissionManage`
2. 权限类型：CREATE、EDIT、DELETE、LIST
3. 与蓝鲸权限中心集成，支持 RBAC 模型

### Q5: PAC 模板和普通模板有什么区别？

| 特性 | PAC 模板 | 普通模板 |
|------|----------|----------|
| 存储方式 | Git 仓库 YAML 文件 | BK-CI 数据库 |
| 编辑方式 | 代码编辑器 / IDE | BK-CI UI |
| 版本管理 | Git 分支 | BK-CI 版本号 |
| 同步机制 | Webhook 自动同步 | 手动保存 |
| 协作方式 | Git 工作流（PR/MR） | BK-CI 权限控制 |
| 回滚方式 | Git revert | BK-CI 版本回滚 |

### Q6: PAC 模板的 YAML 转换失败怎么办？

1. 检查 YAML 语法是否正确
2. 确认使用的是支持的 YAML 版本（v2/v3）
3. 查看 `PipelineTemplateGenerator.transfer()` 的异常日志
4. 使用 `fallbackOnError=true` 可以在转换失败时返回原始 Model

### Q7: 如何启用模板的 PAC 功能？

1. 在代码仓库中创建 `.ci/templates/xxx.yml` 文件
2. 配置 Webhook 触发器
3. 推送代码后自动创建/更新模板
4. 模板的 `enablePac` 标志会自动设为 `true`

### Q8: 分支版本和正式版本的关系？

- 推送到**默认分支**（main/master）→ 创建 `RELEASED` 正式版本
- 推送到**其他分支** → 创建 `BRANCH` 分支版本
- 分支版本可用于测试，不影响正式版本
- 分支合并到默认分支后，自动发布为正式版本

---

## 十二、相关 Skill

| Skill | 说明 |
|-------|------|
| `yaml-pipeline-transfer` | YAML 流水线转换，理解 Model ↔ YAML 转换机制 |
| `pipeline-model-architecture` | 流水线模型架构，理解模板的模型结构 |
| `process-module-architecture` | Process 模块架构，理解模板所在模块 |
| `auth-module-architecture` | 权限模块架构，理解模板权限控制 |
| `store-module-architecture` | 研发商店架构，理解模板商店集成 |
| `microservice-infrastructure` | 微服务基础设施（事件驱动），理解异步实例化机制 |
| `common-technical-practices` | 通用技术实践（分布式锁），理解模板并发控制 |
