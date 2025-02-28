package com.tencent.devops.dispatch.listener

import com.tencent.devops.dispatch.pojo.TPAMonitorEvent
import com.tencent.devops.dispatch.service.ThirdPartyAgentMonitorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TPAMonitorListener @Autowired constructor(
    private val monitorService: ThirdPartyAgentMonitorService
) {
    fun listenTPAMonitorEvent(event: TPAMonitorEvent) {
        monitorService.monitorQueue(event)
    }
}