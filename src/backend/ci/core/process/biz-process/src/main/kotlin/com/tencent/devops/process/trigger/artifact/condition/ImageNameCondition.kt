package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.filter.BranchFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 镜像名条件（仅 IMAGE）：复用 [BranchFilter] 对镜像名做通配匹配。
 *
 * image 为空则不限制；否则把事件镜像名当作 triggerOnBranchName、配置 image 当作 include 表达式。
 * 对不含通配符的纯字符串等价于精确匹配，同时额外支持 `*`/`**` 通配。
 */
class ImageNameCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val imageName = context.triggerParam.image
        if (imageName.isNullOrBlank()) return true
        val image = context.factParam.image ?: ""
        val filter = BranchFilter(
            pipelineId = context.pipelineId,
            triggerOnBranchName = image,
            includedBranches = listOf(imageName),
            excludedBranches = emptyList(),
            includedFailedReason = I18Variable(
                code = WebhookI18nConstants.BK_ARTIFACT_IMAGE_NAME_NOT_MATCH,
                params = listOf()
            ).toJsonStr()
        )
        val response = WebhookFilterResponse()
        val matched = filter.doFilter(response)
        if (!matched) {
            context.response.failedReason = response.failedReason
        }
        return matched
    }
}
