package com.tencent.devops.plugin.pojo.stke

enum class StkeType(val type: String) {
    DEPLOYMENT("deployments"),
    STATEFUL_SET("statefulsets"),
    STATEFUL_SET_PLUS("statefulsetpluses")
}