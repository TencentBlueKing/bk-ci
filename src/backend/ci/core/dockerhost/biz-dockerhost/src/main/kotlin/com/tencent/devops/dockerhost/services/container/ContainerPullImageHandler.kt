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

package com.tencent.devops.dockerhost.services.container

import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.UnauthorizedException
import com.tencent.devops.dockerhost.common.ErrorCodeEnum
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.services.Handler
import com.tencent.devops.dockerhost.services.LocalImageCache
import com.tencent.devops.dockerhost.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ContainerPullImageHandler(
    private val dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
) : Handler<ContainerHandlerContext>(dockerHostConfig, dockerHostBuildApi) {
    override fun handlerRequest(handlerContext: ContainerHandlerContext) {
        with(handlerContext) {
            formatImageName = CommonUtils.normalizeImageName(originImageName)
            val taskId = taskId()

            try {
                LocalImageCache.saveOrUpdate(formatImageName!!)
                val authConfig = CommonUtils.getAuthConfig(
                    imageType = imageType,
                    dockerHostConfig = dockerHostConfig,
                    imageName = formatImageName!!,
                    registryUser = registryUser,
                    registryPwd = registryPwd
                )
                log(buildId, "Start pulling image $formatImageName!!", taskId, containerHashId)
                httpLongDockerCli.pullImageCmd(formatImageName!!).withAuthConfig(authConfig)
                    .exec(MyPullImageResultCallback(buildId, dockerHostBuildApi, taskId, containerHashId))
                    .awaitCompletion()
                log(buildId, "Pull the image successfully, " +
                    "ready to start the build environment...", taskId, containerHashId)
            } catch (t: UnauthorizedException) {
                val errorMessage = "No permission to pull image $formatImageName，" +
                    "Please check if the image path or credentials are correct." +
                        "$buildId|$containerHashId]"
                logger.error(errorMessage, t)
                // 直接失败，禁止使用本地镜像
                throw ContainerException(
                    errorCodeEnum = ErrorCodeEnum.NO_AUTH_PULL_IMAGE_ERROR,
                    message = errorMessage
                )
            } catch (t: NotFoundException) {
                val errorMessage = "Image does not exist $formatImageName!!，" +
                    "Please check if the image path or credentials are correct." +
                        "$buildId|$containerHashId]"
                logger.error(errorMessage, t)
            } catch (t: Throwable) {
                logger.warn("Fail to pull the image $formatImageName!! of build $buildId", t)
                log(
                    buildId = buildId,
                    message = "Failed to pull image: ${t.message}",
                    tag = taskId,
                    containerHashId = containerHashId
                )
                log(
                    buildId = buildId,
                    message = "Trying to boot from a local image...",
                    tag = taskId,
                    containerHashId = containerHashId
                )
            }

            nextHandler.get()?.handlerRequest(this)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerPullImageHandler::class.java)
    }
}
