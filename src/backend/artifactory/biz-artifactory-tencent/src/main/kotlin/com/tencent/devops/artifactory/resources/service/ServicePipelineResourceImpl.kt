package com.tencent.devops.artifactory.resources.service

import com.tencent.devops.artifactory.api.service.ServicePipelineResource
import com.tencent.devops.artifactory.pojo.enums.Permission
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePipelineResourceImpl @Autowired constructor(
    private val pipelineService: PipelineService
) : ServicePipelineResource {

    override fun hasPermission(userId: String, projectId: String, pipelineId: String, permission: Permission): Result<Boolean> {
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
            Permission.VIEW -> BkAuthPermission.VIEW
            Permission.EDIT -> BkAuthPermission.EDIT
            Permission.SHARE -> BkAuthPermission.SHARE
            Permission.LIST -> BkAuthPermission.LIST
            Permission.EXECUTE -> BkAuthPermission.EXECUTE
        }
        return Result(pipelineService.hasPermission(userId, projectId, pipelineId, bkAuthPermission))
    }
}