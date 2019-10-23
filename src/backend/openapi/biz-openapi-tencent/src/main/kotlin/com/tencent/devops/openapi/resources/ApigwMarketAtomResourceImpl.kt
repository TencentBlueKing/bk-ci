package com.tencent.devops.openapi.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.api.ApigwMarketAtomResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomStatistic
import com.tencent.devops.store.pojo.atom.AtomVersion
import org.slf4j.LoggerFactory

@RestResource
class ApigwMarketAtomResourceImpl @Autowired constructor(private val client: Client) : ApigwMarketAtomResource {
    override fun getAtomByCode(atomCode: String, userId: String): Result<AtomVersion?> {
        logger.info("get Atom By Code, atomCode($atomCode),userId($userId)")
        return client.get(ServiceMarketAtomResource::class).getAtomByCode(atomCode, userId)
    }

    override fun getAtomStatisticByCode(atomCode: String, userId: String): Result<AtomStatistic> {
        logger.info("get Atom Statistic By Code, atomCode($atomCode),userId($userId)")
        return client.get(ServiceMarketAtomResource::class).getAtomStatisticByCode(atomCode, userId)
    }

    override fun getAtomPipelinesByCode(
        atomCode: String,
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AtomPipeline>> {
        logger.info("get Atom Pipelines By Code, atomCode($atomCode),userId($userId),page($page),pageSize($pageSize)")
        return client.get(ServiceMarketAtomResource::class).getAtomPipelinesByCode(atomCode, userId, page, pageSize)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwMarketAtomResourceImpl::class.java)
    }
}