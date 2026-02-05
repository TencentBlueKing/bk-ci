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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import com.tencent.devops.process.service.pipeline.version.PipelineVersionPersistenceService
import com.tencent.devops.process.service.`var`.PublicVarGroupReferManageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 分支版本创建
 */
@Service
class PipelineBranchCreateHandler @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineVersionPersistenceService: PipelineVersionPersistenceService,
    private val publicVarGroupReferManageService: PublicVarGroupReferManageService
) : PipelineVersionCreateHandler {
    override fun support(context: PipelineVersionCreateContext): Boolean {
        return context.versionAction == PipelineVersionAction.CREATE_BRANCH
    }

    override fun handle(context: PipelineVersionCreateContext): DeployPipelineResult {
        logger.info("create branch version with context={}", JsonUtil.toJson(context, false))
        with(context) {
            if (!enablePac) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("enablePac")
                )
            }
            if (yamlFileInfo == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("yamlFileInfo")
                )
            }
            if (branchName == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("branchName")
                )
            }
            if (pipelineResourceWithoutVersion.status != VersionStatus.BRANCH) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_STATUS_NOT_MATCHED,
                    params = arrayOf(VersionStatus.BRANCH.name, pipelineResourceWithoutVersion.status.name)
                )
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
            val resourceOnlyVersion = pipelineVersionGenerator.getDefaultVersion(
                versionStatus = pipelineResourceWithoutVersion.status,
                branchName = branchName
            )
            pipelineVersionPersistenceService.initializePipeline(
                context = this, resourceOnlyVersion = resourceOnlyVersion
            )
            resourceOnlyVersion
        } else {
            val resourceOnlyVersion = pipelineVersionGenerator.generateBranchVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                branchName = branchName!!
            )
            pipelineVersionPersistenceService.createBranchVersion(
                context = this, resourceOnlyVersion = resourceOnlyVersion
            )
            resourceOnlyVersion
        }
        publicVarGroupReferManageService.handleVarGroupReferBus(
            PublicVarGroupReferDTO(
                userId = userId,
                projectId = projectId,
                model = pipelineResourceWithoutVersion.model,
                referId = pipelineId,
                referType = PublicVerGroupReferenceTypeEnum.PIPELINE,
                referName = pipelineBasicInfo.pipelineName,
                referVersion = resourceOnlyVersion.version,
                referVersionName = resourceOnlyVersion.versionName
            )
        )

        return DeployPipelineResult(
            pipelineId = pipelineId,
            pipelineName = pipelineSettingWithoutVersion.pipelineName,
            version = resourceOnlyVersion.version,
            versionNum = resourceOnlyVersion.versionNum,
            versionName = resourceOnlyVersion.versionName
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBranchCreateHandler::class.java)
    }
}
