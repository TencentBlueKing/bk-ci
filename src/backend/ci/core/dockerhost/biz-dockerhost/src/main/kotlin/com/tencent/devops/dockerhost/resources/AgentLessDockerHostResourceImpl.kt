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

package com.tencent.devops.dockerhost.resources

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.api.AgentLessDockerHostResource
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.exception.NoSuchImageException
import com.tencent.devops.dockerhost.services.DockerHostBuildAgentLessService
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.process.engine.common.VMUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletRequest

@RestResource
class AgentLessDockerHostResourceImpl @Autowired constructor(
    private val dockerHostBuildAgentLessService: DockerHostBuildAgentLessService,
    private val dockerHostBuildService: DockerHostBuildService
) : AgentLessDockerHostResource {

    override fun startBuild(dockerHostBuildInfo: DockerHostBuildInfo): Result<String> {
        return try {
            Result(dockerHostBuildAgentLessService.createContainer(dockerHostBuildInfo))
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
        logger.warn("[${dockerHostBuildInfo.buildId}] | Stop the container, containerId: ${dockerHostBuildInfo.containerId}")
        dockerHostBuildAgentLessService.stopContainer(dockerHostBuildInfo)

        return Result(true)
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

    companion object {
        private val logger = LoggerFactory.getLogger(AgentLessDockerHostResourceImpl::class.java)
    }
}
