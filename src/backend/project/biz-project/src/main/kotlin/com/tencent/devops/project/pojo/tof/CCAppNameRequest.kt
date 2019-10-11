package com.tencent.devops.project.pojo.tof

/**
 * deng
 * 2019-01-14
 *
 * {
 *   "app_code":"bkci",
 *   "app_secret":"XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<",
 *   "method": "getAppList",
 *   "app_std_req_column": ["DisplayName"],
 *   "app_std_key_values": {
 *     "ApplicationID": 100205
 *   }
 * }
 */
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