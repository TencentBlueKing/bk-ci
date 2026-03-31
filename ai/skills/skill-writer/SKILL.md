---
name: skill-writer
description: 指导用户创建 Agent Skills（支持 CodeBuddy、Cursor 等 IDE）。当用户想要创建、编写、设计新的 Skill，或需要帮助编写 SKILL.md 文件、frontmatter、skill 结构时使用。
---

# Skill Writer

本 Skill 帮助你创建结构良好、符合最佳实践和验证要求的 Agent Skills，适用于 CodeBuddy、Cursor 等支持 Skill 系统的 IDE。

## 触发条件

当用户需要：
- 创建新的 Agent Skill
- 编写或更新 SKILL.md 文件
- 设计 skill 结构和 frontmatter
- 排查 skill 发现问题
- 将现有提示词或工作流转换为 Skills

## 指引

### 第一步：确定 Skill 范围

首先，理解 Skill 应该做什么：

1. **询问澄清问题**：
   - 这个 Skill 应该提供什么具体能力？
   - 什么时候应该使用这个 Skill？
   - 它需要什么工具或资源？
   - 是个人使用还是团队共享？

2. **保持聚焦**：一个 Skill = 一个能力
   - 好的：「PDF 表单填写」、「Excel 数据分析」、「流水线模板管理」
   - 太宽泛：「文档处理」、「数据工具」

### 第二步：选择 Skill 位置

确定在哪里创建 Skill：

**项目级 Skills**（`.codebuddy/skills/` 或 `.cursor/skills/`）：
- 团队工作流和约定
- 项目特定的专业知识
- 共享工具（提交到 git）
- **推荐用于团队协作项目**

**用户级 Skills**（`~/.codebuddy/skills/` 或 `~/.cursor/skills/`）：
- 个人工作流和偏好
- 实验性 Skills
- 个人生产力工具

> **注意**: 不同 IDE 使用不同的目录名：
> - **CodeBuddy**: `.codebuddy/` 或 `~/.codebuddy/`
> - **Cursor**: `.cursor/` 或 `~/.cursor/`
> - 其他 IDE 可能使用不同的目录结构

### 第三步：创建 Skill 结构

创建目录和文件：

```bash
# 项目级（推荐）- CodeBuddy
mkdir -p .codebuddy/skills/skill-name

# 项目级（推荐）- Cursor
mkdir -p .cursor/skills/skill-name

# 用户级 - CodeBuddy
mkdir -p ~/.codebuddy/skills/skill-name

# 用户级 - Cursor
mkdir -p ~/.cursor/skills/skill-name
```

**基本结构**：
```
skill-name/
├── SKILL.md          # 必需 - 主文件
├── reference/        # 可选 - 参考文档目录
│   ├── api-docs.md
│   └── examples.md
├── scripts/          # 可选 - 辅助脚本
│   └── helper.py
└── config.json       # 可选 - 配置文件
```

### 第四步：编写 SKILL.md frontmatter

创建包含必需字段的 YAML frontmatter：

```yaml
---
name: skill-name
description: 简要描述这个 Skill 做什么以及何时使用它
---
```

**字段要求**：

- **name**：
  - 仅允许小写字母、数字、连字符
  - 最多 64 个字符
  - 必须与目录名匹配
  - 好的：`pdf-processor`、`git-commit-helper`、`pipeline-manager`
  - 不好的：`PDF_Processor`、`Git Commits!`

- **description**：
  - 最多 1024 个字符
  - 同时包含「做什么」和「何时使用」
  - 使用用户可能会说的具体触发词
  - 提及文件类型、操作和上下文

### 第五步：编写有效的描述

描述对于 AI 助手发现你的 Skill 至关重要。

**公式**：`[做什么] + [何时使用] + [关键触发词]`

**示例**：

✅ **好的**：
```yaml
description: 管理蓝盾流水线的构建操作，包括查询构建历史、获取启动参数、查看构建状态、启动构建。当用户提及流水线、构建、部署、CI/CD、蓝盾或需要触发构建任务时使用。
```

✅ **好的**：
```yaml
description: 后端微服务开发规范，包括目录结构、分层架构、依赖注入、配置管理。当用户进行后端开发、创建新服务、编写 API 时使用。
```

❌ **太模糊**：
```yaml
description: 帮助处理文档
description: 用于数据分析
```

**技巧**：
- 包含具体文件扩展名（.pdf、.xlsx、.json）
- 提及常见用户短语（「分析」、「提取」、「生成」、「创建」）
- 列出具体操作（而非泛泛的动词）
- 添加上下文线索（「当用户...时使用」）

### 第六步：组织 Skill 内容

使用清晰的 Markdown 结构：

```markdown
# Skill 名称

简要概述这个 Skill 做什么。

## 触发条件

明确说明何时应该使用这个 Skill。

## 核心概念

解释关键术语和概念。

## 常用工作流

### 工作流 1：xxx

```
步骤 1：xxx
步骤 2：xxx
步骤 3：xxx
```

## 快速示例

展示具体的使用示例。

## 最佳实践

- 要遵循的关键约定
- 要避免的常见陷阱

## 工具参考

如有额外文档，链接到 reference/ 目录：
- 详细 API 文档：参阅 [reference/api-docs.md](reference/api-docs.md)
- 更多示例：参阅 [reference/examples.md](reference/examples.md)
```

### 第七步：添加支持文件（可选）

为渐进式披露创建额外文件：

