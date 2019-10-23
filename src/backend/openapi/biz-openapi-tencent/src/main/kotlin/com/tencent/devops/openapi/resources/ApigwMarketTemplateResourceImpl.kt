package com.tencent.devops.openapi.resources

import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.api.ApigwMarketTemplateResource
import org.slf4j.LoggerFactory

@RestResource
class ApigwMarketTemplateResourceImpl @Autowired constructor(private val client: Client) : ApigwMarketTemplateResource {
    override fun list(userId: String): Result<List<MarketItem?>> {
        logger.info("get user'd market template, user($userId)")
        val marketTemplateResp = client.get(ServiceTemplateResource::class).list(userId)
        if (marketTemplateResp.data != null) {
            val marketItemList = (marketTemplateResp.data as MarketTemplateResp).records
            if ((marketTemplateResp.data as MarketTemplateResp).records.isNotEmpty()) {
                // 过滤 没有安装权限的
                return Result(marketItemList.filter {
                    it?.flag == true
                })
            }
        }
        return Result(listOf(null))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwMarketTemplateResourceImpl::class.java)
    }
}