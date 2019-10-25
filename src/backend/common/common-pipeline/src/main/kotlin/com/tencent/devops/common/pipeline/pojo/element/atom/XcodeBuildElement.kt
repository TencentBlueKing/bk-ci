package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("xcode构建任务", description = XcodeBuildElement.classType)
data class XcodeBuildElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "xcode构建任务",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("xcodeproj的相对路径,例如project-aa/Unity-iPhone.xcodeproj", required = true)
    val project: String = "",
    @ApiModelProperty("xcodebuild的-scheme参数值", required = false)
    val scheme: String = "",
    @ApiModelProperty("xcodebuild的-configuration参数值", required = false)
    val configuration: String = "",
    @ApiModelProperty("IOS IPA所在的目录， 默认是result", required = false)
    val iosPath: String = "result",
    @ApiModelProperty("IOS IPA名, 默认是output.ipa", required = false)
    val iosName: String = "output.ipa",
    @ApiModelProperty("XCode 工程的根目录", required = false)
    val rootDir: String = "",
    @ApiModelProperty("启用或禁用bitCode模式, 默认禁止", required = false)
    val enableBitCode: Boolean = false,
    @ApiModelProperty("证书id", required = true)
    val certId: String = "",
    @ApiModelProperty("xcode编译额外参数", required = false)
    val extraParams: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "xcodeBuild"
    }

    override fun getClassType() = classType
}
