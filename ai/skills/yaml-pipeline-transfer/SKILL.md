---
name: yaml-pipeline-transfer
description: 处理 BK-CI YAML 流水线导入导出、YAML 与 Model 双向转换、PAC、模板引用与 YAML 校验时使用。当用户提到 YAML 流水线、PAC、模板化、注释保留、表达式解析或转换调试时优先使用。
---

# YAML 流水线转换

## 适用场景

- YAML 导入为流水线模型
- 流水线模型导出为 YAML
- PAC（Pipeline as Code）接入与同步
- YAML 模板、Step/Job/Stage 模板引用
- YAML 语法、表达式、注释保留、锚点处理相关问题

## 不适用场景

- 只是单纯修改流水线模型结构，不涉及 YAML 表达
- 只是代码库接入、权限、数据库或执行调度问题
- 只是某个插件运行逻辑，而不是 YAML 到模型的转换问题

## 快速指导

1. 先判断问题属于哪一类，再继续读对应参考文档：
   - 转换主链路：`reference/1-transfer-flow.md`
   - PAC 与模板：`reference/2-pac-template.md`
   - 表达式、切面、调试与扩展：`reference/3-debug-extension.md`
2. YAML 转换问题通常不是单点问题，要区分：
   - YAML 文本解析
   - YAML 数据模型
   - YAML 到 `Model`
   - `Model` 到 YAML
   - 模板展开与 PAC 预处理
3. 只要改 YAML 字段或结构，就要同时考虑：
   - `Model` 映射
   - 兼容旧 YAML
   - 导入与导出双向一致性
   - 模板引用和表达式是否受影响
4. PAC、模板、表达式、切面经常彼此叠加，不要把它们当成完全独立的模块。
5. 如果问题是“转换后执行不对”，先确认是 YAML 转换层问题，还是 `process-module-architecture` 的执行链放大了问题。

## 高信号规则

- `yaml-pipeline-transfer` 负责“YAML 如何表示和转换”，`pipeline-model-architecture` 负责“模型长什么样”
- `TransferMapper` 解决序列化、格式化、注释和锚点等文本层问题
- `ModelTransfer` 更偏 YAML 与 `Model` 的业务转换
- 模板、PAC、表达式和切面属于转换前后会共同影响结果的增强层
- 改动 YAML 相关能力时，要始终以“双向一致”和“历史兼容”为目标

## 关键陷阱

- 只修 `yaml2Model`，不修 `model2Yaml`，导致导入导出不对称
- 只验证普通 YAML，不验证模板、PAC、注释保留和锚点场景
- 把模型字段问题误判为 YAML 解析问题，或反过来
- 忽略切面、表达式、模板预处理对最终结果的影响
- 修改转换规则后，没有回归历史 YAML 和已有模板

## 延伸阅读

- 转换主链路：`reference/1-transfer-flow.md`
- PAC 与模板：`reference/2-pac-template.md`
- 调试与扩展：`reference/3-debug-extension.md`
- 涉及模型结构时：再看 `pipeline-model-architecture`
- 涉及执行链路归属时：再看 `process-module-architecture`
