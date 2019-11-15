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

package com.tencent.devops.common.api.util

import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.net.URLEncoder
import java.util.logging.Level
import java.util.logging.Logger

object EncodeUtils {

    private val logger = Logger.getLogger(EncodeUtils::class.java.name)

    fun encodeYml(yml: String): String {
        val sb = StringBuilder()

        val br = BufferedReader(StringReader(yml))

        try {
            var line: String? = br.readLine()
            while (line != null) {
                if (line.trim { it <= ' ' }.startsWith("-")) {
                    line = encodeLine(line)
                }
                sb.append(line).append("\n")
                line = br.readLine()
            }

            return sb.toString()
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Fail to encode the yml - " + yml, e)
            throw e
        }
    }

    fun decodeYml(yml: String): String {
        val sb = StringBuilder()

        val br = BufferedReader(StringReader(yml))

        try {
            var line: String? = br.readLine()
            while (line != null) {
                if (line.trim { it <= ' ' }.startsWith("-")) {
                    line = decodeLine(line)
                }
                sb.append(line).append("\n")
                line = br.readLine()
            }

            return sb.toString()
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Fail to decode the yml - " + yml, e)
            throw e
        }
    }

    fun encodeLine(line: String): String {
        return line.replace(": ", URLEncoder.encode(": ", "utf-8"))
    }

    fun decodeLine(line: String): String {
        return line.replace(URLEncoder.encode(": ", "utf-8"), ": ")
    }
}