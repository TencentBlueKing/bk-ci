package com.tencent.devops.monitoring.consumer.processor.monitor

data class MonitorData(
    val fields: HashMap<String, String> = HashMap(),
    val tags: HashMap<String, String> = HashMap()
)