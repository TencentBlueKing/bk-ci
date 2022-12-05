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

package com.tencent.devops.store.service.atom.action.impl

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.service.atom.action.AtomDecorateFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@Suppress("UNCHECKED_CAST")
class WoaAtomPropsDecorateImplTest {

    @Test

    fun replaceUrl() {
        val b = WoaAtomPropsDecorateImpl()
        Assertions.assertEquals("https://devops.woa.com", b.replaceUrl("http://api.devops.oa.com"))
        Assertions.assertEquals("https://devops.woa.com", b.replaceUrl("https://devops.oa.com"))
        Assertions.assertEquals(
            "https://devops.woa.com/x/y/z?UT=1&cc=aa",
            b.replaceUrl("http://devops.woa.com/x/y/z?UT=1&cc=aa")
        )
        Assertions.assertEquals(
            "https://xxx.apigw.o.woa.com/x/y/z?UT=1&cc=aa",
            b.replaceUrl("http://xxx.apigw.o.oa.com/x/y/z?UT=1&cc=aa")
        )
    }

    @Test
    fun decorateSpecial() {

        val bean2 = WoaAtomPropsDecorateImpl().apply { this.init() }
        val bean1 = FirstAtomPropsDecorateImpl().apply { this.init() }

        val props = AtomDecorateFactory.get(AtomDecorateFactory.Kind.PROPS)
        Assertions.assertEquals(bean1, props)
        Assertions.assertEquals(bean2, props?.getNext())

        val map = mapOf(
            "demo" to "mock",
            "url" to "http://api.devops.oa.com",
            "key1" to mapOf<String, Any>(
                "name" to "kkk",
                "url" to "http://xxx.apigw.o.oa.com/x/y/z?UT=1&cc=aa"
            ),
            "input" to mapOf(
                "level3" to mapOf(
                    "name" to "keyword",
                    "url" to "http://devops.woa.com/x/y/z?UT=1&cc=aa"
                )
            )
        )

        val decorate = props?.decorate(JsonUtil.toJson(map)) as Map<String, Any>
        Assertions.assertEquals("https://devops.woa.com", decorate["url"])
        Assertions.assertEquals(
            "https://xxx.apigw.o.woa.com/x/y/z?UT=1&cc=aa",
            (decorate["key1"] as Map<String, Any>)["url"]
        )
        Assertions.assertEquals(
            "https://devops.woa.com/x/y/z?UT=1&cc=aa",
            ((decorate["input"] as Map<String, Any>)["level3"] as Map<String, Any>)["url"]
        )
    }
}
