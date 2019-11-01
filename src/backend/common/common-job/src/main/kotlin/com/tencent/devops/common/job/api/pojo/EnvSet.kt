package com.tencent.devops.common.job.api.pojo

import org.springframework.stereotype.Component

@Component
data class EnvSet(
    val envHashIds: List<String>,
    val nodeHashIds: List<String>,
    val ipLists: List<IpDto>
) {
    data class IpDto(
        val ip: String,
        val source: Int = 1
    )
}