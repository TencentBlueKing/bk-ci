package com.tencent.devops.store.trigger.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
import com.tencent.devops.store.api.trigger.UserTriggerResource
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
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

    override fun types(userId: String, classifyId: String?, projectCode: String): Result<List<TriggerGroupInfo>> {
        return Result(triggerEventService.types(userId, classifyId, projectCode))
    }

    override fun atoms(
        projectCode: String,
        userId: String,
        type: String?,
        classifyId: String?,
        keyword: String?,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<AtomRespItem>?> {
        return Result(
            triggerEventService.atoms(
                projectCode = projectCode,
                userId = userId,
                type = type,
                classifyId = classifyId,
                keyword = keyword,
                page = page ?: 1,
                pageSize = pageSize ?: 20
            )
        )
    }
}