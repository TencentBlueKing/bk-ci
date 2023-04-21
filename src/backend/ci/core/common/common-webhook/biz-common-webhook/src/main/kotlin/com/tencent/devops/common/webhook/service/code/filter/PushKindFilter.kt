package com.tencent.devops.common.webhook.service.code.filter

import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionType
import org.slf4j.LoggerFactory

class PushKindFilter(
    private val pipelineId: String,
    private val checkCreateAndUpdate: Boolean?,
    private val actionList: List<String>
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectNameFilter::class.java)
    }

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        // isMonitorCreate 为true时 监听创建分支的push事件
        logger.info(
            "$pipelineId|actionList:$actionList|" +
                "checkCreateAndUpdate:$checkCreateAndUpdate|push kind filter"
        )
        if (actionList.isEmpty()) {
            return true
        }
        actionList.forEach {
            if (it == TGitPushActionType.NEW_BRANCH.value && checkCreateAndUpdate != null) {
                return true
            }
            if (it == TGitPushActionType.PUSH_FILE.value && checkCreateAndUpdate != false) {
                return true
            }
        }

        return false
    }
}
