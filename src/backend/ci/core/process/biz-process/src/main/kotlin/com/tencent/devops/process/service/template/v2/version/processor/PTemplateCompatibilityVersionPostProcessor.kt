package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

/**
 * 流水线模板版本创建兼容后置处理器
 */
@Service
@Order(10)
class PTemplateCompatibilityVersionPostProcessor(
    val v1TemplateDao: TemplateDao,
    val v1TemplateSettingService: PipelineSettingDao,
    val dslContext: DSLContext,
) : PTemplateVersionCreatePostProcessor {
    @Value("\${process.template.dual-write.enabled:#{true}}")
    private val dualWriteEnabled: Boolean = true

    @Value("\${process.template.dual-write.strict:#{true}}")
    private val strictMode: Boolean = true

    @Suppress("NestedBlockDepth")
    override fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        with(context) {
            if (!versionAction.isCreateReleaseVersion() || !dualWriteEnabled ||
                context.pipelineTemplateInfo.type != PipelineTemplateType.PIPELINE) {
                return
            }
            logger.info(
                "template compatibility post process begin|" +
                    "project=$projectId|template=$templateId|" +
                    "customVersionName=$customVersionName"
            )
            try {
                val version = pipelineTemplateResource.version
                val v2TemplateInfo = context.pipelineTemplateInfo
                val v2VersionName = pipelineTemplateResource.versionName
                val constraintFlag = v2TemplateInfo.mode == TemplateType.CONSTRAINT
                val templateVersionCount = v1TemplateDao.countTemplateVersionNum(dslContext, projectId, templateId)
                if (constraintFlag && templateVersionCount > 0) {
                    logger.info("v2->v1 constraint template dual write skip|project=$projectId|template=$templateId")
                    return
                }
                val storeFlag = constraintFlag ||
                    v2TemplateInfo.storeStatus != TemplateStatusEnum.NEVER_PUBLISHED
                splitParamsForV1Compatibility(pipelineTemplateResource.model as Model)
                v1TemplateDao.createTemplate(
                    dslContext = transactionContext,
                    projectId = projectId,
                    templateId = templateId,
                    templateName = pipelineTemplateSetting.pipelineName,
                    versionName = customVersionName ?: v2VersionName!!,
                    userId = userId,
                    template = JsonUtil.toJson(pipelineTemplateResource.model, false),
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

    /**
     * 将 v2 版本的合并参数拆分为 v1 版本的 templateParams 和 params
     *
     * v2 版本：params 包含所有参数，其中 constant = true 的来自原 templateParams
     * v1 版本：templateParams 和 params 分开存储
     */
    private fun splitParamsForV1Compatibility(model: Model) {
        val triggerContainer = model.getTriggerContainer()
        val allParams = triggerContainer.params
        // 将参数按 constant 标记分组
        val (templateParams, params) = allParams.partition { it.constant == true }
        triggerContainer.params = params.toMutableList().map {
            // 模版入参+实例化不入参,那么旧变量应该是不入参
            if (it.required && it.asInstanceInput == false) {
                it.copy(required = false)
            } else {
                it
            }
        }
        triggerContainer.templateParams = takeIf { templateParams.isNotEmpty() }?.let {
            templateParams.map { it.copy(constant = false) }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PTemplateCompatibilityVersionPostProcessor::class.java)
    }
}
