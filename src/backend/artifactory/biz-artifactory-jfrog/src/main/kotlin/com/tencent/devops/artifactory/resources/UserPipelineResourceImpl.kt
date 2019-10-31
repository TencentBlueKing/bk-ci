package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.UserPipelineResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.enums.Permission
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineResourceImpl @Autowired constructor(val pipelineService: PipelineService) : UserPipelineResource {
    override fun hasPermissionList(
        userId: String,
        projectId: String,
        path: String,
        permission: Permission
    ): Result<List<FileInfo>> {
        checkParam(userId, projectId, path)
        val bkAuthPermission = when (permission) {
            Permission.VIEW -> AuthPermission.VIEW
            Permission.EDIT -> AuthPermission.EDIT
            Permission.SHARE -> AuthPermission.SHARE
            Permission.LIST -> AuthPermission.LIST
            Permission.EXECUTE -> AuthPermission.EXECUTE
        }
        return Result(pipelineService.list(userId, projectId, path, bkAuthPermission))
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun checkParam(userId: String, projectId: String, path: String) {
        checkParam(userId, projectId)
        if (path.isBlank()) {
            throw ParamBlankException("Invalid path")
        }
    }
}