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

package com.tencent.devops.process.engine.service.template

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.template.TemplateInstances
import com.tencent.devops.process.pojo.template.TemplatePipeline
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.TemplateVersion
import com.tencent.devops.process.template.dao.PTemplateDao
import com.tencent.devops.process.template.dao.TemplatePipelineDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ListTemplateInstanceServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: PTemplateDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineSettingDao: PipelineSettingDao
) :
    ListTemplateInstanceService {

    companion object {
        private val logger = LoggerFactory.getLogger(ListTemplateInstanceServiceImpl::class.java)
    }

    override fun listTemplateInstances(projectId: String, userId: String, templateId: String): TemplateInstances {
        logger.info("[$projectId|$userId|$templateId] List the template instances")
        val associatePipelines = templatePipelineDao.listPipeline(dslContext, templateId)

        val pipelineIds = associatePipelines.map { it.pipelineId }.toSet()
        logger.info("Get the pipelineIds - $associatePipelines")
        val pipelineSettings = pipelineSettingDao.getSettings(dslContext, pipelineIds).groupBy { it.pipelineId }
        logger.info("Get the pipeline settings - $pipelineSettings")
        val hasPermissionList = pipelinePermissionService.getResourceByPermission(
            userId = userId, projectId = projectId, permission = BkAuthPermission.EDIT
        )
        val templatePipelines = associatePipelines.map {
            val pipelineSetting = pipelineSettings[it.pipelineId]
            if (pipelineSetting == null || pipelineSetting.isEmpty()) {
                throw OperationException("流水线设置配置不存在")
            }
            TemplatePipeline(
                it.templateId,
                it.versionName,
                it.version,
                it.pipelineId,
                pipelineSetting[0].name,
                it.updatedTime.timestampmilli(),
                hasPermissionList.contains(it.pipelineId)
            )
        }

        var latestVersion = templateDao.getLatestTemplate(dslContext, projectId, templateId)
        if (latestVersion.type == TemplateType.CONSTRAINT.name) {
            latestVersion = templateDao.getLatestTemplate(dslContext, latestVersion.srcTemplateId)
        }

        return TemplateInstances(
            projectId,
            templateId,
            templatePipelines,
            TemplateVersion(
                latestVersion.version,
                latestVersion.versionName,
                latestVersion.createdTime.timestampmilli(),
                latestVersion.creator
            )
        )
    }
}