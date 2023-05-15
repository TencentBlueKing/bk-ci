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
import com.tencent.devops.process.pojo.PipelineTemplateInfo
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateDetailInfo
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.template.service.PipelineTemplateService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class ServicePTemplateResourceImpl @Autowired constructor(
    private val pipelineTemplateService: PipelineTemplateService,
    private val templateFacadeService: TemplateFacadeService
) : ServicePTemplateResource {

    override fun addMarketTemplate(
        userId: String,
        addMarketTemplateRequest: AddMarketTemplateRequest
    ): Result<Map<String, String>> {
        return templateFacadeService.addMarketTemplate(userId, addMarketTemplateRequest)
    }

    override fun updateMarketTemplateReference(
        userId: String,
        updateMarketTemplateRequest: AddMarketTemplateRequest
    ): Result<Boolean> {
        return templateFacadeService.updateMarketTemplateReference(userId, updateMarketTemplateRequest)
    }

    override fun getTemplateDetailInfo(templateCode: String): Result<TemplateDetailInfo?> {
        return pipelineTemplateService.getTemplateDetailInfo(templateCode)
    }

    override fun checkImageReleaseStatus(userId: String, templateCode: String): Result<String?> {
        return pipelineTemplateService.checkImageReleaseStatus(userId, templateCode)
    }

    override fun getSrcTemplateCodes(projectId: String): Result<List<String>> {
        return templateFacadeService.getSrcTemplateCodes(projectId)
    }

    override fun getTemplateIdBySrcCode(
        srcTemplateId: String,
        projectIds: List<String>
    ): Result<List<PipelineTemplateInfo>> {
        return Result(templateFacadeService.getTemplateIdByTemplateCode(srcTemplateId, projectIds))
    }

    override fun listTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<TemplateListModel> {
        return Result(
            templateFacadeService.listTemplate(
                projectId = projectId,
                userId = userId,
                templateType = templateType,
                storeFlag = storeFlag,
                page = page ?: 1,
                pageSize = pageSize ?: 1000
            )
        )
    }

    override fun getTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?,
        versionName: String?
    ): Result<TemplateModelDetail> {
        return Result(
            templateFacadeService.getTemplate(
                projectId = projectId,
                userId = userId,
                templateId = templateId,
                version = version,
                versionName = versionName
            )
        )
    }

    override fun listAllTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        page: Int?,
        pageSize: Int?
    ): Result<OptionalTemplateList> {
        return Result(
            templateFacadeService.listAllTemplate(
                projectId = projectId,
                templateType = templateType,
                templateIds = null,
                page = page ?: 1,
                pageSize = pageSize ?: 1000
            )
        )
    }

    override fun updateStoreFlag(userId: String, templateId: String, storeFlag: Boolean): Result<Boolean> {
        return templateFacadeService.updateTemplateStoreFlag(
            userId = userId,
            templateId = templateId,
            storeFlag = storeFlag
        )
    }

    override fun listTemplateById(
        templateIds: Collection<String>,
        projectId: String?,
        templateType: TemplateType?
    ): Result<OptionalTemplateList> {
        return Result(
            templateFacadeService.listAllTemplate(
                projectId = projectId,
                templateType = templateType,
                templateIds = templateIds
            )
        )
    }

    override fun checkTemplate(userId: String, projectId: String, templateId: String): Result<Boolean> {
        return Result(templateFacadeService.checkTemplate(templateId, projectId))
    }
}
