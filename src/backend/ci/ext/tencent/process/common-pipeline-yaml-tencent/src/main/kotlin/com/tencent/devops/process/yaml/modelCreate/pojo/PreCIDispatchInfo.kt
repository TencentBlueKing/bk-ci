package com.tencent.devops.process.yaml.modelCreate.pojo

import com.tencent.devops.common.pipeline.matrix.DispatchInfo
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.job.Job

data class PreCIDispatchInfo(
    override val name: String,
    val job: Job,
    val projectCode: String,
    var defaultImage: String,
    val resources: Resources? = null
) : DispatchInfo(name)
