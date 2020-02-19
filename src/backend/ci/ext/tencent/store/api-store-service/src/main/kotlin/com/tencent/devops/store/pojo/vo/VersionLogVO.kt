package com.tencent.devops.store.pojo.vo

import com.tencent.devops.store.pojo.VersionLog
import io.swagger.annotations.ApiModelProperty

data class VersionLogVO(
    @ApiModelProperty("条数")
    val count: Int,
    @ApiModelProperty("日志信息")
    val records: List<VersionLog>
)