package com.tencent.devops.misc.service.process

import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PipelineTemplateDataClearService @Autowired constructor(
    private val processMiscService: ProcessMiscService,
    private val processDataClearService: ProcessDataClearService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateDataClearService::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
    }

    @Value("\${process.draftVersionStoreDays:30}")
    private val draftVersionStoreDays: Long = 30

    fun clearTemplateDraftVersionData(projectId: String, templateId: String) {
        val expireTime = LocalDateTime.now().minusDays(draftVersionStoreDays)
        var offset = 0
        do {
            // 1. 从草稿表中获取模版版本
            val versions = processMiscService.listTemplateDraftVersions(
                projectId = projectId, templateId = templateId,
                limit = DEFAULT_PAGE_SIZE, offset = offset
            )
            if (versions.isEmpty()) return
            // 2. 获取已经发布超过X天的版本
            val expiredVersions = processMiscService.getExpiredTemplateVersions(
                projectId = projectId, templateId = templateId,
                versions = versions, expireTime = expireTime
            )
            if (expiredVersions.isEmpty()) return
            try {
                logger.info("clearTemplateDraftVersionData|$projectId|$templateId|$expiredVersions")
                processDataClearService.deleteTemplateDraftData(
                    projectId = projectId, templateId = templateId,
                    versions = expiredVersions
                )
            } catch (ignored: Throwable) {
                logger.warn(
                    "clearTemplateDraftVersionData failed|$projectId|$templateId|$expiredVersions",
                    ignored
                )
            }
            offset += DEFAULT_PAGE_SIZE
        } while (versions.size == DEFAULT_PAGE_SIZE)
    }
}
