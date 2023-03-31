package com.tencent.devops.dispatch.macos.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel

@ApiModel("母机信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class HostMachineInfo(var id: Int = 0) {
    var name: String? = ""
    var ip: String? = ""
    var userName: String? = ""
    var password: String? = ""
    var createTime: Long? = null
    var updateTime: Long? = null
}
