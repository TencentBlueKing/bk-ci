package com.tencent.devops.scm.pojo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 工蜂Copilot摘要返回值
 */
@Schema(title = "工蜂Copilot摘要返回值")
data class CodeGitCopilotSummary(
    @get:Schema(title = "id")
    val number: Int? = null,
    @get:Schema(title = "任务id, 后续好评差评使用的是这个 id")
    val processId: String? = null,
    @get:Schema(title = "状态  1：运行中 3：失败  5：已生成")
    val status: Int,
    @get:Schema(title = "大模型回答原始文本")
    val resultRaw: String? = null,
    @get:Schema(title = "markdown 渲染后的回答文本")
    val resultHtml: String? = null,
    @get:Schema(title = "随机值")
    val lastPatchSetId: String? = null,
    @get:Schema(title = "创建时间")
    val createdAt: Long? = null,
    @get:Schema(title = "仓库名称")
    var projectName: String? = null
)
