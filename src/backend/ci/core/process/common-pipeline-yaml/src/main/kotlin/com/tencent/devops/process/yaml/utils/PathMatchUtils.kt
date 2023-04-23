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

package com.tencent.devops.process.yaml.utils

import com.tencent.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object PathMatchUtils {
    private val logger = LoggerFactory.getLogger(PathMatchUtils::class.java)
    fun isIncludePathMatch(
        pathList: List<String>?,
        fileChangeSet: Set<String>?,
        doCheck: Boolean = true,
        event: ModelCreateEvent? = null
    ): Boolean {
        if (pathList.isNullOrEmpty() || !doCheck) {
            return true
        }

        pathList.forEach {
            if (it.endsWith("*")) {
                logger.info(
                    "PathMatchUtils|path_end_with_*|" +
                        "$pathList|${event?.projectCode}|${event?.pipelineInfo?.pipelineId}"
                )
                return@forEach
            }
        }

        fileChangeSet?.forEach { path ->
            pathList.forEach { includePath ->
                if (isPathMatch(path, includePath)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if the path match
     * example:
     * fullPath: a/1.txt
     * prefixPath: a/
     */
    private fun isPathMatch(fullPath: String, prefixPath: String): Boolean {
        val fullPathList = fullPath.removePrefix("/").split("/")
        val prefixPathList = prefixPath.removePrefix("/").split("/")
        if (fullPathList.size < prefixPathList.size) {
            return false
        }

        for (i in prefixPathList.indices) {
            val pattern = Pattern.compile(prefixPathList[i].replace("*", "\\S*"))
            val matcher = pattern.matcher(fullPathList[i])
            if (prefixPathList[i] != "*" && !matcher.matches()) {
                return false
            }
        }

        return true
    }

    /**
     * 使用glob模式进行匹配.
     * 预留入口，暂未使用
     */
    private fun isGlobPathMatch(fullPath: String, prefixPath: String): Boolean {
        val pattern = Pattern.compile(GlobsUtils.toUnixRegexPattern(prefixPath))
        val matcher = pattern.matcher(fullPath)
        if (!matcher.matches()) {
            return false
        }
        return true
    }
}
