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

package com.tencent.devops.common.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@Suppress("ALL")
class RegexUtilsTest {

    @Test
    fun splitDomainContextPath() {
        var protocol = "http://"
        val noSubDomain = "donothavewww123.com"
        Assertions.assertNull(RegexUtils.splitDomainContextPath(protocol + noSubDomain)) // no context path

        var contextPath = "/a/b/c.txt"
        Assertions.assertEquals(contextPath,
            RegexUtils.splitDomainContextPath(protocol + noSubDomain + contextPath)!!.second)
        protocol = "https://"
        val httpsDomain = "www.tencent-inc.com"
        contextPath = "/"
        Assertions.assertEquals(httpsDomain,
            RegexUtils.splitDomainContextPath(protocol + httpsDomain + contextPath)!!.first)
        Assertions.assertEquals(contextPath,
            RegexUtils.splitDomainContextPath(protocol + httpsDomain + contextPath)!!.second)

        val domainWithPort = "https://www.tencent.com:12345"
        contextPath = "/report.html"
        Assertions.assertEquals("www.tencent.com", RegexUtils.splitDomainContextPath(domainWithPort + contextPath)!!.first)
        Assertions.assertEquals(contextPath, RegexUtils.splitDomainContextPath(domainWithPort + contextPath)!!.second)
    }

    @Test
    fun trimProtocol() {
        val contextPath = "/a/b/c.txt"

        val noSubDomain = "donothavewww.com"
        Assertions.assertEquals("//$noSubDomain$contextPath",
            RegexUtils.trimProtocol("http://$noSubDomain$contextPath"))

        val httpsDomain = "www.tencent-inc123.com:1234"
        Assertions.assertEquals("//$httpsDomain$contextPath",
            RegexUtils.trimProtocol("https://$httpsDomain$contextPath"))
    }
}
