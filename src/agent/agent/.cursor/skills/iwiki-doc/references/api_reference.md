# iWiki MCP API 参考文档

本文档详细说明 iWiki MCP 提供的所有工具 API 的参数和返回值。

## 调用约定

### 工具可见性

MCP 服务会根据请求头动态过滤工具：

- `x-scope: basic`：仅基础文档工具
- `x-scope: advanced`：仅高级工具（包含 `aiSearchDocument`、`glossaryTermExactSearch`、`glossaryBatchExactSearch`）
- `x-scope: smartsheet`：仅多维表格工具
- `x-scope: all`：默认全部常规工具
- `x-scope: gray`：灰度工具（当前主要是 `addGlossary`）
- `x-scope` 支持逗号组合，例如 `basic,smartsheet`
- `x-readonly: true`：禁用所有写操作工具

注意：`gray` 工具默认不会暴露，只有显式指定相关 scope 时才可见。

### 写入类工具的运行时限制

- `createDocument` 与 `saveDocument` 在当前实现里都会拒绝空 `body`
- 仅修改标题时应使用 `renameDocumentTitle`，不需要传 `body`
- 审批流文档不支持创建、更新、移动、复制、导入
- `saveDocument` 仅在目标文档是 `DOC` 类型时才会触发内容转换

## 文档搜索类

### aiSearchDocument

AI 语义搜索 iWiki 文档内容。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| query | string | ✅ | 搜索关键词或短语 |
| limit | number | ❌ | 返回结果数量，默认 5，推荐 10；当请求头包含 `x-big-model` 时自动扩展为 10 |
| space_ids | string | ❌ | 限定搜索的空间 ID，多个用逗号分隔 |
| topic_ids | string | ❌ | 限定搜索的专题 ID，多个用逗号分隔 |
| doc_objs | array | ❌ | 指定搜索的文档对象列表，每项包含 `doc_id`(number) 和 `is_folder`(boolean) |

**请求头（可选）：**
| 请求头 | 描述 |
|--------|------|
| x-big-model | 传入任意值时，`limit` 自动扩展为 10，忽略调用方传入的值 |

**返回：**
```json
[{
  "docid": "123456",
  "title": "文档标题",
  "content": "匹配的内容片段...",
  "url": "https://iwiki.woa.com/p/123456",
  "source": "来源",
  "update_time": "2025-01-01T00:00:00Z",
  "file_type": "MD",
  "attachment_id": null
}]
```

### searchDocument

传统关键词搜索，支持多维度筛选。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| query | string | ✅ | 搜索关键词 |
| search_type | string[] | ❌ | 搜索类型：space/comment/attachment/page/all |
| space_id | string[] | ❌ | 空间 ID 列表 |
| tags | string | ❌ | 标签，多个用英文逗号分隔 |
| author | string[] | ❌ | 作者列表 |
| from | string | ❌ | 来源：iWiki/km/all |
| offset | number | ❌ | 分页偏移量，用于跳过前 N 条结果并获取后续结果；建议传非负整数 |

### glossaryTermSearch

搜索词库中的词条信息。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| keyword | string | ✅ | 搜索关键词 |
| limit | number | ❌ | 返回数量限制，默认 10 |
| glossary_ids | number[] | ❌ | 词库 id 列表，用于限定搜索范围，不传时搜索所有当前用户有权查看的词库 |

---

### glossaryTermExactSearch

精确搜索（忽略大小写）词库中的词条信息。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| keyword | string | ✅ | 搜索关键词 |
| limit | number | ❌ | 返回数量限制，默认 10 |
| glossary_ids | number[] | ❌ | 词库 id 列表，用于限定搜索范围，不传时搜索所有当前用户有权查看的词库 |

---

### glossaryBatchExactSearch

批量精确搜索词条（带权限），根据多个关键词精确匹配词库中的词条信息（忽略大小写），只返回当前用户有权查看的词库中的词条。每个关键词都会有对应的结果（无匹配时为空数组）。

**API：** `POST /api/glossary/term-search/batch-exact-with-permission`

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| keywords | string[] | ✅ | 关键词列表，不能为空，最多 100 个，每个关键词最多 100 个字符 |
| status | string | ❌ | 词条状态，逗号分隔，默认 `current`。可选值：`current`（当前有效）、`draft`（草稿）、`deleted`（已删除），多个值用逗号分隔，如 `"current,draft"` |
| include_aliases | boolean | ❌ | 是否包含别名匹配，默认 `true` |
| glossary_ids | number[] | ❌ | 词库 id 列表，用于限定搜索范围，不传时搜索所有当前用户有权查看的词库 |

