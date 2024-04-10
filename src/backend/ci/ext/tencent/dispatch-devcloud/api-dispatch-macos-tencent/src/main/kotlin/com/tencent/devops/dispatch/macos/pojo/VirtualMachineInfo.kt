package com.tencent.devops.dispatch.macos.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "虚拟机信息")
@JsonIgnoreProperties(ignoreUnknown = true)
class VirtualMachineInfo(var id: Int = 0) {
    var name: String? = ""
    var ip: String? = ""
    var userName: String? = ""
    var password: String? = ""
    @get:Schema(title = "母机ip", required = true)
    var motherMachineIp: String? = ""
    var status: String? = ""
    var vmTypeId: Int? = 0
    var vmTypeName: String? = ""
    var createTime: Long? = null
    var updateTime: Long? = null
}
