package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 流水线模板版本创建兼容后置处理器
 */
@Service
class PTemplateCompatibilityVersionPostProcessor(
    val v2TemplateInfoService: PipelineTemplateInfoService,
    val v2TemplateResourceService: PipelineTemplateResourceService,
    val v2TemplateSettingService: PipelineTemplateSettingService,
    val v1TemplateDao: TemplateDao,
    val v1TemplateSettingService: PipelineSettingDao,
    val dslContext: DSLContext
) : PTemplateVersionCreatePostProcessor {

    @Value("\${template.maxSaveVersionRecordNum:2}")
    private val maxSaveVersionRecordNum: Int = 2

    override fun postProcessAfterVersionCreate(
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        with(context) {
            if (!versionAction.isCreateReleaseVersion()) {
                return
            }
            val version = pipelineTemplateResource.version
            val versionName = pipelineTemplateResource.versionName

            val v1TemplateRecord = v1TemplateDao.getTemplate(
                dslContext = dslContext,
                projectId = projectId,
                version = version
            )
            val v2TemplateInfo = v2TemplateInfoService.get(
                projectId = projectId,
                templateId = templateId
            )
            val v2TemplateResource = v2TemplateResourceService.get(
                projectId = projectId,
                templateId = templateId,
                version = version
            )
            val v2TemplateSetting = v2TemplateSettingService.get(
                projectId = projectId,
                templateId = templateId,
                settingVersion = v2TemplateResource.settingVersion
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                if (v1TemplateRecord != null) {
                    val saveRecordVersions = v1TemplateDao.listSaveRecordVersions(
                        dslContext = transactionContext,
                        projectId = projectId,
                        templateId = templateId,
                        versionName = versionName!!,
                        saveNum = maxSaveVersionRecordNum
                    )
                    if (saveRecordVersions?.isNotEmpty == true) {
                        // 版本名称为versionName的版本只保存最近maxSaveVersionRecordNum条记录
                        v1TemplateDao.deleteSpecVersion(
                            dslContext = transactionContext,
                            projectId = projectId,
                            templateId = templateId,
                            versionName = versionName,
                            saveVersions = saveRecordVersions.map { it.value1() }
                        )
                    }
                }
                v1TemplateDao.createTemplate(
                    dslContext = transactionContext,
                    projectId = projectId,
                    templateId = templateId,
                    templateName = v2TemplateInfo.name,
                    versionName = v2TemplateResource.versionName!!,
                    userId = userId,
                    template = JsonUtil.toJson(v2TemplateResource.model),
                    type = v2TemplateInfo.mode.name,
                    category = v2TemplateInfo.category,
                    logoUrl = v2TemplateInfo.logoUrl,
                    srcTemplateId = v2TemplateResource.srcTemplateId,
                    storeFlag = v2TemplateInfo.storeStatus != TemplateStatusEnum.NEVER_PUBLISHED,
                    weight = 0,
                    version = version,
                    desc = v2TemplateInfo.desc
                )
                v1TemplateSettingService.saveSetting(
                    dslContext = transactionContext,
                    setting = v2TemplateSetting
                )
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(PTemplateCompatibilityVersionPostProcessor::class.java)
    }
}
