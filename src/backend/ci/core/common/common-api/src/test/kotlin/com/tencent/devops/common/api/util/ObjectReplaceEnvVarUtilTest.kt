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

import com.tencent.devops.common.api.util.JsonUtil.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("ALL", "UNCHECKED_CAST")
class ObjectReplaceEnvVarUtilTest {

    private val envMap: MutableMap<String, String> = HashMap()

    @BeforeEach
    fun setup() {
        envMap["normalStrEnvVar"] = "123"
        envMap["specStrEnvVar"] = "D:\\tmp\\hha"
        envMap["jsonStrEnvVar"] = "{\"abc\":\"123\"}"
    }

    private val lineSeparator = System.getProperty("line.separator")
    private val jsonExcept = "{$lineSeparator" +
        "  \"abc\" : \"变量替换测试_{\\\"abc\\\":\\\"123\\\"}\"$lineSeparator" +
        "}"

    private val arrayJsonExcept = "[ \"变量替换测试_{\\\"abc\\\":\\\"123\\\"}\" ]"

    @Test
    fun replaceList() {
        val testBean = TestBean(
            testBeanKey = "bean变量替换测试_\${specStrEnvVar}",
            testBeanValue = "{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}"
        )
        // 对list对象进行变量替换
        val originDataListObj = ArrayList<Any?>()
        originDataListObj.add("变量替换测试_\${normalStrEnvVar}")
        originDataListObj.add("变量替换测试_\${specStrEnvVar}")
        originDataListObj.add("变量替换测试_\${jsonStrEnvVar}")
        originDataListObj.add("{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}")
        originDataListObj.add("[\"变量替换测试_\${jsonStrEnvVar}\"]")
        originDataListObj.add(testBean)
        val dataMapObj: MutableMap<String, Any?> = HashMap()
        dataMapObj["dataMapKey"] = "变量替换测试_\${specStrEnvVar}"
        dataMapObj["testBean"] = testBean
        originDataListObj.add(dataMapObj)
        val convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataListObj, envMap) as List<*>

        assertEquals("变量替换测试_${envMap["normalStrEnvVar"]}", convertDataObj[0])
        assertEquals("变量替换测试_${envMap["specStrEnvVar"]}", convertDataObj[1])
        assertEquals("变量替换测试_${envMap["jsonStrEnvVar"]}", convertDataObj[2])
        assertEquals(jsonExcept, convertDataObj[3])
        assertEquals(arrayJsonExcept, convertDataObj[4])

