package com.tencent.devops.common.pipeline.pojo.element.market

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("原子发布归档", description = AtomBuildArchiveElement.classType)
data class AtomBuildArchiveElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "原子发布归档",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("原子发布包所在相对路径", required = true)
    val filePath: String = "\${filePath}",
    @ApiModelProperty("目标", required = false)
    val destPath: String = "\${atomCode}/\${version}/\${packageName}"
) : Element(name, id, status) {

    companion object {
        const val classType = "atomBuildArchive"
    }

    override fun getClassType() = classType
}
