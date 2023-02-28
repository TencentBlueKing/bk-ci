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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ParameterUtilsTest {

    @Test
    fun testGetListValueByKey() {
        val plist = mutableListOf<BuildParameters>()
        plist.add(BuildParameters(key = "A", value = true, valueType = BuildFormPropertyType.BOOLEAN))
        val expect = "any"
        plist.add(BuildParameters(key = "B", value = expect, valueType = BuildFormPropertyType.STRING))

        var actual = ParameterUtils.getListValueByKey(plist, "B")
        Assertions.assertEquals(expect, actual)

        actual = ParameterUtils.getListValueByKey(plist, "C")
        Assertions.assertEquals(null, actual)
    }

    @Test
    fun parameterSizeCheck() {
        val data1 = mutableMapOf<String, Any>()
        data1["key1"] = "value1"
        val element1 = MarketBuildLessAtomElement(
            name = "test",
            id = "e-xxx",
            status = "running",
            atomCode = "testAtom",
            version = "1.0",
            data = data1
        )
        Assertions.assertNotEquals(ParameterUtils.element2Str(element1), null)

        val sb = StringBuilder()
        while (sb.length < 65534) {
            sb.append("this is too long value,")
        }
        data1["key2"] = sb.toString()
        val element2 = MarketBuildLessAtomElement(
            name = "test2",
            id = "e-xxx",
            status = "running",
            atomCode = "testAtom2",
            version = "1.0",
            data = data1
        )
        Assertions.assertEquals(ParameterUtils.element2Str(element2), null)
    }

    @Test
    fun findInput() {
        val data1 = mutableMapOf<String, Any>()
        val input = mutableMapOf<String, String>()
        input["key1"] = "value1"
        input["key2"] = "value2"
        data1["key1"] = "value1"
        data1["input"] = input
        val element1 = MarketBuildLessAtomElement(
            name = "test",
            id = "e-xxx",
            status = "running",
            atomCode = "testAtom",
            version = "1.0",
            data = data1
        )
        val json = element1.genTaskParams()["data"]
        val inputData = JsonUtil.toMap(json!!)["input"]
        val inputMap = JsonUtil.toMap(inputData!!)
        val inputKeys = inputMap.keys
        val input1 = ParameterUtils.getElementInput(element1)
        val checkValue = input1?.keys
        Assertions.assertEquals(inputKeys, checkValue)

        val element2 = MarketBuildLessAtomElement(
            name = "test",
            id = "e-xxx",
            status = "running",
            atomCode = "testAtom",
            version = "1.0",
            data = mapOf()
        )
        val input2 = ParameterUtils.getElementInput(element2)
        Assertions.assertEquals(input2, null)
    }
}
