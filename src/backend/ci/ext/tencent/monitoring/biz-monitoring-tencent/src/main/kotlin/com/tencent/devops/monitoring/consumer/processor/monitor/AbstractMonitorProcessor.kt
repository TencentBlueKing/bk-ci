package com.tencent.devops.monitoring.consumer.processor.monitor

import com.tencent.devops.monitoring.client.InfluxdbClient

/**
 * 监控数据插入influxdb基类
 */
abstract class AbstractMonitorProcessor {
    //需要处理的atomCode
    abstract fun atomCode(): String

    //写入的表名
    protected abstract fun measurement(): String

    /**
     * 处理数据
     */
    protected abstract fun process(extData: Map<String, Any>?, monitorData: MonitorData);

    fun process(influxdbClient: InfluxdbClient, atomCode: String, extData: Map<String, Any>?) {
        if (null == extData || extData.isEmpty()) {
            return
        }

        if (atomCode == this.atomCode()) {
            return
        }

        val monitorData = MonitorData()
        process(extData, monitorData)
        influxdbClient.insert(this.measurement(), emptyMap(), monitorData)
    }
}