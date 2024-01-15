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

package com.tencent.devops.artifactory.pojo

import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "版本仓库-文件详细信息-APP")
data class FileDetailForApp(
    @Schema(name = "文件名", required = true)
    val name: String,
    @Schema(name = "平台", required = true)
    val platform: String,
    @Schema(name = "文件大小(byte)", required = true)
    val size: Long,
    @Schema(name = "创建时间", required = true)
    val createdTime: Long,
    @Schema(name = "项目", required = true)
    val projectName: String,
    @Schema(name = "流水线", required = true)
    val pipelineName: String,
    @Schema(name = "构件创建人", required = true)
    val creator: String,
    @Schema(name = "版本体验BundleIdentifier", required = true)
    val bundleIdentifier: String,
    @Schema(name = "logo链接", required = false)
    val logoUrl: String,
    @Schema(name = "文件路径", required = true)
    val path: String,
    @Schema(name = "文件全名", required = true)
    val fullName: String,
    @Schema(name = "文件全路径", required = true)
    val fullPath: String,
    @Schema(name = "仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @Schema(name = "修改时间", required = true)
    val modifiedTime: Long,
    @Schema(name = "md5", required = true)
    val md5: String,
    @Schema(name = "构建号", required = true)
    val buildNum: Int,
    @Schema(name = "nodeMetadata数据", required = true)
    val nodeMetadata: List<MetadataModel> = emptyList()
)
