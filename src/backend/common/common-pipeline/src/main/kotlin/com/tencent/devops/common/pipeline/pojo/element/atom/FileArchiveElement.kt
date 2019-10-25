package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Deprecated("已作废")
@ApiModel("归档构件(旧)", description = FileArchiveElement.classType)
data class FileArchiveElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "文件归档",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待上传文件路径（多个路径中间逗号隔开），支持正则表达式", required = true)
    val regexPaths: String = ""
) : Element(name, id, status) {

    companion object {
        const val classType = "fileArchive"
    }

    override fun getClassType() = classType
}
