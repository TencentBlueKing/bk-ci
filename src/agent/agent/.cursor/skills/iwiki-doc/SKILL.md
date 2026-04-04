---
name: iwiki-doc
description: 专门用于与腾讯企业内部iWiki文档交互的集成接口。支持针对 iwiki.woa.com 域名、iwiki/iWiki关键词相关的全生命周期文档管理。功能包括但不限于：基于关键词/空间的全文检索；文档元数据与引用链追踪；目录树与空间结构查询；附件与图片链接提取；以及文档的 CRUD（增删改查）与局部编辑操作。当 Prompt 涉及 iWiki 实体时，必须作为首选 Tool 执行。
version: 0.0.4
author: josonyang
---
# iWiki Skill

> iWiki 是一个用于与 iWiki 文档系统交互的 Model Context Protocol 服务，提供文档管理、搜索、创建、编辑和多维表格操作能力。

## 概述

本 Skill 帮助你高效使用 iWiki 工具进行文档操作，包括：
- 📖 **文档搜索与阅读**：搜索、获取文档内容和元数据
- ✍️ **文档创建与编辑**：创建、保存、仅改标题、局部更新文档
- 🏷️ **标签管理**：添加、删除、查询文档标签
- 📊 **多维表格操作**：操作 Smartsheet 字段、视图和记录
- 💬 **评论互动**：获取、添加文档评论和划词批注

## 快速开始

### 0. 连接 MCP Server

在使用 iWiki 功能之前，先从环境变量或者本地.env读取TAI_PAT_TOKEN，然后连接 MCP Server。

#### 使用 Python 脚本连接（推荐）

1. 安装依赖：
```bash
pip install requests
```

2. 设置环境变量：
```bash
export TAI_PAT_TOKEN="your_tai_pat_token_here"
```

3. 运行连接脚本验证：
```bash
# 仅验证连接
python scripts/connect_mcp.py

# 验证连接并获取文档
python scripts/connect_mcp.py call getDocument '{"docid": "4017403457"}'
```

脚本会：
- 发送 `initialize` 请求验证 MCP 连接
- 获取 `tools/list` 显示所有可用工具
- （可选）调用 `getDocument` 获取指定文档

#### 认证方式

所有请求需要在 Header 中携带 Bearer Token：
```
Authorization: Bearer TAI_PAT_TOKEN
```

### 0.5 导入文档（无需连接 MCP Server）

文件导入功能通过独立的 HTTP 端点 `POST /import` 实现，**不需要先连接 MCP Server**（无需 `initialize` 和 `tools/list` 步骤），可直接调用。

#### 支持的文件类型
- Markdown 文件（`.md`）
- Word 文档（`.docx`）
- 包含.md 和.docx 的 zip 压缩包(支持包含附件)

#### 使用限制
- 不支持导入到需要审批的文档目录
- 默认覆盖同名文档

#### 命令行导入
```bash
# 1. 设置环境变量
export TAI_PAT_TOKEN="your_tai_pat_token_here"

# 2. 上传 Markdown 文件到指定父目录
python scripts/connect_mcp.py upload ./doc.md 4017403457

# 指定任务类型
python scripts/connect_mcp.py upload ./doc.docx 4017403457 --task-type md_import

# 不覆盖同名文档
python scripts/connect_mcp.py upload ./doc.md 4017403457 --no-cover
```

#### Python 代码调用
```python
from scripts.connect_mcp import MCPClient

client = MCPClient(token="your_tai_pat_token")
result = client.upload_file(
    file_path="./doc.md",
    parent_id=4017403457,
    task_type="md_import",  # 导入任务类型，默认 md_import
    cover=True,             # 是否覆盖同名文档，默认 True
)
print(result)  # {"success": True, "msg": "导入成功", "data": [...]}
```

#### 导入参数说明

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| file_path | string | ✅ | 本地文件路径 |
| parent_id | number | ✅ | 父文档/目录 ID，文件将导入到该目录下 |
| task_type | string | ❌ | 导入任务类型，默认 `md_import` |
| cover | boolean | ❌ | 是否覆盖同名文档，默认 `true` |

#### 内部流程
导入操作在服务端自动完成以下步骤：
1. 获取预签名 URL
2. 上传文件到 COS 对象存储
3. 启动导入任务
4. 轮询等待导入完成（最多 180 秒）

### 1. 查找文档
优先使用 `searchDocument` 进行传统关键词搜索（支持按类型、空间、标签、作者筛选，也支持通过 `offset` 做结果翻页）。
当传统搜索无结果、查询词为句子时，再使用 `aiSearchDocument` 进行 AI 语义搜索（推荐）：
```
搜索关键词: "项目需求文档"
offset: 20   # 跳过前 20 条结果，获取下一页
```

