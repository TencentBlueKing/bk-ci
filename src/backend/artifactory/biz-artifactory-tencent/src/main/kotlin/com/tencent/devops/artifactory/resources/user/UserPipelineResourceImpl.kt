package com.tencent.devops.artifactory.resources.user

import com.tencent.devops.artifactory.api.user.UserPipelineResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.enums.Permission
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineResourceImpl @Autowired constructor(
    val pipelineService: PipelineService
) : UserPipelineResource {

    override fun hasPermissionList(userId: String, projectId: String, path: String, permission: Permission): Result<List<FileInfo>> {
        checkParam(userId, projectId, path)
        val bkAuthPermission = when (permission) {
            Permission.VIEW -> BkAuthPermission.VIEW
            Permission.EDIT -> BkAuthPermission.EDIT
            Permission.SHARE -> BkAuthPermission.SHARE
            Permission.LIST -> BkAuthPermission.LIST
            Permission.EXECUTE -> BkAuthPermission.EXECUTE
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