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

package com.tencent.devops.buildless.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.HostConfig
import com.tencent.devops.buildless.utils.BUILDLESS_POOL_PREFIX
import com.tencent.devops.buildless.utils.ENTRY_POINT_CMD
import com.tencent.devops.buildless.utils.RandomUtil
import org.slf4j.LoggerFactory

/**
 * 创建容器单例类
 */

class SinglePublishContainer {

    fun createBuildLessPoolContainer(
        env: List<String>,
        hostConfig: HostConfig,
        httpDockerCli: DockerClient
    ): String {
        val imageName = "mirrors.tencent.com/ci/tlinux_ci:0.5.0.4"
        val containerName = "$BUILDLESS_POOL_PREFIX-${RandomUtil.randomString()}"

        val container = httpDockerCli.createContainerCmd(imageName)
            .withName(containerName)
            .withLabels(mapOf(BUILDLESS_POOL_PREFIX to ""))
            .withCmd("/bin/sh", ENTRY_POINT_CMD)
            .withEnv(env)
            .withHostConfig(hostConfig)
            .exec()

        httpDockerCli.startContainerCmd(container.id).exec()
        logger.info("===> created container $container")

        return container.id
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SinglePublishContainer::class.java)

        fun getInstance() = Helper.instance
    }

    private object Helper {
        val instance = SinglePublishContainer()
    }
}
