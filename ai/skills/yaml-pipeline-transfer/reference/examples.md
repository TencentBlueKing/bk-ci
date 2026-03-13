# 使用场景与示例

BK-CI YAML 流水线转换的高级使用场景。

---

## 场景 1：PAC 自动同步（含变更检测）

```kotlin
@Service
class PacService(
    private val repositoryService: RepositoryService,
    private val pipelineYamlService: PipelineYamlService
) {
    fun autoSync(
        userId: String,
        projectId: String,
        pipelineId: String
    ) {
        // 1. 获取流水线的 PAC 配置
        val pacConfig = pipelineService.getPacConfig(pipelineId)

        // 2. 检查代码库变更
        val latestCommit = repositoryService.getLatestCommit(
            repoUrl = pacConfig.repoUrl,
            branch = pacConfig.branch,
            filePath = pacConfig.yamlPath
        )

        if (latestCommit.sha != pacConfig.lastCommitSha) {
            // 3. 拉取最新 YAML
            val yaml = repositoryService.getFileContent(
                repoUrl = pacConfig.repoUrl,
                branch = pacConfig.branch,
                filePath = pacConfig.yamlPath
            )

            // 4. 解析并转换（含校验）
            val yamlObject = TransferMapper.to<PreTemplateScriptBuildYamlParser>(yaml)
            val yamlInput = YamlTransferInput(
                userId = userId,
                projectCode = projectId,
                yaml = yamlObject,
                pipelineInfo = PipelineInfo(
                    pipelineId = pipelineId,
                    projectId = projectId
                )
            )

            val model = modelTransfer.yaml2Model(yamlInput)
            pipelineService.updatePipeline(userId, projectId, pipelineId, model)

            // 5. 更新同步记录
            pipelineService.updatePacConfig(pipelineId, pacConfig.copy(
                lastCommitSha = latestCommit.sha,
                lastSyncTime = System.currentTimeMillis()
            ))
        }
    }
}
```

---

## 场景 2：YAML 合并（保留注释）

```kotlin
@Service
class YamlMergeService {
    fun updatePipelineYaml(
        pipelineId: String,
        updates: Map<String, Any>
    ): String {
        // 1. 获取当前 YAML
        val currentYaml = pipelineService.getPipelineYaml(pipelineId)

        // 2. 解析为对象
        val yamlObject = TransferMapper.to<PreTemplateScriptBuildYamlParser>(currentYaml)

        // 3. 应用更新
        val updatedObject = yamlObject.copy(
            name = updates["name"] as? String ?: yamlObject.name,
            desc = updates["desc"] as? String ?: yamlObject.desc
        )

        // 4. 转换为新 YAML
        val newYaml = TransferMapper.toYaml(updatedObject)

        // 5. 合并（保留原 YAML 的注释和锚点）
        return TransferMapper.mergeYaml(currentYaml, newYaml)
    }
}
```

---

## 场景 3：YAML 元素插入（精确定位）

```kotlin
@Service
class YamlElementInsertService {
    fun insertStep(
        yaml: String,
        stageIndex: Int,
        jobId: String,
        stepIndex: Int,
        newStep: PreStep
    ): String {
        // 1. 解析 YAML 为对象
        val yamlObject = TransferMapper.to<ITemplateFilter>(yaml)

        // 2. 定位插入位置
        val position = PositionResponse(
            stageIndex = stageIndex,
            jobId = jobId,
            stepIndex = stepIndex,
            type = PositionResponse.PositionType.STEP
        )

        // 3. 计算节点索引
        val nodeIndex = TransferMapper.indexYaml(
            position = position,
            pYml = yamlObject,
            yml = newStep,
            type = ElementInsertBody.ElementInsertType.INSERT
        )

        // 4. 转换并合并（保留注释）
        val newYaml = TransferMapper.toYaml(yamlObject)
        return TransferMapper.mergeYaml(yaml, newYaml)
    }

    fun updateStepByPosition(
        yaml: String,
        line: Int,
        column: Int,
        updatedStep: PreStep
    ): String {
        // 1. 根据行列号定位节点
        val nodeIndex = TransferMapper.indexYaml(yaml, line, column)
            ?: throw ErrorCodeException(errorCode = "INVALID_POSITION")

        // 2. 解析并更新
        val yamlObject = TransferMapper.to<ITemplateFilter>(yaml)
        // ... 根据 nodeIndex 定位并更新

        // 3. 转换并合并
        val newYaml = TransferMapper.toYaml(yamlObject)
        return TransferMapper.mergeYaml(yaml, newYaml)
    }
}
```

