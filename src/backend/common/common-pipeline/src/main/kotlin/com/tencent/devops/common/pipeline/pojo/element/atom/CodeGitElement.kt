package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.git.GitPullMode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("拉取Git仓库代码", description = CodeGitElement.classType)
data class CodeGitElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("代码库哈希ID", required = true)
    val repositoryHashId: String?,
    @ApiModelProperty("branch name", required = false)
    val branchName: String? = null,
    @ApiModelProperty("revision 用于强制指定commitId", required = false)
    var revision: String? = null,
    @ApiModelProperty("Checkout strategy", required = true)
    val strategy: CodePullStrategy? = CodePullStrategy.INCREMENT_UPDATE,
    @ApiModelProperty("代码存放路径", required = false)
    val path: String? = null,
    @ApiModelProperty("启动Submodule", required = false)
    val enableSubmodule: Boolean? = true,
    @ApiModelProperty("Git指定拉取方式", required = false)
    val gitPullMode: GitPullMode?,
    @ApiModelProperty("新版的git原子的类型")
    val repositoryType: RepositoryType? = null,
    @ApiModelProperty("新版的git代码库名")
    val repositoryName: String? = null,
    @ApiModelProperty("支持虚拟合并分支", required = false)
    val enableVirtualMergeBranch: Boolean? = false
) : Element(name, id, status) {
    companion object {
        const val classType = "CODE_GIT"
        const val modeType = "mode.type"
        const val modeValue = "mode.value"
    }

    override fun genTaskParams(): MutableMap<String, Any> {
        val paramMap = JsonUtil.toMutableMapSkipEmpty(this)
        if (gitPullMode != null) { // 这个是为了方便构建机用的是Map，在运行时可直接key使用
            paramMap[modeType] = gitPullMode.type.name
            paramMap[modeValue] = gitPullMode.value
        }
        return paramMap
    }

    override fun getClassType() = classType
}
