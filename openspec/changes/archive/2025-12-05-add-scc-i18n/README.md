# OpenSpec 提案：添加 SCC 国际化支持

## 📋 文档结构

本提案提供**中英双语**版本：

### 英文版（English）
- 📄 `proposal.md` - 提案概述
- 📋 `tasks.md` - 任务清单
- 📐 `specs/i18n-auth/spec.md` - 规格说明

### 中文版（简体中文）
- 📄 `proposal.zh.md` - 提案概述（中文）
- 📋 `tasks.zh.md` - 任务清单（中文）
- 📐 `specs/i18n-auth/spec.zh.md` - 规格说明（中文）

---

## 🎯 变更概述

**变更 ID**: `add-scc-i18n`  
**类型**: 国际化增强（i18n Enhancement）  
**优先级**: 中（Medium）  
**风险等级**: 低（Low）

### 变更内容
为 SCC（源代码检查）任务和扫描方案添加缺失的国际化条目，共计：
- **2** 个 SCC 扫描方案操作
- **10** 个 SCC 任务操作
- **3** 种语言（中文、英文、日文）
- **36** 条国际化条目（12 × 3）

### 受影响文件
```
support-files/i18n/auth/
├── message_zh_CN.properties  ← 添加 12 条中文条目
├── message_en_US.properties  ← 添加 12 条英文条目
└── message_ja_JP.properties  ← 添加 12 条日文条目
```

---

## 🚀 快速开始

### 1. 审查提案
```bash
# 阅读中文版提案
cat proposal.zh.md

# 或阅读英文版
cat proposal.md
```

### 2. 查看任务清单
```bash
# 中文版任务清单（推荐）
cat tasks.zh.md

# 英文版任务清单
cat tasks.md
```

### 3. 审查规格说明
```bash
# 中文版规格（详细）
cat specs/i18n-auth/spec.zh.md

# 英文版规格
cat specs/i18n-auth/spec.md
```

---

## ✅ 批准流程

### 提案阶段（当前）
- [x] 创建提案文档（proposal.md / proposal.zh.md）
- [x] 编写任务清单（tasks.md / tasks.zh.md）
- [x] 定义规格增量（specs/i18n-auth/spec.md）
- [ ] **等待审批**
- [ ] 解决审批意见

### 实施阶段（待批准后）
- [ ] 修改 `message_zh_CN.properties`
- [ ] 修改 `message_en_US.properties`
- [ ] 修改 `message_ja_JP.properties`
- [ ] 验证格式和编码
- [ ] 提交代码审查
- [ ] 合并到主分支

---

## 📊 变更详情

### 新增的操作键

#### SCC 扫描方案（2个）
| 键名 | 中文 | 英文 | 日文 |
|------|------|------|------|
| `scc_scan_schema_create` | SCC扫描方案创建 | SCC Scan Schema Create | SCC スキャン方案作成 |
| `scc_scan_schema_list` | SCC扫描方案列表 | SCC Scan Schema List | SCC スキャン方案リスト |

#### SCC 任务（10个）
| 键名 | 中文 | 英文 | 日文 |
|------|------|------|------|
| `scc_task_create` | 创建SCC任务 | SCC Task Create | SCC タスクを作成 |
| `scc_task_delete` | 删除SCC任务 | SCC Task Delete | SCC タスクを削除 |
| `scc_task_edit` | 编辑SCC任务 | SCC Task Edit | SCC タスクを編集 |
| `scc_task_enable` | 禁用/启用SCC任务 | SCC Task Enable | SCC タスクを無効化/有効化 |
| `scc_task_execute` | 执行SCC任务 | SCC Task Execute | SCC タスクを実行 |
| `scc_task_list` | SCC任务列表 | SCC Task List | SCC タスクリスト |
| `scc_task_manage` | SCC任务权限管理 | SCC Task Manage | SCC タスク権限管理 |
| `scc_task_view` | 查看SCC任务 | SCC Task View | SCC タスクを閲覧 |
| `scc_task_manage-defect` | 管理SCC任务告警 | Manage SCC Task Defect | SCC タスクアラートを管理 |
| `scc_task_view-defect` | SCC任务告警查看 | View SCC Task Defect | SCC タスクアラート閲覧 |

---

## 🔍 相关代码

### 使用方
```kotlin
// RbacPermissionApplyService.kt (第 489-495 行)
val resourceTypeName = I18nUtil.getCodeLanMessage(
    messageCode = resourceType + AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX,
    defaultMessage = rbacCommonService.getResourceTypeInfo(resourceType).name
)

// 第 550-554 行
actionName = actionInfo?.let {
    I18nUtil.getCodeLanMessage(
        messageCode = "${it.action}$ACTION_NAME_SUFFIX",
        defaultMessage = it.actionName
    )
}
```

### 资源类型定义
```kotlin
// AuthResourceType.kt (第 69-70 行)
SCC_TASK("scc_task"),           // SCC任务
SCC_SCAN_SCHEMA("scc_scan_schema"); // SCC扫描方案
```

---

## 📝 提交规范

合并后的提交信息格式：
```
feat: 添加SCC任务和扫描方案的国际化支持 #<issue号>

- 添加 scc_scan_schema_* 操作的中英日三语翻译
- 添加 scc_task_* 操作的中英日三语翻译
- 共计 36 条国际化条目

Related: #<issue号>
```

---

## 🤝 审批人员

请相关人员审查本提案：

- [ ] **产品负责人**：确认翻译准确性和用户体验
- [ ] **技术负责人**：确认技术实现方案
- [ ] **国际化负责人**：确认多语言翻译质量
- [ ] **代码审查员**：预审查实施计划

---

## 📞 联系方式

如有问题或建议，请：
1. 在本目录添加评论文件（comments.md）
2. 或在对应的 Issue/PR 中讨论
3. 或联系提案发起人

---

**最后更新**: 2025-12-05  
**提案状态**: 等待审批 (Pending Approval)
