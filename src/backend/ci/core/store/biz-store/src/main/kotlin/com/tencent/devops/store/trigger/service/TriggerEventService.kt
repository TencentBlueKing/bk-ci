package com.tencent.devops.store.trigger.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
import com.tencent.devops.store.atom.service.AtomService
import com.tencent.devops.store.common.service.StoreComponentQueryService
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.common.BK_STORE_ALL_TRIGGER
import com.tencent.devops.store.pojo.common.BK_STORE_CLOUD_DESKTOP_TRIGGER
import com.tencent.devops.store.pojo.common.BK_STORE_COMMON_TRIGGER
import com.tencent.devops.store.pojo.common.KEY_ATOM_FORM
import com.tencent.devops.store.pojo.common.QueryComponentsParam
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.TriggerGroupInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerEventService @Autowired constructor(
    private val atomService: AtomService,
    private val storeComponentQueryService: StoreComponentQueryService,
) {
    fun previewEvent(userId:String, storeId: String) : AtomForm? {
        val detailInfo = storeComponentQueryService.getComponentDetailInfoById(
            userId = userId,
            storeId = storeId,
            storeType = StoreTypeEnum.TRIGGER_EVENT
        ) ?: throw InvalidParamException("storeId[$storeId] not found")
        val atomForm = detailInfo.extData?.get(KEY_ATOM_FORM)?.let {
            it as String
            if (it.isNotBlank()) {
                JsonUtil.to(it, AtomForm::class.java)
            } else {
                null
            }
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
            recommendFlag = false,
            serviceScope = null
        ).toInt()
        val componentCount = storeComponentQueryService.getComponentCount(
            userId = userId,
            queryComponentsParam = QueryComponentsParam(
                storeType = StoreTypeEnum.TRIGGER_EVENT.name,
                classifyCode = classifyId
            )
        )
        return listOf(
            TriggerGroupInfo(
                type = BK_STORE_ALL_TRIGGER,
                count = commonTriggerCount + componentCount
            ),
            TriggerGroupInfo(
                type = BK_STORE_COMMON_TRIGGER,
                count = commonTriggerCount
            ),
            TriggerGroupInfo(
                type = BK_STORE_CLOUD_DESKTOP_TRIGGER,
                count = componentCount
            )
        )
    }

    fun atoms(
        projectCode: String,
        userId: String,
        type: String?,
        classifyId: String?,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): AtomResp<AtomRespItem>? {
        if (type.isNullOrEmpty() || type == BK_STORE_COMMON_TRIGGER) {
            val commonTrigger = atomService.getPipelineAtoms(
                projectCode = projectCode,
                userId = userId,
                classifyId = classifyId,
                keyword = keyword,
                jobType = null,
                os = null,
                recommendFlag = null,
                serviceScope = null,
                page = page,
                pageSize = pageSize
            )
        } else {
            val commonTrigger = atomService.getPipelineAtoms(
                projectCode = projectCode,
                userId = userId,
                classifyId = classifyId,
                keyword = keyword,
                jobType = null,
                os = null,
                recommendFlag = null,
                serviceScope = null,
                page = page,
                pageSize = pageSize
            )
        }

        return null
    }

    companion object {
        const val ALL_TRIGGER = "all"
    }
}