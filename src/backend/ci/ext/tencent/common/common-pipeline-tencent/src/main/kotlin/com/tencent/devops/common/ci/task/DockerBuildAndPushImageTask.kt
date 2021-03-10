/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.ci.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
            name = displayName ?: "构建并推送Docker镜像",
            id = null,
            status = null,
            atomCode = taskType,
            version = "2.*",
            data = mapOf("input" to dockerBuildConvertInput)
        )
    }

    private fun convertRepoItem(repoItems: List<DockerRepoItem>?): String? {
        val obj = repoItems?.map {
            mapOf(
                "key" to it.url,
                "value" to "${encode(it.username, "utf8")}:${encode(it.password, "utf8")}"
            )
        } ?: listOf()
        return jacksonObjectMapper().writeValueAsString(obj)
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

    val sourceRepoItemsStr: String? = null,
    val targetRepoItemStr: String? = null
)

data class DockerRepoItem(
    val url: String = "",
    val username: String? = null,
    val password: String? = null
)
