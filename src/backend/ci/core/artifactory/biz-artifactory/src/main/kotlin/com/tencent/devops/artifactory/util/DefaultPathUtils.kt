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

package com.tencent.devops.artifactory.util

import com.tencent.devops.common.api.util.UUIDUtil
import java.io.File
import java.nio.file.Files
import java.util.regex.Pattern

@Suppress("ALL")
object DefaultPathUtils {
    private const val DEFAULT_EXTENSION = "temp"

    fun isFolder(path: String): Boolean {
        return path.endsWith("/")
    }

    fun getParentFolder(path: String): String {
        val tmpPath = path.removeSuffix("/")
        return tmpPath.removeSuffix(getFileName(tmpPath))
    }

    fun getFileName(path: String): String {
        return path.removeSuffix("/").split("/").last()
    }

    fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf(".")
        return if (lastDotIndex == -1) {
            ""
        } else {
            fileName.substring(lastDotIndex + 1)
        }
    }

    fun randomFile(fileExtension: String = DEFAULT_EXTENSION): File {
        val suffix = if (fileExtension.isBlank()) "" else ".$fileExtension"
        return Files.createTempFile(UUIDUtil.generate(), suffix).toFile()
    }

    fun randomFileName(fileExtension: String = DEFAULT_EXTENSION): String {
        val suffix = if (fileExtension.isBlank()) "" else ".$fileExtension"
        return "${UUIDUtil.generate()}$suffix"
    }

    fun resolvePipelineId(path: String): String {
        val roads = path.removePrefix("/").split("/")
        if (roads.size < 2) {
            throw IllegalArgumentException("Path $path doesn't contain pipelineId")
        }
        return roads[0]
    }

    fun resolveBuildId(path: String): String {
        val roads = path.removePrefix("/").split("/")
        if (roads.size < 3) {
            throw IllegalArgumentException("Path $path doesn't contain buildId")
        }
        return roads[1]
    }

    /**
     * 根据manifest文件路径解析镜像名称和版本
     * eg: /jq/manifest/1.0.0/manifest.json
     */
    fun getImageNameAndVersion(manifestPath: String): Pair<String, String> {
        val pattern = Pattern.compile("/(.*)/manifest/(.*)/manifest.json")
        val matcher = pattern.matcher(manifestPath)
        if (!matcher.find()) {
            throw IllegalArgumentException("illegal manifestPath: $manifestPath")
        }
        return Pair(matcher.group(1), matcher.group(2))
    }
}
