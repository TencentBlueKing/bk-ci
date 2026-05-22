# YAML 转换主链路

## 核心角色

YAML 流水线转换通常会涉及两层核心职责：

- 文本与节点层：YAML 字符串、注释、锚点、格式化、节点定位
- 业务模型层：YAML 数据结构与 BK-CI `Model` 的双向转换

典型关键组件：

- `TransferMapper`
- `ModelTransfer`
- `StageTransfer`
- `ContainerTransfer`
- `ElementTransfer`
- `TriggerTransfer`
- `VariableTransfer`

## 如何理解这些组件

### TransferMapper

更偏底层序列化与文本处理，适合解决：

- YAML 字符串与对象互转
- YAML 格式化
- 合并 YAML 时保留注释和锚点
- 节点定位、行列索引、标记位置

这类问题通常表现为：

- 注释丢失
- 锚点失效
- 某些关键字被错误加引号
- merge 后格式错乱

### ModelTransfer

更偏业务转换主入口，适合解决：

- YAML 导入为 `Model`
- `Model` 导出为 YAML
- Setting、Labels 等附属结构转换
- 模板引用后的模型组装

这类问题通常表现为：

- YAML 能解析，但生成的 `Model` 结构不对
- 导出 YAML 缺字段或结构顺序异常
- Trigger / Stage / Finally 组装不完整

## 典型转换路径

### YAML -> Model

常见流程是：

1. 前置切面处理 YAML
2. 构建基础 `Model`
3. 构建触发 Stage
4. 构建普通 Stage
5. 构建 Finally Stage
6. 处理模板引用和变量
7. 执行后置切面

### Model -> YAML

常见流程是：

1. 读取 `Model`
2. 归一化运行时无关字段
3. 转成 YAML 数据模型
4. 处理模板、表达式、变量展示形态
5. 生成 YAML 文本并格式化

## 常见问题判断法

如果是下面问题，优先看 `TransferMapper`：

- 注释丢了
- 锚点没了
- 格式不稳定
- 节点定位错位

如果是下面问题，优先看 `ModelTransfer` 及其下游：

- Stage/Job/Task 层级不对
- Trigger / Finally 没出来
- 导入和导出结果不对称
- 变量、模板、条件转换后语义变化

## 回归检查建议

每次调整 YAML 转换逻辑，至少验证：

- 普通 YAML 导入
- `Model` 导出 YAML
- 导入再导出是否大体稳定
- 带模板 YAML
- 带变量 / 表达式 YAML
- 注释与锚点保留场景
