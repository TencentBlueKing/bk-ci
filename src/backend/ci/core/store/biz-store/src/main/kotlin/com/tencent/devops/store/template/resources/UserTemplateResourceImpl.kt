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

package com.tencent.devops.store.template.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.template.v2.ServicePipelineTemplateV2Resource
import com.tencent.devops.store.api.template.UserTemplateResource
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.InstallTemplateResp
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.MyTemplateItem
import com.tencent.devops.store.pojo.template.MyTemplateItemResponse
import com.tencent.devops.store.pojo.template.PublishStrategyUpdateReq
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.TemplatePublishedVersionInfo
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import com.tencent.devops.store.template.service.MarketTemplatePublishedService
import com.tencent.devops.store.template.service.MarketTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTemplateResourceImpl @Autowired constructor(
    private val marketTemplateService: MarketTemplateService,
    private val storeProjectService: StoreProjectService,
    private val client: Client,
    private val marketTemplatePublishedService: MarketTemplatePublishedService
) : UserTemplateResource {

    override fun getInstalledProjects(
        userId: String,
        templateCode: String
    ): Result<List<InstalledProjRespItem?>> {
        return storeProjectService.getInstalledProjects(userId, templateCode, StoreTypeEnum.TEMPLATE)
    }

    override fun getMyTemplates(
        userId: String,
        templateName: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<MyTemplateItem>?> {
        return marketTemplateService.getMyTemplates(userId, templateName, page, pageSize)
    }

    override fun getMyTemplatesNew(
        userId: String,
        templateName: String?,
        projectName: String?,
        status: TemplateStatusEnum?,
        modifier: String?,
        description: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<MyTemplateItemResponse>> {
        return Result(
            marketTemplateService.getMyTemplatesNew(
                userId = userId,
                templateName = templateName,
                projectName = projectName,
                status = status,
                modifier = modifier,
                description = description,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun listPublishedHistory(
        userId: String,
        templateCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<TemplatePublishedVersionInfo>> {
        return Result(
            marketTemplatePublishedService.list(
                userId = userId,
                templateCode = templateCode,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun installTemplate(
        userId: String,
        installTemplateReq: InstallTemplateReq
    ): Result<Boolean> {
        val installResult = marketTemplateService.installTemplate(
            userId = userId,
            channelCode = ChannelCode.BS,
            installTemplateReq = installTemplateReq
        )
        return Result(
            status = installResult.status,
            message = installResult.message,
            data = installResult.data?.result ?: false
        )
    }

    override fun installTemplateNew(
        userId: String,
        installTemplateReq: InstallTemplateReq
    ): Result<InstallTemplateResp> {
        return marketTemplateService.installTemplate(
            userId = userId,
            channelCode = ChannelCode.BS,
            installTemplateReq = installTemplateReq
        )
    }

    override fun installTemplateV2(
        userId: String,
        installTemplateReq: InstallTemplateReq
    ): Result<InstallTemplateResp> {
        return marketTemplateService.installTemplateV2(
            userId = userId,
            channelCode = ChannelCode.BS,
            installTemplateReq = installTemplateReq
        )
    }

    override fun updatePublishStrategy(
        userId: String,
        templateCode: String,
        strategy: PublishStrategyUpdateReq
    ): Result<Boolean> {
        return client.get(ServicePipelineTemplateV2Resource::class).updatePublishStrategy(
            userId = userId,
            templateId = templateCode,
            strategy = strategy.publishStrategy
        )
    }

    override fun mainPageList(userId: String, page: Int?, pageSize: Int?): Result<List<MarketTemplateMain>> {
        return marketTemplateService.mainPageList(userId, page, pageSize)
    }

    override fun list(
        userId: String,
        keyword: String?,
        classifyCode: String?,
        categoryCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        projectCode: String?,
        excludeProjectCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<MarketTemplateResp> {
        return Result(
            marketTemplateService.list(
                userId = userId.trim(),
                keyword = keyword?.trim(),
                classifyCode = classifyCode?.trim(),
                category = categoryCode?.trim(),
                labelCode = labelCode?.trim(),
                score = score,
                rdType = rdType,
                sortType = sortType,
                projectCode = projectCode,
                excludeProjectCode = excludeProjectCode,
                page = page,
                pageSize = pageSize
            )
        )
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
}
