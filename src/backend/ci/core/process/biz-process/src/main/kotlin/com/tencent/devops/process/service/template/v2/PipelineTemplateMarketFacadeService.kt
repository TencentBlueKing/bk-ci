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
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.template.TemplatePublishedVersionInfo
import com.tencent.devops.store.pojo.template.TemplateVersionInstallHistoryInfo
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

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
        // TODO pac模板 二期，局部模板需要进行改造
        if (template.type != PipelineTemplateType.PIPELINE)
            return Result(null)
        val templateModel = template.model as Model
        var code: String? = null
        val images = mutableSetOf<String>()
        run releaseStatus@{
            templateModel.stages.forEach { stage ->
                stage.containers.forEach imageInfo@{ container ->
                    if (container is VMBuildContainer && container.dispatchType is StoreDispatchType) {
                        val imageCode = (container.dispatchType as StoreDispatchType).imageCode
                        val imageVersion = (container.dispatchType as StoreDispatchType).imageVersion
                        val image = imageCode + imageVersion
                        if (imageCode.isNullOrBlank() || imageVersion.isNullOrBlank()) {
                            return@imageInfo
                        } else {
                            if (images.contains(image)) {
                                return@imageInfo
                            } else {
                                images.add(image)
                            }
                            if (!isRelease(imageCode, imageVersion)) {
                                code = imageCode
                            }
                            return@releaseStatus
                        }
                    } else {
                        return@imageInfo
                    }
                }
            }
        }
        return Result(code)
    }

    private fun isRelease(imageCode: String, imageVersion: String): Boolean {
        val imageStatus = client.get(ServiceStoreImageResource::class)
            .getImageStatusByCodeAndVersion(imageCode, imageVersion).data
        return ImageStatusEnum.RELEASED.name == imageStatus
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateMarketFacadeService::class.java)
        private val updateMarketTemplateExecutorService = Executors.newFixedThreadPool(3)
    }
}
