package com.tencent.devops.environment.model

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.agent.AgentErrorExitData
import com.tencent.devops.common.api.pojo.agent.DockerInitFileInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.pojo.thirdpartyagent.create.AgentPropsSource
import org.slf4j.LoggerFactory

/**
 * Agent 系统属性
 * @see com.tencent.devops.environment.model arch 系统架构
 * @param jdkVersion jdk版本
 * @param dockerInitFileInfo dockerInit文件信息
 * @param exitError agent错误退出信息
 * @param osVersion 系统版本信息
 * @param source 来源信息
 */
data class AgentProps(
    val arch: String,
    val jdkVersion: List<String>,
    val userProps: Map<String, Any>?,
    val dockerInitFileInfo: DockerInitFileInfo?,
    val exitError: AgentErrorExitData?,
    val osVersion: String?,
    val source: AgentPropsSource? = null,
    val sdk: Boolean? = false
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AgentProps::class.java)

        fun parseAgentProps(props: String?): AgentProps? {
            return if (props.isNullOrBlank()) {
                null
            } else {
                try {
                    JsonUtil.to(props, AgentProps::class.java)
                } catch (e: Exception) {
                    // 兼容老数据格式不对的情况
                    null
                }
            }
        }

        fun emptyBySource(source: AgentPropsSource) = AgentProps(
            arch = "",
            jdkVersion = emptyList(),
            userProps = emptyMap(),
            dockerInitFileInfo = null,
            exitError = null,
            osVersion = null,
            source = source
        )

        fun emptyBySdk(sdk: Boolean) = AgentProps(
            arch = "",
            jdkVersion = emptyList(),
            userProps = emptyMap(),
            dockerInitFileInfo = null,
            exitError = null,
            osVersion = null,
            source = null,
            sdk = sdk
        )

        fun getSourceFromRecord(props: String?, os: OS?): AgentPropsSource {
            val source = if (props == null) {
                null
            } else {
                try {
                    JsonUtil.to(props, object : TypeReference<AgentProps>() {}).source
                } catch (e: Exception) {
                    logger.warn("Failed to parse agent props source|props=$props", e)
                    null
                }
            }
            if (source == AgentPropsSource.DEVCLOUD || (source == null && os == OS.LINUX)) {
                return AgentPropsSource.DEVCLOUD
            }
            if (source == AgentPropsSource.REMOTEDEV || (source == null && os == OS.WINDOWS)) {
                return AgentPropsSource.REMOTEDEV
            }
            return AgentPropsSource.IEG_IMATE
        }
    }
}
