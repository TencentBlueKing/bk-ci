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

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import io.swagger.annotations.ApiModelProperty

data class ProjectApprovalInfo(
    @ApiModelProperty("项目名称")
    // @JsonProperty("project_name")
    val projectName: String,
    @ApiModelProperty("项目类型")
    val approvalStatus: Int?,
    @ApiModelProperty("审批时间")
    // @JsonProperty("approval_time")
    val approvalTime: String?,
    @ApiModelProperty("审批人")
    val approver: String?,
    @ApiModelProperty("创建时间")
    // @JsonProperty("created_at")
    val createdAt: String?,
    @ApiModelProperty("创建人")
    val creator: String?,
    @ApiModelProperty("事业群ID")
    // @JsonProperty("bg_id")
    val bgId: String?,
    @ApiModelProperty("事业群名字")
    // @JsonProperty("bg_name")
    val bgName: String?,
    @ApiModelProperty("中心ID")
    // @JsonProperty("center_id")
    val centerId: String?,
    @ApiModelProperty("中心名称")
    // @JsonProperty("center_name")
    val centerName: String?,
    @ApiModelProperty("部门ID")
    // @JsonProperty("dept_id")
    val deptId: String?,
    @ApiModelProperty("部门名称")
    // @JsonProperty("dept_name")
    val deptName: String?,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("英文缩写")
    // @JsonProperty("english_name")
    val englishName: String,
    @ApiModelProperty("logo地址")
    // @JsonProperty("logo_addr")
    val logoAddr: String?,
    @ApiModelProperty("修改人")
    // @JsonProperty("updated_at")
    val updator: String?,
    @ApiModelProperty("修改时间")
    // @JsonProperty("updated_at")
    val updatedAt: String?,
    @ApiModelProperty("项目最大可授权人员范围")
    val subjectScopes: List<SubjectScopeInfo>?,
    @ApiModelProperty("是否权限私密")
    val authSecrecy: Int,
    @ApiModelProperty("项目提示状态,0-不展示,1-展示创建成功,2-展示编辑成功")
    val tipsStatus: Int,
    @ApiModelProperty("项目性质")
    val projectType: Int?
)
