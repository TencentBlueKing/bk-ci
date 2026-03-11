package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwTXProjectResourceV4
import com.tencent.devops.project.api.pojo.ProjectProductInfo
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.OperationalProductVO
import com.tencent.devops.project.pojo.Result
import org.slf4j.LoggerFactory

@RestResource
class ApigwTXProjectResourceV4Impl constructor(
    val client: Client
) : ApigwTXProjectResourceV4 {
    override fun listProjectProductInfos(
        appCode: String?,
        apigwType: String?,
        page: Int,
        pageSize: Int
    ): Result<List<ProjectProductInfo>> {
        logger.info("OPENAPI_TX_PROJECT_V4|$appCode|$apigwType|list project product infos")
        return client.get(ServiceTxProjectResource::class).listProjectProductInfos(page, pageSize)
    }

    override fun getProductsByBgName(
        appCode: String?,
        apigwType: String?,
        bgName: String
    ): Result<List<OperationalProductVO>> {
        logger.info("OPENAPI_TX_PROJECT_V4|$appCode|$apigwType|get_products_by_bg_name|$bgName")
        return client.get(ServiceTxProjectResource::class).getOperationalProductsByBgName(bgName)
    }

    override fun getProductByProductId(
        appCode: String?,
        apigwType: String?,
        productId: Int
    ): Result<OperationalProductVO?> {
        logger.info("OPENAPI_TX_PROJECT_V4|$appCode|$apigwType|get_product_by_product_id|$productId")
        return client.get(ServiceTxProjectResource::class).getProductByProductId(productId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTXProjectResourceV4Impl::class.java)
    }
}
