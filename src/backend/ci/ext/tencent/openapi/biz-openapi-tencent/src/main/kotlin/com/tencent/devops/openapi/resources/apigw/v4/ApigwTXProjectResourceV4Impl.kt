package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwTXProjectResourceV4
import com.tencent.devops.project.api.pojo.ProjectProductInfo
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
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

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTXProjectResourceV4Impl::class.java)
    }
}
