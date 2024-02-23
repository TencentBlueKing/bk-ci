package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "有权限账号")
data class JobCloudAuthorizedAccount(
    @get:Schema(title = "帐号ID", required = true)
    val id: Long,
    @get:Schema(title = "帐号名称", required = true)
    val account: String,
    @get:Schema(title = "账号类型", description = "1：Linux，2：Windows", required = true)
    val type: Int,
    @get:Schema(title = "账号用途", description = "1：系统账号", required = true)
    val category: Int,
    @get:Schema(title = "账号别名", description = "不传则以账号名称作为别名", required = true)
    val alias: String,
    @get:Schema(title = "帐号对应OS", description = "账号用途为系统账号时该字段生效")
    val os: String?,
    @get:Schema(title = "帐号描述")
    val description: String?,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "创建时间", description = "Unix时间戳，单位ms", required = true)
    @JsonProperty("create_time")
    val createTime: Long,
    @get:Schema(title = "最近一次修改人", required = true)
    @JsonProperty("last_modify_user")
    val lastModifyUser: String,
    @get:Schema(title = "最近一次修改时间", description = "Unix时间戳，单位ms", required = true)
    @JsonProperty("last_modify_time")
    val lastModifyTime: Long,
    @get:Schema(title = "数据库账号对应的系统账号ID", description = "账号用途为数据库账号时该字段生效")
    @JsonProperty("db_system_account_id")
    val dbSystemAccountId: Long?,
    @get:Schema(title = "业务ID")
    @JsonProperty("bk_biz_id")
    val bkBizId: Long?,
    @get:Schema(title = "资源范围类型", description = "biz - 业务，biz_set - 业务集")
    @JsonProperty("bk_scope_type")
    var bkScopeType: String?,
    @get:Schema(title = "资源范围ID", description = "与bk_scope_type对应, 表示业务ID或者业务集ID")
    @JsonProperty("bk_scope_id")
    var bkScopeId: String?
)