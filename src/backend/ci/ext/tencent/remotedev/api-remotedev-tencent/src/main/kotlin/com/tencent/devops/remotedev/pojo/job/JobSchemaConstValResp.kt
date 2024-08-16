package com.tencent.devops.remotedev.pojo.job

data class JobSchemaConstValResp(
    val jobType: Set<JobType>,
    val jobActionType: Set<JobActionType>
)
