package com.tencent.devops.repository.pojo.github

import com.tencent.devops.repository.pojo.Repository
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("代码库模型-GitHub代码库")
data class GithubRepository(
    @ApiModelProperty("代码库别名", required = true)
    override val aliasName: String,
    @ApiModelProperty("URL", required = true)
    override val url: String,
    @ApiModelProperty("用户名", required = true)
    override var userName: String = "",
    @ApiModelProperty("github项目名称", example = "Tencent/wepy", required = true)
    override val projectName: String,
    @ApiModelProperty("项目id", required = true)
    override val projectId: String = "",
    @ApiModelProperty("仓库hash id", required = false)
    override val repoHashId: String?
) : Repository {
    companion object {
        const val classType = "github"
    }

    override val credentialId: String
        get() = ""

    override fun getStartPrefix() = "https://github.com/"
    // override fun getStartPrefix() = "https://github.com/"
}