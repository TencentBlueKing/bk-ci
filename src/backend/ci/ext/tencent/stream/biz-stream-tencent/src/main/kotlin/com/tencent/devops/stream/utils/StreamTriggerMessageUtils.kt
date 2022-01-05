package com.tencent.devops.stream.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StreamTriggerMessageUtils @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val gitRequestEventHandle: GitRequestEventHandle
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamTriggerMessageUtils::class.java)
    }

    fun getEventMessageTitle(event: GitRequestEvent, gitProjectId: Long): String {
        val messageTitle = when (event.objectKind) {
            TGitObjectKind.MERGE_REQUEST.value -> {
                val branch = GitCommonUtils.checkAndGetForkBranchName(
                    gitProjectId = gitProjectId,
                    sourceGitProjectId = event.sourceGitProjectId,
                    branch = event.branch,
                    client = client
                )
                "[$branch] Merge requests [!${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            TGitObjectKind.MANUAL.value -> {
                "[${event.branch}] Manual Triggered by ${event.userId}"
            }
            TGitObjectKind.TAG_PUSH.value -> {
                val eventMap = try {
                    objectMapper.readValue<GitTagPushEvent>(event.event)
                } catch (e: Exception) {
                    logger.error("event as GitTagPushEvent error ${e.message}")
                    null
                }
                "[${eventMap?.create_from}] Tag [${event.branch}] pushed by ${event.userId}"
            }
            TGitObjectKind.SCHEDULE.value -> {
                "[${event.branch}] Commit [${event.commitId.subSequence(0, 7)}] schedule"
            }
            else -> {
                if (event.operationKind == TGitPushOperationKind.DELETE.value) {
                    when (event.objectKind) {
                        TGitObjectKind.PUSH.value ->
                            "[${event.branch}] branch [${event.commitId}] delete by ${event.userId}"
                        TGitObjectKind.TAG_PUSH.value ->
                            "[${event.branch}] tag [${event.commitId}] delete by ${event.userId}"
                        else -> "[${event.commitId}] delete by ${event.userId}"
                    }
                } else
                    "[${event.branch}] Commit [${event.commitId.subSequence(0, 7)}] pushed by ${event.userId}"
            }
        }
        return messageTitle
    }

    // TODO: 更新定时触发时这里也要更新
    fun getEventMessageTitle(
        event: GitEvent?,
        gitProjectId: Long,
        objectKind: String,
        branch: String?,
        userId: String?
    ): String {
        val messageTitle = if (event != null) {
            when (event) {
                is GitMergeRequestEvent -> {
                    getEventMessageTitle(gitRequestEventHandle.createMergeEvent(event, ""), gitProjectId)
                }
                is GitTagPushEvent -> {
                    getEventMessageTitle(gitRequestEventHandle.createTagPushEvent(event, ""), gitProjectId)
                }
                is GitPushEvent -> {
                    getEventMessageTitle(gitRequestEventHandle.createPushEvent(event, ""), gitProjectId)
                }
                else -> {
                    ""
                }
            }
        } else {
            if (objectKind == TGitObjectKind.MANUAL.value) {
                "[$branch] Manual Triggered by $userId"
            } else {
                ""
            }
        }
        return messageTitle
    }

    fun getCommitCheckDesc(event: GitRequestEvent, prefix: String): String {
        with(event) {
            return getCommitCheckDesc(
                prefix,
                objectKind,
                extensionAction ?: "",
                userId
            )
        }
    }

    fun getCommitCheckDesc(
        prefix: String,
        objectKind: String,
        action: String,
        userId: String
    ): String {
        val messageTitle = when (objectKind) {
            TGitObjectKind.MERGE_REQUEST.value -> {
                "$prefix Triggered by $userId. [$action]"
            }
            TGitObjectKind.MANUAL.value -> {
                "$prefix Triggered by $userId. [manual]"
            }
            TGitObjectKind.TAG_PUSH.value -> {
                "$prefix Triggered by $userId. [tag-push]"
            }
            TGitObjectKind.SCHEDULE.value -> {
                "$prefix Triggered by stream [schedule]"
            }
            else -> {
                "$prefix Triggered by $userId. [push]"
            }
        }
        return messageTitle
    }
}
