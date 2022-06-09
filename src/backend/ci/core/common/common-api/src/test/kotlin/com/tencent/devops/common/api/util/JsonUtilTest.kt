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

package com.tencent.devops.common.api.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.annotation.SkipLogField
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * @version 1.0
 */
class JsonUtilTest {

    @Test
    fun mapToTest() {
        val bean = NameAndValue(key = "name", value = "this is password 123456", type = TestType.STRING)
        val toMap = JsonUtil.toMap(bean)
        val newBean = JsonUtil.mapTo(toMap, NameAndValue::class.java)
        Assert.assertEquals(bean.key, newBean.key)
        Assert.assertEquals(bean.value, newBean.value)
        Assert.assertEquals(bean.type, newBean.type)
    }

    @Test
    @Suppress("ALL")
    fun toOrNullTest() {
        val json = "      "
        Assert.assertNull(JsonUtil.toOrNull(json, List::class.java))

        val emptyListJson = "[]"
        val list = JsonUtil.toOrNull(emptyListJson, List::class.java)
        Assert.assertNotNull(list)
        Assert.assertEquals(0, list!!.size)

        val notEmptyListJson = "[\"hello\"]"
        val strList = JsonUtil.toOrNull(notEmptyListJson, List::class.java) as List<String>?
        Assert.assertNotNull(strList)
        Assert.assertEquals(1, strList!!.size)

        val illegal = "\"hello\""
        val nullList = JsonUtil.toOrNull(illegal, List::class.java) as List<String>?
        Assert.assertNull(nullList)

        val nullObj = JsonUtil.toOrNull(json, object : TypeReference<List<String>>() {})
        Assert.assertNull(nullObj)

        val listStr = JsonUtil.toOrNull(notEmptyListJson, object : TypeReference<List<String>>() {})
        Assert.assertNotNull(listStr)
        Assert.assertEquals(1, listStr?.size)

        val illegalNull = JsonUtil.toOrNull(illegal, object : TypeReference<List<String>>() {})
        Assert.assertNull(illegalNull)
    }

    @Test
    fun toMutableMapSkipEmpty() {
        val json = "{\"a\": \"1\" , \"emptyKey\": \"\"}"
        val map = JsonUtil.toMutableMapSkipEmpty(json)
        Assert.assertNotNull(map)
        Assert.assertEquals(map.size, 2)
        Assert.assertEquals(map["emptyKey"], "")

        map["str_array"] = ArrayList<String>()

        val mt = JsonUtil.toMutableMapSkipEmpty(map)
        Assert.assertNotNull(mt["a"])
        Assert.assertNull(mt["emptyKey"])
        Assert.assertNull(mt["str_array"])
    }

    @Test
    fun toMutableMap() {
        val json = "{\"a\": \"1\" , \"emptyKey\": \"\"}"
        val map = JsonUtil.toMap(json)
        Assert.assertNotNull(map)
        Assert.assertNotNull(map["emptyKey"])
        Assert.assertEquals(map["a"], "1")
        Assert.assertEquals(map["emptyKey"], "")

        val map2 = JsonUtil.toMap(map)
        Assert.assertNotNull(map2)
        Assert.assertNotNull(map2["emptyKey"])
        Assert.assertEquals(map2["a"], "1")
        Assert.assertEquals(map2["emptyKey"], "")

        val mt = JsonUtil.toMutableMap(map)
        mt["str_array"] = ArrayList<String>()

        println(mt)

        val mutableMap = JsonUtil.toMutableMap(mt)
        Assert.assertNotNull(mutableMap)
        Assert.assertNotNull(mutableMap["emptyKey"])
        Assert.assertNotNull(mutableMap["str_array"])
        Assert.assertEquals(mutableMap["a"], "1")
        Assert.assertEquals(mutableMap["emptyKey"], "")
        mutableMap["a"] = "2"
        Assert.assertEquals(mutableMap["a"], "2")
        println(mutableMap)
    }

