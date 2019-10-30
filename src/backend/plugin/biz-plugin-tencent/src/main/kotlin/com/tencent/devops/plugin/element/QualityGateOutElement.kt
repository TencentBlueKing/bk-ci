package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线(准出)", description = QualityGateOutElement.classType)
data class QualityGateOutElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "质量红线(准出)",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("拦截原子", required = false)
    var interceptTask: String? = null,
    @ApiModelProperty("拦截原子名称", required = false)
    var interceptTaskName: String? = null,
    @ApiModelProperty("审核人", required = false)
    var reviewUsers: Set<String>? = null
) : Element(name, id, status) {
    companion object {
        const val classType = "qualityGateOutTask"
    }

    override fun getTaskAtom() = "qualityGateOutTaskAtom"

    override fun getClassType() = classType
}
