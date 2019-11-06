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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.measure.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("度量数据-原子数据")
data class ElementMeasureData(
    @ApiModelProperty("原子id", required = true)
    val id: String,
    @ApiModelProperty("原子名称", required = true)
    val name: String,
    @ApiModelProperty("工程ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("原子构建结果", required = true)
    val status: BuildStatus,
    @ApiModelProperty("原子构建启动时间", required = true)
    val beginTime: Long,
    @ApiModelProperty("结束时间", required = true)
    val endTime: Long,
    @ApiModelProperty("Element type", required = true)
    val type: String,
    @ApiModelProperty("atomCode", required = false)
    val atomCode: String = "",
    @ApiModelProperty("templateId", required = false)
    val templateId: String? = "",
    @ApiModelProperty("额外信息", required = false)
    var extraInfo: String? = null,
    @ApiModelProperty("错误类型", required = false)
    var errorType: String? = null,
    @ApiModelProperty("错误码标识", required = false)
    var errorCode: Int? = null,
    @ApiModelProperty("错误描述", required = false)
    var errorMsg: String? = null
)