        val convertTestBean = convertDataObj[5] as TestBean
        assertEquals("bean变量替换测试_${envMap["specStrEnvVar"]}", convertTestBean.testBeanKey)
        assertEquals(jsonExcept, convertTestBean.testBeanValue)
    }

    @Test
    fun replaceIllegalJson() {
        val objectJson = "{\"abc:\"变量替换测试_\${normalStrEnvVar}\""
        val convertDataObj1 = ObjectReplaceEnvVarUtil.replaceEnvVar(objectJson, envMap)
        println(convertDataObj1)
        assertEquals("{\"abc:\"变量替换测试_${envMap["normalStrEnvVar"]}\"", convertDataObj1)

        val arrayJson = "[1, \"变量替换测试_\${normalStrEnvVar}\""
        val convertDataObj2 = ObjectReplaceEnvVarUtil.replaceEnvVar(arrayJson, envMap)
        println(convertDataObj2)
        assertEquals("[1, \"变量替换测试_${envMap["normalStrEnvVar"]}\"", convertDataObj2)
    }

    @Test
    fun replaceSet() {

        val testBean = TestBean(
            testBeanKey = "bean变量替换测试_\${specStrEnvVar}",
            testBeanValue = "{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}"
        )
        // 对set对象进行变量替换
        val originDataSetObj = HashSet<Any>()
        originDataSetObj.add("1变量替换测试_\${normalStrEnvVar}")
        originDataSetObj.add("2变量替换测试_\${specStrEnvVar}")
        originDataSetObj.add("3变量替换测试_\${jsonStrEnvVar}")
        originDataSetObj.add("{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}")
        originDataSetObj.add("[\"变量替换测试_\${jsonStrEnvVar}\"]")
        originDataSetObj.add(testBean)

        val setDataMapObj: MutableMap<String, Any?> = HashMap()
        setDataMapObj["dataMapKey"] = "变量替换测试_\${specStrEnvVar}"
        setDataMapObj["testBean"] = testBean
        originDataSetObj.add(setDataMapObj)
        val convertDataObj = (ObjectReplaceEnvVarUtil.replaceEnvVar(originDataSetObj, envMap) as Set<*>)

        convertDataObj.forEach { member ->
            when {
                member is Map<*, *> -> {
                    member.forEach { sm ->
                        when {
                            sm.key.toString() == "testBean" -> {
                                assertEquals(
                                    "bean变量替换测试_${envMap["specStrEnvVar"]}",
                                    (sm.value as TestBean).testBeanKey
                                )
                                assertEquals(jsonExcept, (sm.value as TestBean).testBeanValue)
                            }
                            sm.key.toString() == "dataMapKey" -> {
                                assertEquals("变量替换测试_${envMap["specStrEnvVar"]}", sm.value)
                            }
                            else -> {
                                assertEquals(member.toString(), "setDataMapObj")
                            }
                        }
                    }
                }
                member is TestBean -> {
                    assertEquals("bean变量替换测试_${envMap["specStrEnvVar"]}", member.testBeanKey)
                    assertEquals(jsonExcept, member.testBeanValue)
                }
                member.toString().startsWith("1") -> {
                    assertEquals("1变量替换测试_${envMap["normalStrEnvVar"]}", member)
                }
                member.toString().startsWith("2") -> {
                    assertEquals("2变量替换测试_${envMap["specStrEnvVar"]}", member)
                }
                member.toString().startsWith("3") -> {
                    assertEquals("3变量替换测试_${envMap["jsonStrEnvVar"]}", member)
                }
                member.toString().startsWith("{") -> {
                    assertEquals(jsonExcept, member)
                }
                member.toString().startsWith("[") -> {
                    assertEquals(arrayJsonExcept, member)
                }
                else -> {
                    assertEquals(member.toString(), "convertDataObj")
                }
            }
        }
    }

    @Test
    fun replaceMapWithTestBean() {
        // 对map对象进行变量替换
        val originDataMapObj: MutableMap<String, Any?> = HashMap()
        originDataMapObj["normalStrEnvVarKey"] = "变量替换测试_\${normalStrEnvVar}"
        originDataMapObj["specStrEnvVarKey"] = "变量替换测试_\${specStrEnvVar}"
        originDataMapObj["jsonStrEnvVarKey1"] = "变量替换测试_\${jsonStrEnvVar}"
        originDataMapObj["jsonStrEnvVarKey2"] = "{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}"
        originDataMapObj["jsonStrEnvVarKey3"] = "\${jsonStrEnvVar}"
        var originSubDataMapObj: MutableMap<String?, Any?>? = HashMap()
        originSubDataMapObj!!["normalStrEnvVarKey"] = "变量替换测试_\${normalStrEnvVar}"
        originSubDataMapObj["specStrEnvVarKey"] = "变量替换测试_\${specStrEnvVar}"
        originSubDataMapObj["jsonStrEnvVarKey1"] = "变量替换测试_\${jsonStrEnvVar}"
        originSubDataMapObj["jsonStrEnvVarKey2"] = "\${jsonStrEnvVar}"

        val testBean = TestBean(
            testBeanKey = "变量替换测试_\${specStrEnvVar}",
            testBeanValue = "{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}"
        )
        originSubDataMapObj["testBean"] = testBean
        originDataMapObj["originSubDataMapObj"] = originSubDataMapObj
        val cpb = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataMapObj, envMap)
        val testBeanMap = ((cpb as Map<String, Any>)["originSubDataMapObj"] as Map<String, Any>)["testBean"] as TestBean
        assertEquals("变量替换测试_${envMap["specStrEnvVar"]}", testBeanMap.testBeanKey)
        assertEquals(jsonExcept, testBeanMap.testBeanValue)
        // 判断map中jsonStrEnvVarKey3对应的值进行变量替换后能否正常转换为json串
        assertEquals(envMap["jsonStrEnvVar"], (cpb as Map<String?, Any?>)["jsonStrEnvVarKey3"]!!)
        originSubDataMapObj = cpb["originSubDataMapObj"] as MutableMap<String?, Any?>?
        // 判断嵌套的map中jsonStrEnvVarKey2对应的值进行变量替换后能否正常转换为json串
        assertEquals(envMap["jsonStrEnvVar"], originSubDataMapObj!!["jsonStrEnvVarKey2"]!!)
    }

    @Test
    fun replaceTestComplexBean() {
        // 对普通的javaBean对象进行转换
        val testComplexBean = TestComplexBean()
        testComplexBean.testBeanKey = "变量替换测试_\${specStrEnvVar}"
        testComplexBean.testBeanValue = "[\"变量替换测试_\${jsonStrEnvVar}\"]"

        val dataList = ArrayList<Any?>()
        dataList.add("变量替换测试_\${normalStrEnvVar}")
        dataList.add("变量替换测试_\${specStrEnvVar}")
        dataList.add("变量替换测试_\${jsonStrEnvVar}")
        dataList.add("{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}")
        dataList.add("[\"变量替换测试_\${jsonStrEnvVar}\"]")
        testComplexBean.dataList = dataList

        var dataMap: MutableMap<String?, Any?> = HashMap()
        dataMap["normalStrEnvVarKey"] = " 变量替换测试_\${normalStrEnvVar} "
        dataMap["specStrEnvVarKey"] = "变量替换测试_\${specStrEnvVar}"
        dataMap["jsonStrEnvVarKey1"] = "变量替换测试_\${jsonStrEnvVar}"
        dataMap["jsonStrEnvVarKey2"] = "{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}"
        dataMap["jsonStrEnvVarKey3"] = "[\"变量替换测试_\${jsonStrEnvVar}\"]"
        val subDataMap: MutableMap<String, Any> = HashMap()
        subDataMap["normalStrEnvVarKey"] = "变量替换测试_\${normalStrEnvVar}"
        subDataMap["specStrEnvVarKey"] = "变量替换测试_\${specStrEnvVar}"
        subDataMap["jsonStrEnvVarKey1"] = "变量替换测试_\${jsonStrEnvVar}"
        subDataMap["jsonStrEnvVarKey2"] = "{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}"

        val testBean = TestBean(
            testBeanKey = "bean变量替换测试_\${specStrEnvVar}",
            testBeanValue = "{\"abc\":\"bean变量替换测试_\${jsonStrEnvVar}\"}"
        )
        subDataMap["testBean"] = testBean
        dataMap["subDataMap"] = subDataMap
        testComplexBean.dataMap = dataMap

        val dataSet = HashSet<Any?>()
        dataSet.add("变量替换测试_\${normalStrEnvVar}")
        dataSet.add("变量替换测试_\${specStrEnvVar}")
        dataSet.add("变量替换测试_\${jsonStrEnvVar}")
        dataSet.add("{\"abc\":\"变量替换测试_\${jsonStrEnvVar}\"}")
        dataSet.add("[\"变量替换测试_\${jsonStrEnvVar}\"]")
        testComplexBean.dataSet = dataSet

        // start to test
        var convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(testComplexBean, envMap)
        val convertBean = convertDataObj as TestComplexBean
        assertEquals("变量替换测试_${envMap["specStrEnvVar"]}", convertBean.testBeanKey)

        assertEquals("变量替换测试_${envMap["normalStrEnvVar"]}", convertBean.dataList!![0])
        assertEquals("变量替换测试_${envMap["specStrEnvVar"]}", convertBean.dataList!![1])
        assertEquals("变量替换测试_${envMap["jsonStrEnvVar"]}", convertBean.dataList!![2])
        assertEquals(jsonExcept, convertBean.dataList!![3])
        assertEquals("[ \"变量替换测试_{\\\"abc\\\":\\\"123\\\"}\" ]", convertBean.dataList!![4])

        assertEquals(" 变量替换测试_${envMap["normalStrEnvVar"]} ", convertBean.dataMap!!["normalStrEnvVarKey"])
        assertEquals("变量替换测试_${envMap["specStrEnvVar"]}", convertBean.dataMap!!["specStrEnvVarKey"])
        assertEquals("变量替换测试_${envMap["jsonStrEnvVar"]}", convertBean.dataMap!!["jsonStrEnvVarKey1"])
        assertEquals(jsonExcept, convertBean.dataMap!!["jsonStrEnvVarKey2"])
        assertEquals(arrayJsonExcept, convertBean.dataMap!!["jsonStrEnvVarKey3"])

        // 替换包含null的对象
        dataMap = HashMap()
        dataMap["key1"] = "变量"
        dataMap["key2"] = arrayOf<Any?>(null, "哈哈")

        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(dataMap, envMap) as Map<*, *>
        assertEquals(dataMap["key1"], convertDataObj["key1"])
        assertEquals(toJson(dataMap["key2"]!!), convertDataObj["key2"])
        println("convertDataObj=$convertDataObj")
    }

    @Test
    fun replaceEnvVar() {

        // 对普通字符串进行普通字符串变量替换
        var originDataObj: Any = "变量替换测试_\${normalStrEnvVar}"
        var convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap)
        assertEquals("变量替换测试_123", toJson(convertDataObj))

        // 对普通字符串进行带特殊字符字符串变量替换
        originDataObj = "变量替换测试_\${specStrEnvVar}"
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap)
        assertEquals("变量替换测试_D:\\tmp\\hha", toJson(convertDataObj))

        // 对普通字符串进行json字符串变量替换
        originDataObj = "变量替换测试_\${jsonStrEnvVar}"
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap)
        assertEquals("变量替换测试_{\"abc\":\"123\"}", toJson(convertDataObj))

        // number类型变量替换
        originDataObj = "[1,2,3]"
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap)
        println(toJson(convertDataObj))
        assertEquals(toJson(JsonUtil.to(originDataObj, List::class.java)), toJson(convertDataObj))

        // 魔法数字符创测试
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar("12E2", envMap)
        assertEquals("12E2", toJson(convertDataObj))
        // 替换”[133]-[sid-${normalStrEnvVar}]-[sid-zhiliang-test1]“带多个[]的字符串
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(
            "[133]-[sid-\${normalStrEnvVar}]-[sid-zhiliang-test1]",
            envMap
        )
        assertEquals("[133]-[sid-123]-[sid-zhiliang-test1]", toJson(convertDataObj))
    }

    internal data class TestBean(
        var testBeanKey: String? = null,
        var testBeanValue: String? = null
    )

    internal data class TestComplexBean(
        var testBeanKey: String? = null,
        var testBeanValue: String? = null,
        var dataList: List<*>? = null,
        var dataMap: Map<*, *>? = null,
        var dataSet: Set<*>? = null
    )
}
