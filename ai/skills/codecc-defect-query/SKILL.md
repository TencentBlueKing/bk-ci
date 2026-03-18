---
name: codecc-defect-query
description: Query CodeCC defect alerts via the devops-prod-codecc MCP service. Use this skill whenever the user mentions CodeCC alerts, defect queries, code scanning results, BK-CI/BlueShield pipeline scan issues, or pastes a devops.woa.com or codecc.woa.com URL. Also trigger when the user asks about code quality issues, COVERITY/CPPLINT/ESLINT findings, or wants to check scan statistics for a project, pipeline, or task. Even if the user just pastes a URL without explanation, use this skill to parse it and query the relevant defects.
---

# CodeCC Defect Query via MCP

This skill guides you through querying CodeCC defect alerts using the `devops-prod-codecc` MCP service. It covers URL parsing, channel routing, tool selection, and parameter mapping.

## Step 1: Verify MCP Availability

Before making any query, confirm the MCP service is accessible.

The user configures the MCP as `devops-prod-codecc` in their `mcp.json`. Cursor internally registers it with a `user-` prefix, so the server identifier used in `CallMcpTool` is `user-devops-prod-codecc`. Both names refer to the same service.

Check if the MCP server is available by listing the MCP tool descriptors folder for `user-devops-prod-codecc`. If tools like `user_codecc_task_detail` appear, the MCP is properly configured.

If the MCP is not configured, inform the user and provide setup instructions:

> **You need to configure the `devops-prod-codecc` MCP server first.**
>
> Add the following to your `mcp.json` (the MCP configuration file for your IDE):
>
> ```json
> {
>   "mcpServers": {
>     "devops-prod-codecc": {
>       "type": "streamableHttp",
>       "url": "https://bk-apigateway.apigw.o.woa.com/prod/api/v2/mcp-servers/devops-prod-codecc/mcp/",
>       "headers": {
>         "X-Bkapi-Authorization": "{\"access_token\": \"<your_token>\"}"
>       },
>       "disabled": false
>     }
>   }
> }
> ```
>
> - Get your access_token: https://iwiki.woa.com/p/4009265804
> - Apply for MCP permissions: https://iwiki.woa.com/p/4015166149

Once confirmed, proceed to parse the user's input.

## Step 2: Parse User Input

The user may provide a URL, plain text with IDs, or a mix. Extract the following parameters from whatever they give you.

### BK-CI Pipeline URL

Pattern:
```
https://devops.woa.com/console/pipeline/{projectId}/{pipelineId}/detail/{buildId}/executeDetail
```

Extract: `projectId`, `pipelineId`, `buildId`

**Example:**
```
https://devops.woa.com/console/pipeline/my-project/p-abcdef123456/detail/b-abcdef123456/executeDetail
```
- projectId = `my-project`
- pipelineId = `p-abcdef123456`
- buildId = `b-abcdef123456`

### CodeCC URL (two domain variants)

Pattern A (devops domain):
```
https://devops.woa.com/console/codecc/{projectId}/task/{taskId}/defect/...
```

Pattern B (codecc domain):
```
https://codecc.woa.com/codecc/{projectId}/task/{taskId}/defect/...
```

Extract: `projectId`, `taskId` from the path; `buildId` from `?buildId=` query param if present.

**Example:**
```
https://devops.woa.com/console/codecc/my-project/task/123456/defect/standard/list?dimension=STANDARD&buildId=b-abcdef123456&status=1
```
- projectId = `my-project`
- taskId = `123456`
- buildId = `b-abcdef123456`
- dimension = `STANDARD` (from query param)

### Dimension Extraction Rules

Apply these rules in order:

1. If the URL path contains `/defect/ccn/list` -> dimension = `CCN`
2. If the URL path contains `/defect/dupc/list` -> dimension = `DUPC`
3. If the URL has a `?dimension=` query parameter -> use that value (`STANDARD`, `SECURITY`, or `DEFECT`)
4. If none of the above match -> do NOT pass a dimension parameter (the API will return statistics across all dimensions)

### Plain Text Input

The user might directly provide values like "taskId is 312059" or "check pipeline p-abc123". Extract whatever IDs they mention. If ambiguous, ask the user to clarify.

### GongfengId Sources

The gongfengId (Gongfeng project ID) can come from:

1. A `CODE_xxxxx` projectId — the `xxxxx` part IS the gongfengId (e.g., `CODE_12345` -> gongfengId = `12345`)
2. A Gongfeng URL like `https://git.code.tencent.com/project/12345` -> gongfengId = `12345`
3. The user directly providing a numeric ID

## Step 3: Determine the Channel (Normal vs Gongfeng)

The CodeCC MCP has two sets of APIs: **normal channel** (for standard projects) and **Gongfeng channel** (for open/closed-source governance projects). You must pick the right one based on the `projectId`.

### Routing Rules

**Use Gongfeng channel** if the projectId matches ANY of these patterns:
- `CODE_*` (starts with `CODE_`)
- `CUSTOMPROJ_TEG_CUSTOMIZED` (exact match)
- `CLOSED_SOURCE_*` (starts with `CLOSED_SOURCE_`)
- `GITHUB_*` (starts with `GITHUB_`)

**Use normal channel** for everything else.

### Special Cases

