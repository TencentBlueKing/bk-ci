package com.tencent.devops.store.template.service.impl

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.store.pojo.template.TemplatePublishedVersionInfo
import com.tencent.devops.store.template.dao.MarketTemplatePublishedDao
import com.tencent.devops.store.template.service.MarketTemplatePublishedService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MarketTemplatePublishedServiceImpl(
    private val marketTemplatePublishedDao: MarketTemplatePublishedDao,
    private val dslContext: DSLContext
) : MarketTemplatePublishedService {
    override fun create(templatePublishedVersionInfo: TemplatePublishedVersionInfo) {
        logger.info("create template published history:$templatePublishedVersionInfo")
        marketTemplatePublishedDao.createOrUpdate(
            dslContext = dslContext,
            record = templatePublishedVersionInfo
        )
    }

    override fun deleteVersions(templateCode: String, versions: List<Long>) {
        logger.info("delete template published versions:$templateCode |$versions")
        marketTemplatePublishedDao.delete(
            dslContext = dslContext,
            templateCode = templateCode,
            versions = versions
        )
    }

    override fun delete(templateCode: String) {
        logger.info("delete template published history :$templateCode")
        marketTemplatePublishedDao.delete(
            dslContext = dslContext,
            templateCode = templateCode
        )
    }

    override fun getLatest(templateCode: String): TemplatePublishedVersionInfo? {
        return marketTemplatePublishedDao.getLatestMarketPublishedVersion(
            dslContext = dslContext,
            templateCode = templateCode
        )
    }

    override fun list(
        userId: String,
        templateCode: String,
        page: Int,
        pageSize: Int
    ): Page<TemplatePublishedVersionInfo> {
        val record = marketTemplatePublishedDao.listPublishedHistory(
            dslContext = dslContext,
            templateCode = templateCode,
            page = page,
            pageSize = pageSize
        )
        val count = marketTemplatePublishedDao.countPublishedHistory(
            dslContext = dslContext,
            templateCode = templateCode
        )
        val totalPages = PageUtil.calTotalPage(pageSize, count)
        return Page(
            count = count,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages,
            records = record
        )
    }

    override fun listLatestRecords(templateCodes: List<String>): List<TemplatePublishedVersionInfo> {
        return marketTemplatePublishedDao.listLatestPublishedVersions(
            dslContext = dslContext,
            templateCodes = templateCodes
        )
    }

    override fun offlineTemplate(templateCode: String, templateVersion: Long?) {
        return marketTemplatePublishedDao.offlineTemplate(
            dslContext = dslContext,
            templateCode = templateCode,
            templateVersion = templateVersion
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MarketTemplatePublishedServiceImpl::class.java)
    }
}
