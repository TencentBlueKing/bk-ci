package com.tencent.bk.codecc.task.service.impl

import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.auth.api.external.PermissionService
import com.tencent.devops.common.auth.api.pojo.external.response.AuthMgrResourceResponse
import com.tencent.devops.common.auth.api.pojo.external.response.AuthTaskRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SimplePermissionService @Autowired constructor(
    private val taskRepository: TaskRepository
): PermissionService {
    override fun getMgrResource(projectId: String, resourceTypeCode: String, resourceCode: String): AuthMgrResourceResponse {
        val taskOwner = when (resourceTypeCode) {
            "task" -> {
                taskRepository.findByTaskId(resourceCode.toLong())?.taskOwner
            }
            "pipeline" -> {
                taskRepository.findByPipelineId(resourceCode)?.taskOwner
            }
            else -> throw CodeCCException("unsupported resource type code: $resourceTypeCode")
        } ?: throw CodeCCException("resource not found for: $String, $resourceTypeCode, $resourceCode")

        val taskRole = AuthTaskRole()
        taskRole.userList = taskOwner
        taskRole.roleCode = "manager"
        return AuthMgrResourceResponse(listOf(), listOf(taskRole))
    }
}