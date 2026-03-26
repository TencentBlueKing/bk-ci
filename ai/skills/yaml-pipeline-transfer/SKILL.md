---
name: yaml-pipeline-transfer
description: YAML 流水线转换指南，涵盖 YAML 与 Model 双向转换、PAC（Pipeline as Code）实现、模板引用、触发器配置。当用户需要解析 YAML 流水线、实现 PAC 模式、处理流水线模板或进行 YAML 语法校验时使用。
---

# YAML 流水线转换指南

## 概述

BK-CI 的 YAML 流水线转换支持 Pipeline as Code（PAC）模式，实现 YAML 与 Pipeline Model 之间的双向转换、模板系统、触发器配置等功能。

## 触发条件

当用户需要实现以下功能时，使用此 Skill：
- YAML 流水线解析与转换
- Model 转换为 YAML
- PAC（Pipeline as Code）实现
- 流水线模板引用
- YAML 语法校验
- 自定义 YAML 处理逻辑

---

## 核心组件架构

| 组件 | 位置 | 职责 |
|------|------|------|
| `TransferMapper` | `transfer/TransferMapper.kt` | YAML 序列化/反序列化引擎 |
| `ModelTransfer` | `transfer/ModelTransfer.kt` | YAML <-> Pipeline Model 转换 |
| `ElementTransfer` | `transfer/ElementTransfer.kt` | YAML Step <-> Model Element 转换 |
| `StageTransfer` | `transfer/StageTransfer.kt` | YAML Stage <-> Model Stage 转换 |
| `ContainerTransfer` | `transfer/ContainerTransfer.kt` | YAML Job <-> Model Container 转换 |
| `TriggerTransfer` | `transfer/TriggerTransfer.kt` | 触发器转换（Git/GitHub/定时/手动等） |
| `VariableTransfer` | `transfer/VariableTransfer.kt` | 变量 <-> BuildFormProperty 转换 |

所有路径前缀为 `common-pipeline-yaml/src/main/kotlin/.../`

### 元素类型映射

| YAML Step | Model Element | 说明 |
|-----------|---------------|------|
| `uses: checkout@v2` | `GitCheckoutElement` | 代码检出 |
| `uses: <atomCode>@v*` | `MarketBuildAtomElement` | 研发商店插件 |
| `run: <script>` | `LinuxScriptElement` / `WindowsScriptElement` | 脚本执行 |
| `uses: manual-review@v*` | `ManualReviewUserTaskElement` | 人工审核 |
| `template: <path>` | `StepTemplateElement` | 步骤模板 |

### Container 类型映射

| YAML Job runs-on | Model Container | 说明 |
|------------------|-----------------|------|
| `linux` / `windows` / `macos` | `VMBuildContainer` | 虚拟机 |
| `agent-id: <id>` | `ThirdPartyAgentIdContainer` | 第三方构建机（ID） |
| `agent-name: <name>` | `ThirdPartyAgentNameContainer` | 第三方构建机（名称） |
| `pool: <pool>` | `ThirdPartyAgentNameContainer` | 构建池 |
| `self-hosted: true` | `NormalContainer` | 自托管环境 |

---

## 快速入门：YAML 导入流水线（验证-修复-重试）

标准的 YAML 导入流程，包含验证与错误恢复：

```kotlin
@Service
class PipelineYamlService(
    private val modelTransfer: ModelTransfer,
    private val pipelineService: PipelineService
) {
    fun importFromYaml(
        userId: String,
        projectId: String,
        yaml: String,
        yamlFileName: String? = null
    ): Pipeline {
        // 1. 解析 YAML（可能抛出 YamlFormatException）
        val yamlObject = TransferMapper.to<PreTemplateScriptBuildYamlParser>(yaml)

        // 2. 校验 -> 修复 -> 重试
        val validatedYaml = validateAndFix(yamlObject, yaml)

        // 3. 构建转换输入
        val yamlInput = YamlTransferInput(
            userId = userId,
            projectCode = projectId,
            yaml = validatedYaml,
            yamlFileName = yamlFileName
        )

        // 4. 转换为 Model + Setting
        val model = modelTransfer.yaml2Model(yamlInput)
        val setting = modelTransfer.yaml2Setting(yamlInput)

        // 5. 创建流水线
        return pipelineService.createPipeline(
            userId = userId,
            projectId = projectId,
            model = model,
            setting = setting
        )
    }

    /**
     * 验证-修复-重试循环
     * 常见错误：缺少 name、空 stages、无效触发器配置
     */
    private fun validateAndFix(
        yamlObject: IPreTemplateScriptBuildYamlParser,
        rawYaml: String,
        maxRetries: Int = 3
    ): IPreTemplateScriptBuildYamlParser {
        var current = yamlObject
        var lastError: String? = null

        repeat(maxRetries) { attempt ->
            val errors = collectValidationErrors(current)
            if (errors.isEmpty()) return current

            lastError = errors.joinToString("; ")
            logger.warn("YAML 校验失败 (第 ${attempt + 1} 次): $lastError")

            // 尝试自动修复
            current = attemptAutoFix(current, errors)
        }

        throw ErrorCodeException(
            errorCode = "YAML_VALIDATION_FAILED",
            defaultMessage = "YAML 校验失败，无法自动修复: $lastError"
        )
    }

    private fun collectValidationErrors(
        yaml: IPreTemplateScriptBuildYamlParser
    ): List<String> {
        val errors = mutableListOf<String>()
        if (yaml.name.isNullOrBlank()) errors.add("流水线名称不能为空")
        if (yaml.formatStages().isEmpty()) errors.add("至少需要定义一个 Stage")
        // 检查触发器配置有效性
        // 检查变量引用是否合法
        return errors
    }

    private fun attemptAutoFix(
        yaml: IPreTemplateScriptBuildYamlParser,
        errors: List<String>
    ): IPreTemplateScriptBuildYamlParser {
        // 根据错误类型尝试修复，例如：
        // - 缺少名称：从文件名推断
        // - 无效的变量引用：提示用户修正
        return yaml
    }
}
```