**返回示例：**
```json
{
  "code": "Ok",
  "msg": "ok",
  "data": {
    "keyword1": [
      {
        "term_id": 123,
        "name": "词条名称",
        "glossary_id": 456,
        "glossary_name": "词库名称",
        "summary": "简要说明",
        "detailed_explanation": "详细解释",
        "aliases": ["别名1", "别名2"]
      }
    ],
    "keyword2": []
  }
}
```

**注意事项：**
- 此工具会过滤掉当前用户无权查看的词库中的词条
- 建议在需要批量查询多个词条时使用，避免多次调用单个词条搜索
- 结果按关键词分组返回，方便对应查询

---

### addGlossary

创建或更新词条（走审批流）。仅在指定 `gray` scope 时可见。

**API：** `POST /api/approval/glossaries`

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| glossary_id | number | ✅ | 词库 id |
| action | string | ✅ | 操作类型：`create`（创建）或 `update`（更新） |
| apply_reason | string | ✅ | 申请理由 |
| glossary_body | string | ✅ | 词条内容，JSON 字符串格式（含 name/summary/detailed_explanation/aliases 等字段） |
| term_id | number | ❌ | 词条 id，更新已有词条时必填 |

**返回示例：**
```json
{
  "code": "Ok",
  "msg": "ok",
  "data": { "task_id": "12345" }
}
```

---

## 文档读取类

### getDocument

获取文档完整内容（Markdown 格式）。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | string | ✅ | 文档 ID |

**返回：** Markdown 格式的文档正文内容

### metadata

获取文档元数据信息。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | string | ✅ | 文档 ID |

**返回：**
```json
{
  "id": 123456,
  "title": "文档标题",
  "content_type": "MD",
  "creator": "creator_name",
  "create_time": "2025-01-01T00:00:00Z",
  "modifier": "modifier_name",
  "modify_time": "2025-01-01T00:00:00Z",
  "space_id": 12345
}
```

### getSpacePageTree

获取空间目录树结构。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| parentid | number | ✅ | 父级文档 ID |

**返回：**
```json
[{
  "docid": 123456,
  "title": "子文档标题",
  "parentid": 67890,
  "has_children": true
}]
```

### listImages

获取文档内的图片附件 ID 列表。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | string | ✅ | 文档 ID |

**返回：** 附件 ID 数组 `["111", "222", "333"]`

### getAttachmentDownloadUrl

获取附件的临时下载 URL。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| attachmentid | string | ✅ | 附件 ID |

**返回：** 临时下载链接

---

## 文档写入类

### createDocument

创建新文档。当前实现会先将 `body` 按 `is_html` 转成 JSON，再调用创建接口。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| spaceid | number | ✅ | 空间 ID |
| parentid | number | ✅ | 父文档 ID |
| title | string | ✅ | 文档标题 |
| body | string | ✅ | 文档内容（运行时**不能为空**） |
| contenttype | string | ❌ | 类型：DOC/MD/FOLDER/VIKA，默认 MD |
| is_html | boolean | ❌ | 是否按 HTML 解析 `body`；`true` 使用 `HtmlTransformer`，`false` 使用 `MarkdownTransformer`，默认 `false` |
| body_mode | string | ❌ | 兼容旧参数；由于当前实现会先把 `body` 转为 JSON，常规调用下通常不会真正下发该字段 |

**返回：**
```json
{
  "id": 123456,
  "title": "新文档标题",
  "url": "https://iwiki.woa.com/p/123456"
}
```

### saveDocument

完整更新文档内容。当前实现会先查询目标文档元数据：

- 当目标文档为 `DOC` 类型时，才会根据 `is_html` 转换 `body`
- 当目标文档不是 `DOC` 类型时，会直接把原始 `body` 传给保存接口
- 虽然参数定义里 `body` 显示为可选，但运行时空值会被拒绝

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | number | ✅ | 文档 ID |
| title | string | ✅ | 文档标题 |
| body | string | ✅ | 文档内容（运行时**不能为空**） |
| is_html | boolean | ❌ | 是否按 HTML 解析 `body`；**仅当目标文档为 `DOC` 时生效** |

### renameDocumentTitle

仅修改文档标题，不修改正文内容。适用于标题重命名场景，底层调用 `POST /api/v2/doc/title/rename`。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | number | ✅ | 文档 ID |
| new_title | string | ✅ | 修改后的新标题 |

**返回示例：**
```json
{
  "code": "Ok",
  "msg": "ok",
  "request_id": "274c9f93-3b19-4869-b94d-14875a3e400e"
}
```

### saveDocumentParts

局部更新文档（在头部或尾部追加内容）。当前实现会直接透传 `before`/`after`，不会再做 Markdown/HTML 转换。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | number | ✅ | 文档 ID |
| title | string | ✅ | 文档标题 |
| before | string | ❌ | 插入到文档开头的内容 |
| after | string | ❌ | 追加到文档结尾的内容 |

### moveDocument

