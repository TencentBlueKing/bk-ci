# 流水线模型核心结构

## 四层结构

BK-CI 流水线模型的核心层级是：

```text
Model
  -> Stage[]
    -> Container[]
      -> Element[]
```

- `Model`：整条流水线的配置载体
- `Stage`：阶段边界、阶段级流程控制
- `Container`：执行容器或 Job 级抽象
- `Element`：具体任务、插件、触发器

## 关键代码入口

- `common-pipeline/Model.kt`
- `common-pipeline/container/Stage.kt`
- `common-pipeline/container/Container.kt`
- `common-pipeline/pojo/element/Element.kt`

如果需要落到具体类，优先先定位上面 4 个入口，再继续追子类型。

## Model

`Model` 负责描述一条流水线“长什么样”，常见信息包括：

- 流水线名称和描述
- `stages`
- 模板相关字段
- 创建人、版本号、提示信息
- 部分回调、静态视图、资源信息

高频事实：

- `stages` 是模型的核心字段
- 默认触发容器位于第一个 `Stage` 的第一个 `Container`
- 模板实例化、版本快照、构建重试都依赖 `Model`

## Stage

`Stage` 负责表达阶段边界和阶段级控制，常见关心点：

- `containers`
- `name`
- `stageControlOption`
- `checkIn` / `checkOut`
- `finally`
- 阶段级运行时字段，如 `status`、`executeCount`

重点：

- `finally = true` 的 Stage 具有特殊执行语义
- FinallyStage 不能按普通阶段心智处理
- 阶段级人工审核、准入准出也落在这里

## Container

`Container` 更接近 Job 抽象，除了公共字段，还会按类型分化。

排查或改动时优先先问：

1. 它是触发容器、普通构建容器还是特殊容器？
2. 它是定义阶段能力，还是执行资源能力？
3. 它属于模型结构问题，还是调度/执行问题？

常见相关主题：

- `JobControlOption`
- 构建矩阵 `Matrix`
- 容器级运行条件
- 自定义构建环境

## Element

`Element` 是最细粒度的任务抽象，通常对应插件、脚本、触发器等。

改动 `Element` 时不要只盯着类本身，还要一起检查：

- classType / code / 标识字段
- 前端编排渲染
- 后端多态序列化
- 执行引擎或 Worker 识别
- 校验插件和兼容逻辑

## 常见易混概念

- `id`：系统内部标识，不等于用户看到的名字
- `stageIdForUser`：用户可读或可编辑标识，不等于系统 `id`
- `status` / `executeCount`：多为运行时字段，不应直接按静态配置理解
- 模板字段：属于模型来源和模板继承关系，不等于普通业务字段

## 阅读建议

- 想先搞清“模型到底怎么分层”：从 `Model.kt` 开始
- 想看流程控制或 Finally：优先读 `Stage`
- 想看 Job、Matrix、容器能力：优先读 `Container`
- 想看插件任务：优先读 `Element`
