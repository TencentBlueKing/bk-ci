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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class ServiceTemplateInstanceResourceImpl @Autowired constructor(
    private val templateFacadeService: TemplateFacadeService
) : ServiceTemplateInstanceResource {

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

    override fun countTemplateInstance(projectId: String, templateIds: Collection<String>): Result<Int> {
        return Result(data = templateFacadeService.serviceCountTemplateInstances(projectId, templateIds))
    }

    override fun countTemplateInstanceDetail(
        projectId: String,
        templateIds: Collection<String>
    ): Result<Map<String, Int>> {
        return Result(templateFacadeService.serviceCountTemplateInstancesDetail(projectId, templateIds))
    }

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

    override fun updateTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet {
        return templateFacadeService.updateTemplateInstances(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            versionName = versionName,
            useTemplateSettings = useTemplateSettings,
            instances = instances
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
                pageSize = checkPageSize(pageSize),
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

    private fun checkPageSize(pageSize: Int) = if (pageSize > 30) 30 else pageSize
}
