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

package com.tencent.devops.common.pipeline.type

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.common.pipeline.type.bcs.PublicBcsDispatchType
import com.tencent.devops.common.pipeline.type.codecc.CodeCCDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.exsi.ESXiDispatchType
import com.tencent.devops.common.pipeline.type.kubernetes.KubernetesDispatchType

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "buildType", visible = false)
@JsonSubTypes(
    JsonSubTypes.Type(value = DockerDispatchType::class, name = "DOCKER"),
    JsonSubTypes.Type(value = KubernetesDispatchType::class, name = "KUBERNETES"),
    JsonSubTypes.Type(value = ESXiDispatchType::class, name = "ESXi"),
    JsonSubTypes.Type(value = ThirdPartyAgentIDDispatchType::class, name = "THIRD_PARTY_AGENT_ID"),
    JsonSubTypes.Type(value = ThirdPartyAgentEnvDispatchType::class, name = "THIRD_PARTY_AGENT_ENV"),
    JsonSubTypes.Type(value = ThirdPartyDevCloudDispatchType::class, name = "THIRD_PARTY_DEVCLOUD"),
    JsonSubTypes.Type(value = CodeCCDispatchType::class, name = "CODECC"),
    JsonSubTypes.Type(value = CodeCCDispatchType::class, name = "MACOS"),
    JsonSubTypes.Type(value = PublicBcsDispatchType::class, name = "PUBLIC_BCS")
)
abstract class DispatchType(
    open var value: String,
    @JsonIgnore
    open val routeKeySuffix: DispatchRouteKeySuffix? = null
) {

    fun replaceVariable(variables: Map<String, String>) {
        value = EnvUtils.parseEnv(value, variables)
        replaceField(variables)
    }

//    @JsonIgnore
//    fun buildType(): BuildType {
//        return when (this) {
//            is ThirdPartyAgentIDDispatchType -> {
//                BuildType.THIRD_PARTY_AGENT_ID
//            }
//            is ThirdPartyAgentEnvDispatchType -> {
//                BuildType.THIRD_PARTY_AGENT_ENV
//            }
//            is DockerDispatchType -> {
//                BuildType.DOCKER
//            }
//            else -> {
//                throw InvalidParamException("Unknown build type - $this")
//            }
//        }
//    }

    @JsonIgnore
    abstract fun buildType(): BuildType

    /**
     * 用来替换每种类型的自定义字符串
     */
    protected abstract fun replaceField(variables: Map<String, String>)

    /**
     * 保存至流水线模型前对字符串类型的值进行trim等清理操作
     */
    abstract fun cleanDataBeforeSave()
}
