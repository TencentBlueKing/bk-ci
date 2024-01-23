package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("有权限账号")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudAuthorizedAccount(
    @ApiModelProperty(value = "帐号ID", required = true)
    val id: Long,
    @ApiModelProperty(value = "帐号名称", required = true)
    val account: String,
    @ApiModelProperty(value = "账号类型", notes = "1：Linux，2：Windows", required = true)
    val type: Int,
    @ApiModelProperty(value = "账号用途", notes = "1：系统账号", required = true)
    val category: Int,
    @ApiModelProperty(value = "账号别名", notes = "不传则以账号名称作为别名", required = true)
    val alias: String,
    @ApiModelProperty(value = "帐号对应OS", notes = "账号用途为系统账号时该字段生效")
    val os: String?,
    @ApiModelProperty(value = "帐号描述")
    val description: String?,
    @ApiModelProperty(value = "创建人", required = true)
    val creator: String,
    @ApiModelProperty(value = "创建时间", notes = "Unix时间戳，单位ms", required = true)
    @JsonProperty("create_time")
    val createTime: Long,
    @ApiModelProperty(value = "最近一次修改人", required = true)
    @JsonProperty("last_modify_user")
    val lastModifyUser: String,
    @ApiModelProperty(value = "最近一次修改时间", notes = "Unix时间戳，单位ms", required = true)
    @JsonProperty("last_modify_time")
    val lastModifyTime: Long,
    @ApiModelProperty(value = "数据库账号对应的系统账号ID", notes = "账号用途为数据库账号时该字段生效")
    @JsonProperty("db_system_account_id")
    val dbSystemAccountId: Long?,
    @ApiModelProperty(value = "业务ID")
    @JsonProperty("bk_biz_id")
    val bkBizId: Long?,
    @ApiModelProperty(value = "资源范围类型", notes = "biz - 业务，biz_set - 业务集")
    @JsonProperty("bk_scope_type")
    var bkScopeType: String?,
    @ApiModelProperty(value = "资源范围ID", notes = "与bk_scope_type对应, 表示业务ID或者业务集ID")
    @JsonProperty("bk_scope_id")
    var bkScopeId: String?
)