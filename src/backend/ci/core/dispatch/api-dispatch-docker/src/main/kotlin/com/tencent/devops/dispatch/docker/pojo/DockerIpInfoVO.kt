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

package com.tencent.devops.dispatch.docker.pojo

import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DockerIpInfoVO")
data class DockerIpInfoVO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("构建机IP")
    val dockerIp: String,
    @ApiModelProperty("构建机PORT")
    val dockerHostPort: Int,
    @ApiModelProperty("构建机容量")
    val capacity: Int,
    @ApiModelProperty("构建机已使用量")
    val usedNum: Int,
    @ApiModelProperty("构建机CPU负载")
    val averageCpuLoad: Int,
    @ApiModelProperty("构建机内存负载")
    val averageMemLoad: Int,
    @ApiModelProperty("构建机硬盘负载")
    val averageDiskLoad: Int,
    @ApiModelProperty("构建机硬盘IO负载")
    val averageDiskIOLoad: Int,
    @ApiModelProperty("构建机是否可用")
    val enable: Boolean,
    @ApiModelProperty("是否为灰度节点", required = false)
    val grayEnv: Boolean?,
    @ApiModelProperty("是否为专用机独占", required = false)
    val specialOn: Boolean?,
    @ApiModelProperty("创建时间", required = false)
    val createTime: String?,
    @ApiModelProperty("构建集群", required = false)
    val clusterType: DockerHostClusterType? = DockerHostClusterType.COMMON
)
