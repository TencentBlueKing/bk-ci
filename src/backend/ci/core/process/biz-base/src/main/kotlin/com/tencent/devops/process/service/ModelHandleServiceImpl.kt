package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelHandleService
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.utils.ModelVarRefUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.VarRefDetailDao
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.service.`var`.PublicVarReferInfoService
import com.tencent.devops.process.service.`var`.PublicVarService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ModelHandleServiceImpl @Autowired constructor(
    private val publicVarService: PublicVarService,
    private val publicVarReferInfoService: PublicVarReferInfoService,
    private val varRefDetailDao: VarRefDetailDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineTemplateResourceDao: PipelineTemplateResourceDao
) : ModelHandleService {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelHandleServiceImpl::class.java)
        private const val MODEL_VAR_REF_LOCK_KEY = "MODEL_VAR_REF_LOCK_KEY"
        private const val LOCK_EXPIRED_TIME_IN_SECONDS = 5L
        private const val MAX_RETRY_TIMES = 3
        private const val RETRY_INTERVAL_MILLIS = 500L
    }

    override fun handleModelParams(
        projectId: String,
        model: Model,
        referId: String,
        referType: String,
        referVersion: Int
    ) {
        publicVarService.handleModelParams(
            projectId = projectId,
            model = model,
            referId = referId,
            referType = PublicVerGroupReferenceTypeEnum.valueOf(referType),
            referVersion = referVersion
        )
    }

    override fun handleModelVarReferences(
        userId: String,
        projectId: String,
        resourceId: String,
        resourceType: String,
        resourceVersion: Int
    ) {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "$MODEL_VAR_REF_LOCK_KEY:$projectId:$resourceType:$resourceId:$resourceVersion",
            expiredTimeInSeconds = LOCK_EXPIRED_TIME_IN_SECONDS
        )
        try {
            redisLock.lock()
            logger.info("Start detecting variable references for resource: $resourceId|$resourceVersion")

            val model = getResourceModel(
                projectId = projectId,
                resourceType = resourceType,
                resourceId = resourceId,
                resourceVersion = resourceVersion
            ) ?: return

            // 使用 ModelVarRefUtils 解析变量引用
            val varRefDetails = ModelVarRefUtils.parseModelVarReferences(
                model = model,
                projectId = projectId
            )

            varRefDetails.forEach { varRefDetail ->
                varRefDetail.referVersion = resourceVersion
            }

            logger.info("handleModelVarReferences for varRefDetails: $varRefDetails")
            // 处理变量引用详情
            varRefDetailDao.deleteByResourceId(
                dslContext = dslContext,
                projectId = projectId,
                resourceId = resourceId,
                resourceType = resourceType,
                referVersion = resourceVersion
            )
            if (varRefDetails.isNotEmpty()) {
                varRefDetailDao.batchSave(dslContext, varRefDetails)
            }
            // 处理公共变量组变量引用
            publicVarReferInfoService.handlePublicVarGroupReferences(
                userId = userId,
                model = model,
                varRefDetails = varRefDetails
            )
            logger.info("Variable references update completed for resource: $resourceId")
        } catch (ignored: Throwable) {
            logger.warn("Error while detecting variable references for resource: $resourceId", ignored)
            throw ignored
        } finally {
            redisLock.unlock()
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
