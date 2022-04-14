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

package com.tencent.devops.auth.pojo.resource

import com.tencent.devops.auth.pojo.enum.SystemType
import io.swagger.annotations.ApiModelProperty

data class ResourceInfo(
    @ApiModelProperty("资源编码 不可修改")
    val resourceId: String,
    @ApiModelProperty("资源名称")
    val name: String,
    @ApiModelProperty("资源名称-英文")
    val englishName: String,
    @ApiModelProperty("资源描述")
    val desc: String,
    @ApiModelProperty("资源描述-英文")
    val englishDes: String,
    @ApiModelProperty("上级资源，蓝盾下默认所有的资源都是挂靠在项目下")
    val parent: String?,
    @ApiModelProperty("资源属于蓝盾下哪个系统:CI,REPO,CODECC,TURBO等")
    val system: SystemType,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("最后修改人")
    val updator: String?,
    @ApiModelProperty("创建时间")
    val creatorTime: Long,
    @ApiModelProperty("最后修改时间")
    val updateTime: Long?
)
