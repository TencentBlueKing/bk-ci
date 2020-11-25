package com.tencent.devops.monitoring.consumer.processor.monitor

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CodeCCMonitorProcessor : AbstractMonitorProcessor() {
    override fun atomCode(): String {
        return "CodeccCheckAtomDebug"
    }

    override fun measurement(): String {
        return "CodeccMonitor"
    }

    override fun process(extData: Map<String, Any>, monitorDatas: ArrayList<MonitorData>) {
        val resultMap = extData["BK_CI_CODEC_TOOL_RUN_RESULT"]
        if (resultMap !is Map<*, *>) {
            logger.warn("Can`t get BK_CI_CODEC_TOOL_RUN_RESULT !!!")
            return
        }

        resultMap.values.asSequence().map { buildMonitorData(it, extData) }.filterNotNull().forEach { monitorDatas.add(it) }
    }

    private fun buildMonitorData(it: Any?, extData: Map<String, Any>): MonitorData? {
        logger.info("run data : $it")
        if (it is Map<*, *>) {
            val startTime = (it["startTime"]?.toString() ?: "0").toLong()
            val endTime = (it["endTime"]?.toString() ?: "0").toLong()
            val elapseTime = endTime - startTime

            val monitorData = MonitorData()

            monitorData.tags["bgId"] = extData["BK_CI_CODECC_TASK_BG_ID"]?.toString() ?: "0"
            monitorData.tags["toolName"] = it["toolName"]?.toString() ?: "Unknown"
            monitorData.tags["errorCode"] = it["errorCode"]?.toString() ?: "0"

            monitorData.fields["centerId"] = extData["BK_CI_CODECC_TASK_CENTER_ID"]?.toString() ?: "0"
            monitorData.fields["deptId"] = extData["BK_CI_CODECC_TASK_DEPT_ID"]?.toString() ?: "0"
            monitorData.fields["startTime"] = startTime
            monitorData.fields["endTime"] = endTime
            monitorData.fields["elapseTime"] = elapseTime
            monitorData.fields["status"] = it["status"]?.toString() ?: "Unknown"
            monitorData.fields["errorMsg"] = it["errorMsg"]?.toString() ?: ""
            return monitorData
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeCCMonitorProcessor::class.java)
    }
}