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

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        val (host, imageName, imageTag) = CiYamlUtils.parseImage(inputs.image)

        val devCloudInput = DockerRunDevCloudInput(
            alias = "dockerRun-" + System.currentTimeMillis(),
            image = "$imageName:$imageTag",
            registry = jacksonObjectMapper().writeValueAsString(Registry(host, inputs.userName ?: "", inputs.password
                ?: "")),
            cpu = config.cpu.toString(),
            memory = config.memory,
            params = jacksonObjectMapper().writeValueAsString(Params(
                inputs.env,
                Commandline.translateCommandline(inputs.cmd).toList(),
                null
            ))
        )

        return MarketBuildAtomElement(
            name = displayName ?: "Docker run",
            id = null,
            status = null,
            atomCode = atomCode,
            version = "1.*",
            data = mapOf("input" to devCloudInput)
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
