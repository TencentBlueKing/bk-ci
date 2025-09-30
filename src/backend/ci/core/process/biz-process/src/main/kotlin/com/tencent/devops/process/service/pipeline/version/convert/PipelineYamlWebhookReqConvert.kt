/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.service.pipeline.version.convert

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCreateReq
import com.tencent.devops.process.pojo.pipeline.version.PipelineYamlWebhookReq
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlWebhookReqConvert @Autowired constructor(
    private val pipelineIdGenerator: PipelineIdGenerator,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineVersionCommonConvert: PipelineVersionCommonConvert
) : PipelineVersionCreateReqConverter {
    override fun support(request: PipelineVersionCreateReq): Boolean {
        return request is PipelineYamlWebhookReq
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun convert(
        userId: String,
        projectId: String,
        pipelineId: String?,
        version: Int?,
        request: PipelineVersionCreateReq
    ): PipelineVersionCreateContext {
        request as PipelineYamlWebhookReq
        with(request) {
            if (yamlFileInfo == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                    params = arrayOf(PipelineYamlWebhookReq::yamlFileInfo.name)
                )
            }
            logger.info(
                "Start to convert yaml webhook request|$projectId|$pipelineId|" +
                        "$branchName|$yamlFileName|$dependencyUpgrade|yaml=$yaml"
            )
            val (modelAndSetting, yamlWithVersion) = pipelineVersionGenerator.yaml2model(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                yaml = yaml,
                yamlFileName = yamlFileName
            )

            // 生成流水线ID
            val newPipelineId = pipelineId ?: pipelineIdGenerator.getNextId()
            // 流水线名称实际取值优先级：setting > model > fileName
            val pipelineName = modelAndSetting.setting.pipelineName.takeIf {
                it.isNotBlank()
            } ?: modelAndSetting.model.name.ifBlank {
                yamlFileName
            }

            val versionStatus = if (isDefaultBranch) {
                VersionStatus.RELEASED
            } else {
                VersionStatus.BRANCH
            }

            val versionAction = if (dependencyUpgrade) {
                PipelineVersionAction.DEPENDENCY_UPGRADE
            } else {
                if (isDefaultBranch) {
                    PipelineVersionAction.CREATE_RELEASE
                } else {
                    PipelineVersionAction.CREATE_BRANCH
                }
            }

            val pipelineAsCodeSettings = modelAndSetting.setting.pipelineAsCodeSettings?.copy(
                enable = true
            ) ?: PipelineAsCodeSettings(enable = true)
            val pipelineSettingWithoutVersion = modelAndSetting.setting.copy(
                projectId = projectId,
                pipelineId = newPipelineId,
                pipelineName = pipelineName,
                pipelineAsCodeSettings = pipelineAsCodeSettings
            )

            return pipelineVersionCommonConvert.convert(
                userId = userId,
                projectId = projectId,
                pipelineId = newPipelineId,
                channelCode = ChannelCode.BS,
                version = version,
                model = modelAndSetting.model.copy(
                    name = pipelineName
                ),
                yaml = yamlWithVersion?.yamlStr,
                description = description,
                pipelineSettingWithoutVersion = pipelineSettingWithoutVersion,
                versionStatus = versionStatus,
                versionAction = versionAction,
                repoHashId = yamlFileInfo!!.repoHashId,
                branchName = branchName
            ).copy(
                enablePac = true,
                yamlFileInfo = yamlFileInfo,
                pullRequestUrl = pullRequestUrl,
                pullRequestId = pullRequestId
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlWebhookReqConvert::class.java)
    }
}
