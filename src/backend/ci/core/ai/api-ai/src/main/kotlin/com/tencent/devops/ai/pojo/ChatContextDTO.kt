package com.tencent.devops.ai.pojo

/**
 * 结构化的聊天上下文，由 [com.tencent.devops.ai.service.AiChatService]
 * 在请求入口处解析，存入 ThreadLocal 后传递给 Supervisor 和子智能体。
 *
 * [rawPairs] 保留前端原始 key-value，供 Supervisor 构建
 * Prompt 模板变量和 context_block 使用。
 */
data class ChatContextDTO(
    val projectId: String? = null,
    val pipelineId: String? = null,
    val buildId: String? = null,
    val rawPairs: List<Pair<String, String>> = emptyList()
) {
    companion object {
        fun fromPairs(
            pairs: List<Pair<String, String>>
        ): ChatContextDTO {
            val map = pairs.associate { it }
            return ChatContextDTO(
                projectId = map["projectId"],
                pipelineId = map["pipelineId"],
                buildId = map["buildId"],
                rawPairs = pairs
            )
        }
    }
}