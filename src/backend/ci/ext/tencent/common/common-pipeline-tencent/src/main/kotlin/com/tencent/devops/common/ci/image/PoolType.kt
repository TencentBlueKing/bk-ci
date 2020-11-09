package com.tencent.devops.common.ci.image

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.pipeline.type.pcg.PCGDispatchType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
                logger.error("validatePool, {} , container is null", this)
                throw OperationException("当 pool.type = $this, container参数不能为空")
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
                logger.error("validatePool, {} , container is null", this)
                throw OperationException("当 pool.type = $this, container参数不能为空")
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
                logger.error("validatePool, {} , container is null", this)
                throw OperationException("当 pool.type = $this, container参数不能为空")
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
                logger.error("validatePool , pool.type:{} , macOS is null", this)
                throw OperationException("当 pool.type = ${this}, macOS参数不能为空")
            }
            if (null == pool.macOS.systemVersion) {
                logger.error("validatePool , pool.type:{} , macOS.systemVersion is null", this)
                throw OperationException("当 pool.type = ${this}, macOS.systemVersion参数不能为空")
            }
            if (null == pool.macOS.xcodeVersion) {
                logger.error("validatePool , pool.type:{} , macOS.xcodeVersion is null", this)
                throw OperationException("当 pool.type = ${this}, macOS.xcodeVersion参数不能为空")
            }
        }
    },

    SelfHosted {
        override fun transfer(pool: Pool): DispatchType {
            if (!pool.envName.isNullOrBlank()) {
                return ThirdPartyAgentEnvDispatchType(
                    envName = pool.envName!!,
                    workspace = pool.workspace,
                    agentType = AgentType.NAME
                )
            } else if (!pool.envId.isNullOrBlank()) {
                return ThirdPartyAgentEnvDispatchType(
                    envName = pool.envId!!,
                    workspace = pool.workspace,
                    agentType = AgentType.ID
                )
            } else if (!pool.agentId.isNullOrBlank()) {
                return ThirdPartyAgentIDDispatchType(
                    displayName = pool.agentId!!,
                    workspace = pool.workspace,
                    agentType = AgentType.ID
                )
            } else {
                return ThirdPartyAgentIDDispatchType(
                    displayName = pool.agentName!!,
                    workspace = pool.workspace,
                    agentType = AgentType.NAME
                )
            }
        }

        override fun validatePool(pool: Pool) {
            if (null == pool.agentName || null == pool.agentId || null == pool.envId || null == pool.envName) {
                logger.error("validatePool , pool.type:{} , agentName/agentId/envId/envName is null", this)
                throw OperationException("当 pool.type = ${this}, agentName/agentId/envId/envName参数不能全部为空")
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