package com.tencent.devops.remotedev.service.clientupgrade

import com.tencent.devops.remotedev.pojo.ClientUpgradeComp
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientChannel
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientOS
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeOpType
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ClientChannelUpgradeService @Autowired constructor(
    private val configCacheService: ConfigCacheService,
    private val upgradeProps: UpgradeProps
) {
    fun setChannelVersion(version: String) {
        if (version.isBlank()) {
            logger.warn("Version is blank, skip channel upgrade")
            return
        }
        if (!ClientChannel.isValidVersion(version)) {
            logger.warn(
                "Invalid version format: $version, " +
                    "expected: {major}.{minor}.{patch}-{channel}.{build} " +
                    "or {major}.{minor}.{patch}-release"
            )
            return
        }

        val channel = ClientChannel.parseFromVersion(version)
        val affectedChannels = ClientChannel.getAffectedChannels(channel)

        logger.info(
            "Setting channel version: version=$version, " +
                "parsedChannel=${channel?.name ?: "release"}, " +
                "affectedChannels=${affectedChannels.map { it.name }}"
        )

        val users = affectedChannels
            .flatMap { loadChannelUsers(it) }
            .toSet()

        if (users.isEmpty()) {
            logger.warn(
                "No users found for affected channels: " +
                    "${affectedChannels.map { it.name }}, version=$version"
            )
            return
        }

        val versionMap = users.associateWith { version }
        listOf(ClientOS.MACOS, ClientOS.WINDOWS).forEach { os ->
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = os,
                version = versionMap,
                opType = ClientUpgradeOpType.ADD
            )
        }

        logger.info(
            "Channel version set done: version=$version, " +
                "channel=${channel?.name ?: "release"}, " +
                "userCount=${users.size}"
        )
    }

    private fun loadChannelUsers(channel: ClientChannel): List<String> {
        val raw = configCacheService.get(channel.configKey)
        if (raw.isNullOrBlank()) {
            logger.warn(
                "Channel user list is empty for key: ${channel.configKey}"
            )
            return emptyList()
        }
        return raw.split(";", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    companion object {
        private val logger =
            LoggerFactory.getLogger(ClientChannelUpgradeService::class.java)
    }
}
