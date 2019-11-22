package com.tencent.devops.common.ci.task

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * marketBuild
 */
@ApiModel("插件市场")
data class MarketBuildTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: MarketBuildInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    companion object {
        const val taskType = "marketBuild"
        const val taskVersion = "@latest"
    }

    override fun getTaskType() = taskType
    override fun getTaskVersion() = taskVersion

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        return MarketBuildAtomElement(
                displayName ?: "插件市场插件,atomCode: ${inputs.atomCode}",
                null,
                null,
                inputs.atomCode,
                inputs.version,
                inputs.data
        )
    }
}

@ApiModel("插件市场入参")
data class MarketBuildInput(
    @ApiModelProperty("atomCode", required = true)
    val atomCode: String,
    @ApiModelProperty("name", required = true)
    val name: String,
    @ApiModelProperty("原子版本", required = false)
    var version: String = "1.*",
    @ApiModelProperty("原子参数数据", required = true)
    val data: Map<String, Any> = mapOf()
) : AbstractInput()