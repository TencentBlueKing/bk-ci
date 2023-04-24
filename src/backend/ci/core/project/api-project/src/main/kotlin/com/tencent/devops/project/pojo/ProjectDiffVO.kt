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

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
@ApiModel("项目-显示模型")
data class ProjectDiffVO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("项目ID")
    // @JsonProperty("project_id")
    val projectId: String,
    @ApiModelProperty("项目名称")
    // @JsonProperty("project_name")
    val projectName: String,
    @ApiModelProperty("审批中项目名称")
    // @JsonProperty("project_name")
    val afterProjectName: String,
    @ApiModelProperty("项目代码")
    // @JsonProperty("project_code")
    val projectCode: String,
    @ApiModelProperty("审批状态")
    // @JsonProperty("approval_status")
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
    val bgId: String?,
    @ApiModelProperty("审批中事业群ID")
    val afterBgId: String?,
    @ApiModelProperty("事业群名字")
    val bgName: String?,
    @ApiModelProperty("审批中事业群名字")
    val afterBgName: String?,
    @ApiModelProperty("中心ID")
    val centerId: String?,
    @ApiModelProperty("审批中中心ID")
    val afterCenterId: String?,
    @ApiModelProperty("中心名称")
    val centerName: String?,
    @ApiModelProperty("审批中中心名称")
    val afterCenterName: String?,
    @ApiModelProperty("部门ID")
    val deptId: String?,
    @ApiModelProperty("审批中部门ID")
    val afterDeptId: String?,
    @ApiModelProperty("部门名称")
    val deptName: String?,
    @ApiModelProperty("审批中部门名称")
    val afterDeptName: String?,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("审批中描述")
    val afterDescription: String?,
    @ApiModelProperty("英文缩写")
    val englishName: String,
    @ApiModelProperty("logo地址")
    val logoAddr: String?,
    @ApiModelProperty("审批中logo地址")
    val afterLogoAddr: String?,
    @ApiModelProperty("评论")
    val remark: String?,
    @ApiModelProperty("修改时间")
    // @JsonProperty("updated_at")
    val updatedAt: String?,
    @ApiModelProperty("修改人")
    val updator: String?,
    @ApiModelProperty("项目最大可授权人员范围")
    val subjectScopes: List<SubjectScopeInfo>?,
    @ApiModelProperty("审批中的项目最大可授权人员范围")
    val afterSubjectScopes: List<SubjectScopeInfo>?,
    @ApiModelProperty("项目性质")
    val authSecrecy: Int?,
    @ApiModelProperty("审批中项目性质")
    val afterAuthSecrecy: Int? = null,
    @ApiModelProperty("项目类型")
    val projectType: Int?,
    @ApiModelProperty("审批中项目类型")
    val afterProjectType: Int?
)
