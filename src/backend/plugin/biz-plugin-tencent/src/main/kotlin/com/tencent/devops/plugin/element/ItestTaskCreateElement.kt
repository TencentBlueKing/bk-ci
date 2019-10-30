package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ITest-创建自测任务", description = ItestTaskCreateElement.classType)
data class ItestTaskCreateElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "ITest创建自测任务",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("ITest项目ID", required = true)
    val itestProjectId: String = "",
    @ApiModelProperty("ITest API凭证", required = true)
    val ticketId: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "itestTaskCreate"
    }

    override fun getTaskAtom(): String {
        return "itestTaskCreateTaskAtom"
    }

    override fun getClassType() = classType
}
