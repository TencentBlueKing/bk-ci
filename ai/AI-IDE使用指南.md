# AI IDE 使用指南

本项目提供了 AI 编码助手的配置文件，支持主流 AI IDE 工具。

## 支持的 AI IDE

| IDE | 官网 | 配置目录 |
|-----|------|----------|
| CodeBuddy | https://example.copilot.tencent.com/ide/ | `.codebuddy/` |
| Cursor | https://cursor.sh/ | `.cursor/` |
| Claude Code | https://claude.com/download | `.claude/` |

## 配置方法

### CodeBuddy

将 `rules/` 和 `skills/` 复制到项目根目录的 `.codebuddy/` 下：

```
your-project/
├── .codebuddy/
│   ├── rules/           # 编码规范规则 (.mdc 文件)
│   └── skills/          # 项目技能文档（按需加载）
├── CODEBUDDY.md         # 项目全局配置文件（自动加载）
```

### Cursor

将 `rules/` 和 `skills/` 复制到项目根目录的 `.cursor/` 下：

```
your-project/
├── .cursor/
│   ├── rules/           # 编码规范规则 (.mdc 文件)
│   └── skills/          # 项目技能文档（按需加载）
├── .cursorrules         # 项目全局规则文件（自动加载）
```


### Claude Code

将 `rules/` 和 `skills/` 复制到项目根目录的 `.claude/` 下：

```
your-project/
├── .claude/
│   ├── rules/        # 编码规范规则 (.mdc 文件)
│   └── skills/       # 项目技能文档（按需加载）
├── CLAUDE.md            # 项目全局配置文件
├── CLAUDE.local.md      # 个人本地覆盖配置，通常加入 .gitignore 避免影响他人
```


## 功能对照表

| 功能       | CodeBuddy | Cursor | Claude Code              |
|----------|-----------|--------|--------------------------|
| 编码规则     | `.codebuddy/rules/*.mdc` | `.cursor/rules/*.mdc` | `CLAUDE.md` 或 `.claude/rules` |
| 项目级全局配置  | `CODEBUDDY.md` | `.cursorrules` | `CLAUDE.md`              |
| 自定义技能 | `.codebuddy/skills/` | `.cursor/skills/` | `.claude/skills/`      |
| 个人本地覆盖配置     | - | - | `CLAUDE.local.md`        |

## 目录说明

| 目录 | 作用 |
|------|------|
| `rules/` | 编码规范规则，AI 会自动遵循这些规则生成代码 |
| `skills/` | 项目架构和开发指南，AI 可按需加载理解项目上下文 |

## 注意事项

1. 不同 AI IDE 的配置格式可能略有差异，请根据实际情况调整
2. 建议将 AI 配置目录加入版本控制，方便团队共享