### 2. 读取文档

获取到 docid 后，使用 `getDocument` 获取完整内容：
```
docid: "123456"
```

### 3. 创建文档

使用 `createDocument` 创建新文档：
```
spaceid: 12345         # 空间ID
parentid: 67890        # 父文档ID
title: "新文档标题"
body: "文档内容..."
contenttype: "MD"      # MD/DOC/FOLDER/VIKA
is_html: false         # false=MarkdownTransformer，true=HtmlTransformer
```

`createDocument` 会先将 `body` 转成 JSON 后再写入：
- `is_html=true`：通过 `HtmlTransformer` 转换
- `is_html=false`：通过 `MarkdownTransformer` 转换
- `body_mode` 为兼容旧参数保留，通常不需要再传

## 核心工具列表

### 🔍 搜索类工具

| 工具名 | 用途 | 何时使用 |
|--------|------|----------|
| `searchDocument` | 文档搜索 | **首选**，需要按空间/标签/作者筛选，或通过 `offset` 翻页时 |
| `aiSearchDocument` | AI 语义搜索 | 当需要查找文档内容时 |
| `glossaryTermSearch` | 词条搜索 | 搜索词库中的术语定义 |
| `glossaryTermExactSearch` | 词条精确搜索（忽略大小写） | 精确搜索（忽略大小写）词库中的术语定义 |
| `glossaryBatchExactSearch` | 批量精确搜索词条（带权限） | 需要同时精确搜索多个词条（最多100个），只返回当前用户有权查看的词库中的词条 |
| `addGlossary` | 创建/更新词条 | 需要新建或更新词条时（仅在 gray scope 下可见） |

### 📄 文档读取工具

| 工具名 | 用途 | 何时使用 |
|--------|------|----------|
| `getDocument` | 获取完整内容 | 需要阅读/分析文档时 |
| `metadata` | 获取元数据 | 需要了解作者/时间等信息时 |
| `getSpacePageTree` | 获取目录树 | 需要浏览文档结构时 |
| `listImages` | 获取图片列表 | 需要提取文档中的图片 |
| `getAttachmentDownloadUrl` | 获取附件链接 | 下载附件或图片时 |
| `getDocQuoteList` | 获取文档引用的其他文档 | 需要了解文档的引用来源时 |
| `getDocQuoteListBy` | 获取引用了该文档的其他文档 | 需要分析文档被引用情况时 |

### ✏️ 文档写入工具

| 工具名 | 用途 | 何时使用 |
|--------|------|----------|
| `createDocument` | 创建文档 | 需要新建文档时 |
| `saveDocument` | 完整保存 | **推荐**，更新整个文档内容时 |
| `renameDocumentTitle` | 仅修改标题 | 只需要重命名文档标题、且不改正文时 |
| `saveDocumentParts` | 局部更新 | 在文档头/尾追加内容时,只在用户明确的时候使用 |
| `moveDocument` | 移动文档 | 调整文档位置时 |
| `copyDocument` | 复制文档 | 需要复制文档或文档树时 |

### 🏷️ 标签工具

| 工具名 | 用途 |
|--------|------|
| `getDocumentTags` | 获取文档标签 |
| `addDocumentTags` | 批量添加标签 |
| `deleteDocumentTag` | 删除单个标签 |

### 💬 评论工具

| 工具名 | 用途 |
|--------|------|
| `getComments` | 获取评论（支持分页） |
| `getInlineComment` | 获取划词批注（内联评论），返回id、content、creator、reply_to、mark_content、is_deleted |
| `addComment` | 添加评论/回复 |

### 📊 多维表格（Smartsheet）工具

| 工具名 | 用途 |
|--------|------|
| `smartsheetGetFields` | 获取表格字段结构 |
| `smartsheetAddField` | 添加新字段 |
| `smartsheetDeleteField` | 删除字段 |
| `smartsheetGetViews` | 获取视图列表 |
| `smartsheetGetRecords` | 查询记录（支持筛选/排序） |
| `smartsheetAddRecords` | 批量添加记录 |
| `smartsheetUpdateRecords` | 批量更新记录 |
| `smartsheetDeleteRecords` | 批量删除记录 |

### 📥 文档导入工具

| 工具名 | 用途 | 何时使用 |
|--------|------|----------|
| `upload`（命令行） | 上传文件导入到 iWiki | 需要将本地文件导入为 iWiki 文档时，**无需连接 MCP Server** |

### 🏠 空间管理工具

| 工具名 | 用途 |
|--------|------|
| `getSpaceInfoByKey` | 根据 Key 查询空间 |
| `getSpaceInfoByName` | 根据名称查询空间 |
| `getFavoriteSpaces` | 获取收藏的空间 |
| `getManageSpaces` | 获取管理的空间 |

