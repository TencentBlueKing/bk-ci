package com.tencent.devops.common.webhook.service.code.filter

import org.slf4j.LoggerFactory

/**
 * 不包含过滤器：triggerOn 命中 excluded 列表时拦截（精确匹配）
 */
class NotContainsFilter(
    private val pipelineId: String,
    // 过滤器名字
    private val filterName: String,
    private val triggerOn: String,
    private val excluded: List<String>,
    private val failedReason: String = ""
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(NotContainsFilter::class.java)
    }

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        logger.info("$pipelineId|triggerOn:$triggerOn|excluded:$excluded|$filterName filter")
        val filterResult = excluded.isEmpty() || !excluded.contains(triggerOn)
        if (!filterResult && failedReason.isNotBlank()) {
            response.failedReason = failedReason
        }
        return filterResult
    }
}
