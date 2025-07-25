/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.yaml.v3.models.image

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.docker.ImageType

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Pool(
    val container: String? = null,
    val credentialId: String? = null,
    val third: Boolean? = null,
    val performanceConfigId: String? = "0",
    val env: Map<String, String>? = mapOf(),
    val type: PoolType? = null,
    val agentName: String? = null,
    val envName: String? = null,
    val envProjectId: String? = null,
    val os: VMBaseOS? = null,
    val workspace: String? = null,
    val buildType: BuildType? = BuildType.DEVCLOUD,
    val dockerInfo: ThirdPartyAgentDockerInfo? = null,
    val lockResourceWith: String? = null,
    val image: PoolImage? = null
)

data class PoolImage(
    val imageCode: String,
    val imageVersion: String,
    val imageType: ImageType? = ImageType.THIRD
)

enum class BuildType {
    DOCKER_VM,
    DEVCLOUD
}
