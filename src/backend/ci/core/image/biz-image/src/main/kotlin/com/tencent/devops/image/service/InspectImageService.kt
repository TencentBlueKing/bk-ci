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

package com.tencent.devops.image.service

import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PullImageResultCallback
import com.tencent.devops.image.config.DockerConfig
import com.tencent.devops.image.pojo.CheckDockerImageRequest
import com.tencent.devops.image.pojo.CheckDockerImageResponse
import com.tencent.devops.image.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class InspectImageService @Autowired constructor(
    dockerConfig: DockerConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(InspectImageService::class.java)
    }

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(dockerConfig.dockerConfig)
        .withApiVersion(dockerConfig.apiVersion)
        .build()

    private val dockerCli = DockerClientBuilder.getInstance(config).build()

    fun checkDockerImage(
        userId: String,
        checkDockerImageRequestList: List<CheckDockerImageRequest>
    ): List<CheckDockerImageResponse> {
        logger.info("checkImage userId: $userId, checkDockerImageRequestList: $checkDockerImageRequestList")
        val imageInspectList = mutableListOf<CheckDockerImageResponse>()
        checkDockerImageRequestList.parallelStream().forEach {
            // 判断用户录入的镜像信息是否能正常拉取到镜像
            val imageName = it.imageName
            try {
                val authConfig = CommonUtils.getAuthConfig(
                    imageName = imageName,
                    registryHost = it.registryHost,
                    registryUser = it.registryUser,
                    registryPwd = it.registryPwd
                )
                logger.info("Start pulling the image, image name：$imageName")
                dockerCli.pullImageCmd(imageName).withAuthConfig(authConfig)
                    .exec(MyPullImageResultCallback(userId)).awaitCompletion()
                logger.info("The image was pulled successfully. Image name：$imageName")
            } catch (t: Throwable) {
                logger.warn("Fail to pull the image $imageName of userId $userId", t)
                imageInspectList.add(
                    CheckDockerImageResponse(
                        errorCode = -1,
                        errorMessage = t.message,
                        arch = "",
                        author = "",
                        comment = "",
                        created = "",
                        dockerVersion = "",
                        id = "",
                        os = "",
                        osVersion = "",
                        parent = "",
                        size = 0,
                        repoTags = null,
                        repoDigests = null,
                        virtualSize = 0
                    )
                )
                return@forEach
            }

            // 查询镜像详细信息
            val imageInfo = dockerCli.inspectImageCmd(imageName).exec()
            logger.info("imageInfo: $imageInfo")
            imageInspectList.add(
                CheckDockerImageResponse(
                    errorCode = 0,
                    errorMessage = "",
                    arch = imageInfo.arch,
                    author = imageInfo.author,
                    comment = imageInfo.comment,
                    created = imageInfo.created,
                    dockerVersion = imageInfo.dockerVersion,
                    id = imageInfo.id,
                    os = imageInfo.os,
                    osVersion = imageInfo.osVersion,
                    parent = imageInfo.parent,
                    size = imageInfo.size,
                    repoTags = imageInfo.repoTags,
                    repoDigests = imageInfo.repoDigests,
                    virtualSize = imageInfo.virtualSize
                )
            )
            logger.info("==========================")
        }

        logger.info("imageInspectList: $imageInspectList")

        return imageInspectList
    }

    inner class MyPullImageResultCallback internal constructor(
        private val userId: String
    ) : PullImageResultCallback() {
        private val totalList = mutableListOf<Long>()
        private val step = mutableMapOf<Int, Long>()
        override fun onNext(item: PullResponseItem?) {
            val text = item?.progressDetail
            if (null != text && text.current != null && text.total != 0L) {
                val lays = if (!totalList.contains(text.total!!)) {
                    totalList.add(text.total!!)
                    totalList.size + 1
                } else {
                    totalList.indexOf(text.total!!) + 1
                }
                var currentProgress = text.current!! * 100 / text.total!!
                if (currentProgress > 100) {
                    currentProgress = 100
                }

                if (currentProgress >= step[lays]?.plus(25) ?: 5) {
                    logger.info("$userId pulling images, $lays layer, progress: $currentProgress%")
                    step[lays] = currentProgress
                }
            }
            super.onNext(item)
        }
    }
}

// fun main(args: Array<String>) {
//    println(SecurityUtil.decrypt("7Rq3q4+3wRSkYX78nrcWNw=="))
//    println(SecurityUtil.encrypt("!@#098Bcs"))
// }
