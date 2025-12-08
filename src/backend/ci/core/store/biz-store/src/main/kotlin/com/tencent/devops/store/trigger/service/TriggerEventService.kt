package com.tencent.devops.store.trigger.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.store.atom.service.AtomService
import com.tencent.devops.store.common.service.StoreComponentQueryService
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.common.BK_STORE_ALL_TRIGGER
import com.tencent.devops.store.pojo.common.BK_STORE_COMMON_TRIGGER
import com.tencent.devops.store.pojo.common.KEY_ATOM_FORM
import com.tencent.devops.store.pojo.common.QueryGroupParam
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.enums.StoreGroupByEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.TriggerGroupInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerEventService @Autowired constructor(
    private val atomService: AtomService,
    private val storeComponentQueryService: StoreComponentQueryService
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

    fun types(userId: String, classifyId: String?, projectCode: String): List<TriggerGroupInfo> {
        val commonTriggerCount = atomService.getPipelineAtomCount(
            userId = userId,
            category = AtomCategoryEnum.TRIGGER.name,
            classifyId = classifyId,
            projectCode = projectCode,
            jobType = null,
            keyword = null,
            os = null,
            recommendFlag = null,
            serviceScope = null,
            queryProjectAtomFlag = false
        ).toInt()
        // 按照归属应用分组
        val componentGroupCount = storeComponentQueryService.getComponentGroupCount(
            userId = userId,
            queryGroupParam = QueryGroupParam(
                storeType = StoreTypeEnum.TRIGGER_EVENT,
                groupBy = StoreGroupByEnum.OWNER_STORE_CODE
            )
        )
        // 触发事件总数
        val componentCount = componentGroupCount.count { it.second > 0 }
        val ownerAppCodes = componentGroupCount.map { it.first }.toSet()
        // 查询归属应用信息
        val ownerAppInfos = storeComponentQueryService.getComponentBaseInfoList(
            storeType = StoreTypeEnum.DEVX,
            storeCodes = ownerAppCodes
        ).associate { it.storeCode to it.storeName }
        val finalComponentGroup = componentGroupCount.map {
            TriggerGroupInfo(
                type = it.first,
                name = ownerAppInfos[it.first] ?: it.first,
                count = it.second
            )
        }
        return mutableListOf(
            TriggerGroupInfo(
                type = BK_STORE_ALL_TRIGGER,
                count = commonTriggerCount + componentCount
            )
        ).plus(finalComponentGroup)
    }

    fun commonTrigger(
        projectCode: String,
        userId: String,
        classifyId: String?,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): AtomResp<AtomRespItem>? {
        return atomService.getPipelineAtoms(
            projectCode = projectCode,
            userId = userId,
            classifyId = classifyId,
            keyword = keyword,
            jobType = null,
            os = null,
            recommendFlag = null,
            serviceScope = null,
            page = page,
            pageSize = pageSize,
            queryProjectAtomFlag = false,
            category = AtomCategoryEnum.TRIGGER.name
        ).data
    }

    fun baseTrigger(
        userId: String,
        projectCode: String,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): AtomResp<AtomRespItem>? {
        return storeComponentQueryService.queryComponents(
            userId = userId,
            storeInfoQuery = StoreInfoQuery(
                storeType = StoreTypeEnum.TRIGGER_EVENT.name,
                page = page,
                pageSize = pageSize,
                queryProjectComponentFlag = false
            )
        ).let {
            AtomResp(
                count = it.count,
                page = it.page,
                pageSize = it.pageSize,
                totalPages = it.totalPages,
                records = it.records.map { marketItem ->
                    AtomRespItem(
                        name = marketItem.name,
                        atomCode = marketItem.code,
                        version = marketItem.version,
                        defaultVersion = marketItem.version,
                        classType = MarketBuildLessAtomElement.classType,
                        serviceScope = listOf(SERVICE_SCOPE_PIPELINE),
                        os = marketItem.os ?: emptyList(),
                        logoUrl = marketItem.logoUrl,
                        icon = marketItem.logoUrl,
                        classifyCode = marketItem.classifyCode ?: "",
                        classifyName = marketItem.classifyCode ?: "",
                        category = marketItem.category ?: AtomCategoryEnum.TRIGGER.name,
                        summary = marketItem.summary,
                        docsLink = marketItem.docsLink,
                        atomType = marketItem.rdType ?: AtomTypeEnum.SELF_DEVELOPED.name,
                        atomStatus = marketItem.status,
                        description = marketItem.summary,
                        publisher = marketItem.publisher,
                        creator = marketItem.publisher,
                        modifier = marketItem.modifier,
                        createTime = marketItem.updateTime,
                        updateTime = marketItem.updateTime,
                        defaultFlag = false,
                        latestFlag = true,
                        htmlTemplateVersion = "1.1",
                        buildLessRunFlag = marketItem.buildLessRunFlag,
                        weight = marketItem.recentExecuteNum,
                        recommendFlag = marketItem.recommendFlag,
                        score = marketItem.score,
                        recentExecuteNum = marketItem.recentExecuteNum,
                        uninstallFlag = true,
                        labelList = null,
                        installFlag = marketItem.flag,
                        installed = marketItem.installed,
                        honorInfos = marketItem.honorInfos,
                        indexInfos = marketItem.indexInfos,
                        hotFlag = marketItem.hotFlag
                    )
                }
            )
        }
    }

    fun triggerDetail(
        userId: String,
        projectCode: String,
        atomCode: String,
        version: String,
        sourceCode: String
    ): PipelineAtom? {
        return when (sourceCode) {
            BK_STORE_COMMON_TRIGGER -> {
                atomService.getPipelineAtom(
                    projectCode = projectCode,
                    atomCode = atomCode,
                    version = version,
                    queryOfflineFlag = false
                ).data
            }

            else -> {
                storeComponentQueryService.getComponentDetailInfoByCode(
                    userId = userId,
                    storeType = StoreTypeEnum.TRIGGER_EVENT.name,
                    storeCode = atomCode,
                    ownerStoreCode = sourceCode,
                    version = version
                )?.let { storeDetailInfo ->
                    PipelineAtom(
                        id = storeDetailInfo.storeId,
                        name = storeDetailInfo.name,
                        atomCode = storeDetailInfo.storeCode,
                        version = storeDetailInfo.version,
                        classType = "marketBuild",
                        atomStatus = storeDetailInfo.status,
                        creator = userId,
                        createTime = 0,
                        updateTime = 0,
                        versionList = storeComponentQueryService.getComponentVersionList(
                            userId = userId,
                            storeType = StoreTypeEnum.TRIGGER_EVENT,
                            storeCode = atomCode,
                            storeStatus = StoreStatusEnum.RELEASED
                        ),
                        logoUrl = storeDetailInfo.logoUrl,
                        icon = storeDetailInfo.logoUrl,
                        summary = storeDetailInfo.summary,
                        serviceScope = listOf(SERVICE_SCOPE_PIPELINE),
                        jobType = "AGENT",
                        os = emptyList(),
                        classifyId = storeDetailInfo.classify?.id,
                        classifyCode = storeDetailInfo.classify?.classifyCode ?: "",
                        classifyName = storeDetailInfo.classify?.classifyName ?: "",
                        docsLink = null,
                        category = storeDetailInfo.categoryList?.firstOrNull()?.categoryCode ?: "TASK",
                        atomType = storeDetailInfo.rdType ?: "SELF_DEVELOPED",
                        description = storeDetailInfo.description,
                        atomLabelList = emptyList(),
                        defaultFlag = true,
                        latestFlag = storeDetailInfo.latestFlag,
                        htmlTemplateVersion = "1.1",
                        buildLessRunFlag = false,
                        weight = 0,
                        props = storeDetailInfo.extData?.get(KEY_ATOM_FORM) as Map<String, Any>? ?: mapOf(),
                        data = emptyMap(),
                        recommendFlag = storeDetailInfo.recommendFlag,
                        frontendType = FrontendTypeEnum.NORMAL,
                        visibilityLevel = "PRIVATE"
                    )
                }
            }
        }
    }

    companion object {
        const val SERVICE_SCOPE_PIPELINE = "PIPELINE"
    }
}