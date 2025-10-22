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

import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.transfer.TransferMark
import com.tencent.devops.common.pipeline.pojo.transfer.TransferResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线互转-Response-result")
data class TransferResponseResult(
    @get:Schema(title = "modelAndSetting")
    val modelAndSetting: PipelineModelAndSetting? = null,
    @get:Schema(title = "当前yaml内容")
    val newYaml: String? = null,
    @get:Schema(title = "定位")
    val mark: TransferMark? = null,
    @get:Schema(title = "互转报错信息")
    val error: String? = null,
    @get:Schema(title = "是否支持YAML解析", required = true)
    val yamlSupported: Boolean = true,
    @get:Schema(title = "YAML解析异常信息")
    val yamlInvalidMsg: String? = null
) {
    constructor(transfer: TransferResponse) : this(
        modelAndSetting = transfer.modelAndSetting,
        newYaml = transfer.yamlWithVersion?.yamlStr,
        mark = transfer.mark,
        error = transfer.error,
        yamlSupported = transfer.yamlSupported,
        yamlInvalidMsg = transfer.yamlInvalidMsg
    )
}
