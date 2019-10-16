package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.UserTstackResource
import com.tencent.devops.dispatch.pojo.TstackContainerInfo
import com.tencent.devops.dispatch.service.TstackBuildService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTstackResourceImpl @Autowired constructor(
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val tstackBuildService: TstackBuildService
) : UserTstackResource {
    companion object {
        private val logger = LoggerFactory.getLogger(UserTstackResourceImpl::class.java)
    }

    override fun startDebug(userId: String, projectId: String, pipelineId: String, vmSeqId: String): Result<Boolean> {
        checkParam(userId, projectId, pipelineId, vmSeqId)

        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, pipelineAuthServiceCode, BkAuthResourceType.PIPELINE_DEFAULT, projectId, pipelineId, BkAuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
        }

        return Result(tstackBuildService.startDebug(projectId, pipelineId, vmSeqId))
    }

    override fun getContainerInfo(userId: String, projectId: String, pipelineId: String, vmSeqId: String): Result<TstackContainerInfo?> {
        checkParam(userId, projectId, pipelineId, vmSeqId)

        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, pipelineAuthServiceCode, BkAuthResourceType.PIPELINE_DEFAULT, projectId, pipelineId, BkAuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
        }

        return Result(tstackBuildService.getContainerInfoWithToken(projectId, pipelineId, vmSeqId))
    }

    override fun stopDebug(userId: String, projectId: String, pipelineId: String, vmSeqId: String): Result<Boolean> {
        checkParam(userId, projectId, pipelineId, vmSeqId)

        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, pipelineAuthServiceCode, BkAuthResourceType.PIPELINE_DEFAULT, projectId, pipelineId, BkAuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
        }

        return Result(tstackBuildService.stopDebug(projectId, pipelineId, vmSeqId))
    }

    override fun getGreyWebConsoleProject(userId: String): Result<List<String>> {
        return Result(tstackBuildService.getGreyWebConsoleProject())
    }

    fun checkParam(userId: String, projectId: String, pipelineId: String, vmSeqId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (vmSeqId.isBlank()) {
            throw ParamBlankException("Invalid vmSeqID")
        }
    }
}