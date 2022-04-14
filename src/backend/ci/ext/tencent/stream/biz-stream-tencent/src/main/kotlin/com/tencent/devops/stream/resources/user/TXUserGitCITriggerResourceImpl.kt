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

package com.tencent.devops.stream.resources.user

import com.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.StreamGitYamlString
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.V1TriggerBuildReq
import com.tencent.devops.stream.service.StreamPipelineService
import com.tencent.devops.stream.service.StreamYamlService
import com.tencent.devops.stream.trigger.ManualTriggerService
import com.tencent.devops.stream.trigger.TXManualTriggerService
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.v1.components.V1YamlTrigger
import com.tencent.devops.stream.v1.service.V1StreamYamlService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TXUserGitCITriggerResourceImpl @Autowired constructor(
    private val manualTriggerService: ManualTriggerService,
    private val streamPipelineService: StreamPipelineService,
    private val permissionService: StreamPermissionService,
    private val yamlSchemaCheck: YamlSchemaCheck,
    private val streamYamlService: StreamYamlService,
    private val TXManualTriggerService: TXManualTriggerService,
    private val v1streamYamlService: V1StreamYamlService,
    private val v1YamlTrigger: V1YamlTrigger
) : UserStreamTriggerResourceImpl(
    manualTriggerService = manualTriggerService,
    streamPipelineService = streamPipelineService,
    permissionService = permissionService,
    yamlSchemaCheck = yamlSchemaCheck,
    streamYamlService = streamYamlService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TXUserGitCITriggerResourceImpl::class.java)
    }

    override fun triggerStartup(
        userId: String,
        pipelineId: String,
        triggerBuildReq: TriggerBuildReq
    ): Result<TriggerBuildResult> {
        val gitProjectId = GitCommonUtils.getGitProjectId(triggerBuildReq.projectId)
        checkParam(userId)
        permissionService.checkStreamAndOAuthAndEnable(userId, triggerBuildReq.projectId, gitProjectId)
        val new = with(triggerBuildReq) {
            V1TriggerBuildReq(
                gitProjectId = gitProjectId,
                name = null,
                url = null,
                homepage = null,
                gitHttpUrl = null,
                gitSshUrl = null,
                branch = branch,
                customCommitMsg = customCommitMsg,
                yaml = yaml,
                description = description,
                commitId = commitId
            )
        }
        return Result(TXManualTriggerService.triggerBuild(userId, pipelineId, new))
    }

    override fun checkYaml(userId: String, yaml: StreamGitYamlString): Result<String> {
        // 检查yml版本，根据yml版本选择不同的实现
        val ymlVersion = ScriptYmlUtils.parseVersion(yaml.yaml)
        when {
            ymlVersion == null -> {
                return Result(1, "Invalid yaml")
            }
            ymlVersion.version != "v2.0" -> {
                try {
                    val yamlStr = CiYamlUtils.formatYaml(yaml.yaml)
                    logger.debug("yaml str : $yamlStr")

                    val (validate, message) = v1streamYamlService.validateCIBuildYaml(yamlStr)
                    if (!validate) {
                        logger.error("Check yaml failed, error: $message")
                        return Result(1, "Invalid yaml: $message", message)
                    }
                    v1streamYamlService.createCIBuildYaml(yaml.yaml)

                    return Result("OK")
                } catch (e: Throwable) {
                    logger.error("Check yaml failed, error: ${e.message}, yaml: $yaml")
                    return Result(1, "Invalid yaml", e.message)
                }
            }
            else -> {
                return v1YamlTrigger.checkYamlSchema(userId, yaml.yaml)
            }
        }
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
