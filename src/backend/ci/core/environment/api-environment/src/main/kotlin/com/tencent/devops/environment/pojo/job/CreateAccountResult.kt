package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CreateAccountResult(
    @ApiModelProperty(value = "帐号ID", required = true)
    val id: Long,
    @ApiModelProperty(value = "帐号名称", required = true)
    val account: String,
    @ApiModelProperty(value = "账号类型", notes = "1：Linux，2：Windows", required = true)
    val type: Int,
    @ApiModelProperty(value = "账号用途", notes = "1：系统账号", required = true)
    val category: Int,
    @ApiModelProperty(value = "系统账号密码", notes = "账号用途为系统账号 且 账号类型为Windows时，必传。")
    val password: String,
    @ApiModelProperty(value = "账号别名", notes = "不传则以账号名称作为别名")
    val alias: String?,
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
    val lastModifyTime: Long
) {
    constructor() : this(-1L, "", -1, -1, "", "", "", "", "", -1L, "", -1L)
}