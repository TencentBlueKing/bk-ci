package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelHandleService
import com.tencent.devops.common.pipeline.ModelPublicVarExpansion
import com.tencent.devops.common.pipeline.ModelPublicVarHandleContext
import com.tencent.devops.common.pipeline.ModelVarReferenceHandleContext
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.utils.ModelVarRefUtils
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.service.`var`.PublicVarReferInfoService
import com.tencent.devops.process.service.`var`.PublicVarService
import com.tencent.devops.process.pojo.`var`.VarReferenceRequestWithLock
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
            val modelInfo = context.model ?: getResourceModel(
                projectId = projectId,
                resourceType = resourceType,
                resourceId = resourceId,
                resourceVersion = resourceVersion
            )
            if (modelInfo == null) {
                // getResourceModel 内已打重试/耗尽 warn；此处再落一条业务层结论，避免只有入口无结果
                logger.warn(
                    "Skip variable reference update: resource model not found, " +
                        "resourceId=$resourceId, resourceType=$resourceType, resourceVersion=$resourceVersion"
                )
                return
            }
            modelInfo.handlePublicVarInfo()
            expandPublicVarParamsForAnalysis(
                model = modelInfo,
                projectId = projectId,
                resourceId = resourceId,
                resourceType = resourceType,
                resourceVersion = resourceVersion
            )
            // 使用 ModelVarRefUtils 解析变量引用
            val varRefDetails = ModelVarRefUtils.parseModelVarReferences(
                model = modelInfo,
                projectId = projectId,
                resourceId = resourceId,
                resourceType = resourceType
            )
            varRefDetails.forEach { varRefDetail ->
                varRefDetail.referVersion = resourceVersion
            }
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
            // 只打摘要，避免整份 VarRefDetail 列表刷爆日志（含 positionPath/taskId 等）
            logger.info(
                "Variable references update completed: resourceId=$resourceId, " +
                    "resourceType=$resourceType, resourceVersion=$resourceVersion, " +
                    "refCount=${varRefDetails.size}, " +
                    "varNames=${varRefDetails.map { it.varName }.distinct()}"
            )
        } catch (ignored: Throwable) {
            logger.warn(
                "Error while detecting variable references for resource: " +
                    "$resourceId|$resourceType|$resourceVersion",
                ignored
            )
            throw ignored
        }
    }

    /**
     * 变量引用分析前，将**动态版本**公共变量组成员展开到 triggerContainer.params（仅内存，不落库）。
     *
     * 本路径经 getResourceModel 的 withoutExpansion 加载，已关闭 ModelDeserializer 的自动展开，故需在此手动补齐。
     * 相对自动展开的必要性：支持模板（JSON 无 templateId）、按 resourceVersion 精确展开、
     * 兼容引用式/YAML 存储中 params 缺成员的情形。
     * [handleModelParams] 对固定版组 skip，故本方法对纯固定版流水线为空操作。
     *
     * 为何不能按"params 已含该组成员"短路：存储态 params 记录的是该组**保存时版本**的成员，
     * 组后续升级（增/删变量、改内容）后 params 即过期；动态引用需经 handleModelParams 的 diff
     * 对齐到最新版（补新增、剔已删、刷内容），仅凭组名存在无法判断成员是否最新，故必须实际展开。
     *
     * 草稿排除语义不受影响：变量级汇总按 LATEST_FLAG=true 聚合，草稿引用不计入生效计数。
     * 展开失败时降级为原始 params，不阻断主流程。
     */
    private fun expandPublicVarParamsForAnalysis(
        model: Model,
        projectId: String,
        resourceId: String,
        resourceType: String,
        resourceVersion: Int
    ) {
        val publicVarGroups = model.publicVarGroups
        if (publicVarGroups.isNullOrEmpty()) return
        // 纯固定版本：成员被 pin 在具体版本、不随组升级漂移，且 handleModelParams 对固定版组本就 skip，可安全跳过。
        // 动态版本：即使 params 已含成员也可能是旧版本快照，必须展开以对齐最新版，不能按组名存在短路。
        if (publicVarGroups.none { it.version == null }) return
        try {
            val triggerContainer = model.getTriggerContainer()
            val expandedParams = handleModelParams(
                projectId = projectId,
                modelPublicVarHandleContext = ModelPublicVarHandleContext(
                    referId = resourceId,
                    referType = PublicVarGroupReferenceTypeEnum.valueOf(resourceType),
                    referVersion = resourceVersion,
                    params = triggerContainer.params,
                    publicVarGroups = publicVarGroups
                )
            )
            triggerContainer.params = expandedParams.toMutableList()
        } catch (ignored: Throwable) {
            logger.warn(
                "Expand public var params for analysis failed, fallback to raw params: " +
                    "resourceId=$resourceId, resourceVersion=$resourceVersion",
                ignored
            )
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
                PublicVarGroupReferenceTypeEnum.PIPELINE.name -> {
                    pipelineResourceVersionDao.getVersionModelString(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = resourceId,
                        version = resourceVersion,
                        includeDraft = true
                    )
                }
                PublicVarGroupReferenceTypeEnum.TEMPLATE.name -> {
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

            // 先按存储态加载（关闭反序列化时的自动展开，避免重复 RPC 且能使用正确的 resourceVersion）；
            // 动态版本公共变量成员的展开由 expandPublicVarParamsForAnalysis 在解析前显式完成。
            model = modelString?.let {
                ModelPublicVarExpansion.withoutExpansion { JsonUtil.to(it, ITemplateModel::class.java) }
            } as? Model

            if (model != null) {
                // 模板 Model 的 JSON 中不含 templateId，需从 context 回填
                when (resourceType) {
                    PublicVarGroupReferenceTypeEnum.PIPELINE.name -> model.pipelineId = resourceId
                    PublicVarGroupReferenceTypeEnum.TEMPLATE.name -> model.templateId = resourceId
                }
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
