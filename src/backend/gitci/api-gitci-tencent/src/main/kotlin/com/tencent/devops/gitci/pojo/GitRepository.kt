package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂项目")
data class GitRepository(
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
    override val gitSshUrl: String
) : Repository(gitProjectId, name, url, homepage, gitHttpUrl, gitSshUrl)
