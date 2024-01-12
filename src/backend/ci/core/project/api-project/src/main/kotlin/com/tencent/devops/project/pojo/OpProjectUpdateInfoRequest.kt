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
@Schema(name = "项目信息请求实体")
data class OpProjectUpdateInfoRequest(
    @JsonProperty(value = "project_id", required = true)
    @Schema(name = "项目ID", description = "project_id")
    val projectId: String,
    @Schema(name = "项目名称", description = "project_name")
    @JsonProperty(value = "project_name", required = true)
    val projectName: String,
    @JsonProperty(value = "bg_id", required = true)
    @Schema(name = "项目所属一级机构ID", description = "bg_id")
    val bgId: Long,
    @JsonProperty(value = "bg_name", required = true)
    @Schema(name = "项目所属一级机构名称", description = "bg_name")
    val bgName: String,
    @JsonProperty(value = "business_line_id", required = true)
    @Schema(name = "业务线ID")
    val businessLineId: Long? = null,
    @JsonProperty(value = "business_line_name", required = true)
    @Schema(name = "业务线名称")
    val businessLineName: String? = "",
    @JsonProperty(value = "dept_id", required = true)
    @Schema(name = "项目所属二级机构ID", description = "dept_id")
    val deptId: Long,
    @JsonProperty(value = "dept_name", required = true)
    @Schema(name = "项目所属二级机构名称", description = "dept_name")
    val deptName: String,
    @JsonProperty(value = "center_id", required = true)
    @Schema(name = "项目所属三级机构ID", description = "center_id")
    val centerId: Long,
    @JsonProperty(value = "center_name", required = true)
    @Schema(name = "项目所属三级机构名称", description = "center_name")
    val centerName: String,
    @JsonProperty(value = "project_type", required = true)
    @Schema(name = "项目类型", description = "project_type")
    val projectType: Int,
    @JsonProperty(value = "approver", required = false)
    @Schema(name = "审批人", description = "approver")
    var approver: String?,
    @JsonProperty(value = "updator", required = true)
    @Schema(name = "更新人", description = "updator")
    var updator: String,
    @JsonProperty(value = "approval_status", required = true)
    @Schema(name = "审批状态", description = "approval_status")
    val approvalStatus: Int,
    @JsonProperty(value = "approval_time", required = false)
    @Schema(name = "审批时间", description = "approval_time")
    var approvalTime: Long?,
    @JsonProperty(value = "is_secrecy", required = true)
    @Schema(name = "保密性", description = "is_secrecy")
    val secrecyFlag: Boolean,
    @JsonProperty(value = "cc_app_id", required = false)
    @Schema(name = "应用ID", description = "cc_app_id")
    val ccAppId: Long?,
    @Schema(name = "名称")
    var cc_app_name: String?,
    @Schema(name = "容器类型， 1 - k8s; 2 - mesos")
    val kind: Int?,
    @JsonProperty(value = "enabled")
    @Schema(name = "启用", description = "enabled")
    val enabled: Boolean,
    @JsonProperty(value = "use_bk", required = true)
    @Schema(name = "是否用蓝鲸", description = "use_bk")
    val useBk: Boolean,
    @JsonProperty(value = "labelIdList", required = false)
    @Schema(name = "标签id集合", description = "labelIdList")
    val labelIdList: List<String>?,
    @Schema(name = "混合云CC业务ID")
    val hybridCCAppId: Long?,
    @Schema(name = "支持构建机访问外网")
    val enableExternal: Boolean?,
    @Schema(name = "支持IDC构建机", required = false)
    val enableIdc: Boolean? = false,
    @Schema(name = "流水线数量上限", required = false)
    val pipelineLimit: Int? = 500,
    @Schema(name = "项目相关配置")
    val properties: ProjectProperties? = null,
    @Schema(name = "运营产品id")
    val productId: Int? = null
)
