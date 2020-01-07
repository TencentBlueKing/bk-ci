package com.tencent.devops.store.pojo.vo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.store.pojo.VersionLog
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtensionAndVersionVO (
    @ApiModelProperty("基本信息")
    val extensionServiceVO: ExtensionServiceVO,
    @ApiModelProperty("扩展服务code")
    val versionVO: VersionLog
)