package com.tencent.devops.store.pojo.image.request

import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("镜像状态信息修改请求报文体")
data class ImageStatusInfoUpdateRequest(
    @ApiModelProperty("镜像状态", required = false)
    val imageStatus: ImageStatusEnum? = null,
    @ApiModelProperty("镜像状态描述", required = false)
    val imageStatusMsg: String? = null,
    @ApiModelProperty("镜像发布时间", required = false)
    val pubTime: LocalDateTime? = null,
    @ApiModelProperty("镜像tag", required = false)
    val imageTag: String? = null,
    @ApiModelProperty("是否为最新版本镜像", required = false)
    val latestFlag: Boolean? = null
)