## 使用规范

### ⚠️ 重要约定

1. **搜索优先**
   - 当问题是"某个词是什么意思时"，优先使用 `glossaryTermSearch` 或 `glossaryTermExactSearch`
   - 当需要批量查询多个词条时，使用 `glossaryBatchExactSearch`（最多100个关键词）
   - 首先使用 `searchDocument` 或 `aiSearchDocument` 找到目标文档
   - 当需要翻页或跳过前面结果时，优先给 `searchDocument` 传 `offset`
   - 获取 docid 后再进行其他操作

2. **确认空间 ID**
   - 创建文档前必须确认 `spaceid` 和 `parentid`
   - 使用 `getSpaceInfoByKey` 或 `getSpaceInfoByName` 获取空间信息
   - 当空间名包含空格、斜杠或其他特殊字符时，优先直接传原始空间名，让工具自动编码处理

3. **内容非空检查**
   - `createDocument` 和 `saveDocument` 的 body 参数**不能为空**
   - 仅修改标题时应使用 `renameDocumentTitle`，不要给 `saveDocument` 传空 body
   - MCP 服务会拒绝空内容的写入

4. **审批文档限制**
   - 需要审批的文档**不支持**创建/更新/移动/复制
   - 操作前会自动检查，如遇到审批文档会返回提示

5. **文档复制说明**
   - 使用 `copyDocument` 复制文档到新位置
   - `is_single=1` 只复制单个文档，`is_single=0` 复制整个文档树（包含所有子文档）
   - 复制操作会返回任务ID，可用于查询复制进度
   - 复制的文档不会保留原文档的评论和标签

6. **更新删除约定**
   - 仅修改标题时使用 `renameDocumentTitle`
   - 追加内容时优先使用 `saveDocumentParts`（在头部/尾部追加）
   - 全量更新使用 `saveDocument`
   - 所有的高危操作(如：`deleteDocumentTag`, `moveDocument`, `smartsheetDeleteField`,`smartsheetDeleteRecords`)都需要用户二次确认

7. **内容格式**
    - `createDocument` 与 `saveDocument` 均通过 `is_html` 决定正文转换方式：`true` 使用 `HtmlTransformer`，`false` 使用 `MarkdownTransformer`
    - 当传 Markdown 内容时，建议显式传 `is_html: false`
    - 当传 HTML/XHTML 富文本内容时，建议显式传 `is_html: true`
    - `body_mode` 为兼容旧参数保留；当前正文会先转换为 JSON，通常无需再依赖 `body_mode`
    - 添加评论时内容时，使用XHTML格式
    - XHTML基本元素：，-, , , , , ,<p><h1><h6><strong><em><code><ul><ol><li>，表格：/ 结构<table><tbody><tr><th><td>，内容一定要用合适的XHTML标签包装

8. **文件限制**
    - 在创建、更新等使用场景下，先创建临时文本文件，然后执行完成后，再删除掉这个临时文本文件
    - 在同步一个仓库的md到iWiki时，先找出md里包含的附件，并将附件与md一起打包成临时的zip文件，然后再导入操作

9. **Drawio SVG 图片处理规范**
    - 当 Markdown 文件中引用了本地的 drawio 类型图片（通常为 `.drawio.svg` 或由 drawio 导出的 `.svg` 文件）时，需要**先询问用户**确认以下信息：
      - **哪些图片是 drawio 类型**：用户需明确指出哪些 `.svg` 图片属于 drawio 类型
      - **长度和宽度（可选）**：用户可选择性地提供每张 drawio 图片的 `width` 和 `height` 数据
    - 确认后，在 Markdown 中对 drawio 类型的图片路径追加 GET 参数来标识类型和尺寸信息：
      - **仅标识 drawio 类型**（用户未提供长宽）：在图片路径后追加 `?type=drawio_svg`
        ```markdown
        ![架构图](./images/architecture.drawio.svg?type=drawio_svg)
        ```
      - **同时指定长宽数据**（用户提供了宽高）：追加 `?type=drawio_svg&width=值&height=值`
        ```markdown
        ![架构图](./images/architecture.drawio.svg?type=drawio_svg&width=800&height=600)
        ```
    - 参数说明：
      | 参数 | 是否必填 | 描述 |
      |------|---------|------|
      | `type` | ✅ 必填 | 图片类型标识，drawio 图片固定为 `drawio_svg` |
      | `width` | ❌ 可选 | 图片显示宽度（像素），由用户指定 |
      | `height` | ❌ 可选 | 图片显示高度（像素），由用户指定 |
    - 非 drawio 类型的 SVG 图片和其他格式图片不受此规则影响，仍使用标准 Markdown 语法
    - 示例：一个 md 文件中同时包含 drawio 图片和普通图片
      ```markdown
      # 系统设计文档

      普通截图：
      ![截图](./images/screenshot.png)

      普通 SVG：
      ![图标](./images/logo.svg)

      Drawio 架构图（无尺寸）：
      ![架构图](./images/architecture.drawio.svg?type=drawio_svg)

      Drawio 流程图（指定尺寸）：
      ![流程图](./images/flow.drawio.svg?type=drawio_svg&width=1000&height=750)
      ```

