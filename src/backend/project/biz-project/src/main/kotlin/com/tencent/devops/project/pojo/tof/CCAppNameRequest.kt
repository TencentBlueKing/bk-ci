package com.tencent.devops.project.pojo.tof


data class CCAppNameRequest(
    val app_code: String,
    val app_secret: String,
    val app_std_key_values: CCAppNameApplicationID,
    val method: String = "getAppList",
    val app_std_req_column: List<String> = listOf("DisplayName")
)

data class CCAppNameApplicationID(
    val ApplicationID: Long
)