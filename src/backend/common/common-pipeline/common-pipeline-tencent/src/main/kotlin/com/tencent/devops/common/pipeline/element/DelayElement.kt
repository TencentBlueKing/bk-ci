package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-延时任务", description = DelayElement.classType)
data class DelayElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "延时",
    @ApiModelProperty("id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("延时时间（秒）", required = true)
    val delaySeconds: Int
) : Element(name, id, status) {
    companion object {
        const val classType = "delay"
    }

    override fun getTaskAtom(): String = "delayTaskAtom"

    override fun getClassType() = classType
}
