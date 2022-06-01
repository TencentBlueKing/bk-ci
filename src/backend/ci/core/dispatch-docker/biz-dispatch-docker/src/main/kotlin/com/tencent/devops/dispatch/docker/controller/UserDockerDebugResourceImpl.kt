/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.docker.controller

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.docker.api.user.UserDockerDebugResource
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerDebugDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.docker.pojo.DebugStartParam
import com.tencent.devops.dispatch.docker.service.DockerHostBuildService
import com.tencent.devops.dispatch.docker.service.DockerHostDebugService
import com.tencent.devops.dispatch.docker.service.ExtDebugService
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
@Suppress("ALL")
class UserDockerDebugResourceImpl @Autowired constructor(
    private val dockerHostBuildService: DockerHostBuildService,
    private val dockerHostDebugService: DockerHostDebugService,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val pipelineDockerDebugDao: PipelineDockerDebugDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val dockerHostUtils: DockerHostUtils,
    private val extDebugService: ExtDebugService,
    private val dslContext: DSLContext,
    private val bkTag: BkTag
) : UserDockerDebugResource {
    companion object {
        private val logger = LoggerFactory.getLogger(UserDockerDebugResourceImpl::class.java)
    }

    override fun startDebug(userId: String, debugStartParam: DebugStartParam): Result<String>? {
        // checkPermission(userId, debugStartParam.projectId, debugStartParam.pipelineId, debugStartParam.vmSeqId)

        logger.info("[$userId]| start debug, debugStartParam: $debugStartParam")
        // 查询是否已经有启动调试容器了，如果有，直接返回成功
        val historyPair = dockerHostDebugService.getDebugHistory(
            debugStartParam.pipelineId,
            debugStartParam.vmSeqId
        )
        if (historyPair != null) {
            val wsUrl = dockerHostDebugService.getWsUrl(
                dockerIp = historyPair.first,
                projectId = debugStartParam.projectId,
                pipelineId = debugStartParam.pipelineId,
                containerId = historyPair.second
            )
            logger.info(
                "${debugStartParam.pipelineId}|startDebug|j(${debugStartParam.vmSeqId})|" +
                        "Container Exist|wsUrl=$wsUrl"
            )
            return Result(wsUrl)
        }

        var buildHistory: TDispatchPipelineDockerBuildRecord? = null
        if (debugStartParam.buildId.isNullOrBlank()) {
            // 查询是否存在构建机可启动调试，查看当前构建机的状态，如果running且已经容器，则直接复用当前running的containerId
            val dockerBuildHistoryList = pipelineDockerBuildDao.getLatestBuild(
                dslContext,
                debugStartParam.pipelineId,
                debugStartParam.vmSeqId.toInt()
            )

            if (dockerBuildHistoryList.size > 0 && dockerBuildHistoryList[0].dockerIp.isNotEmpty()) {
                buildHistory = dockerBuildHistoryList[0]
            }
        } else {
            buildHistory = pipelineDockerBuildDao.getBuild(
                dslContext = dslContext,
                buildId = debugStartParam.buildId!!,
                vmSeqId = debugStartParam.vmSeqId.toInt()
            )
        }

        if (buildHistory != null) {
            val containerId = dockerHostDebugService.startDebug(
                dockerIp = buildHistory.dockerIp,
                userId = userId,
                poolNo = buildHistory.poolNo,
                debugStartParam = debugStartParam,
                startupMessage = buildHistory.startupMessage
            )
            return Result(
                dockerHostDebugService.getWsUrl(
                    dockerIp = buildHistory.dockerIp,
                    projectId = debugStartParam.projectId,
                    pipelineId = debugStartParam.pipelineId,
                    containerId = containerId
                )
            )
        } else {
            // 没有构建历史的情况下debug，先确认是否来自其他构建集群
            val debugUrl = extDebugService.startDebug(
                userId = userId,
                projectId = debugStartParam.projectId,
                pipelineId = debugStartParam.pipelineId,
                buildId = debugStartParam.buildId,
                vmSeqId = debugStartParam.vmSeqId
            ) ?: throw ErrorCodeException(
                errorCode = "2103503",
                defaultMessage = "Can not found debug container.",
                params = arrayOf(debugStartParam.pipelineId)
            )

            return Result(debugUrl)
        }
    }

    override fun stopDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String?
    ): Result<Boolean>? {
        checkPermission(userId, projectId, pipelineId, vmSeqId)

        val pipelineDockerDebug = pipelineDockerDebugDao.getDebug(dslContext, pipelineId, vmSeqId)
        if (pipelineDockerDebug != null) {
            return dockerHostDebugService.deleteDebug(pipelineId, vmSeqId)
        } else {
            return Result(
                extDebugService.stopDebug(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    containerName ?: ""
                )
            )
        }
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

    private fun checkPermission(userId: String, projectId: String, pipelineId: String, vmSeqId: String) {
        checkParam(userId, projectId, pipelineId, vmSeqId)

        val consulTag = bkTag.getLocalTag()
        if (!consulTag.contains("stream") && !consulTag.contains("gitci")) {
            validPipelinePermission(
                userId = userId,
                authResourceType = AuthResourceType.PIPELINE_DEFAULT,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT,
                message = "用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)"
            )
        }
    }

    private fun validPipelinePermission(
        userId: String,
        authResourceType: AuthResourceType,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String?
    ) {
        if (!bkAuthPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = authResourceType,
                projectCode = projectId,
                resourceCode = pipelineId,
                permission = permission
            )
        ) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${permission.value}",
                defaultMessage = permission.alias
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permissionMsg)
            )
        }
    }
}
