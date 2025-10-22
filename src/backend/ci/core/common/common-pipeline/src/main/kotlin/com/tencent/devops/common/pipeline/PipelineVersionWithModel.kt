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

package com.tencent.devops.common.pipeline

import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import io.swagger.v3.oas.annotations.media.Schema

data class PipelineVersionWithModel(
    @get:Schema(title = "版本号（流水线唯一递增）", required = true)
    val version: Int,
    @get:Schema(title = "版本名称", required = true)
    val versionName: String?,
    @get:Schema(title = "该版本的源版本号", required = true)
    val baseVersion: Int?,
    @get:Schema(title = "该版本的版本号名", required = true)
    val baseVersionName: String?,
    @get:Schema(title = "流水线模型", required = true)
    val modelAndSetting: PipelineModelAndSetting,
    @get:Schema(title = "流水线YAML编排（含高亮）", required = false)
    val yamlPreview: PreviewResponse?,
    @get:Schema(title = "是否处在可以调试状态", required = false)
    val canDebug: Boolean?,
    @get:Schema(title = "版本变更说明", required = false)
    val description: String?,
    @get:Schema(title = "是否支持YAML解析", required = true)
    val yamlSupported: Boolean,
    @get:Schema(title = "YAML解析异常信息")
    val yamlInvalidMsg: String?,
    @get:Schema(title = "更新操作人", required = true)
    val updater: String?,
    @get:Schema(title = "版本修改时间", required = true)
    val updateTime: Long?
)
