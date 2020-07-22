package com.tencent.bk.codecc.task.service

import com.tencent.devops.common.api.CommonPageVO
import com.tencent.devops.common.api.pojo.Page


interface KafkaSyncService {

    /**
     * 同步任务信息到kafka
     */
    fun syncTaskInfoToKafka(startTrigger : String)

    /**
     * 同步任务信息到kafka
     */
    fun syncTaskInfoToKafkaByType(dataType : String, washTime: String): Boolean

    fun getTaskInfoByCreateFrom(taskType: String, reqVO: CommonPageVO): Page<Long>

    /**
     * 手动执行流水线清单
     */
    fun manualExecuteTriggerPipeline(taskIdList : List<Long>)
}