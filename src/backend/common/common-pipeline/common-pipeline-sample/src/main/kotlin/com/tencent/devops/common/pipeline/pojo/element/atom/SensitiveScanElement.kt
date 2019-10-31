package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("敏感信息检查", description = SensitiveScanElement.classType)
data class SensitiveScanElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("扫描需要排除的路径，多个以分号分隔", required = false)
    val excludePath: String? = ""
) : Element(name, id, status) {

    companion object {
        const val classType = "sensitiveScan"
    }

    override fun getClassType() = classType
}
