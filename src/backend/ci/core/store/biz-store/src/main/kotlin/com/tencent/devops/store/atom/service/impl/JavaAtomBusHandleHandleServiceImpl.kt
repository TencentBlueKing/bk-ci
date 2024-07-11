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

package com.tencent.devops.store.atom.service.impl

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.store.atom.service.AtomBusHandleService
import java.util.regex.Pattern

class JavaAtomBusHandleHandleServiceImpl : AtomBusHandleService {

    override fun handleOsName(osName: String): String {
        return if (osName.uppercase() == OSType.MAC_OS.name) {
            "darwin"
        } else {
            osName.lowercase()
        }
    }

    override fun handleOsArch(osName: String, osArch: String): String {
        // worker就是通过java取的osArch，故无需转换
        return osArch
    }

    override fun handleTarget(reqTarget: String?, target: String): String {
        if (reqTarget.isNullOrBlank()) {
            return target
        }
        val javaPath = target.substringBefore(" -").trim()
        // 获取插件配置的JVM指令
        val pattern = Pattern.compile(" -[^\\s-]*(?=\\s|$)")
        val matcher = pattern.matcher(target)
        val builder = StringBuilder()
        // 执行匹配并将jvm参数连接成字符串
        while (matcher.find()) {
            if (builder.isNotEmpty()) {
                builder.append(" ")
            }
            builder.append(matcher.group())
        }
        // 获取插件jar包路径
        val jarPath = reqTarget.substringAfter("-jar").trim()

        return "$javaPath $builder $jarPath".trim()
    }
}
