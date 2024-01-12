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

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
@Schema(description = "项目信息请求实体")
data class OpProjectUpdateInfoRequest(
    @JsonProperty(value = "project_id", required = true)
    @Schema(description = "项目ID", name = "project_id")
    val projectId: String,
    @Schema(description = "项目名称", name = "project_name")
    @JsonProperty(value = "project_name", required = true)
    val projectName: String,
    @JsonProperty(value = "bg_id", required = true)
    @Schema(description = "项目所属一级机构ID", name = "bg_id")
    val bgId: Long,
    @JsonProperty(value = "bg_name", required = true)
    @Schema(description = "项目所属一级机构名称", name = "bg_name")
    val bgName: String,
    @JsonProperty(value = "business_line_id", required = true)
    @Schema(description = "业务线ID")
    val businessLineId: Long? = null,
    @JsonProperty(value = "business_line_name", required = true)
    @Schema(description = "业务线名称")
    val businessLineName: String? = "",
    @JsonProperty(value = "dept_id", required = true)
    @Schema(description = "项目所属二级机构ID", name = "dept_id")
    val deptId: Long,
    @JsonProperty(value = "dept_name", required = true)
    @Schema(description = "项目所属二级机构名称", name = "dept_name")
    val deptName: String,
    @JsonProperty(value = "center_id", required = true)
    @Schema(description = "项目所属三级机构ID", name = "center_id")
    val centerId: Long,
    @JsonProperty(value = "center_name", required = true)
    @Schema(description = "项目所属三级机构名称", name = "center_name")
    val centerName: String,
    @JsonProperty(value = "project_type", required = true)
    @Schema(description = "项目类型", name = "project_type")
    val projectType: Int,
    @JsonProperty(value = "approver", required = false)
    @Schema(description = "审批人", name = "approver")
    var approver: String?,
    @JsonProperty(value = "updator", required = true)
    @Schema(description = "更新人", name = "updator")
    var updator: String,
    @JsonProperty(value = "approval_status", required = true)
    @Schema(description = "审批状态", name = "approval_status")
    val approvalStatus: Int,
    @JsonProperty(value = "approval_time", required = false)
    @Schema(description = "审批时间", name = "approval_time")
    var approvalTime: Long?,
    @JsonProperty(value = "is_secrecy", required = true)
    @Schema(description = "保密性", name = "is_secrecy")
    val secrecyFlag: Boolean,
    @JsonProperty(value = "cc_app_id", required = false)
    @Schema(description = "应用ID", name = "cc_app_id")
    val ccAppId: Long?,
    @Schema(description = "名称")
    var cc_app_name: String?,
    @Schema(description = "容器类型， 1 - k8s; 2 - mesos")
    val kind: Int?,
    @JsonProperty(value = "enabled")
    @Schema(description = "启用", name = "enabled")
    val enabled: Boolean,
    @JsonProperty(value = "use_bk", required = true)
    @Schema(description = "是否用蓝鲸", name = "use_bk")
    val useBk: Boolean,
    @JsonProperty(value = "labelIdList", required = false)
    @Schema(description = "标签id集合", name = "labelIdList")
    val labelIdList: List<String>?,
    @Schema(description = "混合云CC业务ID")
    val hybridCCAppId: Long?,
    @Schema(description = "支持构建机访问外网")
    val enableExternal: Boolean?,
    @Schema(description = value = "支持IDC构建机", required = false)
    val enableIdc: Boolean? = false,
    @Schema(description = value = "流水线数量上限", required = false)
    val pipelineLimit: Int? = 500,
    @Schema(description = "项目相关配置")
    val properties: ProjectProperties? = null,
    @Schema(description = "运营产品id")
    val productId: Int? = null
)
