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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.pypi.artifact.xml

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class XmlConvertUtilTest {

    // pip search response xml content
    private val responseXmlStr = "<methodResponse>\n" +
        "  <params>\n" +
        "    <param>\n" +
        "      <value>\n" +
        "        <array>\n" +
        "          <data>\n" +
        "            <value>\n" +
        "              <struct>\n" +
        "                <member>\n" +
        "                  <name>_pypi_ordering</name>\n" +
        "                  <value>\n" +
        "                    <int>1</int>\n" +
        "                  </value>\n" +
        "                </member>\n" +
        "                <member>\n" +
        "                  <name>version</name>\n" +
        "                  <value>\n" +
        "                    <string>0.0.3</string>\n" +
        "                  </value>\n" +
        "                </member>\n" +
        "                <member>\n" +
        "                  <name>name</name>\n" +
        "                  <value>\n" +
        "                    <string>http3test</string>\n" +
        "                  </value>\n" +
        "                </member>\n" +
        "                <member>\n" +
        "                  <name>summary</name>\n" +
        "                  <value>\n" +
        "                    <string>A small example package</string>\n" +
        "                  </value>\n" +
        "                </member>\n" +
        "              </struct>\n" +
        "            </value>\n" +
        "            <value>\n" +
        "              <struct>\n" +
        "                <member>\n" +
        "                  <name>_pypi_ordering</name>\n" +
        "                  <value>\n" +
        "                    <int>0</int>\n" +
        "                  </value>\n" +
        "                </member>\n" +
        "                <member>\n" +
        "                  <name>version</name>\n" +
        "                  <value>\n" +
        "                    <string>0.0.1</string>\n" +
        "                  </value>\n" +
        "                </member>\n" +
        "                <member>\n" +
        "                  <name>name</name>\n" +
        "                  <value>\n" +
        "                    <string>http3test</string>\n" +
        "                  </value>\n" +
        "                </member>\n" +
        "                <member>\n" +
        "                  <name>summary</name>\n" +
        "                  <value>\n" +
        "                    <string>A small example package</string>\n" +
        "                  </value>\n" +
        "                </member>\n" +
        "              </struct>\n" +
        "            </value>\n" +
        "          </data>\n" +
        "        </array>\n" +
        "      </value>\n" +
        "    </param>\n" +
        "  </params>\n" +
        "</methodResponse>"

    // pip search  request xml content
    private val requestXml = "<?xml version='1.0'?>\n" +
        "<methodCall>\n" +
        "<methodName>search</methodName>\n" +
        "<params>\n" +
        "<param>\n" +
        "<value><struct>\n" +
        "<member>\n" +
        "<name>name</name>\n" +
        "<value><array><data>\n" +
        "<value><string>http3test</string></value>\n" +
        "</data></array></value>\n" +
        "</member>\n" +
        "<member>\n" +
        "<name>summary</name>\n" +
        "<value><array><data>\n" +
        "<value><string>http3test</string></value>\n" +
        "</data></array></value>\n" +
        "</member>\n" +
        "</struct></value>\n" +
        "</param>\n" +
        "<param>\n" +
        "<value><string>or</string></value>\n" +
        "</param>\n" +
        "</params>\n" +
        "</methodCall>"

    @Test
    fun xml2MethodCallTest() {
        val methodCall = XmlConvertUtil.xml2MethodCall(requestXml)
        Assertions.assertNotNull(methodCall)
    }

    @Test
    fun xml2MethodResponseTest() {
        val methodResponse = XmlConvertUtil.xml2MethodResponse(responseXmlStr)
        Assertions.assertNotNull(methodResponse)
    }
}
