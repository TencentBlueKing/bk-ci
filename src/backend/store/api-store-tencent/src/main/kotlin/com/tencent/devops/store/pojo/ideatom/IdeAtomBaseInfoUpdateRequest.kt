package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("IDE插件基本信息修改请求报文体")
data class IdeAtomBaseInfoUpdateRequest(
    @ApiModelProperty("插件状态", required = false)
    val atomStatus: IdeAtomStatusEnum? = null,
    @ApiModelProperty("插件状态描述", required = false)
    val atomStatusMsg: String? = null,
    @ApiModelProperty("插件发布时间", required = false)
    val pubTime: LocalDateTime? = null,
    @ApiModelProperty("IDE插件代码库tag", required = false)
    val repositoryTag: String? = null,
    @ApiModelProperty("是否为最新课件版本IDE插件", required = false)
    val latestFlag: Boolean? = null
)