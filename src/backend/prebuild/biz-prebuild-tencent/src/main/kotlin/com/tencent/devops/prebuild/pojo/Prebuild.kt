package com.tencent.devops.prebuild.pojo

/**
 * model
 */
data class Prebuild(
    val trigger: List<String>?,
    val pool: Pool?,
    val steps: List<AbstractTask>
)