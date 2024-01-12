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
import com.tencent.devops.project.pojo.enums.ProjectAuthSecrecyStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "项目-新增模型")
data class ProjectCreateInfo(
    @Schema(description = "项目名称")
    val projectName: String,
    @Schema(description = "英文缩写")
    val englishName: String,
    @Schema(description = "项目类型")
    val projectType: Int = 0,
    @Schema(description = "描述")
    val description: String,
    @Schema(description = "BGID")
    val bgId: Long = 0,
    @Schema(description = "BG名称")
    val bgName: String = "",
    @Schema(description = "业务线ID")
    val businessLineId: Long? = null,
    @Schema(description = "业务线名称")
    val businessLineName: String? = "",
    @Schema(description = "部门ID")
    val deptId: Long = 0,
    @Schema(description = "部门名称")
    val deptName: String = "",
    @Schema(description = "中心ID")
    val centerId: Long = 0,
    @Schema(description = "中心名称")
    val centerName: String = "",
    @Schema(description = "是否保密")
    var secrecy: Boolean = false,
    @Schema(description = "kind")
    val kind: Int = 0,
    @Schema(description = "项目相关配置")
    val properties: ProjectProperties? = null,
    @Schema(description = "项目最大可授权人员范围")
    var subjectScopes: List<SubjectScopeInfo>? = emptyList(),
    @Schema(description = "logo地址")
    val logoAddress: String? = null,
    @Schema(description = "项目性质")
    val authSecrecy: Int? = ProjectAuthSecrecyStatus.PUBLIC.value,
    @Schema(description = "是否可用")
    val enabled: Boolean = true,
    @Schema(description = "运营产品ID")
    val productId: Int? = null,
    @Schema(description = "运营产品名称")
    val productName: String? = null
)
