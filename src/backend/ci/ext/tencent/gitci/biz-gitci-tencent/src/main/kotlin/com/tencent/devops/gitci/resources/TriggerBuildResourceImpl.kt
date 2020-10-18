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

package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.TriggerBuildResource
import com.tencent.devops.gitci.pojo.GitYamlString
import com.tencent.devops.gitci.pojo.TriggerBuildReq
import com.tencent.devops.gitci.service.GitCIRequestService
import com.tencent.devops.gitci.service.RepositoryConfService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class TriggerBuildResourceImpl @Autowired constructor(
    private val gitCIRequestService: GitCIRequestService,
    private val repositoryConfService: RepositoryConfService
) : TriggerBuildResource {
    companion object {
        private val logger = LoggerFactory.getLogger(TriggerBuildResourceImpl::class.java)
    }

    override fun triggerStartup(userId: String, triggerBuildReq: TriggerBuildReq): Result<Boolean> {
        checkParam(userId, triggerBuildReq.gitProjectId)
        return Result(gitCIRequestService.triggerBuild(userId, triggerBuildReq))
    }

    override fun checkYaml(userId: String, yaml: GitYamlString): Result<String> {
        try {
            val yamlStr = CiYamlUtils.formatYaml(yaml.yaml)
            logger.debug("yaml str : $yamlStr")

            val (validate, message) = gitCIRequestService.validateCIBuildYaml(yamlStr)
            if (!validate) {
                logger.error("Validate yaml failed, message: $message")
                return Result(1, "Invalid yaml: $message", message)
            }
            gitCIRequestService.createCIBuildYaml(yaml.yaml)
        } catch (e: Throwable) {
            logger.error("check yaml failed, error: ${e.message}, yaml: $yaml")
            return Result(1, "Invalid yaml", e.message)
        }

        return Result("OK")
    }

    override fun getYamlSchema(userId: String): Result<String> {
        val schema = gitCIRequestService.getCIBuildYamlSchema()
        logger.info("ci build yaml schema: $schema")
        return Result(schema)
    }

    override fun getYamlByBuildId(userId: String, gitProjectId: Long, buildId: String): Result<String> {
        checkParam(userId, gitProjectId)
        return Result(gitCIRequestService.getYaml(gitProjectId, buildId))
    }

    private fun checkParam(userId: String, gitProjectId: Long) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