    @Test
    fun to1() {
        val expect: List<NameAndValue> = listOf(NameAndValue(key = "ai", value = "1", type = TestType.INT))
        val toJson = JsonUtil.toJson(expect)
        val actual = JsonUtil.to(toJson, object : TypeReference<List<NameAndValue>>() {})
        Assert.assertEquals(expect.size, actual.size)
        expect.forEachIndexed { index, nameAndValue ->
            Assert.assertEquals(nameAndValue.javaClass, actual[index].javaClass)
            Assert.assertEquals(nameAndValue.key, actual[index].key)
            Assert.assertEquals(nameAndValue.value, actual[index].value)
        }
    }

    @Test
    fun isBoolean() {
        val p = IsBoolean(helmChartEnabled = true, offlined = true, isSecrecy = true, exactResource = 999)
        val json = JsonUtil.toJson(p)
        println(JsonUtil.to(json, IsBoolean::class.java))
    }

    @Test
    fun skipLogFields() {

        val bean = NameAndValue(key = "name", value = "this is password 123456", type = TestType.STRING)
        val allJsonData = JsonUtil.toJson(bean)

        println("正常的Json序列化不受影响: $allJsonData")

        val allFieldsMap = JsonUtil.to<Map<String, Any>>(allJsonData)
        // 所有字段都存在，否则就是有问题
        Assert.assertNotNull(allFieldsMap["key"])
        Assert.assertNotNull(allFieldsMap["value"])
        Assert.assertNotNull(allFieldsMap["valueType"])

        val logJsonString = JsonUtil.skipLogFields(bean)

        Assert.assertNotNull(logJsonString)
        println("脱密后的Json不会有skipLogField的敏感log信息: $logJsonString")

        val haveNoSkipLogFieldsMap = JsonUtil.to<Map<String, Any>>(logJsonString!!)
        // 以下字段受SkipLogField注解影响，是不会出现的，如果有则说明有问题
        Assert.assertNull(haveNoSkipLogFieldsMap["value"])
        Assert.assertNull(haveNoSkipLogFieldsMap["valueType"])
        // 未受SkipLogField注解影响的字段是存在的
        Assert.assertNotNull(haveNoSkipLogFieldsMap["key"])
    }

    @Test
    fun dataTime() {
        val localDate = LocalDate.now()
        val localTime = LocalTime.now()
        val localDateTime = LocalDateTime.now()
        val dateAndTime = DateAndTime(localDate, localTime, localDateTime, emptyList())

        val jsonStr = JsonUtil.toJson(dateAndTime)
        val deserializeObj = JsonUtil.to(jsonStr, DateAndTime::class.java)
        Assert.assertEquals(dateAndTime, deserializeObj)

        val map = JsonUtil.toMap(dateAndTime)
        val mapToObj = JsonUtil.mapTo(map, DateAndTime::class.java)
        Assert.assertEquals(dateAndTime, mapToObj)

        val skipEmptyMap = JsonUtil.toMutableMapSkipEmpty(dateAndTime)
        Assert.assertNull(skipEmptyMap["emptyList"])
    }

    data class NameAndValue(
        val key: String,
        @SkipLogField
        val value: String,
        @SkipLogField("valueType") // 如果字段序列化输出命名与字段不一致，则需要填写
        @get:JsonProperty("valueType")
        val type: TestType
    )

    enum class TestType { STRING, INT }

    data class IsBoolean(
        val helmChartEnabled: Boolean?, // 正确的命名
        @get:JsonProperty("offlined") // 正确的json命名
        val offlined: Boolean?, // 正确的命名
        @get:JsonProperty("is_secrecy") // 错误的json命名
        val isSecrecy: Boolean?, // 错误的字段示例命名，会导致反序列化的空值
        @get:JsonProperty("is_exact_resource")
        val exactResource: Int = 1
    )

    data class DateAndTime(
        val localDate: LocalDate,
        val localTime: LocalTime,
        val localDateTime: LocalDateTime,
        val emptyList: List<String>
    )
}
