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

package com.tencent.devops.common.pipeline

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.JsonUtil.toJson
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("ALL", "UNCHECKED_CAST")
class EnvReplaceAndExpressionTest {

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
        val convertDataObj = toJson(ObjectReplaceEnvVarUtil.replaceEnvVar(originDataListObj, envMap))
        val convertDataObj2 = EnvReplacementParser.parse(value = toJson(originDataListObj), envMap)
        assertEquals(convertDataObj, convertDataObj2)
    }

    @Test
    fun replaceIllegalJson() {
        val objectJson = "{\"abc:\"变量替换测试_\${normalStrEnvVar}\""
        val convertDataObj1 = ObjectReplaceEnvVarUtil.replaceEnvVar(objectJson, envMap)
        val convertDataObj11 = EnvReplacementParser.parse(objectJson, envMap)

        assertEquals(convertDataObj1, convertDataObj11)

        val arrayJson = "[1, \"变量替换测试_\${normalStrEnvVar}\""
        val convertDataObj2 = ObjectReplaceEnvVarUtil.replaceEnvVar(arrayJson, envMap)
        val convertDataObj21 = EnvReplacementParser.parse(arrayJson, envMap)
        assertEquals(convertDataObj2, convertDataObj21)
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
        val convertDataObj = toJson(ObjectReplaceEnvVarUtil.replaceEnvVar(originDataSetObj, envMap))
        val convertDataObj2 = EnvReplacementParser.parse(toJson(originDataSetObj), envMap)
        assertEquals(convertDataObj, convertDataObj2)
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

        val cpb2 = JsonUtil.to(EnvReplacementParser.parse(toJson(originDataMapObj), envMap), Map::class.java)
        val testBeanMap2 = JsonUtil.to(
            toJson(JsonUtil.to(toJson(cpb2["originSubDataMapObj"]!!), Map::class.java)["testBean"]!!),
            TestBean::class.java
        )
        assertEquals(testBeanMap.testBeanKey, testBeanMap2.testBeanKey)
        assertEquals(testBeanMap.testBeanValue, testBeanMap2.testBeanValue)
        // EnvReplacementParser对格式进行了压缩
        assertNotEquals(
            (cpb as Map<String?, Any?>)["jsonStrEnvVarKey3"]!!,
            (cpb2 as Map<String?, Any?>)["jsonStrEnvVarKey3"]!!
        )
        originSubDataMapObj = cpb["originSubDataMapObj"] as MutableMap<String?, Any?>?
        val originSubDataMapObj2 = cpb2["originSubDataMapObj"] as MutableMap<String?, Any?>?
        assertNotEquals(originSubDataMapObj!!["jsonStrEnvVarKey2"]!!, originSubDataMapObj2!!["jsonStrEnvVarKey2"]!!)
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
        var convertDataObj1 = ObjectReplaceEnvVarUtil.replaceEnvVar(testComplexBean, envMap)
        val convertBean1 = convertDataObj1 as TestComplexBean
        val convertBean2 = JsonUtil.to(EnvReplacementParser.parse(toJson(testComplexBean), envMap), TestComplexBean::class.java)

        assertEquals(convertBean1.testBeanKey, convertBean2.testBeanKey)

        assertEquals(convertBean1.dataList!![0], convertBean2.dataList!![0])
        assertEquals(convertBean1.dataList!![1], convertBean2.dataList!![1])
        assertEquals(convertBean1.dataList!![2], convertBean2.dataList!![2])
        assertEquals(convertBean1.dataList!![3], convertBean2.dataList!![3])
        assertEquals(convertBean1.dataList!![4], convertBean2.dataList!![4])

        assertEquals(convertBean1.dataMap!!["normalStrEnvVarKey"], convertBean2.dataMap!!["normalStrEnvVarKey"])
        assertEquals(convertBean1.dataMap!!["specStrEnvVarKey"], convertBean2.dataMap!!["specStrEnvVarKey"])
        assertEquals(convertBean1.dataMap!!["jsonStrEnvVarKey1"], convertBean2.dataMap!!["jsonStrEnvVarKey1"])
        assertEquals(convertBean1.dataMap!!["jsonStrEnvVarKey2"], convertBean2.dataMap!!["jsonStrEnvVarKey2"])
        assertEquals(convertBean1.dataMap!!["jsonStrEnvVarKey3"], convertBean2.dataMap!!["jsonStrEnvVarKey3"])

        // 替换包含null的对象
        dataMap = HashMap()
        dataMap["key1"] = "变量"
        dataMap["key2"] = arrayOf<Any?>(null, "哈哈")

        convertDataObj1 = ObjectReplaceEnvVarUtil.replaceEnvVar(dataMap, envMap) as Map<*, *>
        var convertDataObj2 = JsonUtil.to(EnvReplacementParser.parse(toJson(dataMap), envMap), Map::class.java)
        assertEquals(convertDataObj1["key1"], convertDataObj2["key1"])
        assertEquals(convertDataObj1["key2"], convertDataObj2["key2"])
    }

    @Test
    fun replaceEnvVar() {

        // 对普通字符串进行普通字符串变量替换
        var originDataObj: Any = "变量替换测试_\${normalStrEnvVar}"
        var convertDataObj = toJson(ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap))
        var convertDataObj2 = EnvReplacementParser.parse(toJson(originDataObj), envMap)
        assertEquals(convertDataObj, convertDataObj2)

        // 对普通字符串进行带特殊字符字符串变量替换
        originDataObj = "变量替换测试_\${specStrEnvVar}"
        convertDataObj = toJson(ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap))
        convertDataObj2 = EnvReplacementParser.parse(toJson(originDataObj), envMap)
        assertEquals(convertDataObj, convertDataObj2)

        // 对普通字符串进行json字符串变量替换
        originDataObj = "变量替换测试_\${jsonStrEnvVar}"
        convertDataObj = toJson(ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap))
        convertDataObj2 = EnvReplacementParser.parse(toJson(originDataObj), envMap)
        assertEquals(convertDataObj, convertDataObj2)

        // number类型变量替换
        originDataObj = "[1,2,3]"
        convertDataObj = toJson(ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap))
        convertDataObj2 = EnvReplacementParser.parse(toJson(originDataObj), envMap)
        assertEquals(convertDataObj, convertDataObj2)

        // 魔法数字符创测试
        convertDataObj = toJson(ObjectReplaceEnvVarUtil.replaceEnvVar("12E2", envMap))
        convertDataObj2 = EnvReplacementParser.parse("12E2", envMap)
        assertEquals(convertDataObj, convertDataObj2)
        // 替换”[133]-[sid-${normalStrEnvVar}]-[sid-zhiliang-test1]“带多个[]的字符串
        convertDataObj = toJson(
            ObjectReplaceEnvVarUtil.replaceEnvVar(
                "[133]-[sid-\${normalStrEnvVar}]-[sid-zhiliang-test1]",
                envMap
            )
        )
        convertDataObj2 = EnvReplacementParser.parse(
            "[133]-[sid-\${normalStrEnvVar}]-[sid-zhiliang-test1]",
            envMap
        )
        assertEquals(convertDataObj, convertDataObj2)
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
