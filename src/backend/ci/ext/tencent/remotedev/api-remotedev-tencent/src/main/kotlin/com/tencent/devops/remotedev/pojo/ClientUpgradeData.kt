package com.tencent.devops.remotedev.pojo

data class ClientUpgradeData(
    val macAddress: String
)

data class ClientUpgradeResp(
    val clientUp: Boolean,
    val startUp: Boolean
) {
    companion object {
        fun noUpgrade(): ClientUpgradeResp {
            return ClientUpgradeResp(false, false)
        }
    }
}