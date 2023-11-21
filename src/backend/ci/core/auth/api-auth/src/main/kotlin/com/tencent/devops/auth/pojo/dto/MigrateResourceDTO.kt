package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("迁移资源请求实体")
data class MigrateResourceDTO(
    @ApiModelProperty("资源类型")
    val resourceType: String? = null,
    @ApiModelProperty("项目ID列表")
    val projectCodes: List<String>? = null,
    @ApiModelProperty("是否迁移项目级资源")
    val migrateProjectResource: Boolean? = false,
    @ApiModelProperty("是否迁移其他资源类型的资源")
    val migrateOtherResource: Boolean? = false,
)
