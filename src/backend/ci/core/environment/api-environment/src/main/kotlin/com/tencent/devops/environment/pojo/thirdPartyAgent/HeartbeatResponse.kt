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

package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
@ApiModel("Agent心跳上报模型")
data class HeartbeatResponse(
    @ApiModelProperty("主版本")
    val masterVersion: String,
    @ApiModelProperty("从属版本")
    val slaveVersion: String,
    @ApiModelProperty("构建机状态")
    val AgentStatus: String,
    @ApiModelProperty("通道数量")
    val ParallelTaskCount: Int,
    @ApiModelProperty("环境变量")
    val envs: Map<String, String>,
    @ApiModelProperty("网关地址")
    val gateway: String? = "",
    @ApiModelProperty("文件网关路径")
    val fileGateway: String? = "",
    @ApiModelProperty("Agent的一些属性配置")
    val props: Map<String, Any>,
    @ApiModelProperty("docker最大任务数量")
    val dockerParallelTaskCount: Int,
    @ApiModelProperty("用户国际化语言")
    val language: String
)
