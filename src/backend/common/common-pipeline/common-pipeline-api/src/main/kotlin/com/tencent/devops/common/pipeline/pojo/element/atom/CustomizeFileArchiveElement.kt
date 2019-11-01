package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("归档至自定义版本仓库(已废弃)", description = CustomizeFileArchiveElement.classType)
data class CustomizeFileArchiveElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "归档至自定义版本仓库",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待上传文件路径（多个路径中间逗号隔开），支持正则表达式", required = true)
    val uploadPaths: String,
    @ApiModelProperty("上传文件路径，默认为bk-custom/{projectCode}目录", required = true)
    val destPath: String = ""
) : Element(name, id, status) {

    companion object {
        const val classType = "customizeFileArchive"
    }

    override fun getClassType() = classType
}
