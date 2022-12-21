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
import com.fasterxml.jackson.annotation.JsonProperty

@ApiModel("DynamicParameter模型-ID")
data class DynamicParameterInfo(
    @JsonProperty("id")
    val id: String, // 该行的唯一标识，必填
    @JsonProperty("paramModels")
    val paramModels: List<DynamicParameterInfoParam>
)

data class DynamicParameterInfoParam(
    @JsonProperty("value")
    val value: String? = null, // 值，可做为初始化的默认值
    @JsonProperty("disabled")
    val disabled: Boolean, // 控制是否可编辑
    @JsonProperty("id")
    val id: String, // 该模型的唯一标识，必填
    @JsonProperty("isMultiple")
    val isMultiple: Boolean? = null, // select是否多选
    @JsonProperty("label")
    val label: String? = null, // testLabel
    @JsonProperty("list")
    val list: List<StartUpInfo>? = null, // type是select起作用，需要有id和name字段
    @JsonProperty("listType")
    val listType: String? = null, // 获取列表方式，可以是url或者list
    @JsonProperty("type")
    val type: String, // 可以是input或者select
    @JsonProperty("url")
    val url: String? = null, // type是select且listType是url起作用
    @JsonProperty("dataPath")
    val dataPath: String? = null // 接口返回值，取数的路径，默认为 data.records
)
