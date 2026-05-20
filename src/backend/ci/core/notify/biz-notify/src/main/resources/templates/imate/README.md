# IMate 会话消息模板（创作流）

## 架构选择

模板内容由 **创作流后台**（bk-ci）维护、加载、渲染，IMate 后台只负责把已经渲染好的 HTML 推送到目标会话。这样：

- 模板样式 / 文案迭代不需要 IMate 后台改代码或重新部署；
- bk-ci 这边可对每个场景的占位符做强类型校验；
- IMate 不需要理解我们的业务模型（只是一个渲染容器）。

模板**不入数据库、不在 OP 后台编辑**，直接以 classpath 资源管理：变更走代码评审 + 发版，避免出现「线上模板被误改」的事故。

## 目录结构

```
templates/imate/
├── CREATIVE_STREAM_STAGE_REVIEW.html              # Stage 审核请求（含同意/驳回按钮）
├── CREATIVE_STREAM_PIPELINE_FINISH_SUCCESS.html   # 流水线运行成功
├── CREATIVE_STREAM_PIPELINE_FINISH_FAIL.html      # 流水线运行失败
└── README.md
```

> 模板文件名（去掉 `.html` 后缀）即为 `templateCode`，由 `ImateTemplateRenderer` 在加载时识别；同名常量在 `NotifyUtils.IMATE_TPL_*` 中维护，避免散落字符串字面量。

## 占位符约定

模板中以 `{{var}}` 表达占位符，渲染时通过字符串替换填入。占位符与发送方在 `SendNotifyMessageTemplateRequest.bodyParams` 注入的键名一一对应。

各场景具体支持的占位符见对应 HTML 文件头部注释。常见基础占位符：

| 占位符 | 含义 |
| --- | --- |
| `projectName` | 项目中文名 |
| `pipelineName` | 流水线名 |
| `buildNum` | 构建编号 |
| `dataTime` | 时间戳（发送时刻） |
| `detailUrl` / `detailAppUrl` | 构建详情 Web/移动端链接 |
| `reviewUrl` / `reviewAppUrl` | Stage 审核 Web/移动端链接 |

## 按钮交互契约

为避免「按钮文案在 HTML 里写一份、又在 POJO 字段 `actions` 里写一份」造成的双源真相，本方案约定：

- **按钮全部写在 HTML 里**，统一形如 `<button data-action="APPROVE">同意</button>`；
- IMate 客户端只需在卡片 root 节点上对 `[data-action]` 元素做 click 事件代理；命中后读取：
  - `el.dataset.action` —— 必填，作为回调请求体的 `action` 字段（取值：`APPROVE` / `REJECT`）；
  - `el.dataset.suggest` —— 可选，作为回调请求体的 `suggest` 字段；
- 若按钮在触发回调前还需要前端二次确认 / 收集额外输入（比如「驳回必须填原因」），就在模板里写**内联 JS**，在事件冒泡过程中先弹 prompt 把结果写回 `dataset.suggest`，再让 IMate 客户端的事件代理触发回调。

`ImateSendMessageRequest` 因此不再带 `actions` 字段 —— 业务卡片的 UI 与交互完全由 HTML 自描述，IMate 后端只负责「转发渲染 + 拦截 click 事件 + 回调业务方」。

## 审核回调

`STAGE_REVIEW` 场景下卡片上的两个按钮（`APPROVE` / `REJECT`）由 IMate 端拦截点击事件，调用本端 Open 接口：

```
POST /open/stream-review/callback
Headers:
    X-DEVOPS-BK-TOKEN: <双方约定的固定 token，用于平台间鉴权>
    X-DEVOPS-UID:      <真实点击人>
Body:
{
    "projectId":  "...",
    "pipelineId": "...",
    "buildId":    "...",
    "stageId":    "...",
    "groupId":    "...",     // 可选；不传则使用当前活跃审核组
    "executeCount": 1,
    "action": "APPROVE | REJECT",
    "suggest": "驳回时的审核建议"
}
```

`projectId/pipelineId/buildId/stageId/groupId/executeCount` 由 IMate 后台在收到 bk-ci 发送的卡片时一并保存（详见 `ImateSendMessageRequest`），并在用户点击按钮时**原样回传**给本接口。
