package com.tencent.devops.stream.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class StreamSlaConfig {
    @Value("\${sla.switch:false}")
    val switch: String = "false"
    @Value("\${sla.oteam.url:#{null}}")
    var oteamUrl: String? = null
    @Value("\${sla.oteam.token:#{null}}")
    var oteamToken: String? = null
    @Value("\${sla.oteam.techmap:#{null}}")
    var oteamTechmap: String? = null
    @Value("\${sla.oteam.target.activeProject:#{null}}")
    var oteamActiveProjectTarget: Int? = null
}
