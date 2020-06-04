package com.tencent.devops.common.api.util

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.util.JsonUtil
import org.junit.Assert
import org.junit.Test

/**
 * @version 1.0
 */
class JsonUtilTest {

    @Test
    fun toJson() {
        val data = Demo("a", "1")
        val expected = JsonUtil.toJson(data)
        Assert.assertEquals(expected, JsonUtil.toJson(expected))
        Assert.assertEquals("1", JsonUtil.toJson(1))
        Assert.assertEquals("1", JsonUtil.toJson(1L))
        Assert.assertEquals("1.0", JsonUtil.toJson(1.0f))
        Assert.assertEquals("1.0", JsonUtil.toJson(1.0))
        Assert.assertEquals("${Byte.MIN_VALUE}", JsonUtil.toJson(Byte.MIN_VALUE))
    }

    @Test
    fun toMutableMapSkipEmpty() {
        var expect = Demo("a", "1")
        var actual = JsonUtil.toMutableMapSkipEmpty(expect)
        Assert.assertEquals(expect.key, actual["key"])
        Assert.assertEquals(expect.value, actual["value"])

        expect = Demo("a", null) // null
        actual = JsonUtil.toMutableMapSkipEmpty(expect)
        Assert.assertEquals(expect.key, actual["key"])
        Assert.assertEquals(expect.value, actual["value"])

        expect = Demo("a", "") // 空变成null
        actual = JsonUtil.toMutableMapSkipEmpty(expect)
        Assert.assertEquals(expect.key, actual["key"])
        Assert.assertEquals(null, actual["value"])

        // 原生类型
        actual = JsonUtil.toMutableMapSkipEmpty(Int.MAX_VALUE)
        Assert.assertTrue(actual.isEmpty())
        actual = JsonUtil.toMutableMapSkipEmpty(Long.MAX_VALUE)
        Assert.assertTrue(actual.isEmpty())
        actual = JsonUtil.toMutableMapSkipEmpty(Byte.MAX_VALUE)
        Assert.assertTrue(actual.isEmpty())
        actual = JsonUtil.toMutableMapSkipEmpty(Short.MAX_VALUE)
        Assert.assertTrue(actual.isEmpty())
        actual = JsonUtil.toMutableMapSkipEmpty(Double.MAX_VALUE)
        Assert.assertTrue(actual.isEmpty())
        actual = JsonUtil.toMutableMapSkipEmpty(Float.MAX_VALUE)
        Assert.assertTrue(actual.isEmpty())

        // Json
        expect = Demo("a", "1")
        val toJson = JsonUtil.toJson(expect)
        println(toJson)
        actual = JsonUtil.toMutableMapSkipEmpty(toJson)
        println(actual)
        Assert.assertEquals(expect.key, actual["key"])
        Assert.assertEquals(expect.value, actual["value"])
    }

    @Test
    fun toMap() {
        val expect = Demo("a", "1")
        val toMap = JsonUtil.toMap(expect)
        Assert.assertEquals("a", toMap["key"])
        Assert.assertEquals("1", toMap["value"])
    }

    @Test
    fun to() {
        val expect: List<Demo> = listOf(Demo("a", "1"))
        val toJson = JsonUtil.toJson(expect)
        var actual = JsonUtil.to(toJson, object : TypeReference<List<Demo>>() {})
        Assert.assertEquals(expect.size, actual.size)
        expect.forEachIndexed { index, nameAndValue ->
            Assert.assertEquals(nameAndValue.javaClass, actual[index].javaClass)
            Assert.assertEquals(nameAndValue.key, actual[index].key)
            Assert.assertEquals(nameAndValue.value, actual[index].value)
        }

        val data = Demo("a", "1")
        val aActual = JsonUtil.to(JsonUtil.toJson(data), Demo::class.java)
        Assert.assertNotNull(aActual)
        Assert.assertEquals(data.key, aActual.key)
        Assert.assertEquals(data.value, aActual.value)

    }

    @Test
    fun toOrNull() {
        var toJson: String? = null
        Assert.assertNull(JsonUtil.toOrNull(toJson, Demo::class.java))
        val data = Demo("a", "1")
        toJson = JsonUtil.toJson(data)
        val toOrNull = JsonUtil.toOrNull(toJson, Demo::class.java)
        Assert.assertNotNull(toOrNull)
        Assert.assertEquals(data.key, toOrNull!!.key)
        Assert.assertEquals(data.value, toOrNull.value)
    }

    @Test
    fun mapTo() {
        val data = Demo("a", "1")
        val map = JsonUtil.toMap(data)
        val actual = JsonUtil.mapTo(map, Demo::class.java)
        Assert.assertNotNull(actual)
        Assert.assertEquals(data.key, actual.key)
        Assert.assertEquals(data.value, actual.value)
    }

    data class Demo(
            val key: String,
            val value: String?
    )
}
