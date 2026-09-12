package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactRepositoryType
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_CUSTOM
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_PIPELINE
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

class RepositoryTypeCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val repository = context.triggerParam.repository
        val repoName = context.factParam.repoName
        val matched = when (repository) {
            ArtifactRepositoryType.PIPELINE -> repoName == REPO_PIPELINE
            ArtifactRepositoryType.CUSTOM -> repoName == REPO_CUSTOM
            ArtifactRepositoryType.IMAGE -> true
        }
        if (!matched) {
            context.response.failedReason = I18Variable(
                code = WebhookI18nConstants.BK_ARTIFACT_REPO_TYPE_NOT_MATCH,
                params = listOf()
            ).toJsonStr()
        }
        return matched
    }
}
