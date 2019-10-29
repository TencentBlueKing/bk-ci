package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ITest-创建自测单", description = ItestProcessCreateElement.classType)
data class ItestProcessCreateElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "ITest创建自测单",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("ITest项目ID", required = true)
    val itestProjectId: String = "",
    @ApiModelProperty("ITest API凭证", required = true)
    val ticketId: String = "",
    @ApiModelProperty("版本类型", required = true)
    val versionType: String = "",
    @ApiModelProperty("版本号", required = true)
    val versionName: String = "",
    @ApiModelProperty("基线号", required = true)
    val baselineName: String = "",
    @ApiModelProperty("测试说明", required = true)
    val description: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "itestProcessCreate"
    }

    override fun getTaskAtom(): String {
        return "itestProcessCreateTaskAtom"
    }

    override fun getClassType() = classType
}
