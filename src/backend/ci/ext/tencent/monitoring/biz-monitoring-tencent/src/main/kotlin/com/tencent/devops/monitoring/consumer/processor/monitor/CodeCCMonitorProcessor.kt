package com.tencent.devops.monitoring.consumer.processor.monitor

import org.springframework.stereotype.Component

@Component
class CodeCCMonitorProcessor : AbstractMonitorProcessor() {
    override fun atomCode(): String {
        return "CodeccCheckAtomDebug"
    }

    override fun measurement(): String {
        return "CodeccMonitor"
    }

    override fun process(extData: Map<String, Any>?, monitorData: MonitorData) {
        TODO("Not yet implemented")
    }

}