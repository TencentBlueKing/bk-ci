/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import io.swagger.v3.oas.annotations.media.Schema

data class ProjectApprovalInfo(
    @get:Schema(title = "项目名称")
    val projectName: String,
    @get:Schema(title = "项目类型")
    val approvalStatus: Int?,
    @get:Schema(title = "审批时间")
    val approvalTime: String?,
    @get:Schema(title = "审批人")
    val approver: String?,
    @get:Schema(title = "创建时间")
    val createdAt: String?,
    @get:Schema(title = "创建人")
    val creator: String?,
    @get:Schema(title = "事业群ID")
    val bgId: String?,
    @get:Schema(title = "事业群名字")
    val bgName: String?,
    @get:Schema(title = "业务线ID")
    val businessLineId: Long? = null,
    @get:Schema(title = "业务线名称")
    val businessLineName: String? = "",
    @get:Schema(title = "中心ID")
    val centerId: String?,
    @get:Schema(title = "中心名称")
    val centerName: String?,
    @get:Schema(title = "部门ID")
    val deptId: String?,
    @get:Schema(title = "部门名称")
    val deptName: String?,
    @get:Schema(title = "描述")
    val description: String?,
    @get:Schema(title = "英文缩写")
    val englishName: String,
    @get:Schema(title = "logo地址")
    val logoAddr: String?,
    @get:Schema(title = "修改人")
    val updator: String?,
    @get:Schema(title = "修改时间")
    val updatedAt: String?,
    @get:Schema(title = "项目最大可授权人员范围")
    val subjectScopes: List<SubjectScopeInfo>?,
    @get:Schema(title = "是否权限私密")
    val authSecrecy: Int,
    @get:Schema(title = "项目提示状态,0-不展示,1-展示创建成功,2-展示编辑成功")
    val tipsStatus: Int,
    @get:Schema(title = "项目性质")
    val projectType: Int?,
    @get:Schema(title = "运营产品ID")
    val productId: Int? = null,
    @get:Schema(title = "运营产品名称")
    val productName: String? = null,
    @get:Schema(title = "项目相关配置")
    val properties: ProjectProperties? = null
)
