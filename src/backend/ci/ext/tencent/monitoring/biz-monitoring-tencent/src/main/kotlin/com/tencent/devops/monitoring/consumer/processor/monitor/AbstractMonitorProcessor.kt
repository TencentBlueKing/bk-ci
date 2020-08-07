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
    protected abstract fun process(extData: Map<String, Any>, monitorData: ArrayList<MonitorData>)

    fun process(influxdbClient: InfluxdbClient, atomMonitorData: AtomMonitorData) {

        val extData = atomMonitorData.extData

        logger.info("extData : $extData")

        if (null == extData || extData.isEmpty()) {
            return
        }

        if (atomMonitorData.atomCode == this.atomCode()) {
            return
        }

        val monitorDatas = ArrayList<MonitorData>()
        process(extData, monitorDatas)

        monitorDatas.asSequence().onEach {
            it["projectId"] = atomMonitorData.projectId
            it["pipelineId"] = atomMonitorData.pipelineId
            it["buildId"] = atomMonitorData.buildId
            it["vmSeqId"] = atomMonitorData.vmSeqId
            it["channel"] = atomMonitorData.channel ?: ""
            it["starter"] = atomMonitorData.starter
        }.forEach { influxdbClient.insert(this.measurement(), emptyMap(), it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractMonitorProcessor::class.java)
    }
}