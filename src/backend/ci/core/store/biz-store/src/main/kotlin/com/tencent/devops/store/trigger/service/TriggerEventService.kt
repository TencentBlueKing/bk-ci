package com.tencent.devops.store.trigger.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.atom.dao.AtomQueryParam
import com.tencent.devops.store.atom.service.AtomService
import com.tencent.devops.store.common.service.StoreComponentQueryService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomGroupQueryParam
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.AtomUpgradeRequest
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.BK_STORE_ALL_TRIGGER
import com.tencent.devops.store.pojo.common.KEY_ATOM_FORM
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.enums.StoreGroupByEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.TriggerGroupInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerEventService @Autowired constructor(
    private val storeComponentQueryService: StoreComponentQueryService,
    private val atomService: AtomService,
    private val redisOperation: RedisOperation
) {
    fun previewEvent(userId: String, storeId: String): AtomForm? {
        val detailInfo = storeComponentQueryService.getComponentDetailInfoById(
            userId = userId,
            storeId = storeId,
            storeType = StoreTypeEnum.TRIGGER_EVENT
        ) ?: throw InvalidParamException("storeId[$storeId] not found")
        val atomForm = detailInfo.extData?.get(KEY_ATOM_FORM)?.let {
            JsonUtil.anyTo(it, object : TypeReference<AtomForm>() {})
        }
        return atomForm
    }

    fun listOwnerStoreCodes(userId: String): List<TriggerGroupInfo> {
        // 按照归属应用分组
        val componentGroupCount = atomService.getAtomGroupCount(
            userId = userId,
            atomGroupQueryParam = AtomGroupQueryParam(
                groupBy = StoreGroupByEnum.OWNER_STORE_CODE,
                category = AtomCategoryEnum.TRIGGER,
                serviceScope = ServiceScopeEnum.CREATIVE_STREAM
            )
        )
        // 触发事件总数
        var componentCount = 0
        componentGroupCount.forEach {
            componentCount += it.second
        }
        val ownerAppCodes = componentGroupCount.map { it.first }.toSet()
        // 查询归属应用信息
        val ownerAppInfos = storeComponentQueryService.getComponentBaseInfoList(
            storeType = StoreTypeEnum.DEVX,
            storeCodes = ownerAppCodes
        ).associate { it.storeCode to it.storeName }
        val finalComponentGroup = componentGroupCount.map {
            TriggerGroupInfo(
                ownerStoreCode = it.first,
                name = ownerAppInfos[it.first] ?: I18nUtil.getCodeLanMessage(
                    messageCode = it.first
                ).ifBlank { it.first },
                count = it.second
            )
        }
        return mutableListOf(
            TriggerGroupInfo(
                ownerStoreCode = BK_STORE_ALL_TRIGGER,
                count = componentCount
            )
        ).plus(finalComponentGroup)
    }

    fun list(
        userId: String,
        keyword: String?,
        ownerStoreCode: String?,
        page: Int,
        pageSize: Int
    ): AtomResp<AtomRespItem>? {
        return atomService.getPipelineAtoms(
            userId = userId,
            queryParam = AtomQueryParam(
                classifyId = TRIGGER_CLASSIFY_ID,
                keyword = keyword,
                queryProjectAtomFlag = false,
                projectCode = null,
                category = AtomCategoryEnum.TRIGGER.name,
                serviceScope = ServiceScopeEnum.CREATIVE_STREAM,
                fitOsFlag = null,
                jobType = null,
                os = null,
                queryFitAgentBuildLessAtomFlag = null,
                recommendFlag = null,
                ownerStoreCode = ownerStoreCode
            ),
            page = page,
            pageSize = pageSize
        ).data
    }

    fun triggerDetail(
        userId: String,
        projectId: String,
        atomCode: String,
        version: String,
        ownerStoreCode: String?
    ): PipelineAtom? {
        return atomService.getPipelineAtom(
            projectCode = projectId,
            atomCode = atomCode,
            version = version,
            queryOfflineFlag = false,
            serviceScope = null
        ).data
    }

    /**
     * 将触发事件组件转化成
     */
    @SuppressWarnings("NestedBlockDepth")
    fun transferAtom(
        userId: String,
        storeCode: String?
    ) {
        // 查询触发事件组件
        val storeCodes = if (storeCode.isNullOrBlank()) {
            storeComponentQueryService.getComponentBaseInfoList(
                storeType = StoreTypeEnum.TRIGGER_EVENT,
                storeCodes = null
            ).map { it.storeCode }
        } else {
            listOf(storeCode)
        }
        storeCodes.forEach {
            // 查询组件下的所有版本
            val components = storeComponentQueryService.getComponentBaseInfoList(
                storeType = StoreTypeEnum.TRIGGER_EVENT.name,
                storeCode = it,
                storeStatusList = listOf(StoreStatusEnum.RELEASED),
                page = 1,
                pageSize = 100
            )
            // 遍历所有版本，将ATOM_FORM信息保存到T_ATOM
            components.forEach versionForeach@{ component ->
                try {
                    val detailInfo = storeComponentQueryService.getComponentDataInfoByCode(
                        storeType = component.storeType.name,
                        storeCode = component.storeCode,
                        version = component.version
                    )
                    if (detailInfo == null) {
                        logger.info("storeCode[${component.storeCode}|${component.version}] not found")
                        return@versionForeach
                    }
                    val atomForm = detailInfo.extData?.get(KEY_ATOM_FORM)?.let {
                        JsonUtil.toJson(it, false)
                    }
                    if (atomForm.isNullOrBlank()) {
                        logger.info("storeCode[${component.storeCode}|${component.version}] not found atomForm")
                        return@versionForeach
                    }
                    // 如果存在则升级，否则创建
                    val upgradeAtom = atomService.exists(component.storeCode).data ?: false
                    val result = if (upgradeAtom) {
                        atomService.upgradeAtom(
                            userId = userId,
                            atomRequest = AtomUpgradeRequest(
                                id = component.storeId,
                                name = component.storeName,
                                atomCode = component.storeCode,
                                serviceScope = arrayListOf(ServiceScopeEnum.CREATIVE_STREAM.name),
                                jobType = JobTypeEnum.CREATIVE_STREAM,
                                os = arrayListOf(),
                                classifyId = TRIGGER_CLASSIFY_ID,
                                docsLink = null,
                                atomType = AtomTypeEnum.SELF_DEVELOPED,
                                defaultFlag = true,
                                category = AtomCategoryEnum.TRIGGER,
                                buildLessRunFlag = false,
                                props = atomForm,
                                weight = null,
                                data = null,
                                version = component.version,
                                ownerStoreCode = component.ownerStoreCode,
                                logoUrl = component.logoUrl
                            )
                        )
                    } else {
                        atomService.savePipelineAtom(
                            userId = userId,
                            atomRequest = AtomCreateRequest(
                                id = component.storeId,
                                name = component.storeName,
                                atomCode = component.storeCode,
                                serviceScope = arrayListOf(ServiceScopeEnum.CREATIVE_STREAM.name),
                                jobType = JobTypeEnum.CREATIVE_STREAM,
                                os = arrayListOf(),
                                classifyId = TRIGGER_CLASSIFY_ID,
                                docsLink = null,
                                atomType = AtomTypeEnum.SELF_DEVELOPED,
                                defaultFlag = true,
                                category = AtomCategoryEnum.TRIGGER,
                                buildLessRunFlag = false,
                                props = atomForm,
                                weight = null,
                                data = null,
                                ownerStoreCode = component.ownerStoreCode,
                                logoUrl = component.logoUrl
                            )
                        )
                    }
                    logger.info("transfer atom[${component.storeCode}|${component.version}] result: $result")
                } catch (ignored: Exception) {
                    logger.warn("fail to transfer atom[${component.storeCode}|${component.version}]", ignored)
                }
            }
        }
        // 触发器资源设置为默认插件后，需要删除公共插件标记，后续会自动重建
        redisOperation.delete(StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name))
    }

    companion object {
        const val TRIGGER_CLASSIFY_ID = "e1bea5430f574f9ea3e0312dc7de9efa"
        private val logger = LoggerFactory.getLogger(TriggerEventService::class.java)
    }
}
