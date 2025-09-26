package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.api.util.JsonUtil
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
import org.springframework.stereotype.Service

/**
 * 流水线模板版本创建兼容后置处理器
 */
@Service
class PTemplateCompatibilityVersionPostProcessor(
    val v2TemplateResourceService: PipelineTemplateResourceService,
    val v1TemplateDao: TemplateDao,
    val v1TemplateSettingService: PipelineSettingDao,
    val dslContext: DSLContext
) : PTemplateVersionCreatePostProcessor {

    override fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        with(context) {
            if (!versionAction.isCreateReleaseVersion()) {
                return
            }
            logger.info("template compatibility version post processor :$context")
            val version = pipelineTemplateResource.version
            val v2TemplateInfo = context.pipelineTemplateInfo
            val v2VersionName = pipelineTemplateResource.versionName
            // 假设新表一开始已经有v1名称版本了，此时老接口，旧逻辑再创建一个同名称v1，
            // 那么此时应该将之前新表中的v1版本改为v1-1，并且新版本为v1-2.
            if (!v1VersionName.isNullOrBlank()) {
                val existingCount = v1TemplateDao.countTemplateVersions(
                    dslContext = transactionContext,
                    projectId = projectId,
                    templateId = templateId,
                    versionName = v1VersionName
                )
                if (existingCount == 1) {
                    v2TemplateResourceService.update(
                        transactionContext = transactionContext,
                        PipelineTemplateResourceUpdateInfo(
                            versionName = "$v1VersionName-1"
                        ),
                        PipelineTemplateResourceCommonCondition(
                            projectId = projectId,
                            templateId = templateId,
                            versionName = v1VersionName
                        )
                    )
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
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PTemplateCompatibilityVersionPostProcessor::class.java)
    }
}
