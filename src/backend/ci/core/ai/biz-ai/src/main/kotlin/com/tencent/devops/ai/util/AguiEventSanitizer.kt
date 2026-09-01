package com.tencent.devops.ai.util

/**
 * 过滤底层模型或框架偶发泄漏到前端的原始 tool-call 控制标记。
 *
 * 这类标记不应直接展示给用户，一旦透传通常意味着本轮工具调用
 * 没有被正确解析，继续展示只会污染界面。
 */
object AguiEventSanitizer {

    fun sanitizeEncodedEvent(encoded: String): String? {
        return if (RAW_TOOL_CALL_MARKERS.any(encoded::contains)) {
            null
        } else {
            encoded
        }
    }

    private val RAW_TOOL_CALL_MARKERS = listOf(
        "tool_calls_section_begin",
        "tool_call_begin",
        "tool_call_argument_begin",
        "tool_call_end",
        "tool_calls_section_end"
    )
}
