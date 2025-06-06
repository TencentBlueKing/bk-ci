package com.tencent.devops.repository.pojo.webhook

import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库webhook解析数据")
data class WebhookData(
    @get:Schema(title = "解析后webhook对象", required = true)
    val webhook: Webhook,
    @get:Schema(title = "关联的代码库", required = true)
    val repositories: List<Repository>
)
