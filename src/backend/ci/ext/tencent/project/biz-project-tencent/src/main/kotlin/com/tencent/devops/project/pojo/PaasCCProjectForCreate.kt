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

package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModelProperty

data class PaasCCProjectForCreate(
    @ApiModelProperty("项目名称")
    val project_name: String,
    @ApiModelProperty("英文缩写")
    val english_name: String,
    @ApiModelProperty("项目类型")
    val project_type: Int,
    @ApiModelProperty("描述")
    val description: String,
    @ApiModelProperty("事业群ID")
    val bg_id: Long,
    @ApiModelProperty("事业群名字")
    val bg_name: String,
    @ApiModelProperty("部门ID")
    val dept_id: Long,
    @ApiModelProperty("部门名称")
    val dept_name: String,
    @ApiModelProperty("中心ID")
    val center_id: Long,
    @ApiModelProperty("中心名称")
    val center_name: String,
    @ApiModelProperty("是否保密")
    val is_secrecy: Boolean,
    @ApiModelProperty("kind")
    val kind: Int,
    @ApiModelProperty("项目ID")
    val project_id: String,
    @ApiModelProperty("创建人")
    val creator: String
)
