package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("漏洞扫描-终端（金刚）", description = JinGangAppElement.classType)
data class JinGangAppElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "金刚app扫描",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待扫描文件（多个路径中间逗号隔开）", required = true)
    val files: String = "",
    @ApiModelProperty("源类型(PIPELINE 或者 CUSTOMIZE)", required = true)
    val srcType: String = "",
    @ApiModelProperty("扫描类型（3表示中跑静态，1表示跑静态和跑动态）", required = true)
    val runType: String = ""
) : Element(name, id, status) {

    companion object {
        const val classType = "jinGangApp"
    }

    override fun getTaskAtom(): String {
        return "jinGangAppTaskAtom"
    }

    override fun getClassType() = classType
}
