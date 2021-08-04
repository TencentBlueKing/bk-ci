package com.tencent.devops.process.engine.service.code.filter

import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class CommitMessageFilter(
    private val includeCommitMsg: String?,
    private val excludeCommitMsg: String?,
    private val eventCommitMessage: String,
    private val pipelineId: String
) : WebhookFilter {

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        return isCommitMessageNotExcluded() && isCommitMessageIncluded(response)
    }

    private fun isCommitMessageNotExcluded(): Boolean {
        if (excludeCommitMsg.isNullOrBlank()) return true
        regex.split(excludeCommitMsg).toSet().forEach {
            if (eventCommitMessage.contains(it)) {
                logger.warn(
                    "The exclude commit msg match the git event commit msg for pipeline: " +
                        "$pipelineId, $eventCommitMessage"
                )
                return false
            }
        }
        logger.warn(
            "The exclude commit msg not match the git event commit msg for pipeline: " +
                "$pipelineId, event: $eventCommitMessage, setting: $includeCommitMsg"
        )
        return true
    }

    private fun isCommitMessageIncluded(response: WebhookFilterResponse): Boolean {
        if (includeCommitMsg.isNullOrBlank()) return true
        regex.split(includeCommitMsg).toSet().forEach {
            if (eventCommitMessage.contains(it)) {
                logger.warn(
                    "The include commit msg match the git event commit msg for pipeline: " +
                        "$pipelineId, $eventCommitMessage"
                )
                response.addParam(MATCH_COMMIT_MESSAGE, eventCommitMessage)
                return true
            }
        }
        logger.warn(
            "The include commit msg not match the git event commit msg for pipeline: " +
                "$pipelineId, event: $eventCommitMessage, setting: $includeCommitMsg"
        )
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommitMessageFilter::class.java.name)
        private const val MATCH_COMMIT_MESSAGE = "matchCommitMessage"
        private val regex = Pattern.compile("[,;]")
    }
}
