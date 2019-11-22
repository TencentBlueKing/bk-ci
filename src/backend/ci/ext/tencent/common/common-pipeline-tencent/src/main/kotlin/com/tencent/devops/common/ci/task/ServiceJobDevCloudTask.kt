package com.tencent.devops.common.ci.task

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * ServiceJobDevCloudTask
 */
@ApiModel("创建DevCloud容器（GIT_CI工蜂专用）")
data class ServiceJobDevCloudTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: ServiceJobDevCloudInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    companion object {
        const val taskType = "serviceJobDevCloud"
        const val taskVersion = "@latest"
        const val atomCode = "CreateDevnetContainer"
    }

    override fun getTaskType() = taskType
    override fun getTaskVersion() = taskVersion

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        return MarketBuildAtomElement(
                "拉代码",
                null,
                null,
                atomCode,
                "1.*",
                mapOf("input" to inputs)
        )
    }
}

@ApiModel("创建DevCloud容器")
data class ServiceJobDevCloudInput(
    val image: String,
    @ApiModelProperty("镜像仓库地址", required = false)
    val registryHost: String?,
    @ApiModelProperty("登录镜像仓库使用的用户名", required = false)
    val registryUsername: String?,
    @ApiModelProperty("镜像仓库密码", required = false)
    val registryPassword: String?,
    @ApiModelProperty("参数", required = false)
    val params: String?,
    @ApiModelProperty("服务环境变量", required = false)
    val serviceEnv: String?
) : AbstractInput()