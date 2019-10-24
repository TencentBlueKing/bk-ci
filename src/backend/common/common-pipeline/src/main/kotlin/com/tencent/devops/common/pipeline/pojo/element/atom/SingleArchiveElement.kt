package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("归档构件", description = SingleArchiveElement.classType)
data class SingleArchiveElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "python文件编译",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待上传文件路径（单个路径，不支持**匹配，文件夹必须斜杠/结尾）", required = true)
    val filePath: String = "",
    @ApiModelProperty("上传到的目标路径（仅在自定义归档选择才用到）", required = false)
    val destPath: String = "",
    @ApiModelProperty("是否自定义归档", required = true)
    val customize: Boolean
) : Element(name, id, status) {

    companion object {
        const val classType = "singleArchive"
    }

    override fun getClassType() = classType
}
