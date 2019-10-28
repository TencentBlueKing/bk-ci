package com.tencent.devops.store.pojo.ideatom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安装IDE插件返回报文")
data class InstallIdeAtomResp(
    @ApiModelProperty("devnet环境插件包路径", required = false)
    val atomFileDevnetUrl: String?,
    @ApiModelProperty("idc环境插件包路径", required = false)
    val atomFileIdcUrl: String?
)