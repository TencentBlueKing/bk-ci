package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.ktlint.KtlintReporter
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ktlint代码静态检查", description = KtlintStyleElement.classType)
class KtlintStyleElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行Linux脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("代码存放路径", required = false)
    val path: String? = null,
    @ApiModelProperty("ktlint要检查的文件pattern", required = false)
    val patterns: String?,
    @ApiModelProperty("ktlint命令行参数", required = false)
    val flags: String?,
    @ApiModelProperty("ktlint reporters", required = false)
    val reporters: List<KtlintReporter>?
) : Element(name, id, status) {

    override fun getClassType() = classType

    companion object {
        const val classType = "ktlint"
    }
}
