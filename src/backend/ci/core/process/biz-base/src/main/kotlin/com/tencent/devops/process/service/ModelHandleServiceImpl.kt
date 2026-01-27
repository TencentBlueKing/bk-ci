package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelHandleService
import com.tencent.devops.common.pipeline.ModelPublicVarHandleContext
import com.tencent.devops.common.pipeline.ModelVarReferenceHandleContext
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.utils.ModelVarRefUtils
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.service.`var`.PublicVarReferInfoService
import com.tencent.devops.process.service.`var`.PublicVarService
import com.tencent.devops.process.service.`var`.VarReferenceRequestWithLock
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ModelHandleServiceImpl @Autowired constructor(
    private val publicVarService: PublicVarService,
    private val publicVarReferInfoService: PublicVarReferInfoService,
    private val dslContext: DSLContext,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineTemplateResourceDao: PipelineTemplateResourceDao
) : ModelHandleService {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelHandleServiceImpl::class.java)
        private const val MAX_RETRY_TIMES = 3
        private const val RETRY_INTERVAL_MILLIS = 500L
    }

    override fun handleModelParams(
        projectId: String,
        modelPublicVarHandleContext: ModelPublicVarHandleContext
    ): List<BuildFormProperty> {
        return publicVarService.handleModelParams(
            projectId = projectId,
            modelPublicVarHandleContext = modelPublicVarHandleContext
        )
    }

    override fun handleModelVarReferences(
        userId: String,
        context: ModelVarReferenceHandleContext
    ) {
        val projectId = context.projectId
        val resourceId = context.resourceId
        val resourceType = context.resourceType
        val resourceVersion = context.resourceVersion

        try {
            logger.info("Start detecting variable references for resource: $resourceId|$resourceVersion")
            val modelInfo = context.model ?: getResourceModel(
                projectId = projectId,
                resourceType = resourceType,
                resourceId = resourceId,
                resourceVersion = resourceVersion
            ) ?: return
            modelInfo.handlePublicVarInfo()
            // 使用 ModelVarRefUtils 解析变量引用
            val varRefDetails = ModelVarRefUtils.parseModelVarReferences(
                model = modelInfo,
                projectId = projectId
            )
            varRefDetails.forEach { varRefDetail ->
                varRefDetail.referVersion = resourceVersion
            }
            logger.info("handleModelVarReferences for varRefDetails: $varRefDetails")
            publicVarReferInfoService.handleResourceVarReferencesWithLock(
                VarReferenceRequestWithLock(
                    userId = userId,
                    projectId = projectId,
                    resourceId = resourceId,
                    resourceType = resourceType,
                    resourceVersion = resourceVersion,
                    model = modelInfo,
                    varRefDetails = varRefDetails
                )
            )
            
            logger.info("Variable references update completed for resource: $resourceId")
        } catch (ignored: Throwable) {
            logger.warn("Error while detecting variable references for resource: $resourceId", ignored)
            throw ignored
        }
    }

    private fun getResourceModel(
        projectId: String,
        resourceId: String,
        resourceType: String,
        resourceVersion: Int
    ): Model? {
        
        var retryCount = 0
        var model: Model?

        while (retryCount < MAX_RETRY_TIMES) {
            val modelString = when (resourceType) {
                PublicVerGroupReferenceTypeEnum.PIPELINE.name -> {
                    pipelineResourceVersionDao.getVersionModelString(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = resourceId,
                        version = resourceVersion,
                        includeDraft = true
                    )
                }
                PublicVerGroupReferenceTypeEnum.TEMPLATE.name -> {
                    pipelineTemplateResourceDao.getVersionModelString(
                        dslContext = dslContext,
                        projectId = projectId,
                        templateId = resourceId,
                        version = resourceVersion.toLong(),
                        includeDraft = true
                    )
                }

                else -> null
            }
            
            model = modelString?.let { JsonUtil.to(it, ITemplateModel::class.java) } as? Model
            
            if (model != null) {
                if (retryCount > 0) {
                    logger.info("Successfully got resource model after $retryCount retries: $resourceId")
                }
                return model
            }
            
            retryCount++
            if (retryCount < MAX_RETRY_TIMES) {
                logger.warn("Failed to get resource model, retrying ($retryCount/$MAX_RETRY_TIMES): $resourceId")
                Thread.sleep(RETRY_INTERVAL_MILLIS)
            }
        }
        
        logger.warn("Failed to get resource model after $MAX_RETRY_TIMES retries: $resourceId")
        return null
    }
}
