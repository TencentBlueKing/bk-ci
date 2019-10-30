package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("部署-构件分发", description = ComDistributionElement.classType)
data class ComDistributionElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "部署-构建分发",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("文件上传路径（多个路径中间逗号隔开），支持正则表达式", required = true)
    val regexPaths: String = "",
    @ApiModelProperty("上传最长时间（单位：分钟）,默认10小时", required = false)
    var maxRunningMins: Int = 600,
    @ApiModelProperty("目标IP（多个中间逗号隔开）", required = true)
    val targetIps: String = "",
    @ApiModelProperty("文件上传的目标路径", required = true)
    val targetPath: String = "",
    @ApiModelProperty("业务ID", required = true)
    val appid: Int,
    @ApiModelProperty("是否自定义归档", required = true)
    val customize: Boolean
) : Element(name, id, status) {
    companion object {
        const val classType = "comDistribution"
    }

    override fun getTaskAtom() = "comDistributeTaskAtom"

    override fun getClassType() = classType
}
