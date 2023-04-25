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

@ApiModel("项目信息响应信息体")
data class ProjectInfoResponse(
    @JsonProperty(value = "project_id", required = true)
    @ApiModelProperty("项目ID", name = "project_id")
    val projectId: String,
    @ApiModelProperty("项目名称", name = "project_name")
    @JsonProperty(value = "project_name", required = true)
    val projectName: String,
    @JsonProperty(value = "english_name", required = true)
    @ApiModelProperty("项目英文简称", name = "english_name")
    val projectEnglishName: String,
    @JsonProperty(value = "creator_bg_name", required = true)
    @ApiModelProperty("注册人所属一级机构", name = "creator_bg_name")
    val creatorBgName: String,
    @JsonProperty(value = "creator_dept_name", required = true)
    @ApiModelProperty("注册人所属二级机构", name = "creator_dept_name")
    val creatorDeptName: String,
    @JsonProperty(value = "creator_center_name", required = true)
    @ApiModelProperty("注册人所属三级机构", name = "creator_center_name")
    val creatorCenterName: String,
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
    val projectType: Int?,
    @JsonProperty(value = "approver", required = false)
    @ApiModelProperty("审批人", name = "approver")
    val approver: String?,
    @JsonProperty(value = "approval_time", required = false)
    @ApiModelProperty("审批时间", name = "approval_time")
    val approvalTime: Long?,
    @JsonProperty(value = "approval_status", required = true)
    @ApiModelProperty("审批状态", name = "approval_status")
    val approvalStatus: Int,
    @JsonProperty(value = "is_secrecy", required = true)
    @ApiModelProperty("保密性", name = "is_secrecy")
    val secrecyFlag: Boolean,
    @JsonProperty(value = "creator", required = true)
    @ApiModelProperty("创建人", name = "creator")
    val creator: String,
    @JsonProperty(value = "created_at", required = true)
    @ApiModelProperty("注册时间", name = "created_at")
    val createdAtTime: Long,
    @JsonProperty(value = "cc_app_id", required = false)
    @ApiModelProperty("应用ID", name = "cc_app_id")
    val ccAppId: Long?,
    @JsonProperty(value = "use_bk", required = false)
    @ApiModelProperty("是否用蓝鲸", name = "use_bk")
    val useBk: Boolean?,
    @JsonProperty(value = "is_offlined", required = false)
    @ApiModelProperty("是否停用", name = "is_offlined")
    val offlinedFlag: Boolean?,
    @JsonProperty(value = "kind", required = true)
    @ApiModelProperty("kind", name = "kind")
    val kind: Int?,
    @JsonProperty(value = "enabled", required = true)
    @ApiModelProperty("启用", name = "enabled")
    val enabled: Boolean?,
    @JsonProperty(value = "is_gray", required = true)
    @ApiModelProperty("是否灰度 true：是 false：否", name = "is_gray")
    val grayFlag: Boolean,
    @ApiModelProperty("混合云CC业务ID")
    val hybridCCAppId: Long?,
    @ApiModelProperty("支持构建机访问外网")
    val enableExternal: Boolean?
)
