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

@Suppress("MagicNumber", "ReturnCount")
object RegexUtils {

    private const val line = 2
    private const val domain = 3
    private const val liPort = 4
    private const val context = 5
    private val httpContextPathRegex = Regex("(http[s]?:)(//)([-.a-z0-9A-Z]+)(:\\d+)?(/.*)")

    /**
     * 解析 [url], 并且返回 域名(xx.xx.xx or xx.xx.xx) 以及 ContextPath绝对路径(/ or /xx/yy...)
     * 不合法的 [url] 将会返回 null
     * eg: url = https://xx.xx.xx/a/b/c.txt return xx.xx.xx and /a/b/c.txt
     */
    fun splitDomainContextPath(url: String): Pair<String/*http domain*/, String/*context path*/>? {
        val groups = httpContextPathRegex.find(url)?.groups
        if (groups != null && groups.size == 6) {
            return groups[domain]!!.value to groups[context]!!.value
        }
        return null
    }

    /**
     * 解析[url] 将协议头去掉， 比如 https://xx.xx.xx:1234/a/b/c 返回 //xx.xx.xx:1234/a/b/c 表示遵守当前主站的协议
     */
    fun trimProtocol(url: String?): String? {
        if (url.isNullOrBlank()) {
            return url
        }
        val groups = httpContextPathRegex.find(url)?.groups ?: return url
        return if (groups.size == 6) {
            groups[line]!!.value + groups[domain]!!.value + (groups[liPort]?.value ?: "") + groups[context]!!.value
        } else {
            url
        }
    }
}
