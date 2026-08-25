## Context

V2 模板版本管理中，创建正式版本有三条路径：

1. **V1 兼容创建**：`CompatibilityCreateReqConverter` → `ReleaseCreateHandler` → `PersistenceService.createReleaseVersion()`
2. **V2 原生创建**：`ReleaseCreateReqConverter` → `ReleaseCreateHandler` → `PersistenceService.createReleaseVersion()`
3. **V2 草稿发布**：`DraftReleaseReqConverter` → `DraftReleaseHandler` → `PersistenceService.releaseDraft2ReleaseVersion()`

`PersistenceService.createReleaseVersion()` 会依次调用 `postProcessBeforeVersionCreate` → 事务 → `postProcessInTransactionVersionCreate`。
`PersistenceService.releaseDraft2ReleaseVersion()` 只调用事务 → `postProcessInTransactionVersionCreate`，**缺少** `postProcessBeforeVersionCreate`。

当前重命名逻辑在 `PTemplateCompatibilityVersionPostProcessor.postProcessBeforeVersionCreate` 中，但以 `v1VersionName != null` 为前置条件，只有 V1 兼容路径会设置该值，导致 V2 路径无法触发重命名。

## Goals / Non-Goals

**Goals:**
- 统一三条路径碰到同名 RELEASED 版本时的行为：自动重命名旧版本
- 消除 `v1VersionName` 与 `customVersionName` 的概念冗余
- 将重命名逻辑抽取为独立的 PostProcessor，符合现有架构的扩展点模式
- 补齐 `releaseDraft2ReleaseVersion` 缺失的 `postProcessBeforeVersionCreate` 调用

**Non-Goals:**
- 不改变 V1 双写的核心逻辑（`postProcessInTransactionVersionCreate`）
- 不改变分支版本（BRANCH）的行为
- 不改变 `PipelineTemplateCompatibilityCreateReq` 的 API 接口（保留 `v1VersionName` 字段，由 Converter 内部映射）
- 不改变版本列表展示逻辑
- 不需要数据库表结构变更

## Decisions

### 决策 1：新建独立 PostProcessor 而非修改现有 CompatibilityPostProcessor 的门控条件

将重命名逻辑从 `PTemplateCompatibilityVersionPostProcessor` 中剥离，新建 `PTemplateVersionNameDeduplicatePostProcessor`。

**理由**：
- 版本名称去重是通用需求，不应与 V1 兼容逻辑耦合
- `CompatibilityPostProcessor` 职责应聚焦于 V1 双写，不应承担通用去重
- 新增 PostProcessor 符合开闭原则，不影响现有 PostProcessor 的行为
- 如果只改门控条件（去掉 `v1VersionName` 判断），会让 Compatibility PostProcessor 在非兼容场景下也运行，职责不清晰

### 决策 2：在 `releaseDraft2ReleaseVersion` 中补充 `postProcessBeforeVersionCreate` 调用

**理由**：
- `releaseDraft2ReleaseVersion` 本质是创建正式版本，理应经过 `postProcessBeforeVersionCreate` 扩展点
- 这是架构缺失的修补，不是特例 hack
- 未来其他 PostProcessor 也会自动对草稿发布路径生效

### 决策 3：消除 `v1VersionName`，统一使用 `customVersionName`

**理由**：
- `v1VersionName` 和 `customVersionName` 本质含义一致：用户期望的版本名称
- `CompatibilityCreateReqConverter` 中已经是 `customVersionName = v1VersionName`，二者总是相等
- 消除后减少概念冗余，降低认知负担
- `PipelineTemplateCompatibilityCreateReq` 上保留 `v1VersionName` 字段（API 兼容），只在 Converter 中映射为 `customVersionName`

### 决策 4：提取公共方法到 `PipelineTemplateResourceService`

将重命名逻辑封装为 `renameExistingReleasedVersionIfDuplicate` 方法。

**理由**：
- 消除 PostProcessor 中的长代码块，提高可读性
- 方法语义清晰，可被测试独立验证
- `PipelineTemplateResourceService` 已有 `getLatestResource` 和 `update` 方法，新方法是自然的组合

## Risks / Trade-offs

- **`releaseDraft2ReleaseVersion` 增加 `postProcessBeforeVersionCreate`**：需确认现有 PostProcessor 在此路径下不会产生副作用。当前只有 `PTemplateCompatibilityVersionPostProcessor` 实现了 `postProcessBeforeVersionCreate`，其内部以 `v1VersionName != null` 为门控，草稿发布路径 `v1VersionName` 为 null（消除后 `customVersionName` 也不为 null），该 PostProcessor 的 `postProcessBeforeVersionCreate` 在消除 `v1VersionName` 后会被清空重命名逻辑，所以不会有副作用。
- **`v1VersionName` 消除**：`PipelineTemplateCompatibilityCreateReq.v1VersionName` 保留（API 兼容），只移除 `Context` 中的字段，影响范围可控。
- **PostProcessor 执行顺序**：新的 `DeduplicatePostProcessor` 应在 `CompatibilityPostProcessor` 之前执行，确保重命名发生在 V1 双写之前。可通过 Spring `@Order` 注解控制。
