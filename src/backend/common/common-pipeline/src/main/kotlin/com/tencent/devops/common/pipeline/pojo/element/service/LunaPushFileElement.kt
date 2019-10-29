package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("LUNA-推送文件", description = LunaPushFileElement.classType)
data class LunaPushFileElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "LUNA-推送文件",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String = "",
    @ApiModelProperty("上传到LUNA的目录", required = false)
    val destFileDir: String?,
    @ApiModelProperty("文件来源（PIPELINE-流水线仓库、CUSTOMIZE-自定义仓库）", required = true)
    val fileSource: String = "",
    @ApiModelProperty("文件路径，支持正则表达式(不支持逗号分隔多个文件)", required = true)
    val filePath: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "lunaPushFile"
    }

    override fun getTaskAtom() = "lunaPushFileTaskAtom"

    override fun getClassType() = classType
}
