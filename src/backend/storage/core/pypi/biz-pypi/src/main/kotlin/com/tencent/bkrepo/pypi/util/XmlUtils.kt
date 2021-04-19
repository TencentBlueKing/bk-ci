/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.pypi.util

import com.tencent.bkrepo.pypi.artifact.xml.XmlConvertUtil
import com.tencent.bkrepo.pypi.exception.PypiSearchParamException
import com.tencent.bkrepo.pypi.pojo.PypiSearchPojo
import java.io.BufferedReader

object XmlUtils {
    fun BufferedReader.readXml(): String {
        val stringBuilder = StringBuilder("")
        var mark: String?

        while (this.readLine().also { mark = it } != null) {
            stringBuilder.append(mark)
        }
        return stringBuilder.toString()
    }

    fun getPypiSearchPojo(xmlString: String): PypiSearchPojo {
        val methodCall = XmlConvertUtil.xml2MethodCall(xmlString)
        val action = methodCall.methodName
        val name = methodCall.params.paramList[0]
            .value.struct?.memberList?.get(0)?.value?.array?.data?.valueList?.get(0)?.string

        val summary = methodCall.params.paramList[0]
            .value.struct?.memberList?.get(1)?.value?.array?.data?.valueList?.get(0)?.string

        val operation = methodCall.params.paramList[1]
            .value.string ?: throw PypiSearchParamException("can not found `operation` param")
        return PypiSearchPojo(action, name, summary, operation)
    }
}
