package com.tencent.bk.codecc.task.service

import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldRsp

interface GongfengTriggerOldService {

    /**
     * 原有触发个性化流水线接口
     */
    fun triggerCustomProjectPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        userId: String
    ): TriggerPipelineOldRsp
}