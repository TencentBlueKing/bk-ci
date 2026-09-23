# 实施计划

- [ ] 1. 扩展 `CustomException` 以支持自定义 int 状态码
   - 修改 `CustomException.kt`（路径：`core/common/common-api/src/main/kotlin/com/tencent/devops/common/api/exception/CustomException.kt`），新增一个接受 `Int` 类型状态码和 `String` 消息的构造函数（或新增一个 `statusCode: Int` 属性）
   - 确保向后兼容，原有的 `Response.Status` 构造函数保持不变
   - _需求：2.1、2.2_

- [ ] 2. 修改 `CustomExceptionMapper` 以支持自定义 int 状态码的响应
   - 修改 `CustomExceptionMapper.kt`（路径：`core/common/common-web/src/main/kotlin/com/tencent/devops/common/web/handler/CustomExceptionMapper.kt`），在 `toResponse` 方法中适配新增的 int 状态码属性，使用 `Response.status(int)` 构建响应
   - 确保当使用原有 `Response.Status` 枚举时行为不变
   - _需求：2.1_

- [ ] 3. 在 `BkRepoDownloadService.outerPlistContent` 方法中添加 HEAD 请求校验逻辑
   - 修改 `BkRepoDownloadService.kt`（路径：`ext/tencent/artifactory/biz-artifactory-tencent/src/main/kotlin/com/tencent/devops/artifactory/service/bkrepo/BkRepoDownloadService.kt`）
   - 在获取 `ipaExternalDownloadUrl` 之后、获取 IPA 属性（`bkRepoClient.listMetadata`）之前，添加如下逻辑：
     - 使用 OkHttp 构建 HEAD 请求（`Request.Builder().url(ipaExternalDownloadUrl.url).head().build()`）
     - 调用 `OkhttpUtils.doShortHttp(request)` 发起请求（使用短超时客户端，5秒超时）
     - 判断响应状态码是否为 451
     - 如果是 451，抛出 `CustomException`，状态码为 451，消息为有意义的错误描述
   - 整个 HEAD 请求逻辑需要包裹在 try-catch 中，捕获所有异常（网络超时等），仅在明确收到 451 时抛出异常，其他异常仅记录日志后继续执行
   - 添加必要的 import 语句：`import com.tencent.devops.common.api.util.OkhttpUtils` 和 `import okhttp3.Request`
   - _需求：1.1、1.2、1.3、1.4_
