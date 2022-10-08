package com.tencent.devops.common.auth.utils

import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction

class AuthActionConvertUtils {


    companion object {

        fun covert(actions: List<CodeCCAuthAction>): List<PipelineAuthAction> {
            if (actions.isEmpty()) {
                return emptyList()
            }
            val pipelineActions = mutableSetOf<PipelineAuthAction>()
            actions.forEach {
                pipelineActions.addAll(covert(it))
            }
            return pipelineActions.toList()
        }

        fun covert(action: CodeCCAuthAction?): List<PipelineAuthAction> {
            if (action == null) {
                return emptyList()
            }
            return when (action) {
                CodeCCAuthAction.TASK_MANAGE ->
                    listOf<PipelineAuthAction>(
                        PipelineAuthAction.DELETE,
                        PipelineAuthAction.DOWNLOAD,
                        PipelineAuthAction.EDIT,
                        PipelineAuthAction.EXECUTE,
                        PipelineAuthAction.LIST,
                        PipelineAuthAction.SHARE,
                        PipelineAuthAction.VIEW
                    )
                CodeCCAuthAction.ANALYZE ->
                    listOf<PipelineAuthAction>(
                        PipelineAuthAction.DOWNLOAD,
                        PipelineAuthAction.EXECUTE,
                        PipelineAuthAction.LIST,
                        PipelineAuthAction.SHARE,
                        PipelineAuthAction.VIEW
                    )
                CodeCCAuthAction.DEFECT_MANAGE ->
                    listOf<PipelineAuthAction>(
                        PipelineAuthAction.EXECUTE,
                        PipelineAuthAction.LIST,
                        PipelineAuthAction.SHARE,
                        PipelineAuthAction.VIEW
                    )
                else ->
                    listOf<PipelineAuthAction>(
                        PipelineAuthAction.LIST,
                        PipelineAuthAction.VIEW
                    )
            }
        }

    }


}