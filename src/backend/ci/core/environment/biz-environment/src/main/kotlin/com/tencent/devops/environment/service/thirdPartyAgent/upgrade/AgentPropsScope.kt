package com.tencent.devops.environment.service.thirdPartyAgent.upgrade

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.agent.AgentArchType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.pojo.thirdPartyAgent.JDKInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Agent 属性范围的所有相关配置和操作，
 * Agent版本[getAgentVersion]
 * worker版本[getWorkerVersion]
 * jdk版本串[getJdkVersion],
 * docker版本文件MD5[getDockerInitFileMd5]
 * 升级[getMaxParallelUpgradeCount]
 * 构建机构建的网关[getDefaultGateway]，和是否要启动他的配置[useDefaultGateway]
 * 构建机制品存储的网关[getDefaultFileGateway]，和是否要启动他的配置[useDefaultFileGateway]
 */
@Component
class AgentPropsScope @Autowired constructor(private val redisOperation: RedisOperation) {

    fun setAgentWorkerVersion(agentWorkerVersion: String) {
        redisOperation.set(
            key = getAgentVersionKey(),
            value = agentWorkerVersion,
            expired = false,
            isDistinguishCluster = true
        )
        logger.info("setAgentWorkerVersion| agentWorkerVersion=$agentWorkerVersion")
        invalidateCache(getAgentVersionKey(), isDistinguishCluster = true)
    }

    fun setMasterVersion(masterVersion: String) {
        redisOperation.set(
            key = getAgentMasterVersionKey(),
            value = masterVersion,
            expired = false,
            isDistinguishCluster = true
        )
        logger.info("setAgentWorkerVersion| masterVersion=$masterVersion")
        invalidateCache(getAgentMasterVersionKey(), isDistinguishCluster = true)
    }

    fun getWorkerVersion() = loadCache(getAgentVersionKey(), isDistinguishCluster = true)

    fun getAgentVersion() = loadCache(getAgentMasterVersionKey(), isDistinguishCluster = true)

    fun setJdkVersions(osArchJdkVersionSet: Set<JDKInfo>): Result<Boolean> {
        logger.info("setJdkVersions| osArchJdkVersionSet=$osArchJdkVersionSet")
        val failList = mutableSetOf<String>()
        osArchJdkVersionSet.forEach nt@{ obj ->
            val os = OS.parse(obj.os)
            if (os == null) {
                failList.add("bad os: ${obj.os}, need ${OS.values().asList()}")
                return@nt
            }

            val arch = AgentArchType.parse(obj.archType)
            if (arch == null) {
                failList.add("bad archType: ${obj.archType}, need ${AgentArchType.values().map { it.arch }}")
                return@nt
            }

            val jdkVersionString = obj.jdkVersionString
            val jdkVersionKey = getJdkVersionKey(os, arch)
            redisOperation.set(key = jdkVersionKey, value = jdkVersionString, isDistinguishCluster = true)
            invalidateCache(redisKey = jdkVersionKey, isDistinguishCluster = true)
        }
        if (failList.isNotEmpty()) {
            return Result(data = false, message = "fail list: $failList")
        }
        return success
    }

    fun getJdkVersion(os: String?, arch: String?): String? {
        return OS.parse(os)?.let { osE ->
            if (osE == OS.WINDOWS) {
                //  win目前只支持X86架构
                loadCache(getJdkVersionKey(osE, AgentArchType.AMD64))
            } else {
                // 这里的arch需要和go的编译脚本中的GOARCH统一，因为上报是根据go runtime上报的
                AgentArchType.parse(arch)?.let { archType ->
                    loadCache(getJdkVersionKey(osE, archType), isDistinguishCluster = true)
                }
            }
        }
    }

    private fun getJdkVersionKey(os: OS, arch: AgentArchType): String {
        return when (os) {
            OS.WINDOWS -> CURRENT_AGENT_WINDOWS_386_JDK_VERSION
            OS.MACOS -> if (arch == AgentArchType.ARM64) {
                CURRENT_AGENT_MACOS_ARM64_JDK_VERSION
            } else {
                CURRENT_AGENT_MACOS_AMD64_JDK_VERSION
            }

            OS.LINUX -> when (arch) {
                AgentArchType.ARM64 -> CURRENT_AGENT_LINUX_ARM64_JDK_VERSION
                AgentArchType.MIPS64 -> CURRENT_AGENT_LINUX_MIPS64_JDK_VERSION
                AgentArchType.AMD64 -> CURRENT_AGENT_LINUX_AMD64_JDK_VERSION
            }
        }
    }

