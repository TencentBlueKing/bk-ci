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
import com.tencent.devops.dockerhost.api.ServiceDockerHostResource
import com.tencent.devops.dockerhost.pojo.CheckImageRequest
import com.tencent.devops.dockerhost.pojo.CheckImageResponse
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.Status
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import com.tencent.devops.dockerhost.services.DockerService
import com.tencent.devops.dockerhost.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletRequest

@RestResource
class ServiceDockerHostResourceImpl @Autowired constructor(
    private val dockerService: DockerService,
    private val dockerHostBuildService: DockerHostBuildService
) : ServiceDockerHostResource {
    override fun dockerBuild(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, elementId: String?, dockerBuildParam: DockerBuildParam, request: HttpServletRequest): Result<Boolean> {
        checkReq(request)
        logger.info("Enter ServiceDockerHostResourceImpl.dockerBuild...")
        return Result(dockerService.buildImage(projectId, pipelineId, vmSeqId, buildId, elementId, dockerBuildParam))
    }

    override fun getDockerBuildStatus(vmSeqId: String, buildId: String, request: HttpServletRequest): Result<Pair<Status, String?>> {
        checkReq(request)
        logger.info("Enter ServiceDockerHostResourceImpl.getDockerBuildStatus...")
        return Result(dockerService.getBuildResult(vmSeqId, buildId))
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

    override fun checkImage(buildId: String, checkImageRequest: CheckImageRequest): Result<CheckImageResponse?> {
        return dockerHostBuildService.checkImage(buildId, checkImageRequest)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceDockerHostResourceImpl::class.java)
    }
}