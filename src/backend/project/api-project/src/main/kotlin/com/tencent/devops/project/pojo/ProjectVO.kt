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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-显示模型")
data class ProjectVO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("项目ID")
    @JsonProperty("project_id")
    val projectId: String,
    @ApiModelProperty("项目名称")
    @JsonProperty("project_name")
    val projectName: String,
    @ApiModelProperty("项目代码")
    @JsonProperty("project_code")
    val projectCode: String,
    @ApiModelProperty("项目类型")
    @JsonProperty("project_type")
    val projectType: Int?,
    @ApiModelProperty("审批状态")
    @JsonProperty("approval_status")
    val approvalStatus: Int?,
    @ApiModelProperty("审批时间")
    @JsonProperty("approval_time")
    val approvalTime: String?,
    @ApiModelProperty("审批人")
    val approver: String?,
    @ApiModelProperty("cc业务ID")
    @JsonProperty("cc_app_id")
    val ccAppId: Long?,
    @ApiModelProperty("cc业务名称")
    @JsonProperty("cc_app_name")
    val ccAppName: String?,
    @ApiModelProperty("创建时间")
    @JsonProperty("created_at")
    val createdAt: String?,
    @ApiModelProperty("创建人")
    val creator: String?,
    @ApiModelProperty("数据ID")
    @JsonProperty("data_id")
    val dataId: Long?,
    @ApiModelProperty("部署类型")
    @JsonProperty("deploy_type")
    val deployType: String?,
    @ApiModelProperty("事业群ID")
    @JsonProperty("bg_id")
    val bgId: Long?,
    @ApiModelProperty("事业群名字")
    @JsonProperty("bg_name")
    val bgName: String?,
    @ApiModelProperty("中心ID")
    @JsonProperty("center_id")
    val centerId: Long?,
    @ApiModelProperty("中心名称")
    @JsonProperty("center_name")
    val centerName: String?,
    @ApiModelProperty("部门ID")
    @JsonProperty("dept_id")
    val deptId: Long?,
    @ApiModelProperty("部门名称")
    @JsonProperty("dept_name")
    val deptName: String?,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("英文缩写")
    @JsonProperty("english_name")
    val englishName: String,
    @ApiModelProperty("extra")
    val extra: String?,
    @ApiModelProperty("是否离线")
    @get:JsonProperty("is_offlined")
    val isOfflined: Boolean?,
    @ApiModelProperty("是否保密")
    @get:JsonProperty("is_secrecy")
    val isSecrecy: Boolean?,
    @ApiModelProperty("是否启用图表激活")
    @get:JsonProperty("is_helm_chart_enabled")
    val isHelmChartEnabled: Boolean?,
    @ApiModelProperty("kind")
    val kind: Int?,
    @ApiModelProperty("logo地址")
    @JsonProperty("logo_addr")
    val logoAddr: String?,
    @ApiModelProperty("评论")
    val remark: String?,
    @ApiModelProperty("修改时间")
    @JsonProperty("updated_at")
    val updatedAt: String?,
    @ApiModelProperty("useBK")
    @JsonProperty("use_bk")
    val useBk: Boolean?,
    @ApiModelProperty("启用")
    val enabled: Boolean?,
    @ApiModelProperty("是否灰度")
    val gray: Boolean,
    @ApiModelProperty("支持构建机访问外网")
    val enableExternal: Boolean?
)