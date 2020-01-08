package com.tencent.devops.store.pojo.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.store.pojo.VersionLog
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubmitDTO(
    @ApiModelProperty("扩展点基础信息")
    val baseInfo: UpdateExtensionServiceDTO,
    @ApiModelProperty("扩展点版本日志信息")
    val versionLog: VersionLog
)