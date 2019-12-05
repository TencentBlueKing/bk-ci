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

package com.tencent.devops.store.pojo.container

import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.store.pojo.app.ContainerAppWithVersion
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线-构建容器接口响应信息")
data class ContainerResp(
    @ApiModelProperty("构建容器ID", required = true)
    val id: String,
    @ApiModelProperty("构建容器名称", required = true)
    val name: String,
    @ApiModelProperty("流水线容器类型", required = true)
    var type: String,
    @ApiModelProperty("操作系统", required = true)
    val baseOS: String,
    @ApiModelProperty("容器是否为必需", required = true)
    val required: String,
    @ApiModelProperty("最长排队时间", required = false)
    val maxQueueMinutes: Int?,
    @ApiModelProperty("最长运行时间", required = false)
    val maxRunningMinutes: Int?,
    @ApiModelProperty("默认的构建资源，当操作系统为linux时返回", required = false)
    val defaultPublicBuildResource: String?,
    @ApiModelProperty("支持的构建资源类型", required = false)
    val typeList: List<ContainerBuildType>?,
    @ApiModelProperty("默认的构建资源类型", required = false)
    val defaultBuildType: BuildType?,
    @ApiModelProperty("自定义扩展容器前端表单属性字段的Json串", required = false)
    val props: Map<String, Any>?,
    @ApiModelProperty("编译环境信息", required = false)
    val apps: List<ContainerAppWithVersion>?,
    @ApiModelProperty("支持的构建资源", required = false)
    val resources: Map<BuildType, ContainerResource>?
)