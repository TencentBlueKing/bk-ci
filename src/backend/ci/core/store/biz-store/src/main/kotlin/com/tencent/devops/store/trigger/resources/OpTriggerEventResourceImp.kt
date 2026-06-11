package com.tencent.devops.store.trigger.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.util.ThreadPoolUtil
import com.tencent.devops.store.api.trigger.OpTriggerEventResource
import com.tencent.devops.store.trigger.service.TriggerEventService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OpTriggerEventResourceImp @Autowired constructor(
    private val triggerEventService: TriggerEventService
) : OpTriggerEventResource {

    override fun transfer(userId: String, storeCode: String?): Result<Boolean?> {
        ThreadPoolUtil.submitAction(
            actionTitle = "transfer trigger event component",
            action = {
                triggerEventService.transferAtom(
                    userId = userId,
                    storeCode = storeCode
                )
            }
        )
        return Result(true)
    }
}
