package com.tencent.devops.common.pipeline.type

/**
 * deng
 * 2019-04-01
 */
enum class DispatchRouteKeySuffix(val routeKeySuffix: String) {
    PCG(".pcg.sumeru"),
    DEVCLOUD(".devcloud.public"),
    IDC(".idc.public")
}