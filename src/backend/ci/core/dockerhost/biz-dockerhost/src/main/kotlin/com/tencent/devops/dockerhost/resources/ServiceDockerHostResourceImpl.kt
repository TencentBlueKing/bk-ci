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

package com.tencent.devops.dockerhost.resources

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.api.ServiceDockerHostResource
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.exception.NoSuchImageException
import com.tencent.devops.dockerhost.pojo.CheckImageRequest
import com.tencent.devops.dockerhost.pojo.CheckImageResponse
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.DockerHostLoad
import com.tencent.devops.dockerhost.pojo.DockerLogsResponse
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.pojo.Status
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import com.tencent.devops.dockerhost.services.DockerService
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.process.engine.common.VMUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletRequest

@RestResource
class ServiceDockerHostResourceImpl @Autowired constructor(
    private val dockerService: DockerService,
    private val dockerHostBuildService: DockerHostBuildService
) : ServiceDockerHostResource {

    override fun dockerBuild(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        elementId: String?,
        dockerBuildParam: DockerBuildParam,
        request: HttpServletRequest
    ): Result<Boolean> {
        checkReq(request)
        logger.info("[$buildId]|Enter ServiceDockerHostResourceImpl.dockerBuild...")
        return Result(dockerService.buildImage(
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            buildId = buildId,
            elementId = elementId,
            dockerBuildParam = dockerBuildParam
        ))
    }

    override fun getDockerBuildStatus(
        vmSeqId: String,
        buildId: String,
        request: HttpServletRequest
    ): Result<Pair<Status, String>> {
        checkReq(request)
        logger.info("[$buildId]|Enter ServiceDockerHostResourceImpl.getDockerBuildStatus...")
        return Result(dockerService.getBuildResult(vmSeqId, buildId))
    }

    override fun dockerRun(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        dockerRunParam: DockerRunParam,
        request: HttpServletRequest
    ): Result<DockerRunResponse> {
        checkReq(request)
        logger.info("[$buildId]|Enter ServiceDockerHostResourceImpl.dockerRun...")
        return Result(dockerService.dockerRun(projectId, pipelineId, vmSeqId, buildId, dockerRunParam))
    }

    override fun getDockerRunLogs(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        containerId: String,
        logStartTimeStamp: Int,
        printLog: Boolean?,
        request: HttpServletRequest
    ): Result<DockerLogsResponse> {
        checkReq(request)
        return Result(
            dockerService.getDockerRunLogs(
                projectId,
                pipelineId,
                vmSeqId,
                buildId,
                containerId,
                logStartTimeStamp,
                printLog
            )
        )
    }

    override fun dockerStop(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        containerId: String,
        request: HttpServletRequest
    ): Result<Boolean> {
        checkReq(request)
        logger.info("[$buildId]|Enter ServiceDockerHostResourceImpl.dockerStop...")
        dockerService.dockerStop(projectId, pipelineId, vmSeqId, buildId, containerId)
        return Result(true)
    }

    override fun startBuild(dockerHostBuildInfo: DockerHostBuildInfo): Result<String> {
        return try {
            Result(dockerService.startBuild(dockerHostBuildInfo))
        } catch (e: NoSuchImageException) {
            logger.error("Create container container failed, no such image. pipelineId: ${dockerHostBuildInfo.pipelineId}, vmSeqId: ${dockerHostBuildInfo.vmSeqId}, err: ${e.message}")
            dockerHostBuildService.log(
                buildId = dockerHostBuildInfo.buildId,
                red = true,
                message = "构建环境启动失败，镜像不存在, 镜像:${dockerHostBuildInfo.imageName}",
                tag = VMUtils.genStartVMTaskId(dockerHostBuildInfo.vmSeqId.toString()),
                containerHashId = dockerHostBuildInfo.containerHashId
            )
            Result(2, "构建环境启动失败，镜像不存在, 镜像:${dockerHostBuildInfo.imageName}", "")
        } catch (e: ContainerException) {
            logger.error("Create container failed, rollback build. buildId: ${dockerHostBuildInfo.buildId}, vmSeqId: ${dockerHostBuildInfo.vmSeqId}")
            dockerHostBuildService.log(
                buildId = dockerHostBuildInfo.buildId,
                red = true,
                message = "构建环境启动失败，错误信息:${e.message}",
                tag = VMUtils.genStartVMTaskId(dockerHostBuildInfo.vmSeqId.toString()),
                containerHashId = dockerHostBuildInfo.containerHashId
            )
            Result(2, "构建环境启动失败，错误信息:${e.message}", "")
        }
    }

    override fun endBuild(dockerHostBuildInfo: DockerHostBuildInfo): Result<Boolean> {
        logger.warn("Stop the container, containerId: ${dockerHostBuildInfo.containerId}")
        dockerHostBuildService.stopContainer(dockerHostBuildInfo)

        return Result(true)
    }

    override fun getDockerHostLoad(): Result<DockerHostLoad> {
        return Result(dockerService.getDockerHostLoad())
    }

    override fun getContainerStatus(containerId: String): Result<Boolean> {
        return Result(dockerService.getContainerStatus(containerId))
    }

    private fun checkReq(request: HttpServletRequest) {
        var ip = request.getHeader("x-forwarded-for")
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        if (ip != null && (CommonUtils.getInnerIP() == ip || ip.startsWith("172.32"))) { // 只允许从本机调用
            logger.info("Request from $ip")
        } else {
            logger.info("Request from $ip")
            logger.info("Local ip :${CommonUtils.getInnerIP()}")
            throw PermissionForbiddenException("不允许的操作！")
        }
    }

    override fun checkImage(
        buildId: String,
        checkImageRequest: CheckImageRequest,
        containerId: String?,
        containerHashId: String?
    ): Result<CheckImageResponse?> {
        return dockerHostBuildService.checkImage(buildId, checkImageRequest, containerId, containerHashId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceDockerHostResourceImpl::class.java)
    }
}