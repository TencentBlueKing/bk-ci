package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceUpdateInfo
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 流水线模板版本创建兼容后置处理器
 */
@Service
class PTemplateCompatibilityVersionPostProcessor(
    val v2TemplateResourceService: PipelineTemplateResourceService,
    val v1TemplateDao: TemplateDao,
    val v1TemplateSettingService: PipelineSettingDao,
    val dslContext: DSLContext,
) : PTemplateVersionCreatePostProcessor {
    @Value("\${process.template.dual-write.enabled:#{true}}")
    private val dualWriteEnabled: Boolean = true

    @Value("\${process.template.dual-write.strict:#{true}}")
    private val strictMode: Boolean = true

    override fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        with(context) {
            if (!versionAction.isCreateReleaseVersion() || !dualWriteEnabled) {
                return
            }
            logger.info(
                "template compatibility post process begin|" +
                    "project=$projectId|template=$templateId|v1Name=$v1VersionName"
            )
            try {
                val version = pipelineTemplateResource.version
                val v2TemplateInfo = context.pipelineTemplateInfo
                val v2VersionName = pipelineTemplateResource.versionName

                // 如 v2 已存在与 v1VersionName 同名的 RELEASED 记录，则重命名为 -1
                if (!v1VersionName.isNullOrBlank()) {
                    val existsV2Plain = v2TemplateResourceService.getLatestResource(
                        projectId = projectId,
                        templateId = templateId,
                        status = VersionStatus.RELEASED,
                        versionName = v1VersionName
                    ) != null
                    if (existsV2Plain) {
                        val newName = "$v1VersionName-1"
                        try {
                            val affected = v2TemplateResourceService.update(
                                transactionContext = transactionContext,
                                record = PipelineTemplateResourceUpdateInfo(versionName = newName),
                                commonCondition = PipelineTemplateResourceCommonCondition(
                                    projectId = projectId,
                                    templateId = templateId,
                                    versionName = v1VersionName,
                                    status = VersionStatus.RELEASED
                                )
                            )
                            logger.info(
                                "v2 rename for compatibility|project=$projectId|template=$templateId|" +
                                    "from=$v1VersionName|to=$newName|affected=$affected"
                            )
                        } catch (t: Throwable) {
                            logger.warn(
                                "v2 rename conflict for compatibility, skip|project=$projectId|template=$templateId|" +
                                    "from=$v1VersionName|to=$newName",
                                t
                            )
                        }
                    }
                }

                val storeFlag = v2TemplateInfo.mode == TemplateType.CONSTRAINT ||
                    v2TemplateInfo.storeStatus != TemplateStatusEnum.NEVER_PUBLISHED
                v1TemplateDao.createTemplate(
                    dslContext = transactionContext,
                    projectId = projectId,
                    templateId = templateId,
                    templateName = pipelineTemplateSetting.pipelineName,
                    versionName = v1VersionName ?: v2VersionName!!,
                    userId = userId,
                    template = JsonUtil.toJson(pipelineTemplateResource.model),
                    type = v2TemplateInfo.mode.name,
                    category = v2TemplateInfo.category,
                    logoUrl = v2TemplateInfo.logoUrl,
                    srcTemplateId = pipelineTemplateResource.srcTemplateId,
                    storeFlag = storeFlag,
                    weight = 0,
                    version = version,
                    desc = v2TemplateInfo.desc
                )
                v1TemplateSettingService.saveSetting(
                    dslContext = transactionContext,
                    setting = pipelineTemplateSetting,
                    isTemplate = true
                )
                logger.info("v2->v1 dual write success|project=$projectId|template=$templateId|version=$version")
            } catch (t: Throwable) {
                logger.warn("v2->v1 dual write failed|project=$projectId|template=$templateId", t)
                if (strictMode) throw t
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PTemplateCompatibilityVersionPostProcessor::class.java)
    }
}
