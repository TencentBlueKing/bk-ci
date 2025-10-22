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
 */

package com.tencent.devops.project

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.project.pojo.ProjectProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目信息响应信息体")
data class ProjectInfoResponse(
    @JsonProperty(value = "project_id", required = true)
    @get:Schema(title = "项目ID", description = "project_id")
    val projectId: String,
    @get:Schema(title = "项目名称", description = "project_name")
    @JsonProperty(value = "project_name", required = true)
    val projectName: String,
    @JsonProperty(value = "english_name", required = true)
    @get:Schema(title = "项目英文简称", description = "english_name")
    val projectEnglishName: String,
    @JsonProperty(value = "creator_bg_name", required = true)
    @get:Schema(title = "注册人所属一级机构", description = "creator_bg_name")
    val creatorBgName: String,
    @JsonProperty(value = "creator_dept_name", required = true)
    @get:Schema(title = "注册人所属二级机构", description = "creator_dept_name")
    val creatorDeptName: String,
    @JsonProperty(value = "creator_center_name", required = true)
    @get:Schema(title = "注册人所属三级机构", description = "creator_center_name")
    val creatorCenterName: String,
    @JsonProperty(value = "bg_id", required = true)
    @get:Schema(title = "项目所属一级机构ID", description = "bg_id")
    val bgId: Long? = null,
    @JsonProperty(value = "bg_name", required = true)
    @get:Schema(title = "项目所属一级机构名称", description = "bg_name")
    val bgName: String? = "",
    @JsonProperty(value = "business_line_id", required = true)
    @get:Schema(title = "业务线ID")
    val businessLineId: Long? = null,
    @JsonProperty(value = "business_line_name", required = true)
    @get:Schema(title = "业务线名称")
    val businessLineName: String? = "",
    @JsonProperty(value = "dept_id", required = true)
    @get:Schema(title = "项目所属二级机构ID", description = "dept_id")
    val deptId: Long? = null,
    @JsonProperty(value = "dept_name", required = true)
    @get:Schema(title = "项目所属二级机构名称", description = "dept_name")
    val deptName: String? = null,
    @JsonProperty(value = "center_id", required = true)
    @get:Schema(title = "项目所属三级机构ID", description = "center_id")
    val centerId: Long? = null,
    @JsonProperty(value = "center_name", required = true)
    @get:Schema(title = "项目所属三级机构名称", description = "center_name")
    val centerName: String? = null,
    @JsonProperty(value = "project_type", required = false)
    @get:Schema(title = "项目类型", description = "project_type")
    val projectType: Int?,
    @JsonProperty(value = "approver", required = false)
    @get:Schema(title = "审批人", description = "approver")
    val approver: String?,
    @JsonProperty(value = "approval_time", required = false)
    @get:Schema(title = "审批时间", description = "approval_time")
    val approvalTime: Long?,
    @JsonProperty(value = "approval_status", required = true)
    @get:Schema(title = "审批状态", description = "approval_status")
    val approvalStatus: Int,
    @JsonProperty(value = "is_secrecy", required = true)
    @get:Schema(title = "保密性", description = "is_secrecy")
    val secrecyFlag: Boolean,
    @JsonProperty(value = "creator", required = true)
    @get:Schema(title = "创建人", description = "creator")
    val creator: String,
    @JsonProperty(value = "created_at", required = true)
    @get:Schema(title = "注册时间", description = "created_at")
    val createdAtTime: Long,
    @JsonProperty(value = "cc_app_id", required = false)
    @get:Schema(title = "应用ID", description = "cc_app_id")
    val ccAppId: Long?,
    @JsonProperty(value = "use_bk", required = false)
    @get:Schema(title = "是否用蓝鲸", description = "use_bk")
    val useBk: Boolean?,
    @JsonProperty(value = "is_offlined", required = false)
    @get:Schema(title = "是否停用", description = "is_offlined")
    val offlinedFlag: Boolean?,
    @JsonProperty(value = "kind", required = true)
    @get:Schema(title = "kind", description = "kind")
    val kind: Int?,
    @JsonProperty(value = "enabled", required = true)
    @get:Schema(title = "启用", description = "enabled")
    val enabled: Boolean?,
    @JsonProperty(value = "is_gray", required = true)
    @get:Schema(title = "是否灰度 true：是 false：否", description = "is_gray")
    val grayFlag: Boolean,
    @JsonProperty(value = "is_repo_gray", required = true)
    @get:Schema(title = "是否仓库灰度 true：是 false：否", description = "is_repo_gray")
    val repoGrayFlag: Boolean,
    @JsonProperty(value = "is_codecc_gray", required = true)
    @get:Schema(title = "是否codecc灰度 true：是 false：否", description = "is_codecc_gray")
    val codeCCGrayFlag: Boolean,
    @get:Schema(title = "混合云CC业务ID")
    val hybridCCAppId: Long?,
    @get:Schema(title = "支持构建机访问外网")
    val enableExternal: Boolean? = true,
    @get:Schema(title = "支持IDC构建机")
    val enableIdc: Boolean? = false,
    @get:Schema(title = "流水线数量上限")
    val pipelineLimit: Int? = 500,
    @get:Schema(title = "项目其他设置")
    val properties: ProjectProperties?,
    @get:Schema(title = "产品运营ID")
    val productId: Int? = null
)
