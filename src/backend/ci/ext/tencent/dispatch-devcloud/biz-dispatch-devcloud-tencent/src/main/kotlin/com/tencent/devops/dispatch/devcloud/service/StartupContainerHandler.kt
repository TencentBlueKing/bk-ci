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

package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.constant.DispatchDevcloudMessageCode
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.ENV_DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.dispatch.devcloud.pojo.ENV_JOB_BUILD_TYPE
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.devcloud.pojo.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.devcloud.service.context.DcStartupHandlerContext
import com.tencent.devops.process.engine.common.VMUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
abstract class StartupContainerHandler @Autowired constructor(
    private val commonConfig: CommonConfig,
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchDevCloudClient: DispatchDevCloudClient
) : Handler<DcStartupHandlerContext>() {

    fun generateEnvs(handlerContext: DcStartupHandlerContext): Map<String, Any> {
        // 拼接环境变量
        with(handlerContext) {
            val envs = mutableMapOf<String, Any>()
            if (customBuildEnv != null) {
                envs.putAll(customBuildEnv)
            }
            envs.putAll(
                mapOf(
                    ENV_KEY_PROJECT_ID to projectId,
                    ENV_KEY_AGENT_ID to agentId,
                    ENV_KEY_AGENT_SECRET_KEY to secretKey,
                    ENV_KEY_GATEWAY to gateway,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to "DevCloud",
                    ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.PUBLIC_DEVCLOUD.name),
                    ENV_DEFAULT_LOCALE_LANGUAGE to commonConfig.devopsDefaultLocaleLanguage
                )
            )

            return envs
        }
    }

    fun clearExceptionContainer(
        containerName: String,
        handlerContext: DcStartupHandlerContext
    ) {
        with(handlerContext) {
            try {
                // 下发删除，不管成功失败
                logger.info("$buildLogKey userId: $userId clear exceptionContainer: $containerName")
                dispatchDevCloudClient.operateContainer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    userId = userId,
                    name = containerName,
                    action = Action.DELETE
                )
            } catch (e: Exception) {
                logger.error("$buildLogKey Failed to clear exceptionContainer.", e)
            }
        }
    }

    fun printLog(
        message: String,
        handlerContext: DcStartupHandlerContext
    ) {
        with(handlerContext) {
            buildLogPrinter.addLine(
                buildId = buildId,
                message = message,
                tag = VMUtils.genStartVMTaskId(vmSeqId),
                jobId = containerHashId,
                executeCount = executeCount ?: 1
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartupContainerHandler::class.java)
    }
}
