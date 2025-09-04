package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class HostBizRelation(
    @get:Schema(title = "业务ID", required = true)
    @JsonProperty("bk_biz_id")
    val bkBizId: Int,
    @get:Schema(title = "模块ID", required = true)
    @JsonProperty("bk_module_id")
    val bkModuleId: Int,
    @get:Schema(title = "开发商账户", required = true)
    @JsonProperty("bk_supplier_account")
    val bkSupplierAccount: String,
    @get:Schema(title = "主机ID", required = true)
    @JsonProperty("bk_host_id")
    val bkHostId: Int,
    @get:Schema(title = "集群ID", required = true)
    @JsonProperty("bk_set_id")
    val bkSetId: Int
)