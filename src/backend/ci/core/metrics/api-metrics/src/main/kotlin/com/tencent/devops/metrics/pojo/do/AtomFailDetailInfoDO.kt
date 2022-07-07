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

package com.tencent.devops.metrics.pojo.`do`

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("插件失败详情信息")
data class AtomFailDetailInfoDO(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("流水线ID")
    val pipelineId: String,
    @ApiModelProperty("流水线名称")
    val pipelineName: String,
    @ApiModelProperty("渠道代码")
    val channelCode: String,
    @ApiModelProperty("域名")
    var domain: String? = null,
    @ApiModelProperty("构建ID")
    val buildId: String,
    @ApiModelProperty("构建序号")
    val buildNum: Int,
    @ApiModelProperty("插件代码")
    val atomCode: String,
    @ApiModelProperty("插件名称")
    val atomName: String,
    @ApiModelProperty("插件在model中的位置")
    val atomPosition: String,
    @ApiModelProperty("插件分类代码")
    val classifyCode: String,
    @ApiModelProperty("启动用户")
    val startUser: String,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("启动时间")
    val startTime: LocalDateTime?,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("结束时间")
    val endTime: LocalDateTime?,
    @ApiModelProperty("错误的类型标识")
    val errorType: Int?,
    @ApiModelProperty("错误的类型标识名称")
    var errorTypeName: String? = null,
    @ApiModelProperty("错误的标识码")
    val errorCode: Int?,
    @ApiModelProperty("错误描述")
    val errorMsg: String?
)
