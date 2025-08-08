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

import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import io.swagger.v3.oas.annotations.media.Schema

data class PipelineVersionWithModelRequest(
    @get:Schema(title = "流水线ID（为空时导入并创建流水线）", required = false)
    val pipelineId: String?,
    @get:Schema(title = "草稿的来源版本（前端保存时传递）", required = true)
    val baseVersion: Int,
    @get:Schema(title = "流水线模型", required = true)
    val modelAndSetting: PipelineModelAndSetting?,
    @get:Schema(title = "流水线YAML编排（不为空时以YAML为准）", required = false)
    val yaml: String?,
    @get:Schema(title = "存储格式", required = false)
    val storageType: PipelineStorageType? = PipelineStorageType.MODEL,
    @get:Schema(title = "版本变更说明", required = false)
    val description: String? = null
)
