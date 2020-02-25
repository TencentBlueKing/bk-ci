package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceFeatureUpdateInfo(
    @ApiModelProperty("是否为公共扩展服务， TRUE：是 FALSE：不是  ")
    val publicFlag: Boolean? = null,
    @ApiModelProperty("是否推荐， TRUE：是 FALSE：不是 ")
    val recommentFlag: Boolean? = null,
    @ApiModelProperty("是否官方认证， TRUE：是 FALSE：不是  ")
    val certificationFlag: Boolean? = null,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int? = null,
    @ApiModelProperty("扩展服务可见范围 0：私有 10：登录用户开源")
    val visibilityLevel: Int? = null,
    @ApiModelProperty("代码库hashId")
    val repositoryHashId: String? = null,
    @ApiModelProperty("代码库地址")
    val codeSrc: String? = null,
    @ApiModelProperty("删除标签")
    val deleteFlag: Boolean? = null,
    @ApiModelProperty("添加用户")
    val creatorUser: String? = null,
    @ApiModelProperty("修改用户")
    val modifierUser: String? = null
)