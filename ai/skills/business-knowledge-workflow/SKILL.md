---
name: business-knowledge-workflow
description: 业务知识获取与 Skill 文档编写工作流。当用户需要熟悉新业务模块、提取API接口文档、生成模块依赖图、创建业务流程图、从 iWiki 获取wiki文档或知识库内容、结合代码进行模块分析生成架构文档、或将业务文档沉淀为 Skill 时使用。
---

# 业务知识获取与 Skill 文档编写工作流

从外部文档（如 iWiki）获取业务知识，结合代码分析，沉淀为高质量 Skill 文档的完整工作流。

## 触发条件

当用户需要：
- 熟悉一个新的业务模块或子系统
- 从 iWiki 获取wiki文档或知识库中的业务文档
- 提取API接口文档并整理为结构化参考
- 生成模块依赖图或创建业务流程图
- 将文档内容与实际代码进行交叉验证（模块分析）
- 将业务知识沉淀为可复用的 Skill

## 阶段 1：文档获取

### 1.1 iWiki 搜索与获取

使用 iWiki MCP 工具搜索并获取文档：

```
mcp__iwiki__searchDocument(query="Buildless 架构设计")
```

根据返回的文档列表，获取具体内容：

```
mcp__iwiki__getDocument(docId="<搜索结果中的文档ID>")
```

**搜索策略**：
| 场景 | 搜索词示例 |
|:-----|:----------|
| 了解模块架构 | `Buildless 架构设计` |
| 了解具体功能 | `Buildless 任务调度` |
| 了解数据模型 | `Buildless Redis 存储` |

### 1.2 构建知识框架

从文档中提取并整理为结构化框架：

- **核心概念**：关键术语与定义
- **架构要点**：组件关系、数据流向
- **待验证问题**：需通过代码确认的内容

> **验证检查点**：确认至少提取了核心概念和架构要点，否则扩大搜索范围。

## 阶段 2：代码验证

### 2.1 定位核心代码

根据文档中的类名、方法名、配置项定位代码：

```
search_content(query="class BuildLessStartDispatcher")
```

```
search_content(query="fun canDispatch")
```

```
search_content(query="dispatch.buildless")
```

按顺序执行：先搜索核心类名找到入口，再搜索关键方法理解流程，最后搜索配置项确认参数。

### 2.2 交叉验证

将文档描述与实际代码对照：

| 文档描述 | 代码验证方式 | 判定 |
|:--------|:-----------|:-----|
| Redis Key 格式 | 检查实际 Key 定义常量 | 一致 / 有差异 |
| 流程步骤 | 检查方法调用链 | 一致 / 有差异 |
| 数据模型 | 检查实体类定义 | 一致 / 有差异 |

文档与代码不一致时，以代码为准，在文档中标注差异：

```markdown
> **注意**：iWiki 文档描述为 xxx，但实际代码实现为 yyy
```

### 2.3 补充代码细节

文档通常缺失以下内容，需从代码中补充：
- 异常处理逻辑
- 边界条件判断
- 性能优化细节
- 依赖注入关系
- 配置默认值

> **验证检查点**：确认所有待验证问题已逐项核对，差异已标注。

## 阶段 3：知识沉淀

### 3.1 文档重构

按以下原则重构文档：

| 原则 | 操作 |
|:-----|:-----|
| 去重 | 相同内容只保留一处 |
| 聚合 | 分散的相关内容合并 |
| 分层 | 概述 → 详情 → 代码 |
| 表格化 | 列表内容转表格 |

目标结构：

```markdown
# 模块名称
## 概述（一句话说明 + 核心特点表格）
## 架构设计（组件图 + 组件表格）
## 核心流程（流程说明 + 阶段表）
## 数据模型（字段表格）
## 配置参考（配置项表格）
## 代码索引（文件路径表格）
```

### 3.2 生成 Skill 文档

按照 `skill-writer` 规范创建 frontmatter：

```yaml
---
name: module-name-architecture
description: 模块描述。当用户需要了解 xxx、开发 xxx 功能时使用。
---
```

内容超长时，将详细参考信息放入 `reference/` 目录。

### 3.3 质量检查清单

- [ ] 无重复内容（同一概念只出现一次）
- [ ] 代码示例精简（只保留关键逻辑）
- [ ] 表格优于长文本（表格占比 ≥ 30%）
- [ ] 篇幅精简率 ≥ 50%（相比原始文档）
- [ ] frontmatter 格式正确且 description 包含触发词
- [ ] 文档与代码一致，差异已标注

> **验证检查点**：逐项核对以上清单，全部通过后方可交付。

## 实战案例：Buildless 模块

**文档获取**：

```
mcp__iwiki__searchDocument(query="Buildless 无编译构建机")
mcp__iwiki__getDocument(docId="<返回的文档ID>")
```

提取核心概念：容器池、任务队列、DeferredResult。

**代码验证**：

```
search_content(query="BuildLessStartDispatcher")
search_content(query="BuildLessTaskResource")
search_content(query="dispatch.buildless")
```

逐个 `read_file` 验证文档描述。

**重构结果**：原文档 ~2100 行精简至 ~500 行，合并重复内容，统一流程描述，表格化配置和索引。

## 相关 Skill

- `skill-writer`：Skill 编写规范
- `00-bkci-global-architecture`：全局架构概览
- 各模块架构 Skill（29-xx 系列）：具体模块参考
- 工具详细参数见 `reference/tool-reference.md`
