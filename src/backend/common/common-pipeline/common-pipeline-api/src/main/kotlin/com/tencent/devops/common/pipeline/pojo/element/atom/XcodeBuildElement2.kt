package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("xcode构建任务二(支持xcode10)", description = XcodeBuildElement2.classType)
data class XcodeBuildElement2(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "xcode构建任务",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("xcode工程代码根目录", required = true)
    val project: String = "",
    @ApiModelProperty("证书id", required = true)
    val certId: String = "",
    @ApiModelProperty("xcodebuild的-scheme参数值", required = true)
    val scheme: String = "",
    @ApiModelProperty("xcodebuild的-configuration参数值", required = false)
    val configuration: String = "",
    @ApiModelProperty("生成的IPA包存放目录， 默认是result", required = false)
    val ipaPath: String = "result",
    @ApiModelProperty("xcodebuild对应的method(默认Development，另外还有enterprise，app-store，ad-hoc，package，development-id，mac-application)", required = false)
    val method: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "xcodeBuild2"
    }

    override fun getClassType() = classType
}
