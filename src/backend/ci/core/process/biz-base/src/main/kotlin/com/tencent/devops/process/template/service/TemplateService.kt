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

package com.tencent.devops.process.template.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("LongParameterList", "TooManyFunctions")
@Service
class TemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val templatePipelineDao: TemplatePipelineDao
) {

    fun getTemplateIdByPipeline(projectId: String, pipelineId: String, queryDslContext: DSLContext? = null): String? {
        return templatePipelineDao.get(
            dslContext = queryDslContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.templateId
    }

    fun isTemplatePipeline(projectId: String, pipelineId: String): Boolean {
        return templatePipelineDao.isTemplatePipeline(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    /**
     * 创建模板和流水线关联关系
     */
    fun createRelationBtwTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        pipelineId: String,
        instanceType: String,
        buildNo: BuildNo? = null,
        param: List<BuildFormProperty>? = null,
        fixTemplateVersion: Long? = null
    ): Boolean {
        logger.info("start createRelationBtwTemplate: $userId|$templateId|$pipelineId|$instanceType")
        val latestTemplate = templateDao.getLatestTemplate(dslContext, templateId)
        var rootTemplateId = templateId
        val templateVersion: Long
        val versionName: String

        when {
            fixTemplateVersion != null -> { // 否则以指定的版本
                templateVersion = fixTemplateVersion
                val template = templateDao.getTemplate(dslContext = dslContext, version = fixTemplateVersion)
                    ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
                    )
                versionName = template.versionName
            }
            else -> { // 以指定的模板Id创建
                templateVersion = latestTemplate.version
                versionName = latestTemplate.versionName
            }
        }

        templatePipelineDao.create(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            instanceType = instanceType,
            rootTemplateId = rootTemplateId,
            templateVersion = templateVersion,
            versionName = versionName,
            templateId = templateId,
            userId = userId,
            buildNo = buildNo?.let { JsonUtil.toJson(buildNo, formatted = false) },
            param = param?.let { JsonUtil.toJson(param, formatted = false) }
        )
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateService::class.java)
    }
}