**reference/** 目录：详细 API 文档、高级选项
**scripts/** 目录：辅助脚本和工具
**config.json**：配置数据（如常用流水线列表）

从 SKILL.md 引用它们：
```markdown
详细用法请参阅 [reference/api-docs.md](reference/api-docs.md)。

运行辅助脚本：
\`\`\`bash
python scripts/helper.py input.txt
\`\`\`
```

### 第八步：验证 Skill

检查以下要求：

✅ **文件结构**：
- [ ] SKILL.md 存在于正确位置
- [ ] 目录名与 frontmatter 中的 `name` 匹配

✅ **YAML frontmatter**：
- [ ] 第一行是 `---`
- [ ] 内容前有闭合的 `---`
- [ ] 有效的 YAML（无制表符，正确缩进）
- [ ] `name` 遵循命名规则
- [ ] `description` 具体且少于 1024 字符

✅ **内容质量**：
- [ ] 为 AI 助手提供清晰的指令
- [ ] 提供具体示例
- [ ] 处理边缘情况
- [ ] 列出依赖项（如有）

✅ **测试**：
- [ ] 描述与用户问题匹配
- [ ] Skill 在相关查询时激活
- [ ] 指令清晰可操作

### 第九步：测试 Skill

1. **重启 IDE**（如正在运行）以加载 Skill

2. **提出相关问题**，匹配描述：
   ```
   帮我管理流水线构建
   ```

3. **验证激活**：AI 助手应自动使用该 Skill

4. **检查行为**：确认 AI 助手正确遵循指令

### 第十步：调试（如需要）

如果 AI 助手没有使用 Skill：

1. **使描述更具体**：
   - 添加触发词
   - 包含文件类型
   - 提及常见用户短语

2. **检查文件位置**：
   ```bash
   # CodeBuddy
   ls .codebuddy/skills/skill-name/SKILL.md
   ls ~/.codebuddy/skills/skill-name/SKILL.md
   
   # Cursor
   ls .cursor/skills/skill-name/SKILL.md
   ls ~/.cursor/skills/skill-name/SKILL.md
   ```

3. **验证 YAML**：
   ```bash
   cat SKILL.md | head -n 10
   ```

## 常见模式

### 开发规范类 Skill

```yaml
---
name: backend-development
description: 后端微服务开发规范，包括目录结构、分层架构、依赖注入。当用户进行后端开发、创建新服务时使用。
---

# 后端微服务开发

## 触发条件

当用户需要创建新服务、编写后端代码、设计 API 时使用。

## 目录结构

\`\`\`
service-name/
├── api/
├── biz/
└── boot/
\`\`\`

## 最佳实践

1. 遵循分层架构
2. 使用依赖注入
3. 编写单元测试
```

### 工具集成类 Skill

```yaml
---
name: pipeline-manager
description: 管理蓝盾流水线的构建操作。当用户提及流水线、构建、部署、CI/CD 时使用。
---

# 流水线管理

通过 MCP 工具管理流水线。

## 核心概念

- **projectId**：项目英文名
- **pipelineId**：流水线 ID

## 常用工作流

### 启动构建

\`\`\`
步骤 1：获取启动参数
步骤 2：向用户确认
步骤 3：启动构建
\`\`\`
```

### 多文件渐进式披露 Skill

```yaml
---
name: api-designer
description: 设计 REST API，遵循最佳实践。当用户创建 API 端点、设计路由或规划 API 架构时使用。
---

# API 设计器

快速入门：参阅 [reference/examples.md](reference/examples.md)

详细参考：参阅 [reference/api-docs.md](reference/api-docs.md)

## 指引

1. 收集需求
2. 设计端点
3. 使用 OpenAPI 规范文档化
4. 根据最佳实践审查
```

## Skill 作者最佳实践

1. **一个 Skill，一个目的**：不要创建超大 Skill
2. **具体的描述**：包含用户会说的触发词
3. **清晰的指令**：为 AI 助手编写，而非人类
4. **具体的示例**：展示真实代码，而非伪代码
5. **列出依赖**：在描述中提及所需包
6. **与团队测试**：验证激活和清晰度
7. **版本控制**：在内容中记录变更
8. **渐进式披露**：将高级细节放在单独文件中

## 验证清单

完成 Skill 前，验证：

- [ ] 名称是小写、仅连字符、最多 64 字符
- [ ] 描述具体且少于 1024 字符
- [ ] 描述包含「什么」和「何时」
- [ ] YAML frontmatter 有效
- [ ] 指令是分步的
- [ ] 示例具体且现实
- [ ] 依赖项已记录
- [ ] 文件路径使用正斜杠
- [ ] Skill 在相关查询时激活
- [ ] AI 助手正确遵循指令

## 故障排除

**Skill 不激活**：
- 使描述更具体，添加触发词
- 在描述中包含文件类型和操作
- 添加「当用户...时使用」子句

**多个 Skill 冲突**：
- 使描述更加不同
- 使用不同的触发词
- 缩小每个 Skill 的范围

**Skill 有错误**：
- 检查 YAML 语法（无制表符，正确缩进）
- 验证文件路径（使用正斜杠）
- 确保脚本有执行权限
- 列出所有依赖项

## 示例参考

查看项目中现有的 Skills 作为参考：
- 简单单文件 Skill：`git-commit-specification`
- 带工具集成的 Skill：`managing-devops-pipeline`
- 多文件 Skill：`process-module-architecture` 系列

## 输出格式

创建 Skill 时，我将：

1. 询问关于范围和需求的澄清问题
2. 建议 Skill 名称和位置
3. 创建带有正确 frontmatter 的 SKILL.md 文件
4. 包含清晰的指令和示例
5. 根据需要添加支持文件
6. 提供测试说明
7. 根据所有要求进行验证

结果将是一个完整、可用的 Skill，遵循所有最佳实践和验证规则。
