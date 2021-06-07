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

package com.tencent.devops.common.webhook.util

import com.google.common.base.Splitter
import java.util.regex.Pattern

object WebhookUtils {

    private val separatorPattern = Pattern.compile("[,;]")

    fun convert(commaSeparatedString: String?): List<String> {
        if (commaSeparatedString == null) {
            return emptyList()
        }
        return Splitter.on(separatorPattern)
            .omitEmptyStrings()
            .trimResults()
            .split(commaSeparatedString)
            .toList()
    }

    fun getBranch(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }

    fun getTag(ref: String): String {
        return ref.removePrefix("refs/tags/")
    }

    fun getRelativePath(url: String): String {
        val urlArray = url.split("//")
        if (urlArray.size < 2) {
            return ""
        }

        val path = urlArray[1]
        val repoSplit = path.split("/")
        if (repoSplit.size < 4) {
            return ""
        }
        val domain = repoSplit[0]
        val first = repoSplit[1]
        val second = repoSplit[2]

        return path.removePrefix("$domain/$first/$second").removePrefix("/")
    }

    fun getFullPath(projectRelativePath: String, relativeSubPath: String): String {
        return ("${projectRelativePath.removeSuffix("/")}/" +
            relativeSubPath.removePrefix("/")).removePrefix("/")
    }
}
