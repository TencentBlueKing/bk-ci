package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-跨项目构件分发", description = AcrossProjectDistributionElement.classType)
data class AcrossProjectDistributionElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "跨项目构件分发",
    @ApiModelProperty("id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("路径", required = true)
    val path: String = "",
    @ApiModelProperty("是否自定义仓库", required = true)
    val customized: Boolean,
    @ApiModelProperty("目标项目", required = true)
    val targetProjectId: String = "",
    @ApiModelProperty("目标路径", required = true)
    val targetPath: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "acrossProjectDistribution"
    }

    override fun getTaskAtom() = "acrossProjectDistributionAtom"

    override fun getClassType() = classType
}