移动文档位置。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | number | ✅ | 要移动的文档 ID |
| new_parentid | number | ✅ | 新父目录 ID，不变传 0 |
| position | string | ❌ | 位置：append/below/above，默认 append |
| target_docid | number | ❌ | 目标文档 ID（position 为 below/above 时需要） |

### copyDocument

复制文档到新的位置，支持复制单个文档或整个文档树。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | number | ✅ | 要复制的文档 ID |
| new_parentid | number | ✅ | 新的父目录 ID，复制后的文档将放在此目录下 |
| is_single | number | ❌ | 是否仅复制单个文档：1=仅复制当前文档，0=复制整个文档树（包含所有子文档），默认 1 |
| language | string | ❌ | 语言设置，默认 `zh_CN` |

**返回：**
```json
{
  "code": "Ok",
  "msg": "ok",
  "data": {
    "task_id": "123456"
  }
}
```

**注意事项：**
- 复制操作返回任务 ID，可用于查询复制进度
- 复制的文档不会保留原文档的评论和标签
- `is_single=0` 时会复制整个文档树，包含所有子文档和层级结构

---

## 标签管理类

### getDocumentTags

获取文档标签列表。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | string | ✅ | 文档 ID |

### addDocumentTags

批量添加标签。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docids | number[] | ✅ | 文档 ID 列表 |
| labels | string[] | ✅ | 标签名称列表 |

### deleteDocumentTag

删除单个标签。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | number | ✅ | 文档 ID |
| labelName | string | ✅ | 要删除的标签名称 |

---

## 评论类

### getComments

获取文档评论（分页）。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | number | ✅ | 文档 ID |
| pageIndex | number | ❌ | 页码，默认 1，每页 10 条 |

**返回说明：**
- `next_level_comments` 字段包含回复评论
- `total` 字段为评论总数，可计算总页数

### getInlineComment

获取文档的划词批注（内联评论）。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | number | ✅ | 文档 ID |

**返回示例：**
```json
[
  {
    "id": "123456",
    "content": "批注内容",
    "creator": "用户名",
    "reply_to": "被回复的用户名",
    "mark_content": "划词原文",
    "is_deleted": false
  }
]
```

**返回说明：**
- `id`：批注 ID
- `content`：批注内容
- `creator`：创建者
- `reply_to`：回复对象（如果是回复）
- `mark_content`：划词原文
- `is_deleted`：是否已删除

### addComment

添加评论或回复。当前实现会使用 `HtmlTransformer` 解析 `content`，因此更推荐传 HTML/XHTML 片段。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | number | ✅ | 文档 ID |
| content | string | ✅ | 评论内容，建议传 HTML/XHTML 片段（如 `<p>评论内容</p>`） |
| parent_id | number | ❌ | 父评论 ID，顶级评论传 0 或不传 |

---

## 引用关系类

### getDocQuoteList

获取当前文档引用的其他文档。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | string | ✅ | 文档 ID |
| start | number | ❌ | 起始位置，默认 1 |
| limit | number | ❌ | 每页数量，默认 10 |

**返回：**
```json
[
  { "docid": "123456", "title": "被引用的文档标题" }
]
```

### getDocQuoteListBy

获取引用了当前文档的其他文档。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| docid | string | ✅ | 文档 ID |
| start | number | ❌ | 起始位置，默认 1 |
| limit | number | ❌ | 每页数量，默认 10 |

**返回：**
```json
[
  { "docid": "654321", "title": "引用了该文档的文档标题" }
]
```

---

## 空间管理类

### getSpaceInfoByKey

根据空间 Key 获取信息。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| spaceKey | string | ✅ | 空间 Key（如 `~myname` 或 `woa`） |

### getSpaceInfoByName

根据空间名称获取信息。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| spaceName | string | ✅ | 空间名称（支持模糊匹配；可直接传包含空格或特殊字符的原始名称） |

### getFavoriteSpaces

获取当前用户收藏的空间列表。

**参数：** 无

**返回：**
```json
[
  { "space_id": 12345, "space_name": "空间名称" }
]
```

### getManageSpaces

获取当前用户管理的空间列表。

**参数：** 无

**返回：**
```json
[
  { "space_id": 12345, "space_name": "空间名称" }
]
```

---

## 多维表格（Smartsheet）类

### smartsheetGetFields

获取表格字段定义。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| doc_id | string | ✅ | 多维表格文档 ID |
| viewId | string | ❌ | 视图 ID |

### smartsheetAddField

添加新字段。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| doc_id | number | ✅ | 多维表格文档 ID |
| name | string | ✅ | 字段名称 |
| type | string | ✅ | 字段类型（见下方支持的类型） |
| property | object | ✅ | 字段属性配置 |

