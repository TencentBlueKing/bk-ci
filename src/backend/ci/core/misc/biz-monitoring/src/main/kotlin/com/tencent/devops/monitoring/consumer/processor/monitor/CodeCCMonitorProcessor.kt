/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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

        resultMap.values.asSequence().map { buildMonitorData(it, extData) }.filterNotNull()
            .forEach { monitorDatas.add(it) }
    }

    private fun buildMonitorData(it: Any?, extData: Map<String, Any>): MonitorData? {
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
