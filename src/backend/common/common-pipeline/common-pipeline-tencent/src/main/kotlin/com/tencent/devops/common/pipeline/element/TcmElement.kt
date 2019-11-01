package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("TCM任务执行", description = TcmElement.classType)
data class TcmElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "TCM任务执行",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("cc业务id", required = true)
    val appId: String = "",
    @ApiModelProperty("tcm业务id", required = true)
    val tcmAppId: String = "",
    @ApiModelProperty("模板ID", required = true)
    val templateId: String = "",
    @ApiModelProperty("任务参数", required = false)
    val workJson: List<Map<String, String>>?,
    @ApiModelProperty("是否以保存人身份执行", required = false)
    val startWithSaver: Boolean?
) : Element(name, id, status) {
    companion object {
        const val classType = "tcmElement"
    }

    override fun getTaskAtom(): String {
        return "tcmTaskAtom"
    }

    override fun getClassType() = classType
}
