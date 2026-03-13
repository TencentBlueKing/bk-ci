# BK-CI 流水线模型示例

---

## 最小化 Model JSON

```json
{
  "name": "My Pipeline",
  "desc": "A simple pipeline",
  "stages": [
    {
      "@type": "stage",
      "id": "stage-1",
      "name": "Trigger Stage",
      "containers": [
        {
          "@type": "trigger",
          "id": "0",
          "name": "trigger",
          "elements": [
            {
              "@type": "manualTrigger",
              "id": "T-1-1-1",
              "name": "手动触发"
            }
          ],
          "params": []
        }
      ]
    }
  ]
}
```

---

## 完整 Model JSON 示例

包含触发器、构建 Stage（带审核）、部署 Stage、FinallyStage:

```json
{
  "name": "Full Pipeline Example",
  "desc": "A complete pipeline with all features",
  "stages": [
    {
      "@type": "stage",
      "id": "stage-1",
      "name": "Trigger",
      "containers": [
        {
          "@type": "trigger",
          "id": "0",
          "name": "trigger",
          "elements": [
            {
              "@type": "manualTrigger",
              "id": "T-1-1-1",
              "name": "手动触发",
              "canElementSkip": false,
              "useLatestParameters": false
            },
            {
              "@type": "timerTrigger",
              "id": "T-1-1-2",
              "name": "定时触发",
              "advanceExpression": ["0 0 8 * * ?"],
              "noScm": false
            }
          ],
          "params": [
            {
              "id": "version",
              "name": "版本号",
              "type": "STRING",
              "required": true,
              "defaultValue": "1.0.0",
              "desc": "发布版本号"
            },
            {
              "id": "env",
              "name": "环境",
              "type": "ENUM",
              "required": true,
              "defaultValue": "dev",
              "options": [
                {"key": "dev", "value": "开发环境"},
                {"key": "test", "value": "测试环境"},
                {"key": "prod", "value": "生产环境"}
              ]
            }
          ],
          "buildNo": {
            "buildNo": 1,
            "buildNoType": "EVERY_BUILD_INCREMENT",
            "required": false
          }
        }
      ]
    },
    {
      "@type": "stage",
      "id": "stage-2",
      "name": "Build",
      "stageControlOption": {
        "enable": true,
        "runCondition": "AFTER_LAST_FINISHED"
      },
      "containers": [
        {
          "@type": "vmBuild",
          "id": "1",
          "name": "Build Job",
          "baseOS": "LINUX",
          "dispatchType": {
            "buildType": "DOCKER",
            "value": "bkci/ci:latest"
          },
          "jobControlOption": {
            "enable": true,
            "timeout": 60,
            "runCondition": "STAGE_RUNNING"
          },
          "elements": [
            {
              "@type": "linuxScript",
              "id": "e-2-1-1",
              "name": "编译",
              "scriptType": "SHELL",
              "script": "#!/bin/bash\necho 'Building...'\nmake build",
              "continueNoneZero": false,
              "additionalOptions": {
                "enable": true,
                "timeout": 30,
                "retryWhenFailed": true,
                "retryCount": 2
              }
            },
            {
              "@type": "marketBuild",
              "id": "e-2-1-2",
              "name": "上传制品",
              "atomCode": "uploadArtifact",
              "version": "1.*",
              "data": {
                "input": {
                  "filePath": "./build/output/*",
                  "destPath": "/artifacts/"
                }
              }
            }
          ]
        }
      ]
    },
    {
      "@type": "stage",
      "id": "stage-3",
      "name": "Deploy",
      "checkIn": {
        "manualTrigger": true,
        "reviewGroups": [
          {
            "name": "审核组",
            "reviewers": ["admin", "reviewer"]
          }
        ],
        "timeout": 24,
        "reviewDesc": "请确认是否部署到 ${env} 环境"
      },
      "containers": [
        {
          "@type": "normal",
          "id": "2",
          "name": "Deploy Job",
          "elements": [
            {
              "@type": "marketBuildLess",
              "id": "e-3-1-1",
              "name": "部署",
              "atomCode": "deploy",
              "version": "1.*"
            }
          ]
        }
      ]
    },
    {
      "@type": "stage",
      "id": "stage-4",
      "name": "Finally",
      "finally": true,
      "containers": [
        {
          "@type": "normal",
          "id": "3",
          "name": "Cleanup",
          "elements": [
            {
              "@type": "marketBuildLess",
              "id": "e-4-1-1",
              "name": "发送通知",
              "atomCode": "sendNotify",
              "version": "1.*"
            }
          ]
        }
      ]
    }
  ],
  "pipelineCreator": "admin"
}
```

