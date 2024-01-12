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
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
@Schema(name = "项目-显示模型")
data class ProjectDiffVO(
    @Schema(name = "主键ID")
    val id: Long,
    @Schema(name = "项目ID")
    val projectId: String,
    @Schema(name = "项目名称")
    val projectName: String,
    @Schema(name = "审批中项目名称")
    val afterProjectName: String,
    @Schema(name = "项目代码")
    val projectCode: String,
    @Schema(name = "审批状态")
    val approvalStatus: Int?,
    @Schema(name = "审批时间")
    val approvalTime: String?,
    @Schema(name = "审批人")
    val approver: String?,
    @Schema(name = "创建时间")
    val createdAt: String?,
    @Schema(name = "创建人")
    val creator: String?,
    @Schema(name = "事业群ID")
    val bgId: String?,
    @Schema(name = "审批中事业群ID")
    val afterBgId: String?,
    @Schema(name = "事业群名字")
    val bgName: String?,
    @Schema(name = "审批中事业群名字")
    val afterBgName: String?,
    @Schema(name = "事业线ID")
    val businessLineId: String?,
    @Schema(name = "审批中事业线ID")
    val afterBusinessLineId: Long?,
    @Schema(name = "事业线名称")
    val businessLineName: String?,
    @Schema(name = "审批中事业线名称")
    val afterBusinessLineName: String?,
    @Schema(name = "中心ID")
    val centerId: String?,
    @Schema(name = "审批中中心ID")
    val afterCenterId: String?,
    @Schema(name = "中心名称")
    val centerName: String?,
    @Schema(name = "审批中中心名称")
    val afterCenterName: String?,
    @Schema(name = "部门ID")
    val deptId: String?,
    @Schema(name = "审批中部门ID")
    val afterDeptId: String?,
    @Schema(name = "部门名称")
    val deptName: String?,
    @Schema(name = "审批中部门名称")
    val afterDeptName: String?,
    @Schema(name = "描述")
    val description: String?,
    @Schema(name = "审批中描述")
    val afterDescription: String?,
    @Schema(name = "英文缩写")
    val englishName: String,
    @Schema(name = "logo地址")
    val logoAddr: String?,
    @Schema(name = "审批中logo地址")
    val afterLogoAddr: String?,
    @Schema(name = "评论")
    val remark: String?,
    @Schema(name = "修改时间")
    val updatedAt: String?,
    @Schema(name = "修改人")
    val updator: String?,
    @Schema(name = "项目最大可授权人员范围")
    val subjectScopes: List<SubjectScopeInfo>?,
    @Schema(name = "审批中的项目最大可授权人员范围")
    val afterSubjectScopes: List<SubjectScopeInfo>?,
    @Schema(name = "项目性质")
    val authSecrecy: Int?,
    @Schema(name = "审批中项目性质")
    val afterAuthSecrecy: Int? = null,
    @Schema(name = "项目类型")
    val projectType: Int?,
    @Schema(name = "审批中项目类型")
    val afterProjectType: Int?,
    @Schema(name = "运营产品ID")
    val productId: Int? = null,
    @Schema(name = "审批中运营产品ID")
    val afterProductId: Int? = null
)
