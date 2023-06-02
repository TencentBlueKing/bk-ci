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

package com.tencent.devops.buildless.controller

import com.tencent.devops.buildless.ContainerPoolExecutor
import com.tencent.devops.buildless.api.service.ServiceBuildlessResource
import com.tencent.devops.buildless.common.ErrorCodeEnum
import com.tencent.devops.buildless.exception.BuildLessException
import com.tencent.devops.buildless.exception.NoIdleContainerException
import com.tencent.devops.buildless.pojo.BuildLessEndInfo
import com.tencent.devops.buildless.pojo.BuildLessStartInfo
import com.tencent.devops.buildless.service.BuildLessContainerService
import com.tencent.devops.buildless.utils.ThreadPoolName
import com.tencent.devops.buildless.utils.ThreadPoolUtils
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceBuildLessResourceImpl @Autowired constructor(
    private val containerPoolExecutor: ContainerPoolExecutor,
    private val buildLessContainerService: BuildLessContainerService
) : ServiceBuildlessResource {
    override fun startBuild(buildLessEndInfo: BuildLessStartInfo): Result<String> {
        with(buildLessEndInfo) {
            return try {
                logger.warn("Allocate container, dockerHostBuildInfo: $this")
                containerPoolExecutor.execute(this)
                Result("")
            } catch (e: NoIdleContainerException) {
                logger.warn("$buildId|$vmSeqId|$executionCount No idle container, reject the execution.")
                Result(e.errorCode, e.message)
            } catch (e: BuildLessException) {
                logger.error("$buildId|$vmSeqId|$executionCount allocate container failed. ${e.message}")
                Result(e.errorCode, ": ${e.message}", "")
            } catch (e: Exception) {
                logger.error("$buildId|$vmSeqId|$executionCount allocate container failed.", e)
                Result(ErrorCodeEnum.SYSTEM_ERROR.errorCode, ": ${e.message}", "")
            }
        }
    }

    override fun endBuild(buildLessEndInfo: BuildLessEndInfo): Result<Boolean> {
        logger.warn("${buildLessEndInfo.buildId}|${buildLessEndInfo.vmSeqId} Stop the container, " +
                "containerId: ${buildLessEndInfo.containerId}")

        ThreadPoolUtils.getInstance().getThreadPool(ThreadPoolName.BUILD_END.name).submit {
            buildLessContainerService.stopContainer(
                buildId = buildLessEndInfo.buildId,
                vmSeqId = buildLessEndInfo.vmSeqId.toString(),
                containerId = buildLessEndInfo.containerId
            )

            // 容器关闭后接着创建新容器
            containerPoolExecutor.addContainer()
        }

        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceBuildLessResourceImpl::class.java)
    }
}
