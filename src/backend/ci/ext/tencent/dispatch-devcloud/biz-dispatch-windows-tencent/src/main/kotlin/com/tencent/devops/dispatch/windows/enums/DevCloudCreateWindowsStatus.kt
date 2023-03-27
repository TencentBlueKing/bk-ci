package com.tencent.devops.dispatch.windows.enums

enum class DevCloudCreateWindowsStatus(val title: String) {
    Failed("failed"),
    Succeeded("succeeded"),
    Waiting("waiting"),
    Canceled("canceled"),
    Running("running");
}
