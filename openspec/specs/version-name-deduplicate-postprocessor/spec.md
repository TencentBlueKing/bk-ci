# version-name-deduplicate-postprocessor Specification

## Purpose
TBD - created by archiving change unify-template-version-name-dedup. Update Purpose after archive.
## Requirements
### Requirement: 统一的版本名称去重 PostProcessor

当通过任何路径创建正式版本（RELEASED）时，如果同模板下已存在同名的 RELEASED 版本，系统 SHALL 自动将旧版本重命名（追加 `-{旧version}` 后缀）并标记状态为 `DELETE`，然后允许新版本使用原始名称正常创建。

#### Scenario: V2 草稿发布时存在同名已发布版本
- **GIVEN** 模板 T 已有名为 "v1.0" 的 RELEASED 版本（version=150）
- **WHEN** 用户通过草稿发布路径发布名为 "v1.0" 的新正式版本
- **THEN** 旧版本被重命名为 "v1.0-150"，状态标记为 DELETE，新版本使用 "v1.0" 正常创建

#### Scenario: V2 原生创建时存在同名已发布版本
- **GIVEN** 模板 T 已有名为 "v1.0" 的 RELEASED 版本（version=150）
- **WHEN** 用户通过 V2 原生创建路径（ReleaseCreateReq）创建名为 "v1.0" 的新正式版本
- **THEN** 旧版本被重命名为 "v1.0-150"，状态标记为 DELETE，新版本使用 "v1.0" 正常创建

#### Scenario: V1 兼容路径存在同名已发布版本
- **GIVEN** 模板 T 已有名为 "v1.0" 的 RELEASED 版本（version=150）
- **WHEN** 用户通过 V1 兼容路径（CompatibilityCreateReq）创建名为 "v1.0" 的新正式版本
- **THEN** 旧版本被重命名为 "v1.0-150"，状态标记为 DELETE，新版本使用 "v1.0" 正常创建

#### Scenario: 创建时不存在同名已发布版本
- **GIVEN** 模板 T 不存在名为 "v2.0" 的 RELEASED 版本
- **WHEN** 用户通过任意路径创建名为 "v2.0" 的新正式版本
- **THEN** 不触发任何重命名操作，新版本直接正常创建

#### Scenario: 分支版本不做重命名
- **WHEN** 用户发布草稿为分支版本（VersionStatus.BRANCH）
- **THEN** 不检查版本名称重复，直接正常创建

#### Scenario: 旧版本名称过长时截断处理
- **WHEN** 旧版本名称加上后缀后超过 64 字符限制
- **THEN** 系统 SHALL 截断原始名称以容纳完整后缀，确保重命名后的名称不超过 64 字符

### Requirement: v1VersionName 统一为 customVersionName

`PipelineTemplateVersionCreateContext` 中 SHALL 不再包含 `v1VersionName` 字段，所有使用 `v1VersionName` 的地方 SHALL 改为使用 `customVersionName`。`PipelineTemplateCompatibilityCreateReq` 上的 `v1VersionName` 字段保留（API 兼容），由 Converter 映射为 `customVersionName`。

#### Scenario: V1 兼容 Converter 映射
- **GIVEN** `PipelineTemplateCompatibilityCreateReq` 的 `v1VersionName` 为 "release-1"
- **WHEN** Converter 转换为 Context
- **THEN** Context 中 `customVersionName = "release-1"`，不存在 `v1VersionName` 字段

#### Scenario: V1 双写使用 customVersionName
- **GIVEN** V1 兼容路径创建正式版本，Context 中 `customVersionName = "release-1"`
- **WHEN** `CompatibilityPostProcessor.postProcessInTransactionVersionCreate` 执行 V1 双写
- **THEN** V1 表中的 `versionName` 取 `customVersionName`（即 "release-1"）

### Requirement: 草稿发布路径 SHALL 经过 postProcessBeforeVersionCreate 扩展点

`PipelineTemplatePersistenceService.releaseDraft2ReleaseVersion` SHALL 在事务开始前调用 `postProcessBeforeVersionCreate`，与 `createReleaseVersion` 保持一致。

#### Scenario: 草稿发布时 PostProcessor 前置处理被调用
- **WHEN** 用户通过草稿发布路径发布正式版本
- **THEN** 所有注册的 PostProcessor 的 `postProcessBeforeVersionCreate` 方法 SHALL 被调用

### Requirement: DraftReleaseHandler SHALL 不再拒绝重复版本名称

`PipelineTemplateDraftReleaseHandler` SHALL 不再对同名 RELEASED 版本抛出 `ERROR_TEMPLATE_VERSION_NAME_DUPLICATION` 错误。该功能由 `PTemplateVersionNameDeduplicatePostProcessor` 统一处理。

#### Scenario: 草稿发布时存在同名版本不再报错
- **GIVEN** 模板 T 已有名为 "v1.0" 的 RELEASED 版本
- **WHEN** 用户通过草稿发布路径发布名为 "v1.0" 的新正式版本
- **THEN** 系统 SHALL 不抛出异常，而是通过 PostProcessor 自动重命名旧版本

