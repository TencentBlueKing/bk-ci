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

package com.tencent.devops.dockerhost.services.generator.impl

import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerEnv
import com.tencent.devops.dockerhost.services.generator.DockerEnvGenerator
import com.tencent.devops.dockerhost.services.generator.annotation.EnvGenerator
import com.tencent.devops.dockerhost.pojo.Env
import com.tencent.devops.dockerhost.services.container.ContainerHandlerContext
import com.tencent.devops.dockerhost.utils.COMMON_DOCKER_SIGN
import com.tencent.devops.dockerhost.utils.ENV_DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_PORT
import com.tencent.devops.dockerhost.utils.ENV_JOB_BUILD_TYPE
import com.tencent.devops.dockerhost.utils.ENV_KEY_AGENT_ID
import com.tencent.devops.dockerhost.utils.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dockerhost.utils.ENV_KEY_GATEWAY
import com.tencent.devops.dockerhost.utils.ENV_KEY_PROJECT_ID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@EnvGenerator(description = "默认Docker环境变量生成器")
@Component
class SystemDockerEnvGenerator @Autowired constructor(
    val commonConfig: CommonConfig,
    val dockerHostConfig: DockerHostConfig
) : DockerEnvGenerator {

    override fun generateEnv(handlerContext: ContainerHandlerContext): List<Env> {

        val hostIp = CommonUtils.getInnerIP()
        val gateway = DockerEnv.getGatway()
        val envList = mutableListOf(
            Env(key = ENV_KEY_PROJECT_ID, value = handlerContext.projectId),
            Env(key = ENV_KEY_AGENT_ID, value = handlerContext.agentId ?: ""),
            Env(key = ENV_KEY_AGENT_SECRET_KEY, value = handlerContext.secretKey ?: ""),
            Env(key = ENV_KEY_GATEWAY, value = gateway),
            Env(key = "TERM", value = "xterm-256color"),
            Env(key = "pool_no", value = handlerContext.poolNo.toString()),
            Env(key = "landun_env", value = dockerHostConfig.landunEnv ?: "prod"),
            Env(key = ENV_DOCKER_HOST_IP, value = hostIp),
            Env(key = ENV_DOCKER_HOST_PORT, value = commonConfig.serverPort.toString()),
            Env(key = COMMON_DOCKER_SIGN, value = "docker"),
            Env(key = ENV_JOB_BUILD_TYPE, value = handlerContext.buildType.name),
            Env(key = ENV_DEFAULT_LOCALE_LANGUAGE, value = commonConfig.devopsDefaultLocaleLanguage))

        handlerContext.customBuildEnv?.forEach { k, v ->
            envList.add(Env(key = k, value = v))
        }

        return envList
    }
}
