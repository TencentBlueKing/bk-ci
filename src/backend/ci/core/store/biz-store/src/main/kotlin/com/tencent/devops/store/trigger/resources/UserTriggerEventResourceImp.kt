package com.tencent.devops.store.trigger.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
import com.tencent.devops.store.api.trigger.UserTriggerResource
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.trigger.TriggerGroupInfo
import com.tencent.devops.store.trigger.service.TriggerEventService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserTriggerEventResourceImp @Autowired constructor(
    private val triggerEventService: TriggerEventService
) : UserTriggerResource {
    override fun preview(userId: String, storeId: String): Result<AtomForm?> {
        return Result(triggerEventService.previewEvent(userId, storeId))
    }

    override fun types(userId: String): Result<List<TriggerGroupInfo>> {
        return Result(triggerEventService.types(userId))
    }

    override fun baseTrigger(
        userId: String,
        keyword: String?,
        ownerStoreCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<AtomRespItem>?> {
        return Result(
            triggerEventService.baseTrigger(
                userId = userId,
                keyword = keyword,
                ownerStoreCode = ownerStoreCode,
                page = page ?: 1,
                pageSize = pageSize ?: 20
            )
        )
    }

    override fun triggerDetail(
        userId: String,
        sourceCode: String,
        atomCode: String,
        version: String
    ): Result<PipelineAtom?> {
        return Result(
            triggerEventService.triggerDetail(
                atomCode = atomCode,
                version = version,
                userId = userId,
                sourceCode = sourceCode
            )
        )
    }
}