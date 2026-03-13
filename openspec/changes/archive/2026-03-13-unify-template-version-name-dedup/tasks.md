## 1. 提取公共方法

- [x] 1.1 在 `PipelineTemplateResourceService` 中新增 `renameExistingReleasedVersionIfDuplicate(projectId, templateId, versionName, transactionContext?)` 公共方法，封装查询同名 RELEASED 版本 → 重命名 → 标记 DELETE 的完整逻辑

## 2. 新建版本名称去重 PostProcessor

- [x] 2.1 新建 `PTemplateVersionNameDeduplicatePostProcessor`，实现 `PTemplateVersionCreatePostProcessor` 接口
- [x] 2.2 在 `postProcessBeforeVersionCreate` 中：仅对 `isCreateReleaseVersion()` 的 action 生效，使用 `customVersionName ?: pipelineTemplateResource.versionName` 确定目标名称，调用 `resourceService.renameExistingReleasedVersionIfDuplicate`
- [x] 2.3 通过 `@Order` 注解确保在 `CompatibilityPostProcessor` 之前执行

## 3. 清理 CompatibilityVersionPostProcessor

- [x] 3.1 删除 `postProcessBeforeVersionCreate` 中的全部重命名逻辑（该方法可简化为空实现或日志）
- [x] 3.2 在 `postProcessInTransactionVersionCreate` 中将 `v1VersionName` 引用改为 `customVersionName`（第 139 行 `versionName = v1VersionName ?: v2VersionName!!` → `versionName = customVersionName ?: v2VersionName!!`）
- [x] 3.3 删除因 `v1VersionName` 移除而变为 unused 的 import

## 4. 补齐 releaseDraft2ReleaseVersion 的前置处理

- [x] 4.1 在 `PipelineTemplatePersistenceService.releaseDraft2ReleaseVersion` 中，事务开始前增加 `postProcessBeforeVersionCreate` 调用（与 `createReleaseVersion` 对齐）

## 5. 清理 DraftReleaseHandler

- [x] 5.1 删除 `doHandle()` 中 `if (versionStatus == VersionStatus.RELEASED)` 块内的 `throw ErrorCodeException(ERROR_TEMPLATE_VERSION_NAME_DUPLICATION)` 逻辑

## 6. 消除 v1VersionName

- [x] 6.1 从 `PipelineTemplateVersionCreateContext` 中删除 `v1VersionName` 字段
- [x] 6.2 修改 `PipelineTemplateCompatibilityCreateReqConverter`：移除 `v1VersionName = v1VersionName`，保留 `customVersionName = v1VersionName`
- [x] 6.3 修改 `CompatibilityPostProcessor.postProcessInTransactionVersionCreate` 中对 `v1VersionName` 的引用
- [x] 6.4 检查并清理其他文件中对 `context.v1VersionName` 的引用

## 7. 更新测试

- [x] 7.1 更新 `PipelineTemplateCompatibilityTest` 中所有 `v1VersionName` 引用为 `customVersionName`
- [x] 7.2 新增 `PTemplateVersionNameDeduplicatePostProcessorTest`：覆盖三条路径的重命名、无重复、分支版本跳过等场景
- [x] 7.3 更新或删除 `PipelineTemplateDraftReleaseHandlerTest` 中的重复名称报错相关测试
