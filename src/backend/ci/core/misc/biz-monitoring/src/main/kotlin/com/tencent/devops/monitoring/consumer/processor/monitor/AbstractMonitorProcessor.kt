/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.pojo.AtomMonitorData
import com.tencent.devops.monitoring.client.InfluxdbClient
import org.slf4j.LoggerFactory

/**
 * 监控数据插入influxdb基类
 */
abstract class AbstractMonitorProcessor {
    // 需要处理的atomCode
    abstract fun atomCode(): String

    // 写入的表名
    protected abstract fun measurement(): String

    /**
     * 处理数据
     */
    protected abstract fun process(extData: Map<String, Any>, monitorDatas: ArrayList<MonitorData>)

    fun process(influxdbClient: InfluxdbClient, atomMonitorData: AtomMonitorData) {

        val extData = atomMonitorData.extData

        logger.info("extData : $extData")

        if (null == extData || extData.isEmpty()) {
            return
        }

        if (atomMonitorData.atomCode != this.atomCode()) {
            return
        }

        val monitorDatas = ArrayList<MonitorData>()
        process(extData, monitorDatas)

        // 写入
        monitorDatas.asSequence().onEach {
            it.fields["projectId"] = atomMonitorData.projectId
            it.fields["pipelineId"] = atomMonitorData.pipelineId
            it.fields["buildId"] = atomMonitorData.buildId
            it.fields["vmSeqId"] = atomMonitorData.vmSeqId
            it.fields["channel"] = atomMonitorData.channel ?: ""
            it.fields["starter"] = atomMonitorData.starter
        }.forEach { influxdbClient.insert(this.measurement(), it.tags, it.fields) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractMonitorProcessor::class.java)
    }
}
