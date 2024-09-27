package com.tencent.devops.remotedev.pojo.sdk

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "云桌面SDK上报数据")
data class SdkReportData(
    @get:Schema(title = "上报类型，SdkReportDataType")
    val reportType: Int,
    @get:Schema(title = "上报数据，base64之后的 json 串")
    val reportData: String
)

enum class SdkReportDataType(val value: Int) {
    CODECC_QUALITY_REPORT(1),
    CODECC_CODE_MONITOR(2);

    companion object {
        fun fromValue(value: Int): SdkReportDataType? {
            return values().firstOrNull { it.value == value }
        }
    }
}