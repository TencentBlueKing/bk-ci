package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("自定义产出物报告-非构建机", description = ReportArchiveServiceElement.classType)
data class ReportArchiveServiceElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "python文件编译",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("节点ID", required = false)
    val nodeId: String = "",
    @ApiModelProperty("待上传文件夹）", required = true)
    val fileDir: String = "",
    @ApiModelProperty("入口文件）", required = false)
    val indexFile: String = "",
    @ApiModelProperty("标签别名", required = true)
    val reportName: String = "",
    @ApiModelProperty("开启邮件", required = false)
    val enableEmail: Boolean?,
    @ApiModelProperty("邮件接收者", required = false)
    val emailReceivers: Set<String>?,
    @ApiModelProperty("邮件标题", required = false)
    val emailTitle: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "reportArchiveService"
    }

    override fun getTaskAtom(): String {
        return "reportArchiveServiceTaskAtom"
    }

    override fun getClassType() = classType
}
