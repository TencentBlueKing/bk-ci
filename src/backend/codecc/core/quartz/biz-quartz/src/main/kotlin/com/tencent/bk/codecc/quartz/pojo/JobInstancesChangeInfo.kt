package com.tencent.bk.codecc.quartz.pojo

import com.tencent.bk.codecc.quartz.model.JobInstanceEntity

data class JobInstancesChangeInfo(
    val addJobInstances: List<JobInstanceEntity>,
    val removeJobInstances: List<JobInstanceEntity>
)