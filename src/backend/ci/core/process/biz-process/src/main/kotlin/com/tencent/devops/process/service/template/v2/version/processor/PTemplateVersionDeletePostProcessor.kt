package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionDeleteContext
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Service

/**
 * 流水线模板版本创建审计后置处理器
 */
@Service
class PTemplateVersionDeletePostProcessor(
    private val operationLogService: PipelineOperationLogService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val templateDao: TemplateDao,
    private val dslContext: DSLContext,
    private val pipelineSettingDao: PipelineSettingDao
) {
    fun postProcessAfterDelete(context: PipelineTemplateVersionDeleteContext) {
        with(context) {
            when (versionAction) {
                PipelineVersionAction.DELETE_VERSION -> {
                    val resource = pipelineTemplateResourceService.get(
                        projectId = projectId,
                        templateId = templateId,
                        version = version!!
                    )
                    operationLogService.addOperationLog(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = templateId,
                        version = version.toInt(),
                        operationLogType = OperationLogType.DELETE_PIPELINE_VERSION,
                        params = resource.versionName ?: "",
                        description = null
                    )
                    templateDao.delete(
                        dslContext = dslContext,
                        projectId = projectId,
                        templateId = templateId,
                        versions = setOf(version)
                    )
                }

                PipelineVersionAction.DELETE_ALL_VERSIONS -> {
                    dslContext.transactionResult { configuration ->
                        val transactionContext = DSL.using(configuration)
                        templateDao.delete(
                            dslContext = transactionContext,
                            projectId = projectId,
                            templateId = templateId
                        )
                        pipelineSettingDao.delete(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = templateId
                        )
                    }
                }

                else -> {}
            }
        }
    }
}
