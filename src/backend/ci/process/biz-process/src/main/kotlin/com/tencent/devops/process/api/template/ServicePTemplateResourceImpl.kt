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

package com.tencent.devops.process.api.template

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateDetailInfo
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.template.service.PipelineTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePTemplateResourceImpl @Autowired constructor(
    private val pipelineTemplateService: PipelineTemplateService,
    private val templateService: TemplateService
) : ServiceTemplateResource {

    override fun addMarketTemplate(
        userId: String,
        addMarketTemplateRequest: AddMarketTemplateRequest
    ): Result<Map<String, String>> {
        return templateService.addMarketTemplate(userId, addMarketTemplateRequest)
    }

    override fun updateMarketTemplateReference(
        userId: String,
        updateMarketTemplateRequest: AddMarketTemplateRequest
    ): Result<Boolean> {
        return templateService.updateMarketTemplateReference(userId, updateMarketTemplateRequest)
    }

    override fun getTemplateDetailInfo(templateCode: String, publicFlag: Boolean): Result<TemplateDetailInfo?> {
        return pipelineTemplateService.getTemplateDetailInfo(templateCode, publicFlag)
    }

    override fun listTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?
    ): Result<TemplateListModel> {
        return Result(templateService.listTemplate(projectId, userId, templateType, storeFlag, 1, 9999))
    }

    override fun getTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?
    ): Result<TemplateModelDetail> {
        return Result(templateService.getTemplate(projectId, userId, templateId, version))
    }

    override fun listAllTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?
    ): Result<OptionalTemplateList> {
        return Result(templateService.listAllTemplate(projectId, templateType, null, 1, 9999))
    }

    override fun updateStoreFlag(userId: String, templateId: String, storeFlag: Boolean): Result<Boolean> {
        return templateService.updateTemplateStoreFlag(userId, templateId, storeFlag)
    }

    override fun listTemplateById(templateIds: Collection<String>, templateType: TemplateType?): Result<OptionalTemplateList> {
        return Result(templateService.listAllTemplate(null, templateType, templateIds, null, null))
    }
}