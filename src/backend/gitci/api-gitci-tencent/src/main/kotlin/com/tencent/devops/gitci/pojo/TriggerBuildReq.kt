package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("TriggerBuild请求")
data class TriggerBuildReq(
    @ApiModelProperty("工蜂项目ID")
    override val gitProjectId: Long,
    @ApiModelProperty("工蜂项目名")
    override val name: String,
    @ApiModelProperty("工蜂项目url")
    override val url: String,
    @ApiModelProperty("homepage")
    override val homepage: String,
    @ApiModelProperty("gitHttpUrl")
    override val gitHttpUrl: String,
    @ApiModelProperty("gitSshUrl")
    override val gitSshUrl: String,
    @ApiModelProperty("分支")
    val branch: String,
    @ApiModelProperty("Custom commit message")
    val customCommitMsg: String?,
    @ApiModelProperty("yaml")
    val yaml: String?,
    @ApiModelProperty("描述")
    val description: String?
) : Repository(gitProjectId, name, url, homepage, gitHttpUrl, gitSshUrl)