- **`CODE_xxxxx` projectId**: Extract `xxxxx` as the gongfengId and use it directly with Gongfeng channel APIs.
- **Other closed-source prefixes with only a pipeline URL**: The pipeline APIs (`user_codecc_pipeline_defect_detail` / `user_codecc_pipeline_defect_statistic`) only work for non-governance tasks. If the user has a closed-source project but only gave you a pipeline URL, ask them to provide a CodeCC link or taskId instead.
- **Have a taskId but unsure about channel**: Call `user_codecc_task_detail` first to get the task info (which includes routing metadata). This helps determine whether to use normal or Gongfeng APIs.
- **Cannot determine channel at all**: Try the normal channel first. If the response indicates an error or returns no data, fall back to the Gongfeng channel.

## Step 4: Select the Right MCP Tool

### Tool Reference

| Scenario | Normal Channel | Gongfeng Channel |
|---|---|---|
| Have pipelineId | `user_codecc_pipeline_defect_statistic` | N/A |
| Have pipelineId + need details | `user_codecc_pipeline_defect_detail` | N/A |
| Have taskId | `user_codecc_defect_statistic_by_taskId` | `user_codecc_gongfeng_defect_statistic_by_taskId` |
| Have taskId + need details | `user_codecc_defect_detail_by_taskId` | `user_codecc_gongfeng_defect_detail_by_taskId` |
| Have gongfengId | N/A | `user_codecc_gongfeng_defect_statistic` |
| Have gongfengId + need details | N/A | `user_codecc_gongfeng_defect_detail` |
| Have taskId + need task info | `user_codecc_task_detail` | `user_codecc_task_detail` |

### Query Strategy

1. **Start with statistics** — call the `*_statistic` tool first to get an overview of defect counts across dimensions.
2. **Then drill into details** — if the user wants specific defect records, call the `*_detail` tool with appropriate filters (dimension, toolName, page params).
3. **Task info lookup** — when you have a taskId and need routing info or general task metadata, call `user_codecc_task_detail`.

## Step 5: Call the MCP Tool

Use `CallMcpTool` with server = `user-devops-prod-codecc`.

### Parameter Rules (IMPORTANT)

- **Numeric IDs must be passed as strings**: `taskId`, `gongfengId`, and `buildId` are all string type. For example, taskId should be `"312059"` not `312059`.
- **Exception**: `user_codecc_task_detail` expects `taskId` as a **number** type (e.g., `312059`). This is the ONLY tool where taskId is numeric.
- **Pagination params are numbers**: `pageNum` and `pageSize` are number type (e.g., `1` and `10`, not `"1"` and `"10"`).
- `buildId` format is typically `b-xxxx`. Pass it as a string. If not provided, the API uses the latest build.
- `dimension` valid values: `STANDARD`, `SECURITY`, `DEFECT`, `CCN`, `DUPC`. If omitted, the statistic APIs return data for all dimensions.
- `toolName` valid values include: `COVERITY`, `CPPLINT`, `ESLINT`, `PYLINT`, `CHECKSTYLE`, etc. Only pass if the user wants to filter by a specific scanning tool.
- `pageNum` starts from 1 (default). `pageSize` defaults to 10.

### Call Examples

**Query statistics via pipelineId (normal channel):**
```json
{
  "server": "user-devops-prod-codecc",
  "toolName": "user_codecc_pipeline_defect_statistic",
  "arguments": {
    "query_param": {
      "pipelineId": "p-abcdef123456",
      "buildId": "b-abcdef123456"
    }
  }
}
```

**Query defect details via taskId (normal channel):**
```json
{
  "server": "user-devops-prod-codecc",
  "toolName": "user_codecc_defect_detail_by_taskId",
  "arguments": {
    "query_param": {
      "taskId": "123456",
      "dimension": "STANDARD",
      "buildId": "b-abcdef123456",
      "pageNum": 1,
      "pageSize": 10
    }
  }
}
```

**Query statistics via gongfengId (Gongfeng channel):**
```json
{
  "server": "user-devops-prod-codecc",
  "toolName": "user_codecc_gongfeng_defect_statistic",
  "arguments": {
    "query_param": {
      "gongfengId": "12345"
    }
  }
}
```

**Query defect details via taskId (Gongfeng channel):**
```json
{
  "server": "user-devops-prod-codecc",
  "toolName": "user_codecc_gongfeng_defect_detail_by_taskId",
  "arguments": {
    "query_param": {
      "taskId": "789012",
      "dimension": "STANDARD",
      "pageNum": 1,
      "pageSize": 10
    }
  }
}
```

**Query task detail (to determine routing):**
```json
{
  "server": "user-devops-prod-codecc",
  "toolName": "user_codecc_task_detail",
  "arguments": {
    "query_param": {
      "taskId": 123456
    }
  }
}
```
Note: taskId is a **number** here, not a string.

## Step 6: Present Results

After receiving the MCP response:

1. Summarize the statistics clearly — total defects per dimension, severity breakdown if available.
2. If the user needs details, present defect records in a readable format (table or list) with key fields like file path, line number, checker/rule, severity, and description.
3. If the query returned no results or an error, explain the likely cause and suggest alternatives (e.g., try the other channel, check if the buildId is correct, verify the taskId).

## Error Handling

- **MCP not configured**: Guide the user through setup (see Step 1).
- **Authentication error (401/403)**: The access_token may be expired or the user lacks permissions. Point them to the token refresh guide and permission application link.
- **No data returned**: Could be wrong channel. If you used normal channel, retry with Gongfeng channel (or vice versa). Also check if the buildId or taskId is valid.
- **Closed-source project + pipeline URL only**: Explain that pipeline APIs don't support governance tasks, and ask the user to provide a CodeCC task link or taskId.
