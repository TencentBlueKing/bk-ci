package com.tencent.devops.plugin.codecc.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CodeccConfig {

    /**
     * 代码检查网关地址
     */
    @Value("\${codeccGateway.gateway:}")
    val codeccApiGateWay: String = ""

    @Value("\${codeccGateway.api.createTask:/ms/task/api/service/task}")
    val createPath = "/ms/task/api/service/task"

    @Value("\${codeccGateway.api.updateTask:/ms/task/api/service/task}")
    val updatePath = "/ms/task/api/service/task"

    @Value("\${codeccGateway.api.checkTaskExists:/ms/task/api/service/task/exists}")
    val existPath = "/ms/task/api/service/task/exists"

    @Value("\${codeccGateway.api.deleteTask:/ms/task/api/service/task}")
    val deletePath = "/ms/task/api/service/task"

    @Value("\${codeccGateway.api.codeCheckReport:/api}")
    val report = ""

    @Value("\${codeccGateway.api.getRuleSets:/blueShield/getRuleSetsPath")
    val getRuleSetsPath = ""
}