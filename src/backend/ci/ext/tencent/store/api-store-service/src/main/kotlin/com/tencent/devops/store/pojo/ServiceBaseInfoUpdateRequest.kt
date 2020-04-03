package com.tencent.devops.store.pojo

import com.tencent.devops.store.pojo.common.StoreMediaInfo
import com.tencent.devops.store.pojo.enums.DescInputTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务基本信息修改请求报文体")
data class ServiceBaseInfoUpdateRequest(
    @ApiModelProperty("扩展名称", required = false)
    val serviceName: String? = null,
    @ApiModelProperty("所属分类代码", required = false)
    val classifyCode: String? = null,
    @ApiModelProperty("插件简介", required = false)
    val summary: String? = null,
    @ApiModelProperty("扩展描述", required = false)
    val description: String? = null,
    @ApiModelProperty("扩展logo", required = false)
    val logoUrl: String? = null,
    @ApiModelProperty("发布者", required = false)
    val publisher: String? = null,
    @ApiModelProperty("原子标签列表", required = false)
    val labelIdList: ArrayList<String>? = null,
    @ApiModelProperty("扩展点列表", required = false)
    val extensionItemIdList: Set<String>? = null,
    @ApiModelProperty("媒体信息列表", required = false)
    val mediaList: List<StoreMediaInfo>? = null,
    @ApiModelProperty("描述录入类型")
    val descInputType: DescInputTypeEnum? = DescInputTypeEnum.MANUAL
)