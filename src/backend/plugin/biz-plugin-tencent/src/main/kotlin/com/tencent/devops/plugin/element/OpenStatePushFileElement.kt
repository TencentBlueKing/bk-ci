package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构件分发-openstate(IEG专用)", description = OpenStatePushFileElement.classType)
data class OpenStatePushFileElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "构件分发-openstate(IEG专用)",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("cc应用id", required = true)
    val ccAppId: String = "",
    @ApiModelProperty("源类型(PIPELINE-流水线仓库, CUSTOMIZE-自定义仓库)", required = true)
    var srcType: String = "",
    @ApiModelProperty("源路径", required = true)
    var srcPath: String = "",
    @ApiModelProperty("OpenState的值", required = true)
    var openState: String = "",
    @ApiModelProperty("最大运行时间，默认600分钟", required = true)
    var maxRunningTime: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "openStatePushFile"
    }

    override fun getTaskAtom(): String {
        return "openStatePushFileTaskAtom"
    }

    override fun getClassType() = classType
}
