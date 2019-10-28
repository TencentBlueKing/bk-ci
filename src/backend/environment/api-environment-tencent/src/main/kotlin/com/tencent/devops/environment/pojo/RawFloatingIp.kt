package com.tencent.devops.environment.pojo

data class RawFloatingIp(
    var floatingNetworkId: String? = null,
    var fixedIp: String? = null,
    var floatingIp: String? = null,
    var tenantId: String? = null,
    var status: String? = null,
    var portId: String? = null,
    var id: String? = null
)