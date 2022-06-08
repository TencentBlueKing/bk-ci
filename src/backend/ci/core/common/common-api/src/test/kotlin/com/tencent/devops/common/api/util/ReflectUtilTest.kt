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

import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectUtilTest {

    @Test
    fun isNativeType() {
        val stock = 700
        val boxing: Int? = stock
        val native: Int = stock
        // assertTrue(native !== boxing)
        println("boxing=${boxing!!::class.java}")
        println("native=${native::class.java}")

        val native1: Int = stock
        val native2: Int = stock
        // assertTrue(native1 === native2)
        println("native1=${native1::class.java}")
        println("native2=${native2::class.java}")

        assertTrue(ReflectUtil.isNativeType(1))
        assertTrue(ReflectUtil.isNativeType(1.2))
        assertTrue(ReflectUtil.isNativeType(1.2f))
        assertTrue(ReflectUtil.isNativeType(1L))
        assertTrue(ReflectUtil.isNativeType(true))
        // boxing
        val integer: Int? = 1
        assertTrue(ReflectUtil.isNativeType(integer!!))
        val boolean: Boolean? = false
        assertTrue(ReflectUtil.isNativeType(boolean!!))
        val float: Float? = 1.2f
        assertTrue(ReflectUtil.isNativeType(float!!))
        val double: Double? = 1.2
        assertTrue(ReflectUtil.isNativeType(double!!))
        val long: Long? = 1L
        assertTrue(ReflectUtil.isNativeType(long!!))
    }
}
