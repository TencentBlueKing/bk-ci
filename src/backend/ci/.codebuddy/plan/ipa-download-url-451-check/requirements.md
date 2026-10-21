# 需求文档

## 引言

在 `BkRepoDownloadService` 的 `outerPlistContent` 方法中，获取到 IPA 下载链接（`ipaExternalDownloadUrl`）后，需要对该 URL 发起 HTTP HEAD 请求进行可用性校验。当 HEAD 请求返回的 HTTP 状态码为 451（Unavailable For Legal Reasons）时，说明该资源因合规原因不可下载，此时应抛出业务异常，HTTP 响应状态码同样为 451，阻止后续 plist 内容的生成和返回。

## 需求

### 需求 1

**用户故事：** 作为一名系统运维人员，我希望在生成 IPA 下载 plist 文件之前，系统能自动检测下载链接的合规状态，以便在资源被标记为不可用时及时阻断下载流程并返回明确的错误信息。

#### 验收标准

1. WHEN 系统获取到 `ipaExternalDownloadUrl` THEN 系统 SHALL 对 `ipaExternalDownloadUrl.url` 发起 HTTP HEAD 请求
2. IF HEAD 请求返回的 HTTP 状态码等于 451 THEN 系统 SHALL 抛出业务异常（`CustomException`），异常的 HTTP 状态码为 451
3. IF HEAD 请求返回的 HTTP 状态码不等于 451 THEN 系统 SHALL 继续正常执行后续的 plist 内容生成逻辑
4. WHEN HEAD 请求发生网络异常或超时 THEN 系统 SHALL 不阻断流程，继续正常执行后续逻辑（即仅在明确收到 451 响应时才阻断）

### 需求 2

**用户故事：** 作为一名客户端开发人员，我希望在资源不可用时收到明确的 451 状态码响应，以便在客户端进行相应的错误提示处理。

#### 验收标准

1. WHEN 系统抛出 451 业务异常 THEN 系统 SHALL 返回 HTTP 451 状态码给调用方
2. WHEN 系统抛出 451 业务异常 THEN 系统 SHALL 在响应消息中包含有意义的错误描述信息

## 技术约束

1. `CustomException` 接受 `Response.Status` 枚举作为状态码参数，但 JAX-RS 标准枚举中不包含 451 状态码，需要考虑使用自定义状态码的方式（如直接使用 `Response.Status.fromStatusCode(451)` 或自定义 `Response.StatusType`）
2. HTTP HEAD 请求应使用项目中已有的 OkHttp 客户端（`OkhttpUtils`），通过构建 `Request` 对象并调用 `doHttp` 或 `doShortHttp` 方法实现
3. HEAD 请求应设置合理的超时时间，避免因外部服务响应慢而影响主流程性能
4. 修改位置在 `BkRepoDownloadService.kt` 文件的 `outerPlistContent` 方法中，位于获取 `ipaExternalDownloadUrl` 之后、获取 IPA 属性之前

## 边界情况

1. `ipaExternalDownloadUrl.url` 为空字符串或格式不合法时的处理
2. HEAD 请求超时或网络不可达时不应阻断主流程
3. HEAD 请求返回其他非 200 状态码（如 404、500 等）时不应阻断主流程，仅 451 需要特殊处理
