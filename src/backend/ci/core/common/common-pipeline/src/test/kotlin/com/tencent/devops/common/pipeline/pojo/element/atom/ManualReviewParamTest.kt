package com.tencent.devops.common.pipeline.pojo.element.atom

import org.junit.Assert
import org.junit.Test


internal class ManualReviewParamTest {

    @Test
    fun parseReviewParams() {
        val key = "p1"
        val list = listOf("a", "b")
        val map = mapOf(key to list.toString())
        val param = ManualReviewParam(
            key = key,
            value = listOf("aaa", "bbb"),
            valueType = ManualReviewParamType.MULTIPLE
        )
        try {
            println(param.parseValueWithType(map))
            println(param.parseValueWithType(map)?.javaClass)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Assert.assertTrue(param.parseValueWithType(map)?.equals(list) ?: false)
    }
}
