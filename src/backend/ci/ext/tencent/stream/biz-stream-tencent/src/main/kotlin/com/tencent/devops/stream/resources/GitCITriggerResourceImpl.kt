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

package com.tencent.devops.stream.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.GitCITriggerResource
import com.tencent.devops.stream.pojo.GitYamlString
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.trigger.ManualTriggerService
import com.tencent.devops.stream.trigger.StreamYamlService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class GitCITriggerResourceImpl @Autowired constructor(
    private val streamYamlService: StreamYamlService,
    private val manualTriggerService: ManualTriggerService
) : GitCITriggerResource {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCITriggerResourceImpl::class.java)
    }

    override fun triggerStartup(
        userId: String,
        pipelineId: String,
        triggerBuildReq: TriggerBuildReq
    ): Result<Boolean> {
        checkParam(userId)
        val result = manualTriggerService.triggerBuild(userId, pipelineId, triggerBuildReq)
        logger.info("STREAM|$userId|$pipelineId|triggerBuildReq=$triggerBuildReq|result=$result")
        return Result(true)
    }

    override fun checkYaml(userId: String, yaml: GitYamlString): Result<String> {
        try {
            if (ScriptYmlUtils.isV2Version(yaml.yaml)) {
                return Result("OK")
            }

            val yamlStr = CiYamlUtils.formatYaml(yaml.yaml)
            logger.debug("yaml str : $yamlStr")

            val (validate, message) = streamYamlService.validateCIBuildYaml(yamlStr)
            if (!validate) {
                logger.error("Validate yaml failed, message: $message")
                return Result(1, "Invalid yaml: $message", message)
            }
            streamYamlService.createCIBuildYaml(yaml.yaml)
        } catch (e: Throwable) {
            logger.error("check yaml failed, error: ${e.message}, yaml: $yaml")
            return Result(1, "Invalid yaml", e.message)
        }

        return Result("OK")
    }

    override fun getYamlSchema(userId: String): Result<String> {
        val schema = streamYamlService.getCIBuildYamlSchema()
        logger.info("ci build yaml schema: $schema")
        return Result(schema)
    }

    override fun getYamlByBuildId(userId: String, gitProjectId: Long, buildId: String): Result<String> {
        checkParam(userId)
        return Result(streamYamlService.getYaml(gitProjectId, buildId))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
