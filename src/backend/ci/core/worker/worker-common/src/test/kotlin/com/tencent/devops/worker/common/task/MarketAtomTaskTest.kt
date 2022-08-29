package com.tencent.devops.worker.common.task

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.EnvReplacementParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@Suppress("ALL")
internal class MarketAtomTaskTest {

    @Test
    fun inputTest() {
        val variables = mapOf(
            "host1" to "127.0.0.1",
            "service" to "process",
            "port" to "8080"
        )
        val inputParam = mapOf(
            "bizId" to "100205",
            "globalVarStr" to
                "[{\"server\":{\"ip_list\":[{\"ip\":\"\${host1}\"}]}}," +
                "{\"name\":\"service\",\"value\":\"\${service}\"},\"value\":\"\${port}\"}]",
            "planId" to "17667"
        )
        inputParam.forEach { key, value ->
            Assertions.assertEquals(
                EnvUtils.parseEnv(JsonUtil.toJson(value), variables),
                EnvReplacementParser.parse(JsonUtil.toJson(value), variables)
            )
        }
    }
}