**支持的字段类型：**
```
SingleText, Text, SingleSelect, MultiSelect, Number, Currency,
Percent, DateTime, Attachment, Member, Checkbox, Rating, URL,
Phone, Email, WorkDoc, OneWayLink, TwoWayLink, MagicLookUp,
Formula, AutoNumber, CreatedTime, LastModifiedTime, CreatedBy,
LastModifiedBy, Button
```

### smartsheetDeleteField

删除字段。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| doc_id | string | ✅ | 多维表格文档 ID |
| fieldId | string | ✅ | 要删除的字段 ID |

### smartsheetGetViews

获取视图列表。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| doc_id | string | ✅ | 多维表格文档 ID |
| view_type | string | ❌ | 视图类型筛选 |

**支持的视图类型：**
```
Grid, Gallery, Kanban, Gantt, Calendar, Architecture
```

### smartsheetGetRecords

查询记录数据。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| doc_id | string | ✅ | 多维表格文档 ID |
| pageNum | number | ❌ | 页码，默认 1 |
| pageSize | number | ❌ | 每页数量，默认 100，最大 200 |
| viewId | string | ❌ | 视图 ID |
| maxRecords | number | ❌ | 最大返回记录数 |
| sort | string | ❌ | 排序规则 |
| recordIds | string | ❌ | 记录 ID 列表（逗号分隔） |
| fields | string | ❌ | 字段列表（逗号分隔） |
| filterByFormula | string | ❌ | 筛选公式 |
| cellFormat | string | ❌ | 单元格格式 |
| fieldKey | string | ❌ | 字段键类型：id/name |

### smartsheetAddRecords

批量添加记录。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| doc_id | number | ✅ | 多维表格文档 ID |
| fieldKey | string | ✅ | 字段键类型：id/name |
| records | array | ✅ | 记录数组，每条含 fields 对象 |
| viewId | string | ❌ | 视图 ID |

**records 格式示例：**
```json
[
  { "fields": { "字段名1": "值1", "字段名2": "值2" } },
  { "fields": { "字段名1": "值3", "字段名2": "值4" } }
]
```

### smartsheetUpdateRecords

批量更新记录。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| doc_id | number | ✅ | 多维表格文档 ID |
| fieldKey | string | ✅ | 字段键类型：id/name |
| records | array | ✅ | 记录数组，含 recordId 和 fields |
| viewId | string | ❌ | 视图 ID |

**records 格式示例：**
```json
[
  { "recordId": "rec123", "fields": { "字段名": "新值" } }
]
```

### smartsheetDeleteRecords

批量删除记录。

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| doc_id | string | ✅ | 多维表格文档 ID |
| record_ids | string[] | ✅ | 要删除的记录 ID 数组 |

---

## 文档导入类

### importDocument

导入文件到 iWiki（通过 HTTP `POST /import` 端点）。支持将本地文件（如 Markdown、Word 等）上传并导入为 iWiki 文档。

**请求方式：** `POST /import`（`multipart/form-data`）

**参数：**
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| file | File | ✅ | 要导入的文件；HTTP 端点本身未额外声明文件大小限制 |
| parent_id | number | ✅ | 父文档/目录 ID，文件将导入到该目录下 |
| task_type | string | ❌ | 导入任务类型，默认 `md_import` |
| cover | boolean | ❌ | 是否覆盖同名文档，默认 `true` |

**请求头：**
| 请求头 | 描述 |
|--------|------|
| x-rio-timestamp | RIO 时间戳 |
| x-rio-signature | RIO 签名 |
| x-rio-nonce | RIO 随机数 |
| x-tai-identity | TAI 身份标识 |

**返回：**
```json
{
  "success": true,
  "msg": "导入成功",
  "data": [
    {
      "status": "finish",
      "task_id": "123456"
    }
  ]
}
```

**错误返回：**
```json
{
  "success": false,
  "msg": "错误信息"
}
```

**内部流程：**
1. 获取预签名 URL（`GetImportPresign`）
2. 上传文件到 COS 对象存储（`UploadImportFile`）
3. 启动导入任务（`StartImport`）
4. 轮询等待导入完成（最多 60 次，间隔 3 秒）

**限制：**
- 不支持审批流程文档的导入
- 仓库内 `scripts/connect_mcp.py` 的 `upload_file` 会在本地预检查文件大小，默认限制 50MB

**Python 客户端调用示例：**
```python
client = MCPClient()
result = client.upload_file(
    file_path="./doc.md",
    parent_id=4017403457,
    task_type="md_import",
    cover=True,
)
```

**命令行调用示例：**
```bash
# 上传 Markdown 文件到指定父目录
python connect_mcp.py upload ./doc.md 4017403457

# 指定任务类型
python connect_mcp.py upload ./doc.docx 4017403457 --task-type md_import

# 不覆盖同名文档
python connect_mcp.py upload ./doc.md 4017403457 --no-cover
```
