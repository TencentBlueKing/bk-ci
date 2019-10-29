package com.tencent.devops.common.pipeline.pojo.element.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("部署-标准运维", description = GcloudElement.classType)
data class GcloudElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "部署-标准运维",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("业务ID", required = true)
    var appId: Int,
    @ApiModelProperty("模版ID", required = true)
    var templateId: Int,
    @ApiModelProperty("API访问授权码", required = true)
    var apiAuthCode: String = "",
    @ApiModelProperty("任务参数", required = true)
    var taskParameters: Map<String, String> = mapOf(),
    @ApiModelProperty("模版ID", required = false)
    var timeoutInSeconds: Int
) : Element(name, id, status) {
    companion object {
        const val classType = "gcloud"
    }

    override fun getTaskAtom() = "gcloudTaskAtom"

    override fun getClassType() = classType

    private fun getSetParameters(taskParameters: Map<String, String>): String {
        return ObjectMapper().writeValueAsString(taskParameters)
    }
}
