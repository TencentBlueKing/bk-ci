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

package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建模型-ID")
data class SubPipelineStartUpInfo(
    @ApiModelProperty("参数key值", required = true)
    val key: String,
    @ApiModelProperty("key值是否可以更改", required = true)
    val keyDisable: Boolean,
    @ApiModelProperty("key值前端组件类型", required = true)
    val keyType: String,
    @ApiModelProperty("key值获取方式", required = true)
    val keyListType: String,
    @ApiModelProperty("key值获取路径", required = true)
    val keyUrl: String,
    @ApiModelProperty
    val keyUrlQuery: List<String>,
    @ApiModelProperty("key值获取集合", required = true)
    val keyList: List<StartUpInfo>,
    @ApiModelProperty("key值是否多选", required = true)
    val keyMultiple: Boolean,
    @ApiModelProperty("参数value值", required = true)
    val value: Any,
    @ApiModelProperty("value值是否可以更改", required = true)
    val valueDisable: Boolean,
    @ApiModelProperty("value值前端组件类型", required = true)
    val valueType: String,
    @ApiModelProperty("value值获取方式", required = true)
    val valueListType: String,
    @ApiModelProperty("value值获取路径", required = true)
    val valueUrl: String,
    @ApiModelProperty
    val valueUrlQuery: List<String>,
    @ApiModelProperty("value值获取集合", required = true)
    val valueList: List<StartUpInfo>,
    @ApiModelProperty("value值是否多选", required = true)
    val valueMultiple: Boolean
)
