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

package com.tencent.devops.common.ci.image

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.bcs.PublicBcsDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.pipeline.type.pcg.PCGDispatchType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("ALL")
enum class PoolType {
    DockerOnVm {
        override fun transfer(pool: Pool): DispatchType {
            return DockerDispatchType(
                dockerBuildVersion = pool.container,
                imageType = ImageType.THIRD,
                credentialId = pool.credential?.credentialId
            )
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.container) {
                logger.error("validatePool, {}, container is null", this)
                throw OperationException("当pool.type=$this, container参数不能为空")
            }
        }
    },

    DockerOnDevCloud {
        override fun transfer(pool: Pool): DispatchType {
            return PublicDevCloudDispathcType(
                pool.container!!,
                "0",
                imageType = ImageType.THIRD,
                credentialId = pool.credential?.credentialId
            )
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.container) {
                logger.error("validatePool, {}, container is null", this)
                throw OperationException("当pool.type=$this, container参数不能为空")
            }
        }
    },

    DockerOnBcs {
        override fun transfer(pool: Pool): DispatchType {
            return PublicBcsDispatchType(
                pool.container!!,
                "0",
                imageType = ImageType.THIRD,
                credentialId = pool.credential?.credentialId
            )
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.container) {
                logger.error("validatePool, {}, container is null", this)
                throw OperationException("当pool.type=$this, container参数不能为空")
            }
        }
    },

    DockerOnPcg {
        override fun transfer(pool: Pool): DispatchType {
            return PCGDispatchType(
                pool.container!!
            )
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.container) {
                logger.error("validatePool, {}, container is null", this)
                throw OperationException("当pool.type=$this, container参数不能为空")
            }
        }
    },

    Macos {
        override fun transfer(pool: Pool): DispatchType {
            return MacOSDispatchType(
                macOSEvn = pool.macOS!!.systemVersion + ":" + pool.macOS.xcodeVersion,
                systemVersion = pool.macOS.systemVersion,
                xcodeVersion = pool.macOS.xcodeVersion
            )
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.macOS) {
                logger.error("validatePool, pool.type:{}, macOS is null", this)
                throw OperationException("当pool.type=$this, macOS参数不能为空")
            }
            if (null == pool.macOS.systemVersion) {
                logger.error("validatePool, pool.type:{}, macOS.systemVersion is null", this)
                throw OperationException("当pool.type=$this, macOS.systemVersion参数不能为空")
            }
            if (null == pool.macOS.xcodeVersion) {
                logger.error("validatePool, pool.type:{} , macOS.xcodeVersion is null", this)
                throw OperationException("当pool.type=$this, macOS.xcodeVersion参数不能为空")
            }
        }
    },

    SelfHosted {
        override fun transfer(pool: Pool): DispatchType {
            if (!pool.envName.isNullOrBlank()) {
                return ThirdPartyAgentEnvDispatchType(
                    envName = pool.envName,
                    envProjectId = pool.envProjectId,
                    workspace = pool.workspace,
                    agentType = AgentType.NAME,
                    dockerInfo = ThirdPartyAgentDockerInfo(
                        image = pool.container ?: "",
                        credential = null,
                        options = null,
                        imagePullPolicy = null
                    )
                )
            } else if (!pool.envId.isNullOrBlank()) {
                return ThirdPartyAgentEnvDispatchType(
                    envName = pool.envId,
                    envProjectId = pool.envProjectId,
                    workspace = pool.workspace,
                    agentType = AgentType.ID,
                    dockerInfo = ThirdPartyAgentDockerInfo(
                        image = pool.container ?: "",
                        credential = null,
                        options = null,
                        imagePullPolicy = null
                    )
                )
            } else if (!pool.agentId.isNullOrBlank()) {
                return ThirdPartyAgentIDDispatchType(
                    displayName = pool.agentId!!,
                    workspace = pool.workspace,
                    agentType = AgentType.ID,
                    dockerInfo = ThirdPartyAgentDockerInfo(
                        image = pool.container ?: "",
                        credential = null,
                        options = null,
                        imagePullPolicy = null
                    )
                )
            } else {
                return ThirdPartyAgentIDDispatchType(
                    displayName = pool.agentName!!,
                    workspace = pool.workspace,
                    agentType = AgentType.NAME,
                    dockerInfo = ThirdPartyAgentDockerInfo(
                        image = pool.container ?: "",
                        credential = null,
                        options = null,
                        imagePullPolicy = null
                    )
                )
            }
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.agentName && null == pool.agentId && null == pool.envId && null == pool.envName) {
                logger.error("validatePool, pool.type:{}, agentName/agentId/envId/envName is null", this)
                throw OperationException("当pool.type=$this, agentName/agentId/envId/envName参数不能全部为空")
            }
        }
    }

    ;

    /**
     * 校验pool
     */
    protected abstract fun validatePool(pool: Pool)

    /**
     * 转换pool
     */
    protected abstract fun transfer(pool: Pool): DispatchType

    fun toDispatchType(pool: Pool): DispatchType {
        this.validatePool(pool)
        return this.transfer(pool)
    }

    protected val logger: Logger = LoggerFactory.getLogger(PoolType::class.java)
}
