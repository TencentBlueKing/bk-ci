package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("meta文件扫描任务", description = MetaFileScanElement.classType)
data class MetaFileScanElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "meta文件扫描",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("扫描结果异常是否置任务失败", required = false)
    val isScanException: Boolean = false
) : Element(name, id, status) {
    companion object {
        const val classType = "metaFileScan"
    }

    override fun getClassType() = classType
}
