---
name: pipeline-variable-management
description: 流水线变量管理完整指南，涵盖变量生命周期（创建、初始化、更新、存储、传递、查询）和变量字段扩展（字段定义、类型扩展、作用域、继承）。当用户开发变量功能、处理变量传递、扩展变量字段或调试变量问题时使用。
---

# 流水线变量管理完整指南

## Skill 概述

**Skill 名称**: Pipeline Variable Management  
**适用场景**: 流水线变量的全生命周期管理与字段扩展  
**重要性**: ⭐⭐⭐⭐ (高优先级)  
**文档版本**: 2.0  
**最后更新**: 2025-01

流水线变量是 BK-CI 流水线中的核心功能，支持在流水线编排、构建执行过程中动态传递和管理数据。本 Skill 提供变量管理的两大核心主题：

1. **变量生命周期** - 变量从创建到销毁的完整流程
2. **变量字段扩展** - 如何扩展变量的数据结构

---

## 一、变量管理架构概览

### 1.1 变量的定义与作用

流水线变量是流水线中用于存储和传递数据的核心机制，支持：

- ✅ **配置参数化** - 将流水线配置中的固定值替换为变量
- ✅ **数据传递** - 在不同 Stage/Job/Task 之间传递数据
- ✅ **动态计算** - 运行时动态生成和更新变量值
- ✅ **用户输入** - 手动触发时由用户输入参数值
- ✅ **系统内置** - 提供系统级别的预定义变量

### 1.2 变量的两种核心模型

BK-CI 中流水线变量存在两种数据模型的双向转换：

#### 1. **BuildFormProperty**（后端内部模型）
- **位置**: `common-pipeline/src/main/kotlin/.../BuildFormProperty.kt`
- **用途**: 微服务内部使用，数据库存储格式
- **特点**: 完整的字段定义，包含 Swagger 注解
- **应用**: Process 模块内部处理、数据持久化

#### 2. **Variable**（YAML 模型）
- **位置**: `common-pipeline-yaml/src/main/kotlin/.../yaml/v3/models/Variable.kt`
- **用途**: YAML 流水线定义，对外 API 交互
- **特点**: Jackson 注解，支持 JSON 序列化
- **应用**: PAC 流水线、OpenAPI 接口

#### 3. **转换器（VariableTransfer）**
- **位置**: `common-pipeline-yaml/src/main/kotlin/.../yaml/transfer/VariableTransfer.kt`
- **职责**: 实现 `BuildFormProperty` ↔ `Variable` 的双向转换
- **核心方法**:
  - `transfer(BuildFormProperty): Variable` - 内部模型 → YAML 模型
  - `transfer(Variable): BuildFormProperty` - YAML 模型 → 内部模型

### 1.3 变量的存储与传递

```
┌─────────────────────────────────────────────────────────────────┐
│                       变量存储与传递架构                          │
└─────────────────────────────────────────────────────────────────┘

  ┌──────────────────┐
  │  流水线配置存储   │  BuildFormProperty
  │  (T_PIPELINE_*) │  存储在 Model JSON 中
  └────────┬─────────┘
           │
           ▼
  ┌──────────────────┐
  │   构建启动       │  读取变量定义
  │  (StartBuild)   │  初始化变量值
  └────────┬─────────┘
           │
           ▼
  ┌──────────────────┐
  │  运行时变量存储   │  T_PIPELINE_BUILD_VAR
  │  (BuildVar)     │  构建级别的变量实例
  └────────┬─────────┘
           │
           ├─────────────────┬─────────────────┐
           ▼                 ▼                 ▼
  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
  │   Stage 1      │ │   Stage 2      │ │   Stage 3      │
  │  继承 + 新增   │ │  继承 + 更新   │ │  继承 + 输出   │
  └────────────────┘ └────────────────┘ └────────────────┘
```

---

## 二、两大核心主题

### 2.1 变量生命周期管理

**覆盖内容：**
- 🔄 变量创建与初始化
- 📦 变量存储机制
- 🔀 变量传递与继承
- 🔄 变量动态更新
- 🔍 变量查询与调试

**典型场景：**
- 理解变量在构建执行过程中的完整流转
- 开发新的变量初始化逻辑
- 调试变量传递问题
- 实现变量的动态更新功能

**查阅文档**: [reference/1-lifecycle.md](reference/1-lifecycle.md) (1414 行)

### 2.2 变量字段扩展

**覆盖内容：**
- 🆕 新增变量字段
- 🔧 字段类型定义
- 🔄 模型转换处理
- 🎯 前后端字段同步

**典型场景：**
- 为变量添加新的属性字段
- 扩展变量类型
- 处理历史数据兼容性
- 同步前后端字段定义

**查阅文档**: [reference/2-extension.md](reference/2-extension.md) (534 行)

---

## 三、使用指南

### 3.1 场景 1：理解变量生命周期

**问题示例：**
- "变量值是如何从流水线配置传递到构建执行的？"
- "插件如何获取和更新变量值？"
- "变量的作用域是如何控制的？"

**操作：** 查阅 [reference/1-lifecycle.md](reference/1-lifecycle.md)

**内容包含：**
1. 变量生命周期全景图
2. 六个关键阶段详解
3. 变量存储表结构
4. 变量传递机制
5. 调试方法与常见问题

### 3.2 场景 2：扩展变量字段

