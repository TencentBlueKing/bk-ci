package com.tencent.devops.common.ci.yaml

import com.tencent.devops.common.ci.task.AbstractTask

data class JobDetail(
    val name: String?,
    val type: String?,
    val pool: Pool?,
    val steps: List<AbstractTask>,
    val condition: String?
)