# Notify 通知服务模块架构指南

# Notify 通知服务模块架构指南

## 概述

Notify（通知服务）模块是 BK-CI 的消息通知服务，负责向用户发送各类通知消息，支持多种通知渠道。

**模块职责**：
- 多渠道消息发送（邮件、企业微信、微信、短信、语音）
- 消息模板管理
- 消息发送记录与重试
- 频率限制与防骚扰
- 用户黑名单管理

## 一、模块结构

```
src/backend/ci/core/notify/
├── api-notify/            # API 接口定义
│   └── src/main/kotlin/com/tencent/devops/notify/
│       ├── api/           # Resource 接口
│       │   ├── builds/    # 构建过程通知
│       │   │   └── BuildNotifyResource.kt
│       │   ├── service/   # 服务间调用
│       │   │   ├── ServiceNotifyResource.kt
│       │   │   └── ServiceNotifyMessageTemplateResource.kt
│       │   └── op/        # 运营管理
│       │       ├── OpNotifyMessageTemplateResource.kt
│       │       └── OpNotifyUserBlackListResource.kt
│       ├── constant/      # 常量定义
│       │   ├── NotifyMessageCode.kt
│       │   └── NotifyMQ.kt
│       └── pojo/          # 数据传输对象
│           ├── EmailNotifyMessage.kt
│           ├── RtxNotifyMessage.kt
│           ├── WechatNotifyMessage.kt
│           ├── SmsNotifyMessage.kt
│           ├── VoiceNotifyMessage.kt
│           └── messageTemplate/
├── biz-notify/            # 业务逻辑层
└── boot-notify/           # 启动模块
```

## 二、通知渠道

### 2.1 支持的通知类型

| 类型 | 类名 | 说明 |
|------|------|------|
| EMAIL | `EmailNotifyMessage` | 邮件通知 |
| RTX | `RtxNotifyMessage` | 企业微信（原RTX） |
| WECHAT | `WechatNotifyMessage` | 微信通知 |
| SMS | `SmsNotifyMessage` | 短信通知 |
| VOICE | `VoiceNotifyMessage` | 语音通知 |
| WEWORK | `WeworkNotifyTextMessage` | 企业微信机器人 |

### 2.2 消息模型

```kotlin
// 基础消息类
abstract class BaseMessage(
    open val source: String,           // 消息来源
    open val sender: String,           // 发送者
    open val priority: Int,            // 优先级
    open val frequencyLimit: Int?,     // 频率限制（分钟）
    open val tofSysId: String?,        // TOF系统ID
    open val fromSysId: String?,       // 来源系统ID
    open val delaySeconds: Int?        // 延迟发送秒数
)

// 邮件消息
data class EmailNotifyMessage(
    val receivers: Set<String>,        // 收件人
    val cc: Set<String>?,              // 抄送
    val bcc: Set<String>?,             // 密送
    val title: String,                 // 标题
    val body: String,                  // 内容
    val format: EmailFormat            // 格式（HTML/TEXT）
) : BaseMessage()

// 企业微信消息
data class RtxNotifyMessage(
    val receivers: Set<String>,        // 接收人
    val title: String,                 // 标题
    val body: String                   // 内容
) : BaseMessage()
```

## 三、数据库设计

### 3.1 核心表结构

| 表名 | 说明 |
|------|------|
| `T_NOTIFY_EMAIL` | 邮件发送记录 |
| `T_NOTIFY_RTX` | 企业微信发送记录 |
| `T_NOTIFY_SMS` | 短信发送记录 |
| `T_NOTIFY_WECHAT` | 微信发送记录 |
| `T_COMMON_NOTIFY_MESSAGE_TEMPLATE` | 通用消息模板 |
| `T_EMAIL_NOTIFY_MESSAGE_TEMPLATE` | 邮件模板 |
| `T_RTX_NOTIFY_MESSAGE_TEMPLATE` | 企业微信模板 |
| `T_WECHAT_NOTIFY_MESSAGE_TEMPLATE` | 微信模板 |

### 3.2 关键字段说明

