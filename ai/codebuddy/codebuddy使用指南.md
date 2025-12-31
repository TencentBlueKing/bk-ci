# CodeBuddy 使用指南

## 安装 CodeBuddy

前往官网下载并安装：https://copilot.tencent.com/ide/

## 配置项目规则和技能

将本目录下的 `rules/` 和 `skills/` 文件夹复制到项目根目录的 `.codebuddy/` 目录下。

复制后的目录结构：
```
your-project/
├── .codebuddy/
│   ├── rules/           # 编码规范规则
│   └── skills/          # 项目技能文档
└── ...
```

## 目录说明

| 目录 | 作用 |
|------|------|
| `rules/` | 编码规范规则，AI 会自动遵循这些规则生成代码 |
| `skills/` | 项目架构和开发指南，AI 可按需加载理解项目上下文 |
