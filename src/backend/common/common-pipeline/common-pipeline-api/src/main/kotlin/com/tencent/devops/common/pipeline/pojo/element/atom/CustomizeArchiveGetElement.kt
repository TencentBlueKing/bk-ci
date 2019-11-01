package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("拉取自定义仓库构件", description = CustomizeArchiveGetElement.classType)
data class CustomizeArchiveGetElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "拉取自定义仓库构件",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待下载文件路径（支持正则表达式，多个用逗号隔开）", required = true)
    val downloadPaths: String = "",
    @ApiModelProperty("下载到本地的路径（默认为当前工作空间）", required = false)
    val destPath: String = "",
    @ApiModelProperty("是否找不到文件报404退出", required = false)
    val notFoundContinue: Boolean? = false
) : Element(name, id, status) {

    companion object {
        const val classType = "customizeArchiveGet"
    }

    override fun getClassType() = classType
}
