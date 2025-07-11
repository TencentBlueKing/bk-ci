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

package com.tencent.devops.dispatch.pojo.thirdpartyagent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.common.pipeline.type.agent.Credential
import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch

// 用来下发给agent的docker信息，用来处理一些调度时和下发时的差异数据
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyBuildDockerInfo(
    val agentId: String,
    val secretKey: String,
    val image: String,
    val credential: ThirdPartyBuildDockerInfoCredential?,
    val options: DockerOptions?,
    val imagePullPolicy: String?
) {
    constructor(input: ThirdPartyAgentDockerInfoDispatch) : this(
        agentId = input.agentId,
        secretKey = input.secretKey,
        image = input.image,
        credential = ThirdPartyBuildDockerInfoCredential(input.credential),
        options = input.options,
        imagePullPolicy = input.imagePullPolicy
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyBuildDockerInfoCredential(
    var user: String?,
    var password: String?,
    // 获取凭据失败的错误信息用来给agent上报使用
    var errMsg: String?
) {
    constructor(input: Credential?) : this(
        user = input?.user,
        password = input?.password,
        errMsg = null
    )
}
