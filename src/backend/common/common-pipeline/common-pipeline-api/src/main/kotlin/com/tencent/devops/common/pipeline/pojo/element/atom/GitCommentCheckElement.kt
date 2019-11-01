package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Git Commit comment检查", description = GitCommentCheckElement.classType)
class GitCommentCheckElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "金刚app扫描",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("代码库路径", required = false)
    val path: String?,
    @ApiModelProperty("源分支", required = true)
    val sourceBranch: String,
    @ApiModelProperty("目标分支", required = true)
    val targetBranch: String,
    @ApiModelProperty("git comment匹配规则", required = true)
    val commentPattern: String,
    @ApiModelProperty("匹配失败是否直接终止构建", required = true)
    val failOnMismatch: Boolean = false
) : Element(name, id, status) {

    override fun getClassType() = classType

    companion object {
        const val classType = "gitCommentCheck"
    }
}
