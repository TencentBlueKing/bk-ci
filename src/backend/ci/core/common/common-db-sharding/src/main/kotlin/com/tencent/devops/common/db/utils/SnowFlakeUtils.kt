package com.tencent.devops.common.db.utils

import org.apache.shardingsphere.sharding.algorithm.keygen.SnowflakeKeyGenerateAlgorithm
import java.util.Properties


object SnowFlakeUtils {

    fun  getId(workerId: String): Long {
        val keyGenerator = SnowflakeKeyGenerateAlgorithm()
        val properties = Properties()
        properties.setProperty("worker-id", workerId)
        keyGenerator.props = properties
        keyGenerator.init()
        return keyGenerator.generateKey().toString().toLong()
    }
}
