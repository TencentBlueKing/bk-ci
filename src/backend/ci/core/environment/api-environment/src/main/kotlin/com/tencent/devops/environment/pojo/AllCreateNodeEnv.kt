package com.tencent.devops.environment.pojo

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.utils.I18nUtil

/**
 * 创作环境相关内置环境
 */

// 所有创作节点环境
object AllCreateNodeEnv {
    // WINDOWS
    private const val WINDOWS_ORIGIN_ENV_ID = 2L
    private const val WINDOWS_ENV_ID = -WINDOWS_ORIGIN_ENV_ID
    private val WINDOWS_ENV_HASH_ID = "-${HashUtil.encodeLongId(WINDOWS_ORIGIN_ENV_ID)}"
    private const val ENV_NAME_KEY = "allNodeCreateEnv" // 同windows，仅仅留作兼容老数据
    private const val WINDOWS_ENV_NAME_KEY = "allNodeCreateEnvWindows"

    // LINUX
    private const val LINUX_ORIGIN_ENV_ID = 3L
    private const val LINUX_ENV_ID = -LINUX_ORIGIN_ENV_ID
    private val LINUX_ENV_HASH_ID = "-${HashUtil.encodeLongId(LINUX_ORIGIN_ENV_ID)}"
    private const val LINUX_ENV_NAME_KEY = "allNodeCreateEnvLinux"

    // MACOS
    private const val MACOS_ORIGIN_ENV_ID = 4L
    private const val MACOS_ENV_ID = -MACOS_ORIGIN_ENV_ID
    private val MACOS_ENV_HASH_ID = "-${HashUtil.encodeLongId(MACOS_ORIGIN_ENV_ID)}"
    private const val MACOS_ENV_NAME_KEY = "allNodeCreateEnvMacos"

    fun hasId(id: Long) = id == WINDOWS_ENV_ID || id == LINUX_ENV_ID || id == MACOS_ENV_ID

    fun name(os: OS): String = when (os) {
        OS.WINDOWS -> I18nUtil.getCodeLanMessage(WINDOWS_ENV_NAME_KEY)
        OS.LINUX -> I18nUtil.getCodeLanMessage(LINUX_ENV_NAME_KEY)
        OS.MACOS -> I18nUtil.getCodeLanMessage(MACOS_ENV_NAME_KEY)
    }

    fun hasName(name: String?) = name == I18nUtil.getCodeLanMessage(ENV_NAME_KEY) ||
            name == I18nUtil.getCodeLanMessage(WINDOWS_ENV_NAME_KEY) ||
            name == I18nUtil.getCodeLanMessage(LINUX_ENV_NAME_KEY) ||
            name == I18nUtil.getCodeLanMessage(MACOS_ENV_NAME_KEY)

    fun nameToId(name: String?) = when (name) {
        ENV_NAME_KEY -> WINDOWS_ENV_ID
        WINDOWS_ENV_NAME_KEY -> WINDOWS_ENV_ID
        LINUX_ENV_NAME_KEY -> LINUX_ENV_ID
        MACOS_ENV_NAME_KEY -> MACOS_ENV_ID
        else -> null
    }

    fun hashId(os: OS): String = when (os) {
        OS.WINDOWS -> WINDOWS_ENV_HASH_ID
        OS.LINUX -> LINUX_ENV_HASH_ID
        OS.MACOS -> MACOS_ENV_HASH_ID
    }

    fun hasHashId(id: String?) = id == WINDOWS_ENV_HASH_ID || id == LINUX_ENV_HASH_ID || id == MACOS_ENV_HASH_ID

    fun hashIdToId(hashId: String?) = when (hashId) {
        null -> null
        WINDOWS_ENV_HASH_ID -> WINDOWS_ENV_ID
        LINUX_ENV_HASH_ID -> LINUX_ENV_ID
        MACOS_ENV_HASH_ID -> MACOS_ENV_ID
        else -> HashUtil.decodeIdToLong(hashId)
    }

    fun hashIdToName(hashId: String?) = when (hashId) {
        WINDOWS_ENV_HASH_ID -> I18nUtil.getCodeLanMessage(WINDOWS_ENV_NAME_KEY)
        LINUX_ENV_HASH_ID -> I18nUtil.getCodeLanMessage(LINUX_ENV_NAME_KEY)
        MACOS_ENV_HASH_ID -> I18nUtil.getCodeLanMessage(MACOS_ENV_NAME_KEY)
        else -> null
    }

    private val idList = listOf(WINDOWS_ENV_ID, LINUX_ENV_ID, MACOS_ENV_ID)

    fun idList() = idList

    private val infoList = listOf(
        AllCreateNodeEnvInfo(
            os = OS.WINDOWS,
            name = I18nUtil.getCodeLanMessage(WINDOWS_ENV_NAME_KEY),
            id = WINDOWS_ENV_ID,
            hashId = WINDOWS_ENV_HASH_ID
        ),
        AllCreateNodeEnvInfo(
            os = OS.LINUX,
            name = I18nUtil.getCodeLanMessage(LINUX_ENV_NAME_KEY),
            id = LINUX_ENV_ID,
            hashId = LINUX_ENV_HASH_ID
        ),
        AllCreateNodeEnvInfo(
            os = OS.MACOS,
            name = I18nUtil.getCodeLanMessage(MACOS_ENV_NAME_KEY),
            id = MACOS_ENV_ID,
            hashId = MACOS_ENV_HASH_ID
        )
    )

    fun list() = infoList
}

data class AllCreateNodeEnvInfo(
    val os: OS,
    val name: String,
    val id: Long,
    val hashId: String
)