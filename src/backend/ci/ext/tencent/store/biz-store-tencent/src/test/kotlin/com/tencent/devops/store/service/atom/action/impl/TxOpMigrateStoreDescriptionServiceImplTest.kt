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

import com.tencent.devops.store.service.common.impl.TxOpMigrateStoreDescriptionServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TxOpMigrateStoreDescriptionServiceImplTest {

    @Test
    fun checkLogoUrlConditionTest() {
        val t = TxOpMigrateStoreDescriptionServiceImpl()
        val pathList = t.checkLogoUrlCondition(description)
        Assertions.assertEquals(
            "http://radosgw.open.oa.com/xxx/xx/xx/file/png/xxxxx.png?v=xxxx",
            pathList?.get(0) ?: ""
        )
        Assertions.assertEquals(
            "https://radosgw.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas",
            pathList?.get(1) ?: ""
        )
    }

    @Test
    fun replaceDescriptionTest() {
        val t = TxOpMigrateStoreDescriptionServiceImpl()
        val pathMap = mapOf(
            "http://radosgw.open.oa.com/xxx/xx/xx/file/png/xxxxx.png?v=xxxx".replace("?", "\\?")
                    to "https://test.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas",
            "https://radosgw.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas".replace("?", "\\?")
                    to "https://test.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas"
        )
        println(t.replaceDescription(description, pathMap))
    }

    companion object {
        private const val BK_CI_PATH_REGEX = "(!\\[(.*?)]\\()(http[s]?://radosgw.open.oa.com(.*?))(\\))"
        private const val description = "合质量标准。\\n\\n![image1.png](http://radosgw.open.oa.com/xxx/xx/" +
                "xx/file/png/xxxxx.png?v=xxxx)\\n\\n[了解更多Code合质量标准。\\n\\n![image3.png](https://rados" +
                "gw.open.oa.com/xxx/xx/xx/file/png/xxsxax.png?v=aas)\\n\\n[了解更多Code更多Code合质量标准。" +
                "\\n\\n![image2.png](http://radosgw.open.oa.com/xxx/xx/xx/file/png/xxxsx.png?v=xxxxx)\\n\\n[了解更多Code"
    }
}