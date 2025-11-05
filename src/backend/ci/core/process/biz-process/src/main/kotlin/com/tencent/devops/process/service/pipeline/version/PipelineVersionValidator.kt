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

package com.tencent.devops.process.service.pipeline.version

import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.exception.CustomMessageException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_MAX_PIPELINE_COUNT_PER_PROJECT
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.service.ProjectCacheService
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineVersionValidator @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val projectCacheService: ProjectCacheService
) {

    fun validate(context: PipelineVersionCreateContext) {
        with(context) {
            validateBasicInfo()
            validateModelBasicInfo()
            validatePermission()
        }
    }

    fun PipelineVersionCreateContext.validateBasicInfo() {
        if (pipelineBasicInfo.pipelineName.isBlank()) {
            logger.warn("The pipeline name is empty")
            throw CustomMessageException("The pipeline name cannot be empty.")
        }
        val nameExist = pipelineRepositoryService.isPipelineExist(
            projectId = projectId,
            pipelineName = pipelineBasicInfo.pipelineName,
            channelCode = pipelineBasicInfo.channelCode,
            excludePipelineId = pipelineId
        )
        if (nameExist) {
            logger.warn("The pipeline(${pipelineBasicInfo.pipelineName}) is exist")
            throw ErrorCodeException(
                statusCode = Response.Status.CONFLICT.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
            )
        }
        if (pipelineInfo == null) {
            // 检查用户流水线是否达到上限
            val projectVO = projectCacheService.getProject(projectId)
            if (projectVO?.pipelineLimit != null) {
                val preCount = pipelineRepositoryService.countByProjectIds(setOf(projectId), ChannelCode.BS)
                if (preCount >= projectVO.pipelineLimit!!) {
                    throw OperationException(
                        MessageUtil.getMessageByLocale(
                            ERROR_MAX_PIPELINE_COUNT_PER_PROJECT,
                            I18nUtil.getLanguage(userId),
                            arrayOf("${projectVO.pipelineLimit}")
                        )
                    )
                }
            }
        } else {
            if (pipelineInfo.channelCode != pipelineBasicInfo.channelCode) {
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                    params = arrayOf(pipelineInfo.channelCode.name)
                )
            }
        }
    }

    fun PipelineVersionCreateContext.validateModelBasicInfo() {
        if (pipelineInfo != null) {
            val model = pipelineResourceWithoutVersion.model
            // 只在更新操作时检查stage数量不为1
            if (model.stages.size <= 1) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_WITH_EMPTY_STAGE, params = arrayOf()
            )
        }
    }

    fun PipelineVersionCreateContext.validatePermission() {
        if (!checkPermission) return
        val language = I18nUtil.getLanguage(userId)
        if (pipelineInfo == null) {
            val permission = AuthPermission.CREATE
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = "*",
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE, language, arrayOf(
                    userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), "*"
                )
                )
            )
        } else {
            val permission = AuthPermission.EDIT
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    USER_NOT_PERMISSIONS_OPERATE_PIPELINE, I18nUtil.getLanguage(userId), arrayOf(
                    userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), pipelineId
                )
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVersionValidator::class.java)
    }
}
