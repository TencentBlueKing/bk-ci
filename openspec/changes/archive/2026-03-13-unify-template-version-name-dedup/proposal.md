## Why

V2 模板版本在三条创建正式版本的路径中，对"版本名称重复"的处理逻辑不一致：

| 路径 | 触发方式 | 当前行为 |
|------|---------|---------|
| V1 兼容创建 | `ReleaseCreateHandler` + `CompatibilityPostProcessor` | 重命名旧版本（依赖 `v1VersionName != null`） |
| V2 原生创建 | `ReleaseCreateHandler` | 无任何处理（`v1VersionName` 为 null，PostProcessor 跳过） |
| V2 草稿发布 | `DraftReleaseHandler` | 直接报错拒绝（`ERROR_TEMPLATE_VERSION_NAME_DUPLICATION`） |

根本原因是重命名逻辑被耦合在 V1 兼容专属的 `PTemplateCompatibilityVersionPostProcessor` 中，且以 `v1VersionName` 作为门控条件。实际上 `v1VersionName` 和 `customVersionName` 本质一致（都是用户期望的版本名称），不应作为区分条件。

用户反馈希望 V2 也能像 V1 一样允许重复的版本名称。

## What Changes

- 将"重命名旧同名版本"逻辑从 `PTemplateCompatibilityVersionPostProcessor` 中剥离，提取为独立的 `PTemplateVersionNameDeduplicatePostProcessor`，对所有创建正式版本的路径统一生效
- 在 `PipelineTemplateResourceService` 中提取公共方法 `renameExistingReleasedVersionIfDuplicate`
- 在 `PipelineTemplatePersistenceService.releaseDraft2ReleaseVersion` 中补充 `postProcessBeforeVersionCreate` 调用，使草稿发布路径也经过 PostProcessor 扩展点
- 移除 `DraftReleaseHandler` 中的重复名称报错逻辑
- 从 `PipelineTemplateVersionCreateContext` 中消除 `v1VersionName` 字段，统一使用 `customVersionName`

## Capabilities

### New Capabilities

- `version-name-deduplicate-postprocessor`: 独立的版本名称去重 PostProcessor，统一处理三条路径的重复版本名称

### Modified Capabilities

- `duplicate-version-name-rename`: 从 DraftReleaseHandler 内联逻辑改为由新 PostProcessor 统一处理
- V1 兼容双写中的 `v1VersionName` 引用统一为 `customVersionName`

## Impact

- `PipelineTemplateResourceService.kt`: 新增 `renameExistingReleasedVersionIfDuplicate` 公共方法
- 新建 `PTemplateVersionNameDeduplicatePostProcessor.kt`: 独立的去重 PostProcessor
- `PTemplateCompatibilityVersionPostProcessor.kt`: 删除 `postProcessBeforeVersionCreate` 中的重命名逻辑，`postProcessInTransactionVersionCreate` 中 `v1VersionName` 改为 `customVersionName`
- `PipelineTemplatePersistenceService.kt`: `releaseDraft2ReleaseVersion` 补充 `postProcessBeforeVersionCreate` 调用
- `PipelineTemplateDraftReleaseHandler.kt`: 删除重复名称报错逻辑
- `PipelineTemplateVersionCreateContext.kt`: 删除 `v1VersionName` 字段
- `PipelineTemplateCompatibilityCreateReqConverter.kt`: 不再设置 `v1VersionName`，保留 `customVersionName`
- `PipelineTemplateCompatibilityCreateReq.kt`: 保留 `v1VersionName` 字段（API 兼容），Converter 内部映射
- 测试文件: 更新所有涉及 `v1VersionName` 的测试用例
