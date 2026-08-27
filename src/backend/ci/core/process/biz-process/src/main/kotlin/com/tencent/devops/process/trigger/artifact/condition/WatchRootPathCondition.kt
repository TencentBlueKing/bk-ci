package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.service.code.filter.ContainsFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 监听根路径条件（仅自定义仓库 CUSTOM）：事件的一级目录 rootPath 必须命中配置的 watchRootPath 之一；
 * watchRootPath 为空则不限定。
 */
class WatchRootPathCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val input = context.triggerParam
        val rootPath = context.factParam.rootPath ?: ""
        val filter = ContainsFilter(
            pipelineId = context.pipelineId,
            filterName = "watchRootPath",
            triggerOn = rootPath,
            included = input.watchRootPath,
            failedReason = I18Variable(
                code = WebhookI18nConstants.BK_ARTIFACT_WATCH_ROOT_PATH_NOT_MATCH,
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