**T_NOTIFY_EMAIL**：
```sql
- ID: 消息主键
- SUCCESS: 是否发送成功
- SOURCE: 消息来源
- SENDER: 发送者
- TO: 收件人列表
- CC: 抄送列表
- BCC: 密送列表
- TITLE: 邮件标题
- BODY: 邮件内容
- PRIORITY: 优先级
- RETRY_COUNT: 重试次数
- LAST_ERROR: 最后错误信息
- CONTENT_MD5: 内容MD5（用于频率限制）
- FREQUENCY_LIMIT: 频率限制时长（分钟）
- DelaySeconds: 延迟发送秒数
```

**T_COMMON_NOTIFY_MESSAGE_TEMPLATE**：
```sql
- ID: 模板主键
- TEMPLATE_CODE: 模板代码
- TEMPLATE_NAME: 模板名称
- NOTIFY_TYPE_SCOPE: 适用通知类型
- PRIORITY: 优先级
- SOURCE: 来源
```

## 四、API 接口设计

### 4.1 服务间通知接口

```kotlin
@Path("/service/notify")
interface ServiceNotifyResource {
    
    @POST
    @Path("/email/send")
    fun sendEmailNotify(
        message: EmailNotifyMessage
    ): Result<Boolean>
    
    @POST
    @Path("/rtx/send")
    fun sendRtxNotify(
        message: RtxNotifyMessage
    ): Result<Boolean>
    
    @POST
    @Path("/wechat/send")
    fun sendWechatNotify(
        message: WechatNotifyMessage
    ): Result<Boolean>
    
    @POST
    @Path("/sms/send")
    fun sendSmsNotify(
        message: SmsNotifyMessage
    ): Result<Boolean>
    
    @POST
    @Path("/voice/send")
    fun sendVoiceNotify(
        message: VoiceNotifyMessage
    ): Result<Boolean>
}
```

### 4.2 模板通知接口

```kotlin
@Path("/service/notify/template")
interface ServiceNotifyMessageTemplateResource {
    
    @POST
    @Path("/send")
    fun sendNotifyMessageByTemplate(
        request: SendNotifyMessageTemplateRequest
    ): Result<Boolean>
}

// 模板消息请求
data class SendNotifyMessageTemplateRequest(
    val templateCode: String,          // 模板代码
    val receivers: Set<String>,        // 接收人
    val cc: Set<String>?,              // 抄送
    val titleParams: Map<String, String>?,  // 标题参数
    val bodyParams: Map<String, String>?,   // 内容参数
    val notifyType: Set<NotifyType>?   // 通知类型
)
```

### 4.3 构建通知接口

```kotlin
@Path("/build/notify")
interface BuildNotifyResource {
    
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/send")
    fun sendBuildNotify(
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        request: BuildNotifyRequest
    ): Result<Boolean>
}
```

## 五、消息发送流程

### 5.1 发送时序图

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│ Caller  │     │ Notify  │     │ 频率    │     │ 发送    │
│ Service │     │ Service │     │ 检查    │     │ 渠道    │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ 1.发送请求    │               │               │
     │──────────────>│               │               │
     │               │               │               │
     │               │ 2.频率检查    │               │
     │               │──────────────>│               │
     │               │               │               │
     │               │ 3.检查结果    │               │
     │               │<──────────────│               │
     │               │               │               │
     │               │ 4.记录发送    │               │
     │               │───────┐       │               │
     │               │       │       │               │
     │               │<──────┘       │               │
     │               │               │               │
     │               │ 5.调用渠道    │               │
     │               │──────────────────────────────>│
     │               │               │               │
     │               │ 6.发送结果    │               │
     │               │<──────────────────────────────│
     │               │               │               │
     │ 7.返回结果    │               │               │
     │<──────────────│               │               │
```

### 5.2 重试机制

```kotlin
// 发送失败时的重试逻辑
class NotifyRetryService {
    
