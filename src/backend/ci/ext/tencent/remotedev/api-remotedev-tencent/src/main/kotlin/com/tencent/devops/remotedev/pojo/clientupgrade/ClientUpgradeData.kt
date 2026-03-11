package com.tencent.devops.remotedev.pojo.clientupgrade

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "客户查询升级的信息")
data class ClientUpgradeData(
    @get:Schema(title = "MAC地址")
    val macAddress: String,
    @get:Schema(title = "是否强制升级，即不管是否在后台的可升级批次中")
    val forceUpdate: Boolean? = false
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

enum class ClientUpgradeOpType {
    ADD,
    REMOVE,
    REWRITE
}