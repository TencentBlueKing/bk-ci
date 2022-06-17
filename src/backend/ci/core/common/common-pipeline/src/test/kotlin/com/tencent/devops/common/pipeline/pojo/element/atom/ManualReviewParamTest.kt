package com.tencent.devops.common.pipeline.pojo.element.atom

import org.junit.jupiter.api.Test

internal class ManualReviewParamTest {

    @Test
    fun parseReviewParams() {
        // TODO 待补全单测对比
        val key = "p1"
        val list = listOf("a", "b")
        val map = mapOf(key to list.toString())
        val param = ManualReviewParam(
            key = key,
            value = listOf("aaa", "bbb"),
            valueType = ManualReviewParamType.MULTIPLE
        )
        println(param.parseValueWithType(map))
    }
}
