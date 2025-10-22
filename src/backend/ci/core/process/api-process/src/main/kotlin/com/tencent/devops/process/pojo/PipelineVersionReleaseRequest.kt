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

import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import io.swagger.v3.oas.annotations.media.Schema

data class PipelineVersionReleaseRequest(
    @get:Schema(title = "是否本次开启PAC", required = true)
    var enablePac: Boolean,
    @get:Schema(title = "版本描述", required = false)
    var description: String? = null,
    @get:Schema(title = "模板版本号（为空时默认最新）", required = false)
    var targetAction: CodeTargetAction?,
    @get:Schema(title = "静态流水线组", required = false)
    var staticViews: List<String> = emptyList(),
    @get:Schema(title = "流水线YAML信息", required = false)
    val yamlInfo: PipelineYamlVo?,
    @get:Schema(title = "提交到指定的分支", required = false)
    val targetBranch: String? = null
)
