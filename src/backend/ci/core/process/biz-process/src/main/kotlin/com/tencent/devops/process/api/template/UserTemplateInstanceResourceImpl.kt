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

package com.tencent.devops.process.api.template

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.TemplateCompareModelResult
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.service.template.TemplateFacadeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTemplateInstanceResourceImpl @Autowired constructor(
    private val templateFacadeService: TemplateFacadeService
) :
    UserTemplateInstanceResource {

    @AuditEntry(actionId = ActionId.PIPELINE_CREATE)
    override fun createTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet {
        return templateFacadeService.createTemplateInstances(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            version = version,
            useTemplateSettings = useTemplateSettings,
            instances = instances
        )
    }

    @AuditEntry(actionId = ActionId.PIPELINE_EDIT)
    override fun updateTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet {
        return templateFacadeService.updateTemplateInstances(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            version = version,
            useTemplateSettings = useTemplateSettings,
            instances = instances
        )
    }

    override fun asyncUpdateTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): Result<Boolean> {
        return Result(
            templateFacadeService.asyncUpdateTemplateInstances(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                version = version,
                useTemplateSettings = useTemplateSettings,
                instances = instances
            )
        )
    }

    override fun listTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        page: Int,
        pageSize: Int,
        searchKey: String?,
        sortType: TemplateSortTypeEnum?,
        desc: Boolean?
    ): Result<TemplateInstancePage> {
        return Result(
            templateFacadeService.listTemplateInstancesInPage(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                page = page,
                pageSize = pageSize,
                searchKey = searchKey,
                sortType = sortType,
                desc = desc
            )
        )
    }

    override fun listTemplateInstancesParams(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        pipelineIds: List<PipelineId>
    ): Result<Map<String, TemplateInstanceParams>> {
        return Result(
            templateFacadeService.listTemplateInstancesParams(
                userId = userId,
                projectId = projectId,
                templateId = templateId,
                version = version,
                pipelineIds = pipelineIds.map { it.id }.toSet()
            )
        )
    }

    override fun compareTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        pipelineId: String,
        version: Long
    ): Result<TemplateCompareModelResult> {
        return Result(
            templateFacadeService.compareTemplateInstances(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                pipelineId = pipelineId,
                version = version
            )
        )
    }
}
