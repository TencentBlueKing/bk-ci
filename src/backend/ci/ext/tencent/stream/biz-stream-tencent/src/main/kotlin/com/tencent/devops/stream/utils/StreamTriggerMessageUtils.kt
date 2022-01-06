package com.tencent.devops.stream.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitTagPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isCreateBranch
import com.tencent.devops.stream.pojo.GitRequestEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StreamTriggerMessageUtils @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper
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
                if (event.operationKind == TGitTagPushOperationKind.DELETE.value) {
                    "[${event.branch}] tag [${event.branch}] delete by ${event.userId}"
                } else {
                    val eventMap = try {
                        objectMapper.readValue<GitTagPushEvent>(event.event)
                    } catch (e: Exception) {
                        logger.error("event as GitTagPushEvent error ${e.message}")
                        null
                    }
                    "[${eventMap?.create_from}] Tag [${event.branch}] pushed by ${event.userId}"
                }
            }
            TGitObjectKind.SCHEDULE.value -> {
                "[${event.branch}] Commit [${event.commitId.subSequence(0, 7)}] schedule"
            }
            TGitObjectKind.PUSH.value -> {
                if (event.operationKind == TGitPushOperationKind.DELETE.value) {
                    "[${event.branch}] branch [${event.branch}] delete by ${event.userId}"
                } else {
                    val eventMap = try {
                        objectMapper.readValue<GitPushEvent>(event.event)
                    } catch (e: Exception) {
                        logger.error("event as GitTagPushEvent error ${e.message}")
                        null
                    }
                    if (eventMap?.isCreateBranch() == true) {
                        "Branch [${event.branch}] added by ${event.userId}"
                    } else {
                        "[${event.branch}] Commit [${event.commitId.subSequence(0, 7)}] pushed by ${event.userId}"
                    }
                }
            }
            else -> {
                "[${event.branch}] Commit [${event.commitId.subSequence(0, 7)}] pushed by ${event.userId}"
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
