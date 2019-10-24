package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.net.URLEncoder

@ApiModel("脚本任务（linux和macOS环境）", description = LinuxScriptElement.classType)
data class LinuxScriptElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行Linux脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("脚本类型", required = true)
    val scriptType: BuildScriptType,
    @ApiModelProperty("脚本内容", required = true)
    val script: String,
    @ApiModelProperty("某次执行为非0时（失败）是否继续执行脚本", required = false)
    val continueNoneZero: Boolean?,
    @ApiModelProperty("启用脚本执行失败时归档的文件", required = false)
    val enableArchiveFile: Boolean? = false,
    @ApiModelProperty("脚本执行失败时归档的文件", required = false)
    val archiveFile: String? = null
) : Element(name, id, status) {

    companion object {
        const val classType = "linuxScript"
    }

    override fun genTaskParams(): MutableMap<String, Any> {
        val mutableMap = super.genTaskParams()
        // 帮助转化
        mutableMap["script"] = URLEncoder.encode(script, "UTF-8")
        return mutableMap
    }

    override fun getClassType() = classType
}
