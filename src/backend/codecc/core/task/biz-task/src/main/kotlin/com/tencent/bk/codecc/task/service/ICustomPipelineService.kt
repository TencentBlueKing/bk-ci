package com.tencent.bk.codecc.task.service

import com.tencent.bk.codecc.task.model.CustomProjEntity
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq

interface ICustomPipelineService {

    /**
     * 处理个性化触发项目
     */
    fun handleWithCheckProjPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        userId: String
    ): CustomProjEntity

    /**
     * 新建个性化触发流水线
     */
    fun createCustomizedCheckProjPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        taskId: Long,
        userId: String?,
        projectId: String
    ): String

    /**
     * 新建个性化触发项目
     */
    fun createCustomDevopsProject(customProjEntity: CustomProjEntity, userId: String): String

    /**
     * 获取流水线启动参数
     */
    fun getParamMap(customProjEntity: CustomProjEntity): MutableMap<String, String>

    /**
     * 获取对应个性化业务类
     */
    fun getCustomProjEntity(triggerPipelineReq: TriggerPipelineOldReq): CustomProjEntity?

    /**
     * 更新个性化触发流水线
     */
    fun updateCustomizedCheckProjPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        taskId: Long,
        userId: String?,
        projectId: String,
        pipelineId: String
    )
}