    fun getDefaultGateway(): String = loadCache(DEFAULT_GATEWAY_KEY, isDistinguishCluster = false)

    fun getDefaultFileGateway(): String = loadCache(DEFAULT_FILE_GATEWAY_KEY, isDistinguishCluster = false)

    fun useDefaultGateway(): Boolean =
        loadCache(USE_DEFAULT_GATEWAY_KEY, isDistinguishCluster = false) == "true"

    fun useDefaultFileGateway(): Boolean =
        loadCache(USE_DEFAULT_FILE_GATEWAY_KEY, isDistinguishCluster = false) == "true"

    fun setMaxParallelUpgradeCount(count: Int) {
        redisOperation.set(PARALLEL_UPGRADE_COUNT, count.toString())
        logger.info("setMaxParallelUpgradeCount| count=$count")
        invalidateCache(PARALLEL_UPGRADE_COUNT, isDistinguishCluster = false)
    }

    fun getMaxParallelUpgradeCount(): Int =
        loadCache(PARALLEL_UPGRADE_COUNT, isDistinguishCluster = false).toIntOrNull() ?: DEFAULT_PARALLEL_UPGRADE_COUNT

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

    fun getDockerInitFileMd5(): String = loadCache(getDockerInitFileMd5Key())

    private fun getAgentMasterVersionKey(): String = CURRENT_AGENT_MASTER_VERSION

    private fun getAgentVersionKey(): String = CURRENT_AGENT_VERSION

    private fun getDockerInitFileMd5Key(): String = CURRENT_AGENT_LINUX_AMD64_DOCKER_INIT_FILE_MD5

    private val distinguishCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build { key -> redisOperation.get(key, isDistinguishCluster = true) ?: "" }

    private val singleCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build { key -> redisOperation.get(key, isDistinguishCluster = false) ?: "" }

    private fun loadCache(redisKey: String, isDistinguishCluster: Boolean = true): String =
        (if (isDistinguishCluster) distinguishCache.get(redisKey) else singleCache.get(redisKey)) ?: ""

    private fun invalidateCache(redisKey: String, isDistinguishCluster: Boolean) {
        if (isDistinguishCluster) {
            distinguishCache.invalidate(redisKey)
        } else {
            singleCache.invalidate(redisKey)
        }
    }

    companion object {

        private const val CACHE_EXPIRE_MIN = 1L

        private const val CACHE_SIZE = 100L

        private const val CURRENT_AGENT_MASTER_VERSION = "environment.thirdparty.agent.master.version"

        private const val CURRENT_AGENT_VERSION = "environment.thirdparty.agent.verison"

        private const val CURRENT_AGENT_WINDOWS_386_JDK_VERSION = "environment.thirdparty.agent.win_386_jdk.verison"
        private const val CURRENT_AGENT_MACOS_AMD64_JDK_VERSION = "environment.thirdparty.agent.mac_amd64_jdk.verison"
        private const val CURRENT_AGENT_MACOS_ARM64_JDK_VERSION = "environment.thirdparty.agent.mac_arm64_jdk.verison"
        private const val CURRENT_AGENT_LINUX_AMD64_JDK_VERSION = "environment.thirdparty.agent.linux_amd64_jdk.verison"
        private const val CURRENT_AGENT_LINUX_ARM64_JDK_VERSION = "environment.thirdparty.agent.linux_arm64_jdk.verison"
        private const val CURRENT_AGENT_LINUX_MIPS64_JDK_VERSION =
            "environment.thirdparty.agent.linux_mips64_jdk.verison"

        private const val CURRENT_AGENT_LINUX_AMD64_DOCKER_INIT_FILE_MD5 =
            "environment.thirdparty.agent.linux_amd64.docker_init_file.md5"

        private const val DEFAULT_GATEWAY_KEY = "environment:thirdparty:default_gateway"
        private const val DEFAULT_FILE_GATEWAY_KEY = "environment:thirdparty:default_file_gateway"
        private const val USE_DEFAULT_GATEWAY_KEY = "environment:thirdparty:use_default_gateway"
        private const val USE_DEFAULT_FILE_GATEWAY_KEY = "environment:thirdparty:use_default_file_gateway"
        private const val PARALLEL_UPGRADE_COUNT = "environment.thirdparty.agent.parallel.upgrade.count"
        private const val DEFAULT_PARALLEL_UPGRADE_COUNT = 50
        private val logger = LoggerFactory.getLogger(AgentPropsScope::class.java)
        private val success = Result(true)
    }
}
