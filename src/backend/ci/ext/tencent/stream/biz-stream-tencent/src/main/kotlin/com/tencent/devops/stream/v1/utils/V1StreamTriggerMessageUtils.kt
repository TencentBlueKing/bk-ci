package com.tencent.devops.stream.v1.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitTagPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.stream.v1.pojo.V1GitRequestEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class V1StreamTriggerMessageUtils @Autowired constructor(
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(V1StreamTriggerMessageUtils::class.java)
    }

    @SuppressWarnings("ComplexMethod")
    fun getEventMessageTitle(event: V1GitRequestEvent): String {
        val messageTitle = when (event.objectKind) {
            StreamGitObjectKind.MERGE_REQUEST.value -> {
                "Merge requests [!${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            StreamGitObjectKind.MANUAL.value -> {
                "Manually run by ${event.userId}"
            }
            StreamGitObjectKind.OPENAPI.value -> {
                "Run by OPENAPI(${event.userId})"
            }
            StreamGitObjectKind.TAG_PUSH.value -> {
                if (event.operationKind == TGitTagPushOperationKind.DELETE.value) {
                    "Tag [${event.branch}] deleted by ${event.userId}"
                } else {
                    "Tag [${event.branch}] pushed by ${event.userId}"
                }
            }
            StreamGitObjectKind.SCHEDULE.value -> {
                "Scheduled"
            }
            StreamGitObjectKind.PUSH.value -> {
                if (event.operationKind == TGitPushOperationKind.DELETE.value) {
                    "Branch [${event.branch}] deleted by ${event.userId}"
                } else {
                    val eventMap = try {
                        objectMapper.readValue<GitPushEvent>(event.event)
                    } catch (e: Exception) {
                        logger.warn(
                            "V1StreamTriggerMessageUtils|getEventMessageTitle" +
                                "|event as GitTagPushEvent error ${e.message}"
                        )
                        null
                    }
                    if (eventMap?.create_and_update != null) {
                        "Branch [${event.branch}] added by ${event.userId}"
                    } else {
                        "Commit [${event.commitId.subSequence(0, 7)}] pushed by ${event.userId}"
                    }
                }
            }
            StreamGitObjectKind.ISSUE.value -> {
                "Issue [${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            StreamGitObjectKind.REVIEW.value -> {
                "Review [${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            StreamGitObjectKind.NOTE.value -> {
                val noteEvent = try {
                    JsonUtil.to(event.event, GitNoteEvent::class.java)
                } catch (e: Exception) {
                    null
                }
                "Note [${noteEvent?.objectAttributes?.id}] submitted by ${event.userId}"
            }
            else -> {
                "Commit [${event.commitId.subSequence(0, 7)}] pushed by ${event.userId}"
            }
        }
        return messageTitle
    }

    fun getCommitCheckDesc(event: V1GitRequestEvent, prefix: String): String {
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
            StreamGitObjectKind.MERGE_REQUEST.value -> {
                "$prefix Triggered by $userId. [$action]"
            }
            StreamGitObjectKind.MANUAL.value -> {
                "$prefix Triggered by $userId. [manual]"
            }
            StreamGitObjectKind.TAG_PUSH.value -> {
                "$prefix Triggered by $userId. [tag-push]"
            }
            StreamGitObjectKind.SCHEDULE.value -> {
                "$prefix Triggered by stream [schedule]"
            }
            else -> {
                "$prefix Triggered by $userId. [push]"
            }
        }
        return messageTitle
    }
}
