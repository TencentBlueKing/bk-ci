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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.service.utils

import javax.servlet.http.HttpServletRequest

object CookieUtil {
    fun getCookieValue(request: HttpServletRequest, name: String): String? {

        // cookie数组
        val cookies = request.cookies
        if (null != cookies) {
            for (cookie in cookies) {
                if (cookie.name == name) {
                    return cookie.value
                }
            }
        }

        var value: String? = null
        // Cookie属性中没有获取到，那么从Headers里面获取
        var cookieStr: String? = request.getHeader("Cookie")
        if (cookieStr != null) {
            // 去掉所有空白字符，不限于空格
            cookieStr = cookieStr.replace("\\s*".toRegex(), "")
            val cookieArr = cookieStr.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (cookieItem in cookieArr) {
                val cookieItemArr = cookieItem.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (cookieItemArr[0] == name) {
                    value = cookieItemArr[1]
                    break
                }
            }
        }
        return value
    }
}