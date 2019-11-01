package com.tencent.devops.gitci.pojo.yaml

import com.tencent.devops.gitci.pojo.task.AbstractTask

data class JobDetail(
    val name: String?,
    val pool: Pool?,
    val steps: List<AbstractTask>,
    val condition: String?
)