package com.tencent.bk.codecc.task.service

import com.tencent.bk.codecc.task.pojo.CustomTriggerPipelineModel
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp

interface GongfengTriggerService {

    /**
     * 触发个性化项目流水线
     */
    fun triggerCustomProjectPipeline(
        triggerPipelineReq: TriggerPipelineReq,
        appCode: String,
        userId: String
    ): TriggerPipelineRsp

    /**
     * 手动触发个性化流水线
     */
    fun manualStartupCustomPipeline(customTriggerPipelineModel: CustomTriggerPipelineModel)

    /**
     * 触发工蜂扫描任务
     * 通过代码库唯一标示找到扫描任务，拉取指定CommitId的代码进行扫描
     * 当comitId为空，拉取最新代码库扫描
     */
    fun triggerGongfengTaskByRepoId(repoId: String, commitId: String?): String?

    /**
     * 根据工蜂ID创建扫描任务
     */
    fun createTaskByRepoId(repoId: String, langs: List<String>): Boolean

    /**
     * 停止对应流水线
     */
    fun stopRunningApiTask(
        codeccBuildId: String,
        appCode: String,
        userId: String
    )
}
