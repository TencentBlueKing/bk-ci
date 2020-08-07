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

        resultMap.values.asSequence().map {
            buildMonitorData(it, extData)
        }.filter { it.isNotEmpty() }.forEach { monitorDatas.add(it) }
    }

    private fun buildMonitorData(it: Any?, extData: Map<String, Any>): MonitorData {
        logger.info("run data : $it")
        if (it is Map<*, *>) {
            val startTime = (it["startTime"]?.toString() ?: "0").toLong()
            val endTime = (it["endTime"]?.toString() ?: "0").toLong()
            val elapseTime = endTime - startTime

            val monitorData = MonitorData()
            monitorData["bgId"] = extData["BK_CI_CODECC_TASK_BG_ID"]?.toString() ?: "0"
            monitorData["centerId"] = extData["BK_CI_CODECC_TASK_CENTER_ID"]?.toString() ?: "0"
            monitorData["deptId"] = extData["BK_CI_CODECC_TASK_DEPT_ID"]?.toString() ?: "0"
            monitorData["toolName"] = it["toolName"]?.toString() ?: "Unknown"
            monitorData["startTime"] = startTime.toString()
            monitorData["endTime"] = endTime.toString()
            monitorData["elapseTime"] = elapseTime.toString()
            monitorData["status"] = it["status"]?.toString() ?: "Unknown"
            monitorData["errorCode"] = it["errorCode"]?.toString() ?: "0"
            monitorData["errorMsg"] = it["errorMsg"]?.toString() ?: ""
            return monitorData
        }
        return MonitorData()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeCCMonitorProcessor::class.java)
    }
}