### 流水线导出为 YAML

```kotlin
fun exportToYaml(
    userId: String, projectId: String, pipelineId: String,
    yamlVersion: YamlVersion = YamlVersion.V2_0
): String {
    val model = pipelineService.getModel(userId, projectId, pipelineId)
    val input = ModelTransferInput(
        userId = userId, projectCode = projectId,
        model = model, yamlVersion = yamlVersion
    )
    val yamlObject = modelTransfer.model2Yaml(input)
    return TransferMapper.toYaml(yamlObject)
}
```

### PAC 代码库同步（验证-修复-重试）

```kotlin
fun syncFromRepo(
    userId: String, projectId: String,
    repoUrl: String, branch: String = "master",
    yamlPath: String = ".ci/pipeline.yml"
): Pipeline {
    // 1. 拉取 YAML
    val yaml = repositoryService.getFileContent(repoUrl, branch, yamlPath)

    // 2. 解析并校验（含重试逻辑）
    val yamlObject = try {
        TransferMapper.to<PreTemplateScriptBuildYamlParser>(yaml)
    } catch (e: YamlFormatException) {
        logger.error("YAML 解析失败: ${e.message}")
        throw ErrorCodeException(
            errorCode = "YAML_PARSE_ERROR",
            defaultMessage = "YAML 文件格式错误: ${e.message}"
        )
    }

    // 3. 导入流水线（内部含 validateAndFix）
    return pipelineYamlService.importFromYaml(userId, projectId, yaml, yamlPath)
}
```

---

## YAML 版本与结构概览

BK-CI 支持两个 YAML 版本：

| 版本 | 标识 | 说明 |
|------|------|------|
| **v2.0** | `version: v2.0` | 当前主版本 |
| **v3.0** | `version: v3.0` | 增强版本（支持更多特性） |

完整 YAML 结构和 Schema 定义见 [reference/yaml-schema.md](reference/yaml-schema.md)。

### 最小可用 YAML 示例

```yaml
version: v2.0
name: 最小示例

on:
  push:
    branches:
      - master

stages:
  - name: Build
    jobs:
      build:
        runs-on: linux
        steps:
          - name: Build
            run: echo "Hello BK-CI"
```

---

## 模板系统

BK-CI 支持四种模板类型：

### Extends 模板（全局继承）

```yaml
extends:
  template: templates/base.yml
  parameters:
    BUILD_TYPE: release
```

### Step / Job / Stage 模板

```yaml
# Step 模板引用
steps:
  - template: templates/deploy-step.yml
    parameters:
      env: ${{ variables.DEPLOY_ENV }}

# Job 模板引用
jobs:
  test:
    template: templates/test-job.yml
    parameters:
      node-version: 16

# Stage 模板引用
stages:
  - template: templates/build-stage.yml
    parameters:
      platform: linux
```

---

## 表达式系统

```yaml
# 变量引用
${{ variables.BUILD_TYPE }}
${{ on.push.branch }}
${{ resources.repositories.my-repo }}

# 条件判断
if: ${{ eq(variables.BUILD_TYPE, 'release') }}
if: ${{ and(eq(variables.BUILD_TYPE, 'release'), ne(on.push.branch, 'master')) }}

# 状态函数
if: success()    # 前序步骤成功
if: failure()    # 前序步骤失败
if: always()     # 总是执行
if: cancelled()  # 被取消
```

---

## BK-CI 特有功能