    @Scheduled(fixedDelay = 60000)  // 每分钟检查
    fun retryFailedMessages() {
        // 查询失败且未超过重试次数的消息
        val failedMessages = notifyDao.getFailedMessages(
            maxRetryCount = 3
        )
        
        failedMessages.forEach { message ->
            try {
                sendMessage(message)
                notifyDao.updateSuccess(message.id)
            } catch (e: Exception) {
                notifyDao.incrementRetryCount(message.id, e.message)
            }
        }
    }
}
```

## 六、消息模板

### 6.1 模板变量

模板支持变量替换，使用 `${}` 语法：

```
标题模板：【BK-CI】${projectName} 流水线构建${status}
内容模板：
您好，${userName}：
    您的流水线 ${pipelineName} 构建${status}。
    构建号：${buildNum}
    触发人：${triggerUser}
    耗时：${duration}
```

### 6.2 内置模板

| 模板代码 | 说明 |
|---------|------|
| `BUILD_START` | 构建开始通知 |
| `BUILD_SUCCESS` | 构建成功通知 |
| `BUILD_FAIL` | 构建失败通知 |
| `BUILD_CANCEL` | 构建取消通知 |
| `QUALITY_INTERCEPT` | 质量红线拦截通知 |
| `QUALITY_REVIEW` | 质量红线审核通知 |
| `PERMISSION_APPLY` | 权限申请通知 |

## 七、与其他模块的关系

### 7.1 依赖关系

```
Notify 模块
    │
    ├──< Process（流水线）
    │    - 构建状态变更通知
    │    - 流水线执行结果通知
    │
    ├──< Quality（质量红线）
    │    - 红线拦截通知
    │    - 审核请求通知
    │
    ├──< Auth（权限）
    │    - 权限申请通知
    │    - 权限审批通知
    │
    └──< Project（项目）
         - 项目成员变更通知
         - 项目审批通知
```

### 7.2 MQ 消息

```kotlin
object NotifyMQ {
    const val EXCHANGE_NOTIFY = "e.notify"
    const val QUEUE_NOTIFY_EMAIL = "q.notify.email"
    const val QUEUE_NOTIFY_RTX = "q.notify.rtx"
    const val QUEUE_NOTIFY_WECHAT = "q.notify.wechat"
}
```

## 八、开发规范

### 8.1 发送通知示例

```kotlin
// 通过 Client 发送邮件
client.get(ServiceNotifyResource::class).sendEmailNotify(
    EmailNotifyMessage(
        source = "process",
        sender = "DevOps",
        receivers = setOf("user1@example.com", "user2@example.com"),
        title = "构建成功通知",
        body = "您的流水线构建已完成",
        format = EmailFormat.HTML,
        priority = NotifyPriority.HIGH.ordinal
    )
)

// 通过模板发送
client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
    SendNotifyMessageTemplateRequest(
        templateCode = "BUILD_SUCCESS",
        receivers = setOf("user1"),
        titleParams = mapOf(
            "projectName" to "my-project",
            "status" to "成功"
        ),
        bodyParams = mapOf(
            "pipelineName" to "my-pipeline",
            "buildNum" to "100",
            "duration" to "5分钟"
        ),
        notifyType = setOf(NotifyType.EMAIL, NotifyType.RTX)
    )
)
```

### 8.2 新增通知模板

1. 在 `T_COMMON_NOTIFY_MESSAGE_TEMPLATE` 表中注册模板
2. 在对应渠道模板表中添加具体内容
3. 使用 `ServiceNotifyMessageTemplateResource` 发送

### 8.3 频率限制

```kotlin
// 设置频率限制，5分钟内相同内容不重复发送
val message = EmailNotifyMessage(
    // ... 其他参数
    frequencyLimit = 5  // 5分钟内不重发
)
```

## 九、常见问题

**Q: 如何避免消息重复发送？**
A: 使用 `frequencyLimit` 参数，系统会根据 `CONTENT_MD5` 判断相同内容是否在限制时间内已发送。

**Q: 发送失败如何处理？**
A: 系统会自动重试，最多重试3次。可通过 `T_NOTIFY_*` 表的 `RETRY_COUNT` 和 `LAST_ERROR` 字段查看状态。

**Q: 如何添加用户到黑名单？**
A: 通过 `OpNotifyUserBlackListResource` 接口管理黑名单，黑名单用户不会收到任何通知。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
