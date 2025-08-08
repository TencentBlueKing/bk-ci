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

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.yaml.v3.models.job.Container2
import com.tencent.devops.process.yaml.v3.models.job.Container3
import com.tencent.devops.process.yaml.v3.models.job.Credentials
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnPoolType
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v3.models.job.RunsOn
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("ALL")
enum class PoolType {
    DockerOnVm {
        override fun transfer(pool: Pool): DispatchType {
            return DockerDispatchType(
                dockerBuildVersion = pool.container ?: pool.image?.imageCode,
                imageType = pool.image?.imageType ?: ImageType.THIRD,
                credentialId = pool.credentialId,
                imageVersion = pool.image?.imageVersion,
                imageCode = pool.image?.imageCode,
                performanceConfigId = pool.performanceConfigId?.toInt() ?: 0,
                imageName = pool.container ?: pool.image?.imageCode
            )
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.container && pool.image == null) {
                throw OperationException("When pool.type=$this, the container parameter cannot be empty")
            }
        }

        override fun transfer(dispatcher: DispatchType): RunsOn? {
            if (dispatcher is DockerDispatchType) {
                return RunsOn(
                    selfHosted = null,
                    poolName = JobRunsOnType.DOCKER.type,
                    hwSpec = dispatcher.performanceConfigId.toString(),
                    container = when (dispatcher.imageType) {
                        ImageType.BKSTORE -> Container2(
                            imageCode = dispatcher.dockerBuildVersion,
                            imageVersion = dispatcher.imageVersion,
                            credentials = dispatcher.credentialId
                        )

                        ImageType.THIRD -> Container2(
                            image = "${dispatcher.dockerBuildVersion}:${dispatcher.imageVersion}",
                            credentials = dispatcher.credentialId
                        )

                        else -> null
                    }
                )
            }
            return null
        }
    },

    SelfHosted {
        override fun transfer(pool: Pool): DispatchType {
            return when {
                /*构建资源锁定*/
                !pool.lockResourceWith.isNullOrBlank() -> ThirdPartyAgentIDDispatchType(
                    displayName = pool.lockResourceWith,
                    workspace = pool.workspace,
                    agentType = AgentType.REUSE_JOB_ID,
                    dockerInfo = pool.dockerInfo,
                    reusedInfo = null
                )

                !pool.envName.isNullOrBlank() -> ThirdPartyAgentEnvDispatchType(
                    envProjectId = pool.envProjectId,
                    envName = pool.envName,
                    workspace = pool.workspace,
                    agentType = AgentType.NAME,
                    dockerInfo = pool.dockerInfo,
                    reusedInfo = null
                )

                else -> ThirdPartyAgentIDDispatchType(
                    displayName = pool.agentName!!,
                    workspace = pool.workspace,
                    agentType = AgentType.NAME,
                    dockerInfo = pool.dockerInfo,
                    reusedInfo = null
                )
            }
        }

        override fun transfer(dispatcher: DispatchType): RunsOn? {
            if (dispatcher is ThirdPartyAgentEnvDispatchType || dispatcher is ThirdPartyAgentIDDispatchType) {
                return RunsOn(
                    selfHosted = true,
                    poolName = getPoolName(dispatcher),
                    nodeName = getNodeName(dispatcher),
                    lockResourceWith = getLockResourceWith(dispatcher),
                    poolType = getPoolType(dispatcher)?.name,
                    workspace = getWorkspace(dispatcher),
                    container = makeContainer(getDockerInfo(dispatcher)),
                    envProjectId = getEnvProjectId(dispatcher)
                )
            }
            return null
        }

        private fun getPoolName(dispatcher: DispatchType): String? = when (dispatcher) {
            is ThirdPartyAgentEnvDispatchType -> dispatcher.envName
            else -> null
        }

        private fun getNodeName(dispatcher: DispatchType): String? = when (dispatcher) {
            is ThirdPartyAgentIDDispatchType -> dispatcher.displayName
            else -> null
        }

        private fun getPoolType(dispatcher: DispatchType): JobRunsOnPoolType? = when (dispatcher) {
            is ThirdPartyAgentEnvDispatchType -> {
                when (dispatcher.agentType) {
                    AgentType.NAME -> JobRunsOnPoolType.ENV_NAME
                    AgentType.ID -> JobRunsOnPoolType.ENV_ID
                    AgentType.REUSE_JOB_ID -> JobRunsOnPoolType.AGENT_REUSE_JOB
                }
            }

            is ThirdPartyAgentIDDispatchType -> {
                when (dispatcher.agentType) {
                    AgentType.NAME -> JobRunsOnPoolType.AGENT_NAME
                    AgentType.ID -> JobRunsOnPoolType.AGENT_ID
                    AgentType.REUSE_JOB_ID -> JobRunsOnPoolType.AGENT_REUSE_JOB
                }
            }

            else -> null
        }

        private fun getWorkspace(dispatcher: DispatchType): String? = when (dispatcher) {
            is ThirdPartyAgentEnvDispatchType -> dispatcher.workspace
            is ThirdPartyAgentIDDispatchType -> dispatcher.workspace
            else -> null
        }

        private fun getDockerInfo(dispatcher: DispatchType): ThirdPartyAgentDockerInfo? = when (dispatcher) {
            is ThirdPartyAgentEnvDispatchType -> dispatcher.dockerInfo
            is ThirdPartyAgentIDDispatchType -> dispatcher.dockerInfo
            else -> null
        }

        private fun getEnvProjectId(dispatcher: DispatchType): String? = when (dispatcher) {
            is ThirdPartyAgentEnvDispatchType -> dispatcher.envProjectId
            else -> null
        }

        private fun getLockResourceWith(dispatcher: DispatchType): String? = when {
            dispatcher is ThirdPartyAgentEnvDispatchType &&
                dispatcher.agentType == AgentType.REUSE_JOB_ID -> dispatcher.value

            dispatcher is ThirdPartyAgentIDDispatchType &&
                dispatcher.agentType == AgentType.REUSE_JOB_ID -> dispatcher.value

            else -> null
        }

        private fun makeContainer(dockerInfo: ThirdPartyAgentDockerInfo?): Container3? {
            if (dockerInfo == null) return null
            // 使用研发商店镜像的情况
            if (dockerInfo.storeImage != null) {
                return Container3(
                    imageVersion = dockerInfo.storeImage?.imageVersion,
                    imageCode = dockerInfo.storeImage?.imageCode,
                    credentials = with(dockerInfo.credential) {
                        when {
                            this == null -> null
                            credentialId != null -> dockerInfo.credential?.credentialId?.ifBlank { null }
                            user != null && password != null -> Credentials(user!!, password!!)
                            else -> null
                        }
                    },
                    options = dockerInfo.options,
                    imagePullPolicy = dockerInfo.imagePullPolicy
                )
            }
            return Container3(
                image = dockerInfo.image,
                credentials = with(dockerInfo.credential) {
                    when {
                        this == null -> null
                        credentialId != null -> dockerInfo.credential?.credentialId?.ifBlank { null }
                        user != null && password != null -> Credentials(user!!, password!!)
                        else -> null
                    }
                },
                options = dockerInfo.options,
                imagePullPolicy = dockerInfo.imagePullPolicy
            )
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.agentName && null == pool.envName && null == pool.lockResourceWith) {
                throw OperationException(
                    "When pool.type=$this, " +
                        "node-name/pool-name/lock-resource-with cannot be all empty"
                )
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

    /**
     * 转换runsOn
     */
    protected abstract fun transfer(dispatcher: DispatchType): RunsOn?

    fun toRunsOn(dispatcher: DispatchType): RunsOn? {
        return this.transfer(dispatcher)
    }

    fun toDispatchType(pool: Pool): DispatchType {
        this.validatePool(pool)
        return this.transfer(pool)
    }

    protected val logger: Logger = LoggerFactory.getLogger(PoolType::class.java)
}
