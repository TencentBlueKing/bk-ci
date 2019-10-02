package com.tencent.devops.scm.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("创建git仓库响应体")
class GitRepositoryResp(
    @ApiModelProperty("仓库名称", required = true)
    val name: String,
    @ApiModelProperty("仓库地址", required = true)
    val repositoryUrl: String
)