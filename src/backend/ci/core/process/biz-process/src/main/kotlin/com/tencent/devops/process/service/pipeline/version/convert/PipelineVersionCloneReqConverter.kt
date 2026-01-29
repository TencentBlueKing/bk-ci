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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCloneReq
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCreateReq
import com.tencent.devops.process.service.pipeline.PipelineSettingVersionService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 流水线版本克隆转换器
 * 从指定版本读取流水线的 Model 和 Setting，创建一个新的版本
 */
@Service
class PipelineVersionCloneReqConverter(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineSettingVersionService: PipelineSettingVersionService,
    private val pipelineVersionCreateContextFactory: PipelineVersionCreateContextFactory
) : PipelineVersionCreateReqConverter {

    override fun support(request: PipelineVersionCreateReq): Boolean {
        return request is PipelineVersionCloneReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        pipelineId: String?,
        version: Int?,
        request: PipelineVersionCreateReq
    ): PipelineVersionCreateContext {
        request as PipelineVersionCloneReq
        logger.info(
            "Start to convert clone version request|$projectId|$pipelineId|${request.sourceVersion}"
        )

        // 获取源版本的资源信息
        val sourceResource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId!!,
            version = request.sourceVersion,
            includeDraft = false
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(request.sourceVersion.toString())
        )

        // 获取源版本的设置信息
        val sourceSetting = pipelineSettingVersionService.getPipelineSetting(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            detailInfo = null,
            version = sourceResource.settingVersion!!
        )

        // 使用上下文工厂创建上下文
        return pipelineVersionCreateContextFactory.create(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS,
            version = version,
            model = sourceResource.model,
            yaml = sourceResource.yaml,
            baseVersion = request.sourceVersion,
            description = request.description,
            pipelineSettingWithoutVersion = sourceSetting,
            versionStatus = sourceResource.status?:VersionStatus.RELEASED,
            versionAction = PipelineVersionAction.CREATE_RELEASE
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVersionCloneReqConverter::class.java)
    }
}
