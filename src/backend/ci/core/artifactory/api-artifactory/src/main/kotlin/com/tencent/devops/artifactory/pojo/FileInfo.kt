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

package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本仓库-文件信息")
data class FileInfo(
    @get:Schema(title = "文件名", required = true)
    val name: String,
    @get:Schema(title = "文件全名", required = true)
    val fullName: String,
    @get:Schema(title = "文件路径", required = true)
    val path: String,
    @get:Schema(title = "文件全路径", required = true)
    val fullPath: String,
    @get:Schema(title = "文件大小(byte)", required = true)
    val size: Long,
    @get:Schema(title = "是否文件夹", required = true)
    val folder: Boolean,
    @get:Schema(title = "更新时间", required = true)
    val modifiedTime: Long,
    @get:Schema(title = "仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @get:Schema(title = "元数据", required = true)
    val properties: List<Property>?,
    @get:Schema(title = "app版本", required = true)
    val appVersion: String? = null,
    @get:Schema(title = "下载短链接", required = true)
    val shortUrl: String? = null,
    @get:Schema(title = "下载链接", required = false)
    var downloadUrl: String? = null,
    @get:Schema(title = "MD5", required = false)
    var md5: String? = null,
    @get:Schema(title = "docker registry", required = false)
    var registry: String? = null
) : Comparable<FileInfo> {
    constructor(
        name: String,
        fullName: String,
        path: String,
        fullPath: String,
        size: Long,
        folder: Boolean,
        modifiedTime: Long,
        artifactoryType: ArtifactoryType
    ) :
        this(name = name,
            fullName = fullName,
            path = path,
            fullPath = fullPath,
            size = size,
            folder = folder,
            modifiedTime = modifiedTime,
            artifactoryType = artifactoryType,
            properties = null)

    override fun compareTo(other: FileInfo): Int {
        val thisLevel = level(this.name)
        val otherLevel = level(other.name)
        return when {
            thisLevel > otherLevel -> 1
            thisLevel < otherLevel -> -1
            else -> this.name.toLowerCase().compareTo(other.name.toLowerCase())
        }
    }

    private fun level(name: String): Int {
        return when {
            name.endsWith(".shell.apk") || name.endsWith("_enterprise_sign.ipa") -> -2
            name.endsWith(".apk") || name.endsWith(".ipa") -> -1
            else -> 0
        }
    }
}
