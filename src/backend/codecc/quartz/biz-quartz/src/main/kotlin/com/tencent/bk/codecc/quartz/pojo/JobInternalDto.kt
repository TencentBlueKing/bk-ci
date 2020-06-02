package com.tencent.bk.codecc.quartz.pojo

import com.tencent.bk.codecc.quartz.model.JobInstanceEntity

data class JobInternalDto(
    val operType: OperationType,
    val jobInstance: JobInstanceEntity
)