### 📝 URL 格式说明

| URL 类型 | 示例 | 说明 |
|----------|------|------|
| 个人空间 | `https://iwiki.woa.com/space/~myname` | Key 为 `~myname` |
| 普通空间 | `https://iwiki.woa.com/space/devcloud` | Key 为 `devcloud` |
| 文档页面 | `https://iwiki.woa.com/p/123456` | docid 为 `123456` |
| 专题 | `https://iwiki.woa.com/topic/1232323` | topic_id 为 `1232323` |

### 📊 多维表格操作流程

1. **了解表格结构**
   ```
   smartsheetGetFields(doc_id) -> 获取所有字段定义
   smartsheetGetViews(doc_id) -> 获取视图列表
   ```

2. **查询数据**
   ```
   smartsheetGetRecords(doc_id, {
     pageNum: 1,
     pageSize: 100,
     filterByFormula: "条件表达式",
     fields: "字段1,字段2"
   })
   ```

3. **写入数据**
   ```
   smartsheetAddRecords(doc_id, fieldKey: "name", records: [
     { fields: { "字段名": "值" } }
   ])
   ```

### 支持的字段类型

```
SingleText, Text, SingleSelect, MultiSelect, Number, Currency,
Percent, DateTime, Attachment, Member, Checkbox, Rating, URL,
Phone, Email, WorkDoc, OneWayLink, TwoWayLink, MagicLookUp,
Formula, AutoNumber, CreatedTime, LastModifiedTime, CreatedBy,
LastModifiedBy, Button
```

### 支持的视图类型

```
Grid（表格）, Gallery（画廊）, Kanban（看板）,
Gantt（甘特图）, Calendar（日历）, Architecture（架构）
```

## 常见场景

### 场景 1：搜索并阅读文档

```mermaid
graph LR
    A[searchDocument] --> B{找到文档?}
    B -->|是| C[getDocument]
    B -->|否| D[aiSearchDocument]
    D --> E{找到文档?}
    E -->|是| C
    E -->|否| F[尝试其他关键词或调整 offset]
    C --> G[阅读/分析内容]
```

### 场景 2：创建新文档

```mermaid
graph LR
    A[确定目标空间] --> B[getSpaceInfoByKey]
    B --> C[获取 spaceid]
    C --> D[确定父目录 parentid]
    D --> E[createDocument]
```

### 场景 3：更新文档

```mermaid
graph LR
    A{更新范围}
    A -->|仅改标题| B[renameDocumentTitle]
    A -->|追加内容| C[saveDocumentParts]
    A -->|全量更新| D[saveDocument]
    B --> E[提供 id 和 new_title]
    C --> F[指定 before/after]
    D --> G[提供完整 body]
```

### 场景 3.5：复制文档

```mermaid
graph LR
    A[确定源文档ID] --> B[确定目标父目录ID]
    B --> C{复制范围}
    C -->|单个文档| D[copyDocument is_single=1]
    C -->|整个文档树| E[copyDocument is_single=0]
    D --> F[返回任务ID]
    E --> F
```

### 场景 4：导入本地文件（无需连接 MCP）

```mermaid
graph LR
    A[准备本地文件] --> B[确定父目录 parent_id]
    B --> C[upload 命令上传]
    C --> D{导入成功?}
    D -->|是| E[文档已创建]
    D -->|否| F[检查文件/权限]
```

### 场景 5：操作多维表格

```mermaid
graph LR
    A[smartsheetGetFields] --> B[了解字段结构]
    B --> C{操作类型}
    C -->|查询| D[smartsheetGetRecords]
    C -->|新增| E[smartsheetAddRecords]
    C -->|更新| F[smartsheetUpdateRecords]
    C -->|删除| G[smartsheetDeleteRecords]
```

## 错误处理

| 错误信息 | 原因 | 解决方案 |
|----------|------|----------|
| `body 为空值` | 内容参数为空 | 确保传入有效的文档内容 |
| `MCP不支持审批流程` | 文档需要审批 | 手动在 iWiki 网页操作 |

## 参考资料

- [API 参考文档](./references/api_reference.md)
