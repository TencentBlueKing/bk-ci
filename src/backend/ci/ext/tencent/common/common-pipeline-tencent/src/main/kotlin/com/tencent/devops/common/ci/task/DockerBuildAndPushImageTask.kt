package com.tencent.devops.common.ci.task

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.net.URLEncoder.encode

@ApiModel("构建并推送Docker镜像")
data class DockerBuildAndPushImageTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: DockerBuildAndPushImageInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {

    companion object {
        const val taskType = "DockerBuildAndPushImage"
        const val taskVersion = "@latest"
    }

    override fun getTaskType() = taskType
    override fun getTaskVersion() = taskVersion

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {

        val dockerBuildConvertInput = DockerBuildAndPushImageConvertInput(
            targetImageName = inputs.targetImageName,
            targetImageTag = inputs.targetImageTag,
            dockerBuildDir = inputs.dockerBuildDir,
            dockerFilePath = inputs.dockerFilePath,
            dockerBuildArgs = inputs.dockerBuildArgs,
            dockerBuildHosts = inputs.dockerBuildHosts,
            sourceRepoItemsStr = convertRepoItem(inputs.sourceRepoItems),
            targetRepoItemStr = convertRepoItem(listOf(inputs.targetRepoItem!!))
        )

        return MarketBuildAtomElement(
            "构建并推送Docker镜像",
            null,
            null,
            taskType,
            "2.*",
            mapOf("input" to dockerBuildConvertInput)
        )
    }

    private fun convertRepoItem(repoItems: List<DockerRepoItem>?): List<Map<String, String>>? {
        return repoItems?.map {
            mapOf(
                "key" to it.url,
                "value" to "${encode(it.username, "utf8")}:${encode(it.password, "utf8")}"
            )
        }
    }
}

data class DockerBuildAndPushImageInput(

    var targetImageName: String = "",
    var targetImageTag: String = "",
    val dockerBuildDir: String? = null,
    val dockerFilePath: String = "",
    val dockerBuildArgs: String? = null,
    val dockerBuildHosts: String? = null,

    // 目前工蜂ci专用
    val sourceRepoItems: List<DockerRepoItem>? = null,
    val targetRepoItem: DockerRepoItem? = null
) : AbstractInput()

data class DockerBuildAndPushImageConvertInput(
    var targetMirror: String = "",
    var targetImageName: String = "",
    var targetImageTag: String = "",
    val targetTicketId: String? = null,

    val dockerBuildDir: String? = null,
    val dockerFilePath: String = "",
    val dockerBuildArgs: String? = null,
    val dockerBuildHosts: String? = null,

    val sourceRepoItemsStr: List<Map<String, String>>? = null,
    val targetRepoItemStr: List<Map<String, String>>? = null
)

data class DockerRepoItem(
    val url: String = "",
    val username: String? = null,
    val password: String? = null
)
