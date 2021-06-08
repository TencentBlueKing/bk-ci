/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.npm.util

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.npm.constant.MAINTAINERS
import com.tencent.bkrepo.npm.constant.PACKAGE
import com.tencent.bkrepo.npm.constant.STAR_USERS
import com.tencent.bkrepo.npm.pojo.metadata.NpmVersionMetadata
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object NpmUtils {
    private const val TARBALL_PATH = "/%s/%s/%s-%s.tgz"

    fun formatPackageName(name: String, scope: String? = null): String {
        val builder = StringBuilder()
        scope?.let { builder.append(StringPool.AT).append(it).append(StringPool.SLASH) }
        return builder.append(name).toString()
    }
    fun formatTarballPath(packageName: String, version: String, delimiter: String = "-"): String {
        return TARBALL_PATH.format(packageName, delimiter, packageName, version)
    }

    fun resolveVersionName(filename: String, name: String): String {
        return filename.substringBeforeLast(".tgz").substringAfter("$name-")
    }

    fun formatDate(date: LocalDateTime): String {
        return date.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    fun getCompatiblePath(downloadPath: String): String {
        return if (downloadPath.contains("/download/")) {
            downloadPath.replace("/download/", "/-/")
        } else {
            downloadPath.replace("/-/", "/download/")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveMaintainers(extension: Map<String, Any>): MutableList<Map<String, String>> {
        return (extension[MAINTAINERS] as MutableList<Map<String, String>>?) ?: mutableListOf(emptyMap())
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveStarUsers(extension: Map<String, Any>): List<String> {
        return (extension[STAR_USERS] as List<String>?).orEmpty()
    }

    /**
     * 从[versionPackage]中解析[NpmVersionMetadata]
     */
    fun resolveVersionMetadata(versionPackage: PackageVersion): NpmVersionMetadata {
        return versionPackage.extension[PACKAGE].toString().readJsonString()
    }
}
