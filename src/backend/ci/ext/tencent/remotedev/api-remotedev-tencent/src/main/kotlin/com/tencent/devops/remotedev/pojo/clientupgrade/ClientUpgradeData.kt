package com.tencent.devops.remotedev.pojo.clientupgrade

data class ClientUpgradeData(
    val macAddress: String
)

data class ClientUpgradeResp(
    val clientVersion: String?,
    val startVersion: String?
) {
    companion object {
        fun noUpgrade(): ClientUpgradeResp {
            return ClientUpgradeResp(null, null)
        }
    }
}

enum class ClientUpgradeType {
    CLIENT,
    START
}

enum class ClientUpgradeOpType {
    ADD,
    REMOVE,
    REWRITE
}