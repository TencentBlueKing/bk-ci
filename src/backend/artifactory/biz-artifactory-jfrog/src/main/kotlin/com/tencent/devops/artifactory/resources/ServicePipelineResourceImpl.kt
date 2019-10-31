package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.ServicePipelineResource
import com.tencent.devops.artifactory.pojo.enums.Permission
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePipelineResourceImpl @Autowired constructor(private val pipelineService: PipelineService) :
    ServicePipelineResource {
    override fun hasPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: Permission
    ): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        val bkAuthPermission = when (permission) {
            Permission.VIEW -> AuthPermission.VIEW
            Permission.EDIT -> AuthPermission.EDIT
            Permission.SHARE -> AuthPermission.SHARE
            Permission.LIST -> AuthPermission.LIST
            Permission.EXECUTE -> AuthPermission.EXECUTE
        }
        return Result(pipelineService.hasPermission(userId, projectId, pipelineId, bkAuthPermission))
    }
}