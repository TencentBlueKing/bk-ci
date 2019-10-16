package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目信息请求实体")
data class OpProjectUpdateInfoRequest(
    @JsonProperty(value = "project_id", required = true)
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("项目名称")
    @JsonProperty(value = "project_name", required = true)
    val projectName: String,
    @JsonProperty(value = "bg_id", required = true)
    @ApiModelProperty("项目所属一级机构ID")
    val bgId: Long,
    @JsonProperty(value = "bg_name", required = true)
    @ApiModelProperty("项目所属一级机构名称")
    val bgName: String,
    @JsonProperty(value = "dept_id", required = true)
    @ApiModelProperty("项目所属二级机构ID")
    val deptId: Long,
    @JsonProperty(value = "dept_name", required = true)
    @ApiModelProperty("项目所属二级机构名称")
    val deptName: String,
    @JsonProperty(value = "center_id", required = true)
    @ApiModelProperty("项目所属三级机构ID")
    val centerId: Long,
    @JsonProperty(value = "center_name", required = true)
    @ApiModelProperty("项目所属三级机构名称")
    val centerName: String,
    @JsonProperty(value = "project_type", required = true)
    @ApiModelProperty("项目类型")
    val projectType: Int,
    @JsonProperty(value = "approver", required = false)
    @ApiModelProperty("审批人")
    var approver: String?,
    @JsonProperty(value = "updator", required = true)
    @ApiModelProperty("更新人")
    var updator: String,
    @JsonProperty(value = "approval_status", required = true)
    @ApiModelProperty("审批状态")
    val approvalStatus: Int,
    @JsonProperty(value = "approval_time", required = false)
    @ApiModelProperty("审批时间")
    var approvalTime: Long?,
    @JsonProperty(value = "is_secrecy", required = true)
    @ApiModelProperty("保密性")
    val secrecyFlag: Boolean,
    @JsonProperty(value = "cc_app_id", required = false)
    @ApiModelProperty("应用ID")
    val ccAppId: Long?,
    @ApiModelProperty("名称")
    var cc_app_name: String?,
    @ApiModelProperty("容器类型， 1 - k8s; 2 - mesos")
    val kind: Int?,
    @JsonProperty(value = "enabled")
    @ApiModelProperty("启用")
    val enabled: Boolean,
    @JsonProperty(value = "use_bk", required = true)
    @ApiModelProperty("是否用蓝鲸")
    val useBk: Boolean,
    @JsonProperty(value = "labelIdList", required = false)
    @ApiModelProperty("标签id集合")
    val labelIdList: List<String>?,
    @ApiModelProperty("混合云CC业务ID")
    val hybridCCAppId: Long?,
    @ApiModelProperty("支持构建机访问外网")
    val enableExternal: Boolean?
)
