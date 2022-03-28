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
 *
 */

package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目consul tag修改入参")
data class ProjectTagUpdateDTO(
    @JsonProperty(value = "routerTag", required = true)
    @ApiModelProperty("项目对应的router tags", name = "routerTag")
    val routerTag: String,
    @JsonProperty(value = "projectCodeList", required = false)
    @ApiModelProperty("项目编码集合", name = "projectCodeList")
    val projectCodeList: List<String>?,
    @JsonProperty(value = "bgId", required = false)
    @ApiModelProperty("BgId", name = "bgId")
    val bgId: Long?,
    @JsonProperty(value = "centerId", required = false)
    @ApiModelProperty("centerId", name = "centerId")
    val centerId: Long?,
    @JsonProperty(value = "deptId", required = false)
    @ApiModelProperty("deptId", name = "deptId")
    val deptId: Long?,
    @JsonProperty(value = "channel", required = false)
    @ApiModelProperty("channel", name = "channel")
    val channel: String?
)
