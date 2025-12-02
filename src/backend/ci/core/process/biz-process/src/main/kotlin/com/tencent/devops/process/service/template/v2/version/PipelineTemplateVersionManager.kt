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

package com.tencent.devops.process.service.template.v2.version

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateVersionValidator
import com.tencent.devops.process.service.template.v2.version.convert.PipelineTemplateVersionReqConverter
import com.tencent.devops.process.service.template.v2.version.hander.PipelineTemplateVersionCreateHandler
import com.tencent.devops.process.service.template.v2.version.hander.PipelineTemplateVersionDeleteHandler
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线版本管理
 */
@Service
class PipelineTemplateVersionManager @Autowired constructor(
    private val versionReqConverters: List<PipelineTemplateVersionReqConverter>,
    private val versionCreateHandlers: List<PipelineTemplateVersionCreateHandler>,
    private val pipelineTemplateVersionValidator: PipelineTemplateVersionValidator,
    private val versionDeleteHandler: PipelineTemplateVersionDeleteHandler,
    private val templateDao: TemplateDao,
    private val dslContext: DSLContext
) {

    fun deployTemplate(
        userId: String,
        projectId: String,
        templateId: String? = null,
        version: Long? = null,
        request: PipelineTemplateVersionReq
    ): DeployTemplateResult {
        val context = getConverter(request).convert(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version,
            request = request
        )
        pipelineTemplateVersionValidator.validate(context = context)
        return getHandler(context).handle(context = context)
    }

    fun deleteVersion(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?,
        versionName: String? = null
    ) {
        if (version == null && versionName == null) {
            throw IllegalArgumentException("Version and version name cannot be null")
        }
        // TODO pac模板 待稳定后，使用新表
        val finalVersion = version ?: (templateDao.getTemplate(
            dslContext = dslContext,
            templateId = templateId,
            versionName = versionName
        ) ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_NOT_EXISTS)).version

        val context = PipelineTemplateVersionDeleteContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = finalVersion,
            versionAction = PipelineVersionAction.DELETE_VERSION
        )
        versionDeleteHandler.handle(context = context)
    }

    fun deleteAllVersions(
        userId: String,
        projectId: String,
        templateId: String
    ) {
        val context = PipelineTemplateVersionDeleteContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            versionAction = PipelineVersionAction.DELETE_ALL_VERSIONS
        )
        versionDeleteHandler.handle(context = context)
    }

    fun inactiveBranch(
        userId: String,
        projectId: String,
        templateId: String,
        branch: String
    ) {
        val context = PipelineTemplateVersionDeleteContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            branch = branch,
            versionAction = PipelineVersionAction.INACTIVE_BRANCH
        )
        versionDeleteHandler.handle(context = context)
    }

    private fun getHandler(context: PipelineTemplateVersionCreateContext): PipelineTemplateVersionCreateHandler {
        return versionCreateHandlers.find { it.support(context) }
            ?: throw IllegalArgumentException("Unsupported version event: ${context.versionAction}")
    }

    private fun getConverter(request: PipelineTemplateVersionReq): PipelineTemplateVersionReqConverter {
        return versionReqConverters.find { it.support(request) }
            ?: throw IllegalArgumentException("Unsupported version request: $request")
    }
}