---

## 常用 Kotlin 操作示例

### 创建 Model

```kotlin
val model = Model.defaultModel(pipelineName = "My Pipeline", userId = "admin")

val buildStage = Stage(
    id = "stage-2",
    name = "Build Stage",
    containers = listOf(
        VMBuildContainer(
            id = "1",
            name = "Build Job",
            baseOS = VMBaseOS.LINUX,
            elements = listOf(
                LinuxScriptElement(id = "e-1", name = "Compile", script = "./build.sh")
            )
        )
    )
)
model.stages.add(buildStage)
```

### 遍历 Model

```kotlin
fun traverseModel(model: Model, action: (Element) -> Unit) {
    model.stages.forEach { stage ->
        stage.containers.forEach { container ->
            container.elements.forEach { element -> action(element) }
        }
    }
}
```

### 更新 Element 状态

```kotlin
fun updateElementStatus(model: Model, elementId: String, status: BuildStatus) {
    traverseModel(model) { element ->
        if (element.id == elementId) {
            element.status = status.name
            element.executeCount++
        }
    }
    updateModel(buildId, model)
}
```

### 获取触发器参数

```kotlin
val triggerContainer = model.getTriggerContainer()
val variables = triggerContainer.params.associate { it.id to (it.value ?: it.defaultValue) }
```

### 矩阵展开

```kotlin
val parentContainer = VMBuildContainer(
    matrixControlOption = MatrixControlOption(
        strategyStr = """{"os": ["linux", "windows"], "version": ["1.0", "2.0"]}"""
    )
)
val childContainers = matrixService.expand(parentContainer)
parentContainer.groupContainers = childContainers.toMutableList()
parentContainer.matrixGroupFlag = true
childContainers.forEach { child ->
    child.matrixGroupId = parentContainer.containerHashId
    child.matrixContext = mapOf("os" to "linux", "version" to "1.0")
}
```

### Post 任务配置

```kotlin
val postElement = LinuxScriptElement(
    id = "e-2", name = "Cleanup", script = "./cleanup.sh",
    additionalOptions = ElementAdditionalOptions(
        elementPostInfo = ElementPostInfo(
            parentElementId = "e-1",
            postCondition = "failure"
        )
    )
)
```

---

## 扩展指南

### 新增 Element 类型

1. 定义 Element 子类（在 common-pipeline 模块）:
```kotlin
data class MyCustomElement(
    override val name: String = "我的自定义插件",
    override var id: String? = null,
    override var status: String? = null,
    val myParam1: String,
    override var additionalOptions: ElementAdditionalOptions? = null
) : Element(name, id, status, additionalOptions = additionalOptions) {
    companion object { const val classType = "myCustomElement" }
    override fun getClassType() = classType
    override fun getAtomCode() = "myCustomAtom"
}
```
2. 在 Element.kt 的 `@JsonSubTypes` 中注册
3. 实现 Worker 端执行逻辑

### 新增 Container 类型

1. 定义 Container 实现类
2. 在 Container.kt 的 `@JsonSubTypes` 中注册
3. 在 Dispatch 服务中实现调度逻辑

---

## 调试方法

```kotlin
// 打印 Model JSON
val modelJson = JsonUtil.toJson(model, formatted = true)
logger.info("Model: $modelJson")

// 检查结构
fun debugModel(model: Model) {
    println("Pipeline: ${model.name}")
    model.stages.forEachIndexed { sIndex, stage ->
        println("  Stage[$sIndex]: ${stage.id} - ${stage.name} - ${stage.status}")
        stage.containers.forEachIndexed { cIndex, container ->
            println("    Container[$cIndex]: ${container.id} - ${container.name} - ${container.status}")
            container.elements.forEachIndexed { eIndex, element ->
                println("      Element[$eIndex]: ${element.id} - ${element.name} - ${element.status}")
            }
        }
    }
}
```

### 常见问题

| 问题 | 原因 | 排查 |
|------|------|------|
| Model 反序列化失败 | `@type` 不匹配 | 检查 JSON 中 `@type` 是否在 `@JsonSubTypes` 注册 |
| 构建状态不更新 | 未持久化 | 检查 `buildDetailService.updateModel()` 调用 |
| 版本号不递增 | 状态非 RELEASED | 检查 `versionStatus` |
