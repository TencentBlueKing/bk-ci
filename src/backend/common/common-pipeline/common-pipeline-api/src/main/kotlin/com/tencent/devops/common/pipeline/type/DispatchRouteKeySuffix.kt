package com.tencent.devops.common.pipeline.type

enum class DispatchRouteKeySuffix(val routeKeySuffix: String) {
    PCG(".pcg.sumeru"),
    DEVCLOUD(".devcloud.public"),
    IDC(".idc.public"),
    GITCI(".gitci.public"),
    CODECC(".codecc.scan")
}