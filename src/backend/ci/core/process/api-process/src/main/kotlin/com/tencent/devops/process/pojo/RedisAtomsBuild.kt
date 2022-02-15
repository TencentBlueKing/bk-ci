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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.api.pojo.Zone
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("")
data class RedisAtomsBuild(
    @ApiModelProperty("构建机名称标识，不唯一", required = false)
    val vmName: String,
    @ApiModelProperty("项目id", required = false)
    val projectId: String,
    @ApiModelProperty("流水线id", required = false)
    val pipelineId: String,
    @ApiModelProperty("构建号id", required = false)
    val buildId: String,
    @ApiModelProperty("Job序号，根据编排（忽略掉第1个触发Job）从左到右，从上到下，从1开始", required = false)
    val vmSeqId: String,
    @ApiModelProperty("流水线的来源，见ChannelCode枚举说明", required = false)
    val channelCode: String?,
    @ApiModelProperty("SVN代码库所在区域，作废字段", required = false)
    val zone: Zone?,
    @ApiModelProperty("用到的插件code和路径键值对", required = false)
    val atoms: Map<String, String> = mapOf(), // 用插件框架开发的插件信息 key为插件code，value为下载路径
    @ApiModelProperty("执行次数，第1次执行为1，重试>1", required = false)
    val executeCount: Int? = 1,
    @ApiModelProperty("构建用户id", required = false)
    val userId: String?
)