### TransferMapper 自定义行为

- **`on` 关键字不加引号**：`CustomStringQuotingChecker` 确保 YAML `on:` 不被引号包裹
- **十六进制数字加引号**：`0x` 开头的值自动加引号
- **去除尾随空格**：自定义 YAML 生成器移除换行符前的空格
- **锚点保留**：`CustomAnchorGenerator` 保持原始锚点命名
- **智能合并**：`mergeYaml()` 使用 Myers Diff 算法保留注释和锚点

### 切面系统（PipelineTransferAspectWrapper）

在转换过程中注入自定义逻辑（校验、日志、扩展）：

```kotlin
val aspectWrapper = PipelineTransferAspectWrapper()
aspectWrapper.setYaml4Yaml(yamlObject, BEFORE)   // YAML->Model 前置
aspectWrapper.setModel4Model(model, AFTER)        // Model->YAML 后置
aspectWrapper.setYamlTriggerOn(triggerOn, BEFORE) // 触发器处理
aspectWrapper.setModelElement4Model(element, AFTER) // 元素处理
```

### YAML 合并（保留注释）

```kotlin
// 推荐：使用 mergeYaml 保留原 YAML 的注释和锚点
val merged = TransferMapper.mergeYaml(oldYaml, newYaml)

// 不推荐：直接覆盖（丢失注释）
val newYaml = TransferMapper.toYaml(yamlObject)
```

---

## 最佳实践

1. **明确指定版本**：始终写 `version: v2.0`
2. **变量命名**：使用大写下划线（`BUILD_TYPE`），不用驼峰
3. **模板复用**：提取公共配置到 `extends` 模板
4. **条件执行**：使用 `if` 表达式而非脚本内 `if` 判断
5. **保留注释**：更新 YAML 时使用 `mergeYaml()` 而非直接覆盖
6. **转换前校验**：始终调用验证逻辑，捕获 `YamlFormatException` 和 `ModelCreateException`

---

## 检查清单

- [ ] 确定 YAML 版本（v2.0 / v3.0）
- [ ] 了解 YAML 与 Model 的映射关系
- [ ] 确认需要支持的触发器类型
- [ ] 确认需要支持的元素类型
- [ ] 确认是否需要模板引用
- [ ] 确认是否需要保留注释和锚点（使用 mergeYaml）
- [ ] 添加 YAML 校验逻辑（验证-修复-重试）
- [ ] 添加异常处理（捕获 `YamlFormatException`、`ModelCreateException`）
- [ ] 添加单元测试覆盖转换逻辑
- [ ] 考虑性能优化（缓存、异步）

---

## 参考文档

- [API 与组件详解](reference/api-reference.md) — 转换器接口、数据模型、切面系统完整定义
- [YAML Schema 完整结构](reference/yaml-schema.md) — 完整 YAML 配置字段说明
- [使用场景与示例](reference/examples.md) — PAC 同步、YAML 合并、元素插入等高级场景

## 相关 Skills

- [流水线变量管理](../pipeline-variable-management/SKILL.md) — 变量转换逻辑
- [01-后端微服务开发](../01-backend-microservice-development/SKILL.md) — 微服务架构
- [27-设计模式](../27-design-patterns/SKILL.md) — 工厂模式、策略模式

## 相关文件路径

### 核心转换类
- `common-pipeline-yaml/src/main/kotlin/.../transfer/TransferMapper.kt`
- `common-pipeline-yaml/src/main/kotlin/.../transfer/ModelTransfer.kt`
- `common-pipeline-yaml/src/main/kotlin/.../transfer/ElementTransfer.kt`
- `common-pipeline-yaml/src/main/kotlin/.../transfer/StageTransfer.kt`
- `common-pipeline-yaml/src/main/kotlin/.../transfer/ContainerTransfer.kt`
- `common-pipeline-yaml/src/main/kotlin/.../transfer/TriggerTransfer.kt`
- `common-pipeline-yaml/src/main/kotlin/.../transfer/VariableTransfer.kt`

### YAML 模型定义
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/PreTemplateScriptBuildYamlParser.kt`
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/PreTemplateScriptBuildYamlV3Parser.kt`
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/on/TriggerOn.kt`
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/stage/Stage.kt`
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/job/Job.kt`
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/step/Step.kt`

### JSON Schema
- `common-pipeline-yaml/src/main/resources/schema/V3_0/ci.json`
- `common-pipeline-yaml/src/main/resources/schema/V2_0/ci.json`

### 测试用例
- `common-pipeline-yaml/src/test/kotlin/.../transfer/MergeYamlTest.kt`
- `common-pipeline-yaml/src/test/kotlin/.../parsers/template/YamlTemplateTest.kt`
- `common-pipeline-yaml/src/test/resources/samples/`
