package com.tencent.devops.store.pojo.image.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像基本信息（不影响执行的信息）修改请求报文体")
data class ImageBaseInfoUpdateRequest(
    @ApiModelProperty("镜像名称", required = false)
    val imageName: String? = null,
    @ApiModelProperty("所属分类ID", required = false)
    val classifyId: String? = null,
    @ApiModelProperty("功能标签", required = false)
    val labelIdList: List<String>? = null,
    @ApiModelProperty("镜像所属范畴CATEGORY_CODE", required = false)
    val category: String? = null,
    @ApiModelProperty("镜像简介", required = false)
    val summary: String? = null,
    @ApiModelProperty("镜像描述", required = false)
    val description: String? = null,
    @ApiModelProperty("镜像logo", required = false)
    val logoUrl: String? = null,
    @ApiModelProperty("发布者", required = false)
    val publisher: String? = null,
    @ApiModelProperty(value = "镜像大小", required = false)
    val imageSize: String? = null,
    @ApiModelProperty("删除标识", required = false)
    val deleteFlag: Boolean? = null
)