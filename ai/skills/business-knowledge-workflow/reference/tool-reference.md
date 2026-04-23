# 工具使用速查

## iWiki MCP 工具

| 工具 | 用途 | 常用参数 |
|:-----|:-----|:--------|
| `searchDocument` | 搜索文档 | `query` |
| `getDocument` | 获取内容 | `docId` |
| `getSpacePageTree` | 获取目录 | `spaceKey`, `parentId` |
| `metadata` | 获取元数据 | `docId` |

### iWiki 调用示例

```
mcp__iwiki__searchDocument(query="Buildless 架构设计")
mcp__iwiki__getDocument(docId="12345")
mcp__iwiki__getSpacePageTree(spaceKey="BKCI", parentId="root")
```

## 代码分析工具

| 工具 | 用途 | 示例 |
|:-----|:-----|:-----|
| `search_content` | 搜索代码内容 | 类名、方法名、字符串 |
| `search_file` | 搜索文件名 | `*.kt`, `*Service.kt` |
| `read_file` | 读取文件 | 查看完整实现 |
| `list_files` | 列出目录 | 了解模块结构 |

### 代码分析调用示例

```
search_content(query="class BuildLessStartDispatcher")
search_file(query="*DispatchService.kt")
read_file(path="src/backend/ci/core/dispatch/biz-dispatch/src/main/kotlin/...")
list_files(path="src/backend/ci/core/dispatch/")
```