**问题示例：**
- "如何为变量添加一个新的 `allowModify` 字段？"
- "如何扩展变量的可选值列表？"
- "如何确保新字段在 YAML 和内部模型之间正确转换？"

**操作：** 查阅 [reference/2-extension.md](reference/2-extension.md)

**内容包含：**
1. 变量字段扩展完整路径
2. BuildFormProperty 字段定义
3. Variable (YAML) 字段定义
4. VariableTransfer 转换器实现
5. 前端字段同步
6. 测试与验证

---

## 四、核心类与文件速查

### 4.1 变量定义类

| 类名 | 位置 | 用途 |
|------|------|------|
| `BuildFormProperty` | `common-pipeline/.../BuildFormProperty.kt` | 后端内部变量模型 |
| `Variable` | `common-pipeline-yaml/.../Variable.kt` | YAML 变量模型 |
| `VariableTransfer` | `common-pipeline-yaml/.../VariableTransfer.kt` | 模型转换器 |

### 4.2 变量处理服务

| 类名 | 位置 | 职责 |
|------|------|------|
| `PipelineVariableService` | `process/biz-base/.../PipelineVariableService.kt` | 变量业务逻辑 |
| `BuildVariableService` | `process/biz-base/.../BuildVariableService.kt` | 构建变量管理 |
| `VariableAcrossInfoUtil` | `process/biz-base/.../VariableAcrossInfoUtil.kt` | 跨 Job 变量传递 |

### 4.3 数据库表

| 表名 | 用途 |
|------|------|
| `T_PIPELINE_BUILD_VAR` | 构建级别的变量实例（运行时） |
| `T_PIPELINE_SETTING` | 流水线配置（包含变量定义） |
| `T_PIPELINE_MODEL_TASK` | 插件配置（引用变量） |

---

## 五、开发流程

### 5.1 变量功能开发流程

```
1. 明确需求
   ↓
2. 确定涉及的生命周期阶段
   │  → 查阅 reference/1-lifecycle.md
   ↓
3. 是否需要扩展字段？
   ├─ 是 → 查阅 reference/2-extension.md
   │       执行字段扩展流程
   ↓
4. 实现后端逻辑
   │  → 修改 Service/DAO 层
   ↓
5. 前端同步（如需）
   │  → 更新 Vue 组件
   ↓
6. 测试验证
   │  → 单元测试 + 集成测试
   ↓
7. 文档更新
```

### 5.2 常见开发任务

| 任务 | 参考文档 | 关键类 |
|------|----------|--------|
| 新增变量类型 | 2-extension.md | `BuildFormProperty`, `Variable` |
| 变量初始化逻辑 | 1-lifecycle.md | `BuildVariableService` |
| 跨 Job 传递 | 1-lifecycle.md | `VariableAcrossInfoUtil` |
| 变量权限控制 | 1-lifecycle.md | `PipelineVariableService` |
| 变量表达式解析 | - | `utility-components` Skill (reference/2-expression-parser.md) |

---

## 六、与其他 Skill 的关系

### 6.1 依赖的 Skill

- **`pipeline-model-architecture`** - 理解变量在 Model 中的位置
- **`process-module-architecture`** - 理解变量处理的代码架构
- **`utility-components`** (reference/2-expression-parser.md) - 变量引用的表达式解析

### 6.2 被引用的场景

- **PAC 流水线** → 变量在 YAML 中的定义
- **流水线编排** → 变量的创建和配置
- **构建执行** → 变量的初始化和传递
- **插件开发** → 插件如何读取和更新变量

---

## 七、详细文档导航

| 文档 | 内容 | 行数 | 文件大小 |
|------|------|------|----------|
| [1-lifecycle.md](reference/1-lifecycle.md) | 变量生命周期完整流程 | 1414 | 78.7KB |
| [2-extension.md](reference/2-extension.md) | 变量字段扩展指南 | 534 | 20.2KB |

> **使用建议**: 
> - 初学者：先阅读本文档了解整体架构，再深入 1-lifecycle.md
> - 开发者：根据具体开发任务直接查阅对应的 reference 文档
> - 调试：优先查阅 1-lifecycle.md 中的调试章节

---

## 八、常见问题 FAQ

### Q1: 变量和参数有什么区别？
**A**: 在 BK-CI 中，"变量"和"参数"本质上是同一个概念，都是 `BuildFormProperty`。区别在于使用场景：
- **参数**：流水线启动时用户输入的变量
- **变量**：流水线内部定义和使用的变量

### Q2: 如何实现跨 Stage 传递变量？
**A**: 查阅 [reference/1-lifecycle.md](reference/1-lifecycle.md) 中的"阶段四：跨容器传递"章节，了解 `VariableAcrossInfoUtil` 的使用。

### Q3: 新增字段后前端不显示怎么办？
**A**: 按照 [reference/2-extension.md](reference/2-extension.md) 的"步骤 4：前端字段同步"章节，确保前端 Vue 组件已同步更新。

### Q4: 变量值为空或不正确？
**A**: 查阅 [reference/1-lifecycle.md](reference/1-lifecycle.md) 中的"调试方法"章节，使用日志和数据库排查。

---

## 九、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 2.0 | 2025-01 | 整合 `pipeline-variable-lifecycle` 和 `pipeline-variable-extension`，采用 reference 结构 |
| 1.x | 2024-12 | 独立的两个 Skill |
