package com.tencent.devops.ai.agent.external.caller

import com.tencent.devops.ai.pojo.ExternalAgentInfo

/**
 * 外部智能体调用器接口，定义与第三方智能体平台的通信协议。
 *
 * 每个平台（Knot、蓝鲸智能体等）实现此接口，封装各自的
 * 请求构建、认证鉴权和响应解析逻辑。
 */
interface ExternalAgentCaller {
    /** 平台标识，与 [ExternalAgentInfo.platform] 匹配。 */
    fun platform(): String

    /**
     * 向外部智能体发送查询并返回响应文本。
     *
     * @param config 外部智能体配置信息
     * @param query 用户查询内容
     * @param conversationId 多轮会话 ID，映射到平台特定字段
     *        （Knot: conversation_id, BkAiDev: thread_id），
     *        空字符串表示新会话
     * @param chatHistory 历史对话记录，用于提供上下文，
     *        每条记录包含 "role" 和 "content" 键
     */
    fun call(
        config: ExternalAgentInfo,
        query: String,
        conversationId: String = "",
        chatHistory: List<Map<String, String>> = emptyList()
    ): String
}
