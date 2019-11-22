package com.tencent.devops.common.ci.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.apache.tools.ant.types.Commandline

/**
 * docker run in devcloud
 */
@ApiModel("Docker通用插件")
data class DockerRunDevCloudTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: DockerRunInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    companion object {
        const val taskType = "dockerRun"
        const val taskVersion = "@latest"
        const val atomCode = "DockerRunDevCloud"
    }

    override fun getTaskType() = taskType
    override fun getTaskVersion() = taskVersion

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        val (host, imageName, imageTag) = CiYamlUtils.parseImage(inputs.image)

        val devCloudInput = DockerRunDevCloudInput(
                "dockerRun-" + System.currentTimeMillis(),
                "$imageName:$imageTag",
                jacksonObjectMapper().writeValueAsString(Registry(host, inputs.userName ?: "", inputs.password ?: "")),
                config.cpu.toString(),
                config.memory,
                jacksonObjectMapper().writeValueAsString(Params(
                        inputs.env,
                        Commandline.translateCommandline(inputs.cmd).toList(),
                        null
                ))
        )

        return MarketBuildAtomElement(
                "Docker run",
                null,
                null,
                atomCode,
                "1.*",
                mapOf("input" to devCloudInput)
        )
    }
}

data class NfsVolume(
    val server: String,
    val path: String,
    val mountPath: String
)

data class Params(
    val env: Map<String, String>?,
    val command: List<String>?,
    val nfsVolume: List<NfsVolume>?
)

data class Registry(
    val host: String,
    val username: String,
    val password: String
)

data class DockerRunDevCloudInput(
    val alias: String,
    val image: String,
    val registry: String,
    val cpu: String,
    val memory: String,
    val params: String
) : AbstractInput()

@ApiModel("Docker通用插件参数")
data class DockerRunInput(
    @ApiModelProperty("镜像名", required = true)
    val image: String,
    @ApiModelProperty("仓库信息", required = false)
    val userName: String? = null,
    @ApiModelProperty("password", required = false)
    val password: String? = null,
    @ApiModelProperty("pullType", required = true)
    val cmd: String,
    @ApiModelProperty("env", required = false)
    val env: Map<String, String>?
) : AbstractInput()