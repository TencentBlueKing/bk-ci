package com.tencent.devops.auth.pojo

import com.tencent.bk.sdk.iam.dto.InstancesDTO
import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("组权限详情")
data class RelatedResourceInfo(
    @ApiModelProperty("资源类型")
    val type: String,
    @ApiModelProperty("资源类型名")
    @BkFieldI18n(keyPrefixName = "resourceType")
    val name: String,
    @ApiModelProperty("资源实例")
    val instances: InstancesDTO
)
