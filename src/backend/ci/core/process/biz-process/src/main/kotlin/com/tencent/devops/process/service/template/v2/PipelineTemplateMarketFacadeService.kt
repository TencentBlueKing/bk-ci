package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.control.lock.PipelineTemplateTriggerUpgradesLock
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateTriggerUpgradesEvent
import com.tencent.devops.process.pojo.template.MarketTemplateRequest
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.MarketTemplateV2Request
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateMarketCreateReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingUpdateInfo
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionManager
import com.tencent.devops.store.api.image.ServiceStoreImageResource
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.template.TemplatePublishedVersionInfo
import com.tencent.devops.store.pojo.template.TemplateVersionInstallHistoryInfo
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线市场模版门面类
 */
@Service
class PipelineTemplateMarketFacadeService @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val client: Client,
    private val pipelineTemplateVersionManager: PipelineTemplateVersionManager,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation
) {

    /**
     * 传播研发商店模板事件至依赖模板
     * */
    fun propagateTemplateUpdateToDependents(
        userId: String,
        projectId: String,
        updateMarketTemplateRequest: MarketTemplateRequest
    ): Boolean {
        logger.info("update market template reference:$userId|$projectId|$updateMarketTemplateRequest")
        with(updateMarketTemplateRequest) {
            val srcTemplateId = templateCode
            val category = JsonUtil.toJson(categoryCodeList ?: emptyList<String>(), false)
            val projectId2TemplateIdOfDependent = templateDao.listTemplateReferenceId(
                dslContext = dslContext,
                templateId = srcTemplateId
            )
            val dependentTemplateList = projectId2TemplateIdOfDependent.keys.toList()
            if (dependentTemplateList.isEmpty()) return true

            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                // 修改老表
                pipelineSettingDao.updateSettingName(
                    dslContext = transactionContext,
                    pipelineIdList = dependentTemplateList,
                    name = templateName
                )
                templateDao.updateTemplateReference(
                    dslContext = transactionContext,
                    srcTemplateId = srcTemplateId,
                    name = templateName,
                    category = category,
                    logoUrl = logoUrl
                )
            }
            // 同步新表，将关联的数据表，进行批量刷数据
            projectId2TemplateIdOfDependent.forEach { (projectId, templateId) ->
                pipelineTemplateInfoService.update(
                    record = PipelineTemplateInfoUpdateInfo(
                        name = templateName,
                        category = category,
                        logoUrl = logoUrl
                    ),
                    commonCondition = PipelineTemplateCommonCondition(
                        projectId = projectId,
                        templateId = templateId
                    )
                )
                pipelineTemplateSettingService.update(
                    record = PipelineTemplateSettingUpdateInfo(
                        name = templateName
                    ),
                    commonCondition = PipelineTemplateSettingCommonCondition(
                        projectId = projectId,
                        templateId = templateId
                    )
                )
            }
        }
        return true
    }

    /**
     * 上架模板至研发商店-更新关联模板相关
     * （1）更新模板关联标识storeFlag
     * （2）更新发布策略
     * （3）更新关联模板基本信息
     * （4）记录模板版本上架记录
     * （5）将关联并且自动升级的模板进行自动升级版本
     * */
    fun handleMarketTemplatePublished(request: MarketTemplateV2Request): Boolean {
        with(request) {
            // 更新老表 研发商店关联标识
            templateDao.updateStoreFlag(
                dslContext = dslContext,
                userId = publisher,
                projectId = projectId,
                templateId = templateCode,
                storeFlag = true
            )
            // 更新新表 研发商店关联标识/发布策略
            pipelineTemplateInfoService.update(
                transactionContext = dslContext,
                record = PipelineTemplateInfoUpdateInfo(
                    storeStatus = TemplateStatusEnum.RELEASED,
                    publishStrategy = request.publishStrategy,
                    updater = publisher
                ),
                commonCondition = PipelineTemplateCommonCondition(
                    projectId = projectId,
                    templateId = templateCode
                )
            )
            // 传播模板更新事件至依赖模板
            propagateTemplateUpdateToDependents(
                userId = publisher,
                projectId = projectId,
                updateMarketTemplateRequest = MarketTemplateRequest(request)
            )
            // 触发关联模板自动升级
            if (templateVersion != null) {
                pipelineEventDispatcher.dispatch(
                    PipelineTemplateTriggerUpgradesEvent(
                        projectId = projectId,
                        source = "PIPELINE_TEMPLATE_TRIGGER_UPGRADES",
                        pipelineId = "",
                        userId = publisher,
                        templateId = templateCode,
                        version = templateVersion!!
                    )
                )
            }
        }
        return true
    }

    fun updateStoreStatus(
        userId: String,
        projectId: String,
        templateId: String,
        storeStatus: TemplateStatusEnum,
        version: Long?
    ): Boolean {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            if (version != null) {
                pipelineTemplateResourceService.update(
                    transactionContext = context,
                    record = PipelineTemplateResourceUpdateInfo(
                        storeStatus = storeStatus,
                        updater = userId
                    ),
                    commonCondition = PipelineTemplateResourceCommonCondition(
                        projectId = projectId,
                        templateId = templateId,
                        version = version
                    )
                )
            } else {
                pipelineTemplateResourceService.update(
                    transactionContext = context,
                    record = PipelineTemplateResourceUpdateInfo(
                        storeStatus = storeStatus,
                        updater = userId
                    ),
                    commonCondition = PipelineTemplateResourceCommonCondition(
                        projectId = projectId,
                        templateId = templateId,
                        storeStatus = if (storeStatus == TemplateStatusEnum.UNDERCARRIAGED) {
                            TemplateStatusEnum.RELEASED
                        } else {
                            TemplateStatusEnum.UNDERCARRIAGED
                        }
                    )
                )
                pipelineTemplateInfoService.update(
                    transactionContext = context,
                    record = PipelineTemplateInfoUpdateInfo(
                        storeStatus = storeStatus,
                        updater = userId
                    ),
                    commonCondition = PipelineTemplateCommonCondition(
                        projectId = projectId,
                        templateId = templateId
                    )
                )
            }
        }
        return true
    }

    fun updatePublishStrategy(
        userId: String,
        templateId: String,
        strategy: UpgradeStrategyEnum
    ): Boolean {
        logger.info("update publish strategy :$userId|$templateId|$strategy")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineTemplateInfoService.get(
                templateId = templateId
            )
            pipelineTemplateInfoService.update(
                transactionContext = context,
                record = PipelineTemplateInfoUpdateInfo(
                    publishStrategy = strategy,
                    updater = userId
                ),
                commonCondition = PipelineTemplateCommonCondition(
                    templateId = templateId
                )
            )
        }
        return true
    }

    // 发布模板版本后触发自动升级事件
    fun releaseTemplateVersionAndTriggerUpgrades(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ) {
        PipelineTemplateTriggerUpgradesLock(redisOperation = redisOperation, templateId = templateId).use {
            it.lock()
            val startEpoch = System.currentTimeMillis()
            val templateResource = pipelineTemplateResourceService.get(
                projectId = projectId,
                templateId = templateId,
                version = version
            )
            pipelineTemplateResourceService.update(
                record = PipelineTemplateResourceUpdateInfo(
                    storeStatus = TemplateStatusEnum.RELEASED
                ),
                commonCondition = PipelineTemplateResourceCommonCondition(
                    projectId = projectId,
                    templateId = templateId,
                    version = version
                )
            )
            // 同步上传当前版本至研发商店
            client.get(ServiceTemplateResource::class).createMarketTemplatePublishedVersion(
                TemplatePublishedVersionInfo(
                    projectCode = projectId,
                    templateCode = templateId,
                    version = version,
                    versionName = templateResource.versionName!!,
                    number = templateResource.number,
                    published = true,
                    creator = userId,
                    updater = userId
                )
            )

            val templatesOfNeedToUpgrade = pipelineTemplateInfoService.list(
                commonCondition = PipelineTemplateCommonCondition(
                    mode = TemplateType.CONSTRAINT,
                    srcTemplateProjectId = projectId,
                    srcTemplateId = templateId,
                    upgradeStrategy = UpgradeStrategyEnum.AUTO
                )
            )

            templatesOfNeedToUpgrade.forEach { templateInfo ->
                installNewVersion(
                    templateInfo = templateInfo,
                    srcTemplateProjectId = templateResource.projectId,
                    srcTemplateId = templateResource.templateId,
                    srcTemplateVersion = templateResource.version,
                    srcTemplateNumber = templateResource.number,
                    srcTemplateVersionName = templateResource.versionName!!
                )
            }
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch}) ms to release template($templateId) " +
                    "version($version) and trigger upgrades"
            )
        }
    }

    fun installNewVersion(
        templateInfo: PipelineTemplateInfoV2,
        srcTemplateProjectId: String,
        srcTemplateId: String,
        srcTemplateVersion: Long,
        srcTemplateNumber: Int,
        srcTemplateVersionName: String
    ) {
        // 查询父模板上架的版本是否已经安装过
        val isInstalled = pipelineTemplateResourceService.getOrNull(
            commonCondition = PipelineTemplateResourceCommonCondition(
                projectId = templateInfo.projectId,
                templateId = templateInfo.id,
                srcTemplateProjectId = srcTemplateProjectId,
                srcTemplateId = srcTemplateId,
                srcTemplateVersion = srcTemplateVersion
            )
        ) != null
        if (isInstalled)
            return
        val isSyncSetting = templateInfo.settingSyncStrategy == UpgradeStrategyEnum.AUTO
        val pipelineTemplateMarketCreateReq = PipelineTemplateMarketCreateReq(
            marketTemplateProjectId = srcTemplateProjectId,
            marketTemplateId = srcTemplateId,
            marketTemplateVersion = srcTemplateVersion,
            copySetting = isSyncSetting,
            name = templateInfo.name
        )
        pipelineTemplateVersionManager.deployTemplate(
            userId = templateInfo.creator,
            projectId = templateInfo.projectId,
            templateId = templateInfo.id,
            request = pipelineTemplateMarketCreateReq
        )
        client.get(ServiceTemplateResource::class).createTemplateInstallHistory(
            TemplateVersionInstallHistoryInfo(
                srcMarketTemplateProjectCode = srcTemplateProjectId,
                srcMarketTemplateCode = srcTemplateId,
                projectCode = templateInfo.projectId,
                templateCode = templateInfo.id,
                version = srcTemplateVersion,
                versionName = srcTemplateVersionName,
                number = srcTemplateNumber
            )
        )
    }

    @Suppress("NestedBlockDepth")
    /**
     * 校验模板（指定版本）中引用的构建镜像是否全部处于【已发布】状态。
     *
     * 逻辑说明：
     * 1. 获取模板资源与其模型，仅在模板类型为 PIPELINE 时检查镜像；否则直接返回 null。
     * 2. 遍历所有阶段与容器，仅处理 VMBuildContainer 且分发类型为 StoreDispatchType 的场景。
     * 3. 以 `imageCode@imageVersion` 作为唯一键去重，避免对相同镜像重复调用远端状态接口。
     * 4. 一旦发现第一个未发布的镜像，立即短路返回该镜像的 imageCode；若全部发布则返回 null。
     *
     * 设计考量：
     * - 命名参数：统一采用命名参数，降低参数顺序误用风险并提升可读性。
     * - 安全转换：使用 `as?` 避免不必要的强转异常。
     * - 早退出：非流水线类型模板直接返回，减少分支嵌套。
     *
     * @param userId 操作人 ID（用于审计/日志，当前逻辑不参与判定）
     * @param projectId 项目 ID
     * @param templateId 模板 ID
     * @param version 模板版本号
     * @return Result<String?> 未发布镜像的 imageCode；若全部发布或无需校验则为 null
     */
    fun checkImageReleaseStatus(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<String?> {
        logger.info("start checkImageReleaseStatus templateCode is:$projectId|$templateId|$version")
        val template = pipelineTemplateResourceService.get(
            projectId = projectId,
            templateId = templateId,
            version = version
        )
        // TODO pac模板 二期，局部模板需要进行改造：当前仅在 PIPELINE 类型模板上检查镜像发布状态
        if (template.type != PipelineTemplateType.PIPELINE) return Result(data = null)

        val templateModel = template.model as Model
        var unreleasedCode: String? = null
        // 记录已检查过的镜像 key（imageCode@imageVersion），避免重复远程查询
        val visitedImages = mutableSetOf<String>()

        outer@ for (stage in templateModel.stages) {
            for (container in stage.containers) {
                // 仅处理构建机容器，并且分发方式为商店镜像（StoreDispatchType）
                val dispatch = (container as? VMBuildContainer)?.dispatchType as? StoreDispatchType ?: continue
                val imageCode = dispatch.imageCode
                val imageVersion = dispatch.imageVersion
                if (imageCode.isNullOrBlank() || imageVersion.isNullOrBlank()) continue

                val key = "$imageCode@$imageVersion"
                if (!visitedImages.add(key)) continue // 已检查，跳过

                // 发现未发布镜像则立即短路，返回首个未发布镜像的 code
                if (!isRelease(imageCode = imageCode, imageVersion = imageVersion)) {
                    unreleasedCode = imageCode
                    break@outer
                }
            }
        }
        return Result(data = unreleasedCode)
    }

    /**
     * 查询镜像发布状态。
     *
     * @param imageCode 镜像 code
     * @param imageVersion 镜像版本
     * @return Boolean 是否为【已发布】状态
     */
    private fun isRelease(imageCode: String, imageVersion: String): Boolean {
        return client.get(ServiceStoreImageResource::class)
            .isReleasedStatus(imageCode = imageCode, imageVersion = imageVersion).data!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateMarketFacadeService::class.java)
    }
}
