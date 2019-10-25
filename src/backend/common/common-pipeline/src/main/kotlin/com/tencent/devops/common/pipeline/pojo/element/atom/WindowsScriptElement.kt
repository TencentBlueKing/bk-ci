package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.net.URLEncoder

@ApiModel("脚本任务（windows环境）", description = WindowsScriptElement.classType)
data class WindowsScriptElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行Windows的bat脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("脚本内容", required = true)
    val script: String,
    @ApiModelProperty("脚本类型", required = true)
    val scriptType: BuildScriptType
) : Element(name, id, status) {

    companion object {
        const val classType = "windowsScript"
    }

    override fun genTaskParams(): MutableMap<String, Any> {
        val mutableMap = super.genTaskParams()
        // 帮助转化
        mutableMap["script"] = URLEncoder.encode(script, "UTF-8")
        return mutableMap
    }

    override fun getClassType() = classType
}
