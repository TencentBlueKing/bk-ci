package com.tencent.devops.common.webhook.service.code.filter

import org.slf4j.LoggerFactory

/**
 * TAPD 单据状态过滤器
 *
 * - included 为空表示监听全部状态
 * - excluded 命中即不触发，优先级高于 included
 * - triggerOn 为状态英文 key；缺失时若配置了 included 则不触发，未配置 included 则保持旧行为
 */
class TapdStatusFilter(
    private val pipelineId: String,
    private val filterName: String = "tapdStatus",
    private val triggerOn: String,
    private val included: List<String>,
    private val excluded: List<String>,
    private val includedFailedReason: String = "",
    private val excludedFailedReason: String = ""
) : WebhookFilter {

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        logger.info(
            "$pipelineId|triggerOn:$triggerOn|included:$included|excluded:$excluded|$filterName filter"
        )
        if (triggerOn.isBlank()) {
            if (included.isEmpty()) return true
            response.failedReason = includedFailedReason
            return false
        }
        if (excluded.contains(triggerOn)) {
            response.failedReason = excludedFailedReason
            return false
        }
        if (included.isNotEmpty() && !included.contains(triggerOn)) {
            response.failedReason = includedFailedReason
            return false
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TapdStatusFilter::class.java)
    }
}
