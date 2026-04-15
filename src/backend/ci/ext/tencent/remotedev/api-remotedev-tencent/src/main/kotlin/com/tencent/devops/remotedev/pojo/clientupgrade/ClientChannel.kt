package com.tencent.devops.remotedev.pojo.clientupgrade

enum class ClientChannel(val configKey: String) {
    ALPHA("remotedev:clientChannel:alpha"),
    BETA("remotedev:clientChannel:beta"),
    GRAY("remotedev:clientChannel:gray");

    companion object {
        private val VERSION_PATTERN = Regex(
            """^\d+\.\d+\.\d+-((alpha|beta|gray)\.\d+|release)$""",
            RegexOption.IGNORE_CASE
        )

        private val RELEASE_PATTERN = Regex(
            """^\d+\.\d+\.\d+-release$""",
            RegexOption.IGNORE_CASE
        )

        fun isValidVersion(version: String): Boolean =
            VERSION_PATTERN.matches(version)

        fun isReleaseVersion(version: String): Boolean =
            RELEASE_PATTERN.matches(version)

        fun parse(channel: String): ClientChannel? =
            entries.find { it.name.equals(channel, ignoreCase = true) }

        fun parseFromVersion(version: String): ClientChannel? {
            val suffix = version.substringAfterLast("-", "")
            if (suffix.isBlank()) return null
            val channelName = suffix.substringBefore(".")
            if (channelName.equals("release", ignoreCase = true)) {
                return null
            }
            return parse(channelName)
        }

        fun getAffectedChannels(
            channel: ClientChannel?
        ): List<ClientChannel> = when (channel) {
            ALPHA -> listOf(ALPHA)
            BETA -> listOf(ALPHA, BETA)
            GRAY -> entries.toList()
            null -> entries.toList()
        }
    }
}
