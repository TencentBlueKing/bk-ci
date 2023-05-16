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
import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.docker.api.user.UserDockerHostResource
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerDebugDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.docker.pojo.ContainerInfo
import com.tencent.devops.dispatch.docker.pojo.DebugStartParam
import com.tencent.devops.dispatch.docker.pojo.DockerHostLoad
import com.tencent.devops.dispatch.docker.service.DockerHostBuildService
import com.tencent.devops.dispatch.docker.service.debug.impl.DockerHostDebugServiceImpl
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserDockerHostResourceImpl @Autowired constructor(
    private val dockerHostBuildService: DockerHostBuildService,
    private val dockerHostDebugService: DockerHostDebugServiceImpl,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val pipelineDockerDebugDao: PipelineDockerDebugDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val dockerHostUtils: DockerHostUtils,
    private val dslContext: DSLContext,
    private val bkTag: BkTag
) : UserDockerHostResource {
    companion object {
        private val logger = LoggerFactory.getLogger(UserDockerHostResourceImpl::class.java)
    }

    override fun startDebug(userId: String, debugStartParam: DebugStartParam): Result<Boolean>? {
        checkPermission(userId, debugStartParam.projectId, debugStartParam.pipelineId, debugStartParam.vmSeqId)

        logger.info("[$userId]| start debug, debugStartParam: $debugStartParam")
        // 查询是否已经有启动调试容器了，如果有，直接返回成功
        val result = dockerHostDebugService.getDebugStatus(debugStartParam.pipelineId, debugStartParam.vmSeqId)
        if (result.status == 0) {
            logger.info(
                "${debugStartParam.pipelineId}|startDebug|j(${debugStartParam.vmSeqId})|" +
                        "Container Exist|ContainerId=${result.data?.containerId}"
            )
            return Result(true)
        }

        // 查询是否存在构建机可启动调试，查看当前构建机的状态，如果running且已经容器，则直接复用当前running的containerId
        val dockerBuildHistoryList = pipelineDockerBuildDao.getLatestBuild(
            dslContext,
            debugStartParam.pipelineId,
            debugStartParam.vmSeqId.toInt()
        )

        val dockerIp: String
        val poolNo: Int
        if (dockerBuildHistoryList.size > 0 && dockerBuildHistoryList[0].dockerIp.isNotEmpty()) {
            val dockerBuildHistory = dockerBuildHistoryList[0]
            // running状态且容器已创建，则复用
            if (dockerBuildHistory.status == PipelineTaskStatus.RUNNING.status &&
                dockerBuildHistory.containerId.isNotEmpty()
            ) {
                val containerStatusRunning = dockerHostDebugService.checkContainerStatus(
                    projectId = debugStartParam.projectId,
                    pipelineId = debugStartParam.pipelineId,
                    vmSeqId = debugStartParam.vmSeqId,
                    dockerIp = dockerBuildHistory.dockerIp,
                    containerId = dockerBuildHistory.containerId
                )

                if (containerStatusRunning) {
                    pipelineDockerDebugDao.insertDebug(
                        dslContext = dslContext,
                        projectId = debugStartParam.projectId,
                        pipelineId = debugStartParam.pipelineId,
                        vmSeqId = debugStartParam.vmSeqId,
                        poolNo = dockerBuildHistory.poolNo,
                        status = PipelineTaskStatus.RUNNING,
                        token = "",
                        imageName = "",
                        hostTag = dockerBuildHistory.dockerIp,
                        containerId = dockerBuildHistory.containerId,
                        buildEnv = "",
                        registryUser = "",
                        registryPwd = "",
                        imageType = ""
                    )

                    logger.info(
                        "${debugStartParam.pipelineId}|startDebug|j(${debugStartParam.vmSeqId})|" +
                                "Container running|ContainerId=${dockerBuildHistory.containerId}"
                    )
                    return Result(true)
                }
            }

            dockerIp = dockerBuildHistory.dockerIp
            poolNo = dockerBuildHistory.poolNo

            dockerHostDebugService.startDebug(
                dockerIp = dockerIp,
                userId = userId,
                poolNo = poolNo,
                debugStartParam = debugStartParam,
                startupMessage = dockerBuildHistory.startupMessage
            )
        } else {
            throw ErrorCodeException(
                errorCode = "${ErrorCodeEnum.NO_CONTAINER_IS_READY_DEBUG.errorCode}",
                defaultMessage = "Can not found debug container.",
                params = arrayOf(debugStartParam.pipelineId)
            )
        }

        return Result(true)
    }

    override fun getDebugStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String
    ): Result<ContainerInfo>? {
        checkPermission(userId, projectId, pipelineId, vmSeqId)

        return dockerHostDebugService.getDebugStatus(pipelineId, vmSeqId)
    }

    override fun stopDebug(userId: String, projectId: String, pipelineId: String, vmSeqId: String): Result<Boolean>? {
        checkPermission(userId, projectId, pipelineId, vmSeqId)

        return dockerHostDebugService.deleteDebug(pipelineId, vmSeqId)
    }

    override fun getContainerInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String
    ): Result<ContainerInfo>? {
        checkPermission(userId, projectId, pipelineId, vmSeqId)

        return dockerHostBuildService.getContainerInfo(buildId, vmSeqId.toInt())
    }

    override fun getGreyWebConsoleProject(userId: String): Result<List<String>> {
        return Result(dockerHostDebugService.getGreyWebConsoleProj())
    }

    override fun cleanIp(userId: String, projectId: String, pipelineId: String, vmSeqId: String): Result<Boolean>? {
        checkParam(userId, projectId, pipelineId, vmSeqId)
        return dockerHostDebugService.cleanIp(projectId, pipelineId, vmSeqId)
    }

    override fun getDockerHostLoad(userId: String): Result<DockerHostLoad> {
        return Result(dockerHostBuildService.getDockerHostLoad(userId))
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
            val language = I18nUtil.getLanguage(userId)
            val permission = AuthPermission.EDIT
            validPipelinePermission(
                userId = userId,
                authResourceType = AuthResourceType.PIPELINE_DEFAULT,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    language,
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(language),
                        pipelineId
                    )
                )
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
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = CommonMessageCode.USER_NO_PIPELINE_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permission.getI18n(I18nUtil.getLanguage(userId)))
            )
        }
    }
}
