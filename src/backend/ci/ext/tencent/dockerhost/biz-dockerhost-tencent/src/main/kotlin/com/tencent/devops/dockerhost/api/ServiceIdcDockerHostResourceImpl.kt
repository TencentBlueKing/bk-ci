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

package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.constant.I18NConstant.BK_BUILD_ENVIRONMENT_STARTS_SUCCESSFULLY
import com.tencent.devops.common.api.constant.I18NConstant.BK_DOCKER_BUILDER_RUNS_TOO_MANY
import com.tencent.devops.common.api.constant.I18NConstant.BK_FAILED_TO_START_ERROR_MESSAGE
import com.tencent.devops.common.api.constant.I18NConstant.BK_FAILED_TO_START_IMAGE_NOT_EXIST
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.exception.NoSuchImageException
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.MAX_CONTAINER_NUM
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceIdcDockerHostResourceImpl @Autowired constructor(
    private val dockerHostBuildService: DockerHostBuildService
) : ServiceIdcDockerHostResource {

    override fun startBuild(dockerHostBuildInfo: DockerHostBuildInfo): Result<String> {
        try {
            val containerNum = dockerHostBuildService.getContainerNum()
            if (containerNum >= MAX_CONTAINER_NUM) {
                logger.warn("Too many containers in this host, break to start build.")
                return Result(1,
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_DOCKER_BUILDER_RUNS_TOO_MANY,
                            language = I18nUtil.getDefaultLocaleLanguage(),
                            params = arrayOf(CommonUtils.getInnerIP(), containerNum.toString())
                        )
                )
            }
            logger.warn("Create container, dockerStartBuildInfo: $dockerHostBuildInfo")

            val containerId = dockerHostBuildService.createContainer(dockerHostBuildInfo)
            dockerHostBuildService.log(
                buildId = dockerHostBuildInfo.buildId,
                message = MessageUtil.getMessageByLocale(
                    messageCode = BK_BUILD_ENVIRONMENT_STARTS_SUCCESSFULLY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                tag = dockerHostBuildInfo.containerId,
                containerHashId = dockerHostBuildInfo.containerHashId
            )
            return Result(containerId)
        } catch (e: NoSuchImageException) {
            logger.warn(
                "BKSystemMonitor|Create container container failed, no such image. " +
                    "pipelineId: ${dockerHostBuildInfo.pipelineId}, " +
                    "vmSeqId: ${dockerHostBuildInfo.vmSeqId}, err: ${e.message}"
            )
            dockerHostBuildService.log(
                buildId = dockerHostBuildInfo.buildId,
                message = MessageUtil.getMessageByLocale(
                        messageCode = BK_FAILED_TO_START_IMAGE_NOT_EXIST,
                        language = I18nUtil.getDefaultLocaleLanguage(),
                        params = arrayOf(dockerHostBuildInfo.imageName)
                    ),
                tag = dockerHostBuildInfo.containerId,
                containerHashId = dockerHostBuildInfo.containerHashId
            )
            return Result(2, e.message, "")
        } catch (e: ContainerException) {
            logger.error(
                "BKSystemMonitor|Create container failed, rollback build. " +
                    "buildId: ${dockerHostBuildInfo.buildId}, vmSeqId: ${dockerHostBuildInfo.vmSeqId}"
            )
            dockerHostBuildService.log(
                buildId = dockerHostBuildInfo.buildId,
                message = MessageUtil.getMessageByLocale(
                        messageCode = BK_FAILED_TO_START_ERROR_MESSAGE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + "：${e.message}",
                tag = dockerHostBuildInfo.containerId,
                containerHashId = dockerHostBuildInfo.containerHashId
            )
            return Result(1, e.message, "")
        }
    }

    override fun endBuild(dockerHostBuildInfo: DockerHostBuildInfo): Result<Boolean> {
        logger.warn("Stop the container, containerId: ${dockerHostBuildInfo.containerId}")
        dockerHostBuildService.stopContainer(dockerHostBuildInfo)

        return Result(true)
    }

    override fun getContainerCount(): Result<Int> {
        return Result(0, "success", dockerHostBuildService.getContainerNum())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceIdcDockerHostResourceImpl::class.java)
    }
}
