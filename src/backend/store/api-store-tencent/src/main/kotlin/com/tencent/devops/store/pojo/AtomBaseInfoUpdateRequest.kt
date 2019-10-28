package com.tencent.devops.store.pojo

import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线-插件基本信息修改请求报文体")
data class AtomBaseInfoUpdateRequest(
    @ApiModelProperty("插件名称", required = false)
    val name: String? = null,
    @ApiModelProperty("所属分类代码", required = false)
    val classifyCode: String? = null,
    @ApiModelProperty("插件简介", required = false)
    val summary: String? = null,
    @ApiModelProperty("插件描述", required = false)
    val description: String? = null,
    @ApiModelProperty("插件logo", required = false)
    val logoUrl: String? = null,
    @ApiModelProperty("发布者", required = false)
    val publisher: String? = null,
    @ApiModelProperty("原子标签列表", required = false)
    val labelIdList: ArrayList<String>? = null,
    @ApiModelProperty(value = "项目可视范围", required = false)
    val visibilityLevel: VisibilityLevelEnum? = null,
    @ApiModelProperty(value = "插件代码库不开源原因", required = false)
    val privateReason: String? = null
)