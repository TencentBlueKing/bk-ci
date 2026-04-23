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

package com.tencent.devops.process.service.pipeline.version.handler

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReqSource
import com.tencent.devops.process.service.PipelineRemoteAuthService
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import com.tencent.devops.process.service.pipeline.version.PipelineVersionPersistenceService
import com.tencent.devops.process.service.`var`.PublicVarGroupReferManageService
import com.tencent.devops.process.yaml.PipelineYamlCommonService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineTemplateInstanceHandler @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineVersionPersistenceService: PipelineVersionPersistenceService,
    private val pipelineYamlCommonService: PipelineYamlCommonService,
    private val pipelineRemoteAuthService: PipelineRemoteAuthService,
    private val publicVarGroupReferManageService: PublicVarGroupReferManageService
) : PipelineVersionCreateHandler {
    override fun support(context: PipelineVersionCreateContext) =
        context.versionAction == PipelineVersionAction.TEMPLATE_INSTANCE

    override fun handle(context: PipelineVersionCreateContext): DeployPipelineResult {
        with(context) {
            logger.info(
                "handle template instance|" +
                        "$projectId|$pipelineId|$version|" +
                        "${templateInstanceBasicInfo?.templateId}|${templateInstanceBasicInfo?.templateVersion}"
            )
            if (templateInstanceBasicInfo == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("templateInstanceBasicInfo")
                )
            }
            if (enablePac) {
                if (targetAction == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("targetAction")
                    )
                }
                if (yamlFileInfo == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("yamlFileInfo")
                    )
                }
                if (pipelineResourceWithoutVersion.yaml == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("yaml content")
                    )
                }
            }
            val lock = PipelineModelLock(redisOperation, pipelineId)
            try {
                lock.lock()
                return doHandle()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineVersionCreateContext.doHandle(): DeployPipelineResult {
        val resourceOnlyVersion = if (pipelineInfo == null) {
            pipelineVersionGenerator.getDefaultVersion(
                versionStatus = pipelineResourceWithoutVersion.status,
                branchName = branchName
            )
        } else {
            pipelineVersionGenerator.generateInstanceVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                newModel = pipelineResourceWithoutVersion.model,
                enablePac = enablePac,
                repoHashId = yamlFileInfo?.repoHashId,
                targetAction = targetAction,
                targetBranch = branchName,
                templateId = templateInstanceBasicInfo!!.templateId,
                templateVersion = templateInstanceBasicInfo.templateVersion
            )
        }

        // 检查推送参数
        enablePac.takeIf { it }?.let {
            pipelineYamlCommonService.checkPushParam(
                projectId = projectId,
                pipelineId = pipelineId,
                content = pipelineResourceWithoutVersion.yaml!!,
                repoHashId = yamlFileInfo!!.repoHashId,
                filePath = yamlFileInfo.filePath,
                targetAction = targetAction!!,
                versionName = resourceOnlyVersion.versionName,
                targetBranch = targetBranch
            )
        }

        when {
            pipelineInfo == null -> {
                pipelineVersionPersistenceService.initializePipeline(
                    context = this,
                    resourceOnlyVersion = resourceOnlyVersion
                )
                if (pipelineResourceWithoutVersion.status == VersionStatus.RELEASED) {
                    addRemoteAuth(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        model = pipelineResourceWithoutVersion.model
                    )
                }
            }

            pipelineResourceWithoutVersion.status == VersionStatus.RELEASED -> {
                pipelineVersionPersistenceService.createReleaseVersion(
                    context = this,
                    resourceOnlyVersion = resourceOnlyVersion
                )
            }

            else -> {
                pipelineVersionPersistenceService.createBranchVersion(
                    context = this,
                    resourceOnlyVersion = resourceOnlyVersion
                )
            }
        }

        // 同步变量组引用关系
        publicVarGroupReferManageService.handleVarGroupReferBus(
            PublicVarGroupReferDTO(
                userId = userId,
                projectId = projectId,
                model = pipelineResourceWithoutVersion.model,
                referId = pipelineId,
                referType = PublicVarGroupReferenceTypeEnum.PIPELINE,
                referName = pipelineSettingWithoutVersion.pipelineName,
                referVersion = resourceOnlyVersion.version,
                referVersionName = resourceOnlyVersion.versionName
            )
        )

        // 推送文件
        val yamlFileReleaseResult = enablePac.takeIf { it }?.let {
            pipelineVersionPersistenceService.releaseYamlFile(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion,
                source = PipelineYamlFileReleaseReqSource.TEMPLATE_INSTANCE
            )
        }

        return DeployPipelineResult(
            pipelineId = pipelineId,
            pipelineName = pipelineSettingWithoutVersion.pipelineName,
            version = resourceOnlyVersion.version,
            versionNum = resourceOnlyVersion.versionNum,
            versionName = resourceOnlyVersion.versionName,
            targetUrl = yamlFileReleaseResult?.pullRequestUrl,
            pullRequestId = yamlFileReleaseResult?.pullRequestId
        )
    }

    /**
     * 初始化远程触发token
     */
    fun addRemoteAuth(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model
    ) {
        val elementList = model.stages[0].containers[0].elements
        var isAddRemoteAuth = false
        elementList.forEach {
            if (it is RemoteTriggerElement) {
                isAddRemoteAuth = true
            }
        }
        if (isAddRemoteAuth) {
            logger.info("template Model has RemoteTriggerElement project[$projectId] pipeline[$pipelineId]")
            pipelineRemoteAuthService.generateAuth(pipelineId, projectId, userId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateInstanceHandler::class.java)
    }
}
