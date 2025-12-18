---
name: 07-git-commit-specification
description: git提交规范
---

# Git 提交规范

Git 提交规范.

## 触发条件

当用户需要提交代码、编写 commit message、进行代码合并时，使用此 Skill。

## 提交格式

```
标记: 提交描述 #issue编号
```

## 标记类型

| 标记 | 说明 | 示例 |
|------|------|------|
| `feat`/`feature` | 新功能 | `feat: 【PAC模板】流水线模板支持PAC特性 #11414` |
| `bug`/`fix`/`bugfix` | Bug修复 | `bug_12412 stream流水线邮件通知格式异常` |
| `refactor` | 重构（不改变功能） | `refactor: 优化流水线查询性能` |
| `perf` | 性能优化 | `perf: 减少数据库查询次数` |
| `test` | 测试相关 | `test: 添加流水线服务单元测试` |
| `docs` | 文档变更 | `docs: 更新API文档` |
| `merge` | 分支合并 | `merge: 合并develop到master` |
| `depend` | 依赖变更 | `depend: 升级Spring Boot版本` |
| `chore` | 构建脚本 | `chore: 更新Gradle配置` |
| `del` | 破坏性删除 | `del: 移除废弃的API（需特别批准）` |

## 提交规范

### 1. 提交信息格式

```bash
# 标准格式
feat: 添加流水线模板功能 #1234

# 带范围的格式
feat(process): 添加流水线模板功能 #1234

# Bug修复格式
fix: 修复构建日志丢失问题 #5678
```

### 2. PR 提交前准备

```bash
# 使用 rebase 精简 commit
git rebase -i HEAD~5

# 合并多个 commit
pick abc1234 feat: 添加功能A
squash def5678 fix: 修复功能A的bug
squash ghi9012 refactor: 重构功能A

# 同步上游代码
git fetch upstream
git rebase upstream/develop
```

### 3. 分支命名规范

```bash
# 功能分支
feature/pipeline-template-support

# Bug修复分支
bugfix/build-log-missing

# 热修复分支
hotfix/critical-security-issue
```

## 提交检查清单

- [ ] 提交信息符合格式规范
- [ ] 关联了相应的 Issue 编号
- [ ] 代码通过本地测试
- [ ] 使用 rebase 精简了 commit
- [ ] 没有包含敏感信息

## 破坏性变更

使用 `del` 标记时需要：
1. 在设计文档中说明迁移路径
2. 获得项目维护者批准
3. 在 CHANGELOG 中详细记录

## 相关文档

- `docs/specification/commit-spec.md` - 完整提交规范
- `CONTRIBUTING.md` - 贡献指南
