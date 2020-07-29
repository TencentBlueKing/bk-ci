package com.tencent.bk.codecc.task.service

import com.tencent.bk.codecc.task.pojo.CustomTriggerPipelineModel
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp

interface GongfengTriggerService {

    /**
     * 触发个性化项目流水线
     */
    fun triggerCustomProjectPipeline(triggerPipelineReq: TriggerPipelineReq, appCode : String, userId: String) : TriggerPipelineRsp

    /**
     * 手动触发个性化流水线
     */
    fun manualStartupCustomPipeline(customTriggerPipelineModel: CustomTriggerPipelineModel)
}