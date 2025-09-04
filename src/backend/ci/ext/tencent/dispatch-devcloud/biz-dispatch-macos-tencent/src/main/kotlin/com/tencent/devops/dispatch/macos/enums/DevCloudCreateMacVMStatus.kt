package com.tencent.devops.dispatch.macos.enums

enum class DevCloudCreateMacVMStatus(val title: String) {
    failed("failed"),
    succeeded("succeeded"),
    waiting("waiting"),
    canceled("canceled"),
    running("running");
}
