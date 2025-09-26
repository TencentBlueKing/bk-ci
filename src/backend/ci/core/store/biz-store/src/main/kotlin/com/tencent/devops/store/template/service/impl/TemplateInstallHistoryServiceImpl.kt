package com.tencent.devops.store.template.service.impl

import com.tencent.devops.store.pojo.template.TemplateVersionInstallHistoryInfo
import com.tencent.devops.store.template.dao.TemplateInstallHistoryDao
import com.tencent.devops.store.template.service.TemplateInstallHistoryService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TemplateInstallHistoryServiceImpl(
    private val dslContext: DSLContext,
    private val templateInstallHistoryDao: TemplateInstallHistoryDao
) : TemplateInstallHistoryService {
    override fun create(installHistoryInfo: TemplateVersionInstallHistoryInfo) {
        logger.info("create template install history:$installHistoryInfo")
        templateInstallHistoryDao.create(
            dslContext = dslContext,
            record = installHistoryInfo
        )
    }

    override fun delete(templateCode: String) {
        logger.info("delete template install history:$templateCode")
        templateInstallHistoryDao.delete(
            dslContext = dslContext,
            templateCode = templateCode
        )
    }

    override fun deleteVersions(srcTemplateCode: String, templateCode: String, versions: List<Long>) {
        logger.info("delete template install versions history:$srcTemplateCode|$templateCode|$versions")
        templateInstallHistoryDao.delete(
            dslContext = dslContext,
            srcTemplateCode = srcTemplateCode,
            templateCode = templateCode,
            versions = versions
        )
    }

    override fun getRecently(templateCode: String): TemplateVersionInstallHistoryInfo? {
        return templateInstallHistoryDao.getRecentlyInstalledVersion(
            dslContext = dslContext,
            templateCode = templateCode
        )
    }

    override fun getLatest(templateCode: String): TemplateVersionInstallHistoryInfo? {
        return listLatestRecords(listOf(templateCode)).firstOrNull { it.templateCode == templateCode }
    }

    override fun listLatestRecords(templateCodes: List<String>): List<TemplateVersionInstallHistoryInfo> {
        return templateInstallHistoryDao.listLatestInstalledVersions(
            dslContext = dslContext,
            templateCodes = templateCodes
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateInstallHistoryServiceImpl::class.java)
    }
}
