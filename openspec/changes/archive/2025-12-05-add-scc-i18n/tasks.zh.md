# 任务清单：添加 SCC 任务和扫描方案的国际化支持

## 概述
为 SCC 任务和扫描方案操作添加缺失的国际化条目。

## 任务列表

### 任务 1：添加中文国际化条目
**文件**：`support-files/i18n/auth/message_zh_CN.properties`  
**位置**：在 `rule_list.actionName` 之后（约第 178 行）  
**优先级**：高  
**预计耗时**：5 分钟

**需要添加的条目**：
```properties
scc_scan_schema_create.actionName=SCC扫描方案创建
scc_scan_schema_list.actionName=SCC扫描方案列表
scc_task_create.actionName=创建SCC任务
scc_task_delete.actionName=删除SCC任务
scc_task_edit.actionName=编辑SCC任务
scc_task_enable.actionName=禁用/启用SCC任务
scc_task_execute.actionName=执行SCC任务
scc_task_list.actionName=SCC任务列表
scc_task_manage.actionName=SCC任务权限管理
scc_task_manage-defect.actionName=管理SCC任务告警
scc_task_view.actionName=查看SCC任务
scc_task_view-defect.actionName=SCC任务告警查看
```

**验证要点**：
- ✅ 中文翻译自然流畅
- ✅ 与现有 `codecc_task_*` 命名风格一致
- ✅ 使用正确的术语（"告警"而非"缺陷"）

---

### 任务 2：添加英文国际化条目
**文件**：`support-files/i18n/auth/message_en_US.properties`  
**位置**：在 `rule_list.actionName` 之后（约第 177 行）  
**优先级**：高  
**预计耗时**：5 分钟

**需要添加的条目**：
```properties
scc_scan_schema_create.actionName=SCC Scan Schema Create
scc_scan_schema_list.actionName=SCC Scan Schema List
scc_task_create.actionName=SCC Task Create
scc_task_delete.actionName=SCC Task Delete
scc_task_edit.actionName=SCC Task Edit
scc_task_enable.actionName=SCC Task Enable
scc_task_execute.actionName=SCC Task Execute
scc_task_list.actionName=SCC Task List
scc_task_manage.actionName=SCC Task Manage
scc_task_manage-defect.actionName=Manage SCC Task Defect
scc_task_view.actionName=SCC Task View
scc_task_view-defect.actionName=View SCC Task Defect
```

**验证要点**：
- ✅ 使用标题大小写（Title Case）
- ✅ 术语一致性（Defect 而非 Alert）
- ✅ 与现有英文条目格式一致

---

### 任务 3：添加日文国际化条目
**文件**：`support-files/i18n/auth/message_ja_JP.properties`  
**位置**：在 `rule_list.actionName` 之后（约第 178 行）  
**优先级**：高  
**预计耗时**：5 分钟

**需要添加的条目**：
```properties
scc_scan_schema_create.actionName=SCC スキャン方案作成
scc_scan_schema_list.actionName=SCC スキャン方案リスト
scc_task_create.actionName=SCC タスクを作成
scc_task_delete.actionName=SCC タスクを削除
scc_task_edit.actionName=SCC タスクを編集
scc_task_enable.actionName=SCC タスクを無効化/有効化
scc_task_execute.actionName=SCC タスクを実行
scc_task_list.actionName=SCC タスクリスト
scc_task_manage.actionName=SCC タスク権限管理
scc_task_manage-defect.actionName=SCC タスクアラートを管理
scc_task_view.actionName=SCC タスクを閲覧
scc_task_view-defect.actionName=SCC タスクアラート閲覧
```

**验证要点**：
- ✅ 使用适当的片假名（カタカナ）和汉字
- ✅ 动词使用日语自然表达（"を作成"、"を閲覧"）
- ✅ 与现有日文条目风格一致

---

### 任务 4：格式和编码验证
**优先级**：中  
**预计耗时**：3 分钟  
**可并行执行**：是

**验证检查清单**：
- [x] **键名格式**：所有键遵循 `{resource}_{action}.actionName` 模式
- [x] **字符编码**：所有文件使用 UTF-8 编码
- [x] **行尾符**：使用 Unix 风格（LF）或 Windows 风格（CRLF），保持与原文件一致
- [x] **无重复键**：确保没有重复的键定义
- [x] **逻辑分组**：条目放置在 `rule_*` 之后，`env_node_*` 之前
- [x] **空格和对齐**：等号前后无多余空格
- [x] **特殊字符**：中文、日文字符正确显示，无乱码

---

### 任务 5：功能验证（可选）
**优先级**：低  
**预计耗时**：10 分钟  
**依赖**：任务 1-3 完成

**验证步骤**：
1. 启动 BK-CI 后端服务
2. 调用 `getRedirectInformation` API，传入 SCC 相关资源类型和操作
3. 验证返回的 `actionName` 字段显示正确的翻译
4. 切换不同语言环境（zh_CN、en_US、ja_JP）重复验证

**预期结果**：
- 中文环境下显示中文操作名称
- 英文环境下显示英文操作名称
- 日文环境下显示日文操作名称

---

## 任务依赖关系图

```
任务 1（中文）  ─┐
任务 2（英文）  ─┼─→ 任务 4（验证格式）─→ 任务 5（功能验证）
任务 3（日文）  ─┘
```

**说明**：
- 任务 1、2、3 可以并行执行
- 任务 4 需要等待任务 1-3 全部完成
- 任务 5 是可选的，用于端到端验证

---

## 完成标准

✅ **定义完成（Definition of Done）**：
- [x] 三个语言文件各添加 12 条国际化条目（共 36 条）
- [x] 所有条目格式正确，无语法错误
- [x] 翻译准确、自然，符合各语言表达习惯
- [x] Git diff 显示清晰，仅修改预期行
- [ ] 通过代码审查
- [ ] 提交信息符合规范：`feat: 添加SCC任务和扫描方案的国际化支持 #<issue号>`

---

## 回滚方案

如果发现问题，执行以下命令回滚：
```bash
git checkout support-files/i18n/auth/message_zh_CN.properties
git checkout support-files/i18n/auth/message_en_US.properties
git checkout support-files/i18n/auth/message_ja_JP.properties
```
