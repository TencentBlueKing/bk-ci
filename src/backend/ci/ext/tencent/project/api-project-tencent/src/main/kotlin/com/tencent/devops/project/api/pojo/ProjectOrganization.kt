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

package com.tencent.devops.project.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目组织信息")
data class ProjectOrganization(
    @JsonProperty(value = "project_id", required = true)
    @ApiModelProperty("项目ID", name = "project_id")
    val projectId: String,
    @ApiModelProperty("项目名称", name = "project_name")
    @JsonProperty(value = "project_name", required = true)
    val projectName: String,
    @JsonProperty(value = "english_name", required = true)
    @ApiModelProperty("项目英文简称", name = "english_name")
    val projectEnglishName: String,
    @JsonProperty(value = "bg_id", required = true)
    @ApiModelProperty("项目所属一级机构ID", name = "bg_id")
    val bgId: Long,
    @JsonProperty(value = "bg_name", required = true)
    @ApiModelProperty("项目所属一级机构名称", name = "bg_name")
    val bgName: String,
    @JsonProperty(value = "dept_id", required = true)
    @ApiModelProperty("项目所属二级机构ID", name = "dept_id")
    val deptId: Long,
    @JsonProperty(value = "dept_name", required = true)
    @ApiModelProperty("项目所属二级机构名称", name = "dept_name")
    val deptName: String,
    @JsonProperty(value = "center_id", required = true)
    @ApiModelProperty("项目所属三级机构ID", name = "center_id")
    val centerId: Long,
    @JsonProperty(value = "center_name", required = true)
    @ApiModelProperty("项目所属三级机构名称", name = "center_name")
    val centerName: String,
    @JsonProperty(value = "project_type", required = false)
    @ApiModelProperty("项目类型", name = "project_type")
    val projectType: Int?
)