---

## 场景 4：模板引用示例

### Extends 模板（全局继承）

```yaml
# 主 YAML
extends:
  template: templates/base.yml
  parameters:
    BUILD_TYPE: release
    DEPLOY_ENV: prod

variables:
  CUSTOM_VAR: custom-value
```

```yaml
# templates/base.yml
version: v2.0
name: Base Template

variables:
  BUILD_TYPE:
    value: ${{ parameters.BUILD_TYPE }}
  DEPLOY_ENV:
    value: ${{ parameters.DEPLOY_ENV }}

stages:
  - name: Build
    jobs:
      build:
        runs-on: linux
        steps:
          - name: Build
            run: ./build.sh
```

### Step 模板

```yaml
# templates/deploy-step.yml
- name: Deploy to ${{ parameters.env }}
  run: |
    echo "Deploying to ${{ parameters.env }}"
    ./deploy.sh --env=${{ parameters.env }}
```

---

## 场景 5：扩展自定义 YAML 字段

通过继承 `IPreTemplateScriptBuildYamlParser` 注册新版本：

```kotlin
@JsonSubTypes.Type(value = CustomYamlParser::class, name = "v4.0")
data class CustomYamlParser(
    override val version: String?,
    override val name: String?,
    // ... 继承所有字段

    // 新增自定义字段
    val customField: String?
) : IPreTemplateScriptBuildYamlParser {
    // 实现必要方法
}
```

---

## 场景 6：调试 YAML 转换

```kotlin
// 1. 启用 Debug 日志
logger.debug("YAML Input: $yaml")
logger.debug("YAML Object: ${JsonUtil.toJson(yamlObject)}")

// 2. 使用切面记录转换过程
val aspectWrapper = PipelineTransferAspectWrapper()
aspectWrapper.setYaml4Yaml(yamlObject, BEFORE) { yaml ->
    logger.info("Before YAML: ${TransferMapper.toYaml(yaml)}")
}

// 3. 分步转换定位问题
val model = modelTransfer.yaml2Model(yamlInput)
logger.info("Model: ${JsonUtil.toJson(model)}")

// 4. 使用 getYamlLevelOneIndex 检查结构
val index = TransferMapper.getYamlLevelOneIndex(yaml)
logger.info("YAML Index: $index")
```

---

## 常见问题

### Q1: 如何处理 YAML 中的特殊字符？

`CustomStringQuotingChecker` 自动处理：
- 十六进制数字（`0x123`）自动加引号
- 布尔关键字（`yes`/`no`）自动加引号
- `on` 关键字不加引号（BK-CI 特殊处理）

### Q2: 如何在 YAML 中使用锚点和别名？

TransferMapper 自动管理锚点，`mergeYaml` 保留锚点引用：

```yaml
defaults: &defaults
  runs-on: linux
  timeout-minutes: 60

stages:
  - name: Build
    jobs:
      build:
        <<: *defaults
        steps:
          - run: ./build.sh
```

### Q3: 大型 YAML 文件性能优化？

1. 使用流式解析（`getYamlFactory().parse()`）
2. 缓存已解析的模板（`TransferCacheService`）
3. 分批处理 Stage/Job/Step
4. 异步转换非关键路径
