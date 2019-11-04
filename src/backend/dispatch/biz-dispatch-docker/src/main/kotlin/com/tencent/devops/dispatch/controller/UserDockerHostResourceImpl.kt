/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.UserDockerHostResource
import com.tencent.devops.dispatch.pojo.ContainerInfo
import com.tencent.devops.dispatch.pojo.DebugStartParam
import com.tencent.devops.dispatch.service.DockerHostBuildService
import com.tencent.devops.dispatch.service.DockerHostDebugService
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.app.BuildEnv
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserDockerHostResourceImpl @Autowired constructor(
    private val dockerHostBuildService: DockerHostBuildService,
    private val dockerHostDebugService: DockerHostDebugService,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val client: Client
) : UserDockerHostResource {
    companion object {
        private val logger = LoggerFactory.getLogger(UserDockerHostResourceImpl::class.java)
    }

    override fun startDebug(userId: String, debugStartParam: DebugStartParam): Result<Boolean>? {
        checkParam(userId, debugStartParam.projectId, debugStartParam.pipelineId, debugStartParam.vmSeqId)

        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, pipelineAuthServiceCode, AuthResourceType.PIPELINE_DEFAULT,
                        debugStartParam.projectId, debugStartParam.pipelineId, AuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程(${debugStartParam.projectId})下编辑流水线(${debugStartParam.pipelineId})")
            throw PermissionForbiddenException("用户($userId)无权限在工程(${debugStartParam.projectId})下编辑流水线(${debugStartParam.pipelineId})")
        }

        // 先查询是否已经有启动调试容器了，如果有，直接返回成功
        val result = dockerHostDebugService.getDebugStatus(debugStartParam.pipelineId, debugStartParam.vmSeqId)
        if (result.status == 0) {
            logger.info("Container already exists, ContainerInfo: $result.data")
            return Result(true)
        }

        val buildEnvStr = if (null != debugStartParam.buildEnv && debugStartParam.buildEnv!!.isNotEmpty()) {
            try {
                val buildEnvs = client.get(ServiceContainerAppResource::class).getApp("linux")
                val buildEnvResult = mutableListOf<BuildEnv>()
                if (!(!buildEnvs.isOk() && null != buildEnvs.data && buildEnvs.data!!.isNotEmpty())) {
                    for (buildEnvParam in debugStartParam.buildEnv!!) {
                        for (buildEnv in buildEnvs.data!!) {
                            if (buildEnv.name == buildEnvParam.key && buildEnv.version == buildEnvParam.value) {
                                buildEnvResult.add(buildEnv)
                            }
                        }
                    }
                }
                ObjectMapper().writeValueAsString(buildEnvResult)
            } catch (e: Exception) {
                logger.error("get build env failed msg: $e")
                ""
            }
        } else {
            ""
        }
        logger.info("Container ready to start, buildEnvStr: $buildEnvStr")

        with(debugStartParam) {
            dockerHostDebugService.insertDebug(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                imageCode = imageCode,
                imageVersion = imageVersion,
                imageName = imageName,
                buildEnvStr = buildEnvStr,
                imageType = ImageType.getType(imageType),
                credentialId = credentialId
            )
        }

        return Result(true)
    }

    override fun getDebugStatus(userId: String, projectId: String, pipelineId: String, vmSeqId: String): Result<ContainerInfo>? {
        checkParam(userId, projectId, pipelineId, vmSeqId)

        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, pipelineAuthServiceCode, AuthResourceType.PIPELINE_DEFAULT, projectId, pipelineId, AuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
        }

        return dockerHostDebugService.getDebugStatus(pipelineId, vmSeqId)
    }

    override fun stopDebug(userId: String, projectId: String, pipelineId: String, vmSeqId: String): Result<Boolean>? {
        checkParam(userId, projectId, pipelineId, vmSeqId)

        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, pipelineAuthServiceCode, AuthResourceType.PIPELINE_DEFAULT, projectId, pipelineId, AuthPermission.EDIT)) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
        }

        return dockerHostDebugService.deleteDebug(pipelineId, vmSeqId)
    }

    override fun getContainerInfo(userId: String, projectId: String, pipelineId: String, buildId: String, vmSeqId: String): Result<ContainerInfo>? {
        checkParam(userId, projectId, pipelineId, vmSeqId)
        if (buildId.isBlank()) {
            throw ParamBlankException("BuildId参数非法")
        }
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, pipelineAuthServiceCode, AuthResourceType.PIPELINE_DEFAULT, projectId, pipelineId, AuthPermission.VIEW)) {
            throw PermissionForbiddenException("用户（$userId) 无权限获取流水线($pipelineId)详情")
        }

        return dockerHostBuildService.getContainerInfo(buildId, vmSeqId.toInt())
    }

    override fun getGreyWebConsoleProject(userId: String): Result<List<String>> {
        return Result(dockerHostDebugService.getGreyWebConsoleProj())
    }

    override fun cleanIp(userId: String, projectId: String, pipelineId: String, vmSeqId: String): Result<Boolean>? {
        checkParam(userId, projectId, pipelineId, vmSeqId)
        return dockerHostDebugService.cleanIp(projectId, pipelineId, vmSeqId)
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