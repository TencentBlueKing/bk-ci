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

package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

/**
 *  {
 *     "bg_id": 956,
 *     "bg_name": "",
 *     "cc_app_id": 215,
 *     "center_id": 17633,
 *     "center_name": "",
 *     "created_at": "2017-12-12T15:34:14+08:00",
 *     "creator": "",
 *     "data_id": 0,
 *     "deploy_type": "[2]",
 *     "dept_id": 967,
 *     "dept_name": "",
 *     "description": "",
 *     "english_name": "a90",
 *     "is_offlined": false,
 *     "kind": 2,
 *     "logo_addr": "",
 *     "project_id": "31d94d9e090642668f1775fdb5263efb",
 *     "project_name": "测试",
 *     "project_type": 5,
 *     "updated_at": "2018-01-22T11:03:51+08:00",
 *     "use_bk": true,
 *     "approval_status":"1"//1-审核中 2-已审批 3-已驳回
 *   }
 */
data class BkAuthProject(
    @JsonProperty("bg_id", required = true)
    @ApiModelProperty(name = "bg_id")
    val bgId: Int,
    @JsonProperty("bg_name", required = true)
    @ApiModelProperty(name = "bg_name")
    val bgName: String,
    @JsonProperty("cc_app_id", required = true)
    @ApiModelProperty(name = "cc_app_id")
    val ccAppId: String,
    @JsonProperty("center_id", required = true)
    @ApiModelProperty(name = "center_id")
    val centerId: String,
    @JsonProperty("center_name", required = true)
    @ApiModelProperty(name = "center_name")
    val centerName: String,
    @JsonProperty("created_at", required = true)
    @ApiModelProperty(name = "created_at")
    val createdAt: String,
    @JsonProperty("creator", required = true)
    @ApiModelProperty(name = "creator")
    val creator: String,
    @JsonProperty("dept_id", required = true)
    @ApiModelProperty(name = "dept_id")
    val deptId: Int,
    @JsonProperty("dept_name", required = true)
    @ApiModelProperty(name = "dept_name")
    val deptName: String,
    @JsonProperty("description", required = true)
    @ApiModelProperty(name = "description")
    val description: String,
    @JsonProperty("english_name", required = true)
    @ApiModelProperty(name = "english_name")
    val projectCode: String,
    @get:JsonProperty("is_offlined", required = true)
    @ApiModelProperty(name = "is_offlined")
    val isOfflined: Boolean,
    @JsonProperty("logo_addr", required = true)
    @ApiModelProperty(name = "logo_addr")
    val logoAddr: String,
    @JsonProperty("project_id", required = true)
    @ApiModelProperty(name = "project_id")
    val projectId: String,
    @JsonProperty("project_name", required = true)
    @ApiModelProperty(name = "project_name")
    val projectName: String,
    @JsonProperty("project_type", required = true)
    @ApiModelProperty(name = "project_type")
    val projectType: Int,
    @JsonProperty("updated_at", required = true)
    @ApiModelProperty(name = "updated_at")
    val updatedAt: String,
    @JsonProperty("use_bk", required = true)
    @ApiModelProperty(name = "use_bk")
    val useBk: Boolean,
    @JsonProperty("approval_status", required = true)
    @ApiModelProperty(name = "approval_status")
    val approvalStatus: String
)
