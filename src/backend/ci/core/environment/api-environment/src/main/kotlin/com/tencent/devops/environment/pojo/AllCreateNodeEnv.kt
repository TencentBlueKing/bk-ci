package com.tencent.devops.environment.pojo

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.utils.I18nUtil

/**
 * 创作环境相关内置环境
 */

// 所有创作节点环境
object AllCreateNodeEnv {
    private const val WINDOWS_ENV_ID = -2L
    private const val LINUX_ENV_ID = -3L
    private const val MACOS_ENV_ID = -4L

    // 同windows，留作兼容老数据
    private const val ENV_NAME_KEY = "allNodeCreateEnv"
    private const val WINDOWS_ENV_NAME_KEY = "allNodeCreateEnv"
    private const val LINUX_ENV_NAME_KEY = "allNodeCreateEnvLinux"
    private const val MACOS_ENV_NAME_KEY = "allNodeCreateEnvMacos"
    fun name(os: OS): String = when (os) {
        OS.WINDOWS -> I18nUtil.getCodeLanMessage(WINDOWS_ENV_NAME_KEY)
        OS.LINUX -> I18nUtil.getCodeLanMessage(LINUX_ENV_NAME_KEY)
        OS.MACOS -> I18nUtil.getCodeLanMessage(MACOS_ENV_NAME_KEY)
    }

    fun hashId(os: OS): String = when (os) {
        OS.WINDOWS -> "-${HashUtil.encodeLongId(2)}"
        OS.LINUX -> "-${HashUtil.encodeLongId(3)}"
        OS.MACOS -> "-${HashUtil.encodeLongId(4)}"
    }

    fun hasName(name: String) = name == I18nUtil.getCodeLanMessage(ENV_NAME_KEY) ||
            name == I18nUtil.getCodeLanMessage(WINDOWS_ENV_NAME_KEY) ||
            name == I18nUtil.getCodeLanMessage(LINUX_ENV_NAME_KEY) ||
            name == I18nUtil.getCodeLanMessage(MACOS_ENV_NAME_KEY)

    fun hasHashId(id: String) = id == "-${HashUtil.encodeLongId(2)}" ||
            id == "-${HashUtil.encodeLongId(3)}" ||
            id == "-${HashUtil.encodeLongId(4)}"

    fun hashIdToId(hashId: String) = when (hashId) {
        "-${HashUtil.encodeLongId(2)}" -> WINDOWS_ENV_ID
        "-${HashUtil.encodeLongId(3)}" -> LINUX_ENV_ID
        "-${HashUtil.encodeLongId(4)}" -> MACOS_ENV_ID
        else -> HashUtil.decodeIdToLong(hashId)
    }
}