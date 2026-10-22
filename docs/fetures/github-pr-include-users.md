# GitHub Pull Request 包含人员过滤

## 问题与实现

GitHub 触发器模型已有 `includeUsers` 字段，PR 过滤器也已支持包含人员判断，但参数转换没有传递该字段。因此配置包含名单后，PR 过滤器仍按空名单处理。

将 `includeUsers` 传递到 `WebHookParams`，支持流水线变量替换，并在 PR 触发条件摘要中展示包含人员。继续复用现有 `GithubPrTriggerHandler` 和 `UserFilter`，不新增过滤逻辑或依赖。

## 行为

- 包含名单匹配 webhook 的 `sender.login`，即本次事件的操作人。
- 操作人与 PR 作者、受理人可以不同；名单不按 PR 作者或受理人判断。
- 包含名单为 null 或空列表时不限制操作人，兼容已有流水线。
- 操作人在包含名单中且不在排除名单中时，人员过滤通过；排除名单优先。
- PR 动作、分支等其他过滤条件继续独立生效；人员匹配不代表最终触发流水线。

示例：`includeUsers: ["alice"]` 允许由 `alice` 操作的受支持 PR 事件通过人员过滤。由 `bob` 操作的事件返回 `bkRepoTriggerUserNotMatch`；若同时配置 `excludeUsers: "alice"`，则返回 `bkRepoTriggerUserIgnored`。

## 验证

沿用模块现有 JUnit / MockK 测试，覆盖参数传递、变量替换、空名单兼容、包含与排除优先级、操作人与作者/受理人不同，以及 JSON 反序列化后的默认动作和 merge 动作过滤。

在 `src/backend/ci` 下运行（JDK 17）：

```shell
./gradlew :core:common:common-webhook:biz-common-webhook:test --tests '*GithubWebhookElementParamsTest' --tests '*GithubPrTriggerHandlerTest'
```

插件表单需要提供 `includeUsers` 输入项，才能在页面配置包含名单。
