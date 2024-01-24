package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class HostBizRelation(
    @ApiModelProperty(value = "业务ID", required = true)
    @JsonProperty("bk_biz_id")
    val bkBizId: Int,
    @ApiModelProperty(value = "模块ID", required = true)
    @JsonProperty("bk_module_id")
    val bkModuleId: Int,
    @ApiModelProperty(value = "开发商账户", required = true)
    @JsonProperty("bk_supplier_account")
    val bkSupplierAccount: String,
    @ApiModelProperty(value = "主机ID", required = true)
    @JsonProperty("bk_host_id")
    val bkHostId: Int,
    @ApiModelProperty(value = "集群ID", required = true)
    @JsonProperty("bk_set_id")
    val bkSetId: Int
)