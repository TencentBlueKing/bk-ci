package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.trigger.artifact.ArtifactWebhookUtils
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 监听根路径条件（仅自定义仓库）：watchRootPath 必须非空，且事件目录 path 必须以该值为目录前缀。
 * 例如配置 /aaa/bbb/ 可命中 /aaa/bbb/ccc/，不会命中 /aaa/bbbx/。
 */
class WatchRootPathCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val watchRootPath = context.triggerParam.watchRootPath
        val eventPath = context.factParam.path ?: ""
        if (!watchRootPath.isNullOrBlank() &&
            ArtifactWebhookUtils.startsWithWatchPath(eventPath, watchRootPath)
        ) return true
        context.response.failedReason = I18Variable(
            code = WebhookI18nConstants.BK_ARTIFACT_WATCH_ROOT_PATH_NOT_MATCH,
            params = listOf()
        ).toJsonStr()
        return false
    }
}
