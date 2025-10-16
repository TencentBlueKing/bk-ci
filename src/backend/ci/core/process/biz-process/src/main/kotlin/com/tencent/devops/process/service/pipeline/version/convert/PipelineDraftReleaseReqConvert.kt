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
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileInfo
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCreateReq
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineDraftReleaseReqConvert @Autowired constructor(
    private val pipelineInfoService: PipelineInfoService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val dslContext: DSLContext,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineVersionCreateContextFactory: PipelineVersionCreateContextFactory
) : PipelineVersionCreateReqConverter {
    override fun support(request: PipelineVersionCreateReq): Boolean {
        return request is PipelineVersionReleaseRequest
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun convert(
        userId: String,
        projectId: String,
        pipelineId: String?,
        version: Int?,
        request: PipelineVersionCreateReq
    ): PipelineVersionCreateContext {
        request as PipelineVersionReleaseRequest
        with(request) {
            if (pipelineId == null) {
                throw IllegalArgumentException("pipelineId is null")
            }
            if (version == null) {
                throw IllegalArgumentException("version is null")
            }
            logger.info(
                "Start to convert draft release request|$projectId|$pipelineId|" +
                        "$version|$enablePac|$targetAction|${yamlInfo?.repoHashId}|${yamlInfo?.filePath}|$targetBranch"
            )
            val pipelineInfo = pipelineInfoService.getPipelineInfo(
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
            val draftResource = pipelineResourceVersionDao.getVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                includeDraft = true
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_DRAFT_EXISTS,
            )
            if (draftResource.status != VersionStatus.COMMITTING) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_RELEASE_MUST_DRAFT_VERSION
                )
            }
            if (enablePac) {
                if (targetAction == null) {
                    throw IllegalArgumentException("targetAction is null")
                }
                if (yamlInfo == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                        params = arrayOf(PipelineVersionReleaseRequest::yamlInfo.name)
                    )
                }
                // 对前端的YAML信息进行校验
                val filePath = yamlInfo!!.filePath
                if (!filePath.endsWith(".yaml") && !filePath.endsWith(".yml")) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_YAML_FILENAME,
                        params = arrayOf(filePath)
                    )
                }
                if (draftResource.yaml == null) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_YAML_CONTENT_IS_EMPTY
                    )
                }
            }

            val (versionStatus, branchName) = pipelineVersionGenerator.getDraftReleaseStatusAndBranchName(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                enablePac = enablePac,
                repoHashId = yamlInfo?.repoHashId,
                targetAction = targetAction,
                targetBranch = targetBranch
            )

            val draftSetting = draftResource.settingVersion?.let {
                pipelineSettingFacadeService.userGetSetting(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = it
                )
            } ?: pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            )
            val pipelineAsCodeSettings = if (enablePac) {
                draftSetting.pipelineAsCodeSettings?.copy(enable = true) ?: PipelineAsCodeSettings(enable = true)
            } else {
                draftSetting.pipelineAsCodeSettings
            }
            val pipelineSettingWithoutVersion = draftSetting.copy(pipelineAsCodeSettings = pipelineAsCodeSettings)

            return pipelineVersionCreateContextFactory.create(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = pipelineInfo.channelCode,
                version = version,
                model = draftResource.model,
                yaml = draftResource.yaml,
                baseVersion = draftResource.baseVersion,
                pipelineSettingWithoutVersion = pipelineSettingWithoutVersion,
                versionStatus = versionStatus,
                branchName = branchName,
                versionAction = PipelineVersionAction.RELEASE_DRAFT,
                repoHashId = yamlInfo?.repoHashId
            ).copy(
                yamlFileInfo = yamlInfo?.let { PipelineYamlFileInfo(it) },
                enablePac = enablePac,
                targetAction = targetAction,
                targetBranch = targetBranch
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDraftReleaseReqConvert::class.java)
    }
}
