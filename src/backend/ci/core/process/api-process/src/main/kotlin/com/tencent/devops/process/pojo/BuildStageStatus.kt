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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "历史构建阶段状态")
data class BuildStageStatus(
    @get:Schema(title = "阶段ID", required = true)
    val stageId: String,
    @get:Schema(title = "阶段名称", required = true)
    val name: String,
    @get:Schema(title = "阶段状态", required = false, readOnly = true)
    var status: String? = null,
    @get:Schema(title = "阶段标签", required = false, readOnly = true)
    var tag: List<String>? = null,
    @get:Schema(title = "阶段启动时间", required = false, readOnly = true)
    var startEpoch: Long? = null,
    @get:Schema(title = "容器运行时间", required = false, readOnly = true)
    var elapsed: Long? = null,
    @get:Schema(title = "各项耗时", required = true)
    var timeCost: BuildRecordTimeCost? = null,
    @get:Schema(title = "前端", required = false, readOnly = true)
    var showMsg: String? = null
)
