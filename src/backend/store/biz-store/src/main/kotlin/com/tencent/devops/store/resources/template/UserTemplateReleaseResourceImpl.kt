package com.tencent.devops.store.resources.template

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.template.UserTemplateReleaseResource
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.service.template.TemplateReleaseService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTemplateReleaseResourceImpl @Autowired constructor(
    private val templateReleaseService: TemplateReleaseService
) : UserTemplateReleaseResource {

    override fun addMarketTemplate(
        userId: String,
        templateCode: String,
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean> {
        return templateReleaseService.addMarketTemplate(userId, templateCode, marketTemplateRelRequest)
    }

    override fun updateMarketTemplate(
        userId: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?> {
        return templateReleaseService.updateMarketTemplate(userId, marketTemplateUpdateRequest)
    }

    override fun getProcessInfo(userId: String, templateId: String): Result<StoreProcessInfo> {
        return templateReleaseService.getProcessInfo(userId, templateId)
    }

    override fun cancelRelease(userId: String, templateId: String): Result<Boolean> {
        return templateReleaseService.cancelRelease(userId, templateId)
    }

    override fun offlineTemplate(userId: String, templateCode: String, version: String?, reason: String?): Result<Boolean> {
        return templateReleaseService.offlineTemplate(userId, templateCode, version, reason)
    }
}