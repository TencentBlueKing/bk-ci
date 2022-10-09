package com.tencent.devops.common.auth.api

import com.alibaba.fastjson.JSONObject
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.common.auth.api.external.AuthExRegisterApi
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction
import com.tencent.devops.common.auth.pojo.GithubAuthProperties
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory

class GithubAuthExRegisterApi(
    private val client: Client,
    private val authTaskService: AuthTaskService,
    private val properties: GithubAuthProperties
) : AuthExRegisterApi {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun registerCodeCCTask(user: String, taskId: String, taskName: String, projectId: String): Boolean {
        val pipelineId = authTaskService.getTaskPipelineId(taskId.toLong())
        if (pipelineId.isEmpty()) {
            return true
        }
        PipelineAuthAction.values().forEach {
            registerCodeCCTaskPermission(user, pipelineId, it.actionName, projectId)
        }
        return true
    }

    private fun registerCodeCCTaskPermission(user: String, pipelineId: String, action: String, projectId: String) {
        val grantInstanceDTO = GrantInstanceDTO(
            properties.pipelineResourceType ?: "pipelineId",
            pipelineId,
            null,
            action,
            user
        )
        val result = client.getDevopsService(ServicePermissionAuthResource::class.java).grantInstancePermission(
            user, properties.token ?: "", projectId, grantInstanceDTO
        )
        if (result.isNotOk()) {
            logger.error(
                "registerCodeCCTaskPermission $user $pipelineId $action $projectId fail," +
                        " result ${JSONObject.toJSONString(result)}"
            )
        }
    }

    override fun deleteCodeCCTask(taskId: String, projectId: String): Boolean {
        return true
    }
}