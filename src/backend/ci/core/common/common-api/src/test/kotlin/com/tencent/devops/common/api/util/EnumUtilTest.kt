/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.enums.MyEnum
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnumUtilTest {

    @Test
    fun getReflectionFactory() {
        assertNotNull(EnumUtil.reflectionFactory)
    }

    @Test
    fun changeEnum() {
        val additionalValues = arrayOf("橘子", "水果", false)
        EnumUtil.addEnum(MyEnum::class.java, "ORANGE", additionalValues)

        MyEnum.values().forEach {
            println("${it.cnName} ${it.ordinal} ${it.check} ${it.type}")
        }

        assertNotNull(MyEnum.valueOf("ORANGE"))
        // 动态实例
        val orange = MyEnum.valueOf("ORANGE")
        assertArrayEquals(additionalValues, arrayOf(orange.cnName, orange.type, orange.check))

        // 修改现有枚举
        EnumUtil.addEnum(MyEnum::class.java, MyEnum.APPLE.name, additionalValues)
        val newApple = MyEnum.valueOf(MyEnum.APPLE.name)
        assertNotEquals(MyEnum.APPLE/*编译时期确定了，与后来修改的不是同一个实例，不相等*/, newApple)
        assertNotEquals(MyEnum.APPLE.type/*这里被编译时已经替换成了常量"公司"*/, newApple.type)
        assertNotEquals(MyEnum.APPLE.cnName/*这里被编译时已经替换成了常量"苹果"*/, newApple.cnName)
        assertNotEquals(MyEnum.APPLE.check/*这里被编译时已经替换成了常量true*/, newApple.check)

        val whenCaseEquals = when (newApple) {
            MyEnum.APPLE /*这里是对比name所以会成立*/ -> {
                true
            }
            else -> false
        }
        assertTrue(whenCaseEquals)
    }
}
