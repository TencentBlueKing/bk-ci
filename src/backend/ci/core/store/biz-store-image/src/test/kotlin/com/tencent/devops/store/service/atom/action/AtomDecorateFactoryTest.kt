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

package com.tencent.devops.store.service.atom.action

import com.tencent.devops.store.service.atom.action.impl.FirstAtomDataDecorateImpl
import com.tencent.devops.store.service.atom.action.impl.FirstAtomPropsDecorateImpl
import org.junit.Assert
import org.junit.Test
import javax.annotation.Priority

@Suppress("MagicNumber")
class AtomDecorateFactoryTest {

    @Test
    fun test() {
        val bean2 = AtomProps2().apply { this.init() }
        val bean4 = AtomProps4().apply { this.init() }
        val bean3 = AtomProps3().apply { this.init() }
        val bean1 = AtomProps1().apply { this.init() }
        val beanOther = AtomData5().apply { this.init() } // 不同分类

        val json = "{\"p\": 0, \"str\": \"hello\" }"
        val props = AtomDecorateFactory.get(AtomDecorateFactory.Kind.PROPS)
        Assert.assertNotEquals(beanOther, props)
        Assert.assertEquals(bean4, props)
        Assert.assertEquals(bean3, props?.getNext())
        Assert.assertEquals(bean2, props?.getNext()?.getNext())
        Assert.assertEquals(bean1, props?.getNext()?.getNext()?.getNext())
        val decorateMap = props?.decorate(json)
        @Suppress("UNCHECKED_CAST")
        Assert.assertEquals(10, (decorateMap as Map<String, Any>)["p"].toString().toInt())

        val data = AtomDecorateFactory.get(AtomDecorateFactory.Kind.DATA)
        Assert.assertEquals(beanOther, data)
        val decorate = data?.decorate(json)
        @Suppress("UNCHECKED_CAST")
        Assert.assertEquals(5, (decorate as Map<String, Any>)["p"].toString().toInt())
    }

    @Priority(1)
    class AtomProps1 : FirstAtomPropsDecorateImpl() {

        override fun type() = AtomDecorateFactory.Kind.PROPS

        override fun decorateSpecial(obj: Map<String, Any>): Map<String, Any> {
            val p = javaClass.getDeclaredAnnotation(Priority::class.java)?.value ?: 0
            val toMutableMap = obj.toMutableMap()
            toMutableMap["p"] = toMutableMap["p"].toString().toInt() + p
            println("AtomProps$p=${toMutableMap["p"]}")
            return super.decorateSpecial(toMutableMap)
        }
    }

    @Priority(2)
    class AtomProps2 : AtomProps1()

    @Priority(3)
    class AtomProps3 : AtomProps1()

    @Priority(4)
    class AtomProps4 : AtomProps1()

    @Priority(5)
    class AtomData5 : FirstAtomDataDecorateImpl() {

        override fun type() = AtomDecorateFactory.Kind.DATA

        override fun decorateSpecial(obj: Map<String, Any>): Map<String, Any> {
            val p = javaClass.getDeclaredAnnotation(Priority::class.java)?.value ?: 0
            val toMutableMap = obj.toMutableMap()
            toMutableMap["p"] = toMutableMap["p"].toString().toInt() + p
            println("AtomData$p=${toMutableMap["p"]}")
            return super.decorateSpecial(toMutableMap)
        }
    }
}
