package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("拉取流水线已归档构件", description = BuildArchiveGetElement.classType)
data class BuildArchiveGetElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "构建产物文件归档下载",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("流水线id", required = true)
    val pipelineId: String = "",
    @ApiModelProperty("构建号（不传则取最新的构建号）", required = false)
    val buildNo: String = "",
    @ApiModelProperty("待下载文件路径（支持正则表达式，多个用逗号隔开）", required = true)
    val srcPaths: String = "",
    @ApiModelProperty("下载到本地的路径（不填则为当前工作空间）", required = false)
    val destPath: String = "",
    @ApiModelProperty("是否传最新构建号(LASTEST 表示最新构建号, ASSIGN 指定构建号)", required = true)
    val buildNoType: String = "",
    @ApiModelProperty("是否找不到文件报404退出)", required = false)
    val notFoundContinue: Boolean? = false
) : Element(name, id, status) {

    companion object {
        const val classType = "buildArchiveGet"
    }

    override fun getClassType() = classType
}
