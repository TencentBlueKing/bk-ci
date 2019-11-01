package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Deprecated("已作废")
@ApiModel("部署-构件分发(已废弃)", description = DeployDistributionElement.classType)
data class DeployDistributionElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "部署-构建分发",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("文件上传路径（多个路径中间逗号隔开），支持正则表达式", required = true)
    val regexPaths: String = "",
    @ApiModelProperty("目标IP（多个中间逗号隔开）", required = true)
    val targetIps: String = "",
    @ApiModelProperty("文件上传的目标路径", required = true)
    val targetPath: String = "",
    @ApiModelProperty("上传最长时间（单位：秒）,默认10分钟", required = false)
    var maxRunningMins: Int = 600,
    @ApiModelProperty("业务ID", required = true)
    val appid: Int
) : Element(name, id, status) {
    companion object {
        const val classType = "deployDistribution"
    }

    override fun getClassType() = classType
}
