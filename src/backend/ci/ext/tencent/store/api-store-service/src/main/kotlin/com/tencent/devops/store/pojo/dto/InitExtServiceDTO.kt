package com.tencent.devops.store.pojo.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InitExtServiceDTO(
    @ApiModelProperty("扩展服务code")
    val serviceCode: String,
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("调试项目Code")
    val projectCode: String,
    @ApiModelProperty("服务语言")
    val language: String? = "java",
    @ApiModelProperty("认证方式", required = false)
    val authType: String? = TokenTypeEnum.OAUTH.toString(),
    @ApiModelProperty(value = "插件代码库不开源原因", required = false)
    val privateReason: String? = null,
    @ApiModelProperty("扩展点列表")
    val extensionItemList: Set<String>,
    @ApiModelProperty(value = "项目可视范围", required = false)
    val visibilityLevel: VisibilityLevelEnum? = VisibilityLevelEnum.LOGIN_PUBLIC
)