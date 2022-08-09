package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.ApiModelProperty

data class PublishersRequest(
    @ApiModelProperty("发布者标识", required = true)
    val publishersCode: String,
    @ApiModelProperty("发布者名称", required = true)
    val name: String,
    @ApiModelProperty("发布者类型", required = true)
    val publishersType: PublisherType,
    @ApiModelProperty("主体负责人", required = true)
    val owners: List<String>,
    @ApiModelProperty("成员", required = true)
    val members: List<String>,
    @ApiModelProperty("技术支持", required = false)
    val helper: String? = null,
    @ApiModelProperty("是否认证", required = true)
    val certificationFlag: Boolean,
    @ApiModelProperty("组件类型", required = true)
    val storeType: StoreTypeEnum,
    @ApiModelProperty("实体组织架构", required = true)
    val organization: String,
    @ApiModelProperty("所属工作组BG", required = true)
    val ownerDeptName: String
)
