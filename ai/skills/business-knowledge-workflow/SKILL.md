---
name: business-knowledge-workflow
description: 获取陌生业务知识并沉淀为 BK-CI skill 或架构文档时使用，例如阅读 iWiki、结合代码交叉验证、提炼模块边界和重写知识文档。当用户要先理解业务再写文档时优先使用。
---

# 业务知识工作流

## 适用场景

- 需要快速理解一个陌生业务模块
- 需要从 iWiki 或其他文档系统获取背景知识
- 需要把文档描述和实际代码做交叉验证
- 需要把知识沉淀成 skill、架构文档或模块说明

## 不适用场景

- 已经明确知道该修改哪段代码，只是去实现
- 只是压缩已有 skill，不需要重新获取业务知识
- 只是补普通说明，不需要形成复用知识资产

## 快速指导

1. 这个 skill 关注的是“先理解，再验证，最后沉淀”，不是直接改代码。
2. 业务知识获取通常分三段：
   - 文档发现与初筛：`reference/1-document-discovery.md`
   - 代码交叉验证：`reference/2-code-cross-check.md`
   - 沉淀为 skill / 架构文档：`reference/3-skill-distillation.md`
3. 文档内容默认不可信到可以直接复述，必须经过代码或运行链路验证。
4. 沉淀时优先写模块边界、关键对象、主链路和常见坑，而不是复制百科说明。
5. 如果最后产物是 skill，再联动看 `skill-writer`。

## 高信号规则

- 业务文档和代码实现经常不完全一致
- 好的业务知识文档应该能回答“这模块解决什么问题、主链路是什么、改动时先看哪里”
- 沉淀结果要优先服务后续开发和排障，而不是保留原始资料堆积

## 关键陷阱

- 只读文档，不做代码核验
- 把外部资料原样搬运进 skill
- 输出结构没有边界感，导致后续继续膨胀

## 延伸阅读

- 文档发现与初筛：`reference/1-document-discovery.md`
- 代码交叉验证：`reference/2-code-cross-check.md`
- 沉淀为 skill / 架构文档：`reference/3-skill-distillation.md`
- 如果你要写 skill：再看 `skill-writer`
