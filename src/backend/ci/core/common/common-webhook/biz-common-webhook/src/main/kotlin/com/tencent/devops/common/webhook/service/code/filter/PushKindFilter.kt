package com.tencent.devops.common.webhook.service.code.filter

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PushActionType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PushOperationType
import org.slf4j.LoggerFactory

class PushKindFilter(
    private val pipelineId: String,
    private val operationKind: String,
    private val actionKind: String,
    private val isMonitorCreate: Boolean,
    private val isMonitorUpdate: Boolean
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectNameFilter::class.java)
    }

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        // isMonitorCreate 为true时 监听创建分支的push事件
        logger.info(
            "$pipelineId|isMonitorCreate:$isMonitorCreate|isMonitorUpdate:$isMonitorUpdate|" +
                    "operationKind:$operationKind|actionKind:$actionKind|push kind filter"
        )
        var isUpdatePush = false
        if(isMonitorUpdate) isUpdatePush = isUpdatePush(operationKind, actionKind)
        var isCreatePush = false
        if(isMonitorCreate) {
            // 目前来看 不是pushFile事件 即为createBranch事件，但是还是用不同方法做区分，增加代码可读性和之后扩展
            isCreatePush = isCreatePush(operationKind, actionKind)
        }

        return isUpdatePush || isCreatePush
    }

    private fun isCreatePush(operationKind: String, actionKind: String) :Boolean{
        if(operationKind == PushOperationType.CREATE.value && actionKind == PushActionType.CREATE_BRANCH.value) {
            // 如果operationKind 为create 并且是在工蜂仓库上创建则非更新
            return true
        }
        // 否则即为updatFile 并且 用户本地向远程仓库创建新分支会被认为是更新
        return false
    }

    private fun isUpdatePush(operationKind: String, actionKind: String):Boolean{
        if(operationKind == PushOperationType.CREATE.value && actionKind == PushActionType.CREATE_BRANCH.value) {
            // 如果operationKind 为create 并且是在工蜂仓库上创建则非更新
            return false
        }
        // 否则即为updatFile 并且 用户本地向远程仓库创建新分支会被认为是更新
        return true
    }
}
