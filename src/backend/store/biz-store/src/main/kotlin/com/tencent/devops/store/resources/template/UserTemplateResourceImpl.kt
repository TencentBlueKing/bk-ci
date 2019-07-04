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

package com.tencent.devops.store.resources.template

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.template.UserTemplateResource
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.pojo.template.MyTemplateItem
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.TemplateProcessInfo
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
import com.tencent.devops.store.service.common.StoreProjectService
import com.tencent.devops.store.service.template.MarketTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTemplateResourceImpl @Autowired constructor(
    private val marketTemplateService: MarketTemplateService,
    private val storeProjectService: StoreProjectService
) :
    UserTemplateResource {

    override fun getInstalledProjects(
        accessToken: String,
        userId: String,
        templateCode: String
    ): Result<List<InstalledProjRespItem?>> {
        return storeProjectService.getInstalledProjects(accessToken, userId, templateCode, StoreTypeEnum.TEMPLATE)
    }

    override fun getMyTemplates(
        userId: String,
        templateName: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<MyTemplateItem>?> {
        return marketTemplateService.getMyTemplates(userId, templateName, page, pageSize)
    }

    override fun installTemplate(
        accessToken: String,
        userId: String,
        installTemplateReq: InstallTemplateReq
    ): Result<Boolean> {
        return marketTemplateService.installTemplate(
            accessToken,
            userId,
            installTemplateReq.projectCodeList,
            installTemplateReq.templateCode
        )
    }

    override fun mainPageList(userId: String, page: Int?, pageSize: Int?): Result<List<MarketTemplateMain>> {
        return marketTemplateService.mainPageList(userId, page, pageSize)
    }

    override fun list(
        userId: String,
        templateName: String?,
        classifyCode: String?,
        categoryCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<MarketTemplateResp> {
        return Result(
            marketTemplateService.list(
                userId.trim(),
                templateName?.trim(),
                classifyCode?.trim(),
                categoryCode?.trim(),
                labelCode?.trim(),
                score,
                rdType,
                sortType,
                page,
                pageSize
            )
        )
    }

    override fun addMarketTemplate(
        userId: String,
        templateCode: String,
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean> {
        return marketTemplateService.addMarketTemplate(userId, templateCode, marketTemplateRelRequest)
    }

    override fun updateMarketTemplate(
        userId: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?> {
        return marketTemplateService.updateMarketTemplate(userId, marketTemplateUpdateRequest)
    }

    override fun delete(userId: String, templateCode: String): Result<Boolean> {
        return marketTemplateService.delete(userId, templateCode)
    }

    override fun getTemplateDetailById(userId: String, templateId: String): Result<TemplateDetail?> {
        return marketTemplateService.getTemplateDetailById(userId, templateId)
    }

    override fun getTemplateDetailByCode(userId: String, templateCode: String): Result<TemplateDetail?> {
        return marketTemplateService.getTemplateDetailByCode(userId, templateCode)
    }

    override fun getProcessInfo(templateId: String): Result<TemplateProcessInfo> {
        return marketTemplateService.getProcessInfo(templateId)
    }

    override fun cancelRelease(userId: String, templateId: String): Result<Boolean> {
        return marketTemplateService.cancelRelease(userId, templateId)
    }

    override fun offlineTemplate(
        userId: String,
        templateCode: String,
        version: String?,
        reason: String?
    ): Result<Boolean> {
        return marketTemplateService.offlineTemplate(userId, templateCode, version, reason)
    }
}