package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("海外蓝鲸-脚本执行", description = JobCloudsFastExecuteScriptElement.classType)
data class JobCloudsFastExecuteScriptElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "混合云版-作业平台-脚本执行",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("目标业务ID", required = true)
    val targetAppId: Int,
    @ApiModelProperty("脚本内容", required = true)
    val content: String = "",
    @ApiModelProperty("超时时间", required = true)
    var scriptTimeout: Int,
    @ApiModelProperty("脚本参数", required = true)
    var scriptParams: String? = null,
    @ApiModelProperty("脚本参数", required = true)
    var paramSensitive: Boolean,
    @ApiModelProperty("脚本类型", required = true)
    val type: Int,
    @ApiModelProperty("目标机器账户名", required = true)
    var account: String = "",
    @ApiModelProperty("openstate的值", required = true)
    var openState: String
) : Element(name, id, status) {
    companion object {
        const val classType = "jobCloudsFastExecuteScript"
    }

    override fun getTaskAtom(): String {
        return "jobCloudsFastExecuteScriptTaskAtom"
    }

    override fun getClassType() = classType
}
