package com.tencent.devops.remotedev.pojo.strategy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import io.swagger.v3.oas.annotations.media.Schema

enum class StrategyType {
    PERSONAL_DESKTOP_LOCK_SCREEN,
    PUBLIC_DESKTOP_LOCK_SCREEN,
    SILENT_UPGRADE,
    QUIT_ON_EXIT,
    LOCK_SCREEN_SAVER;

    companion object {
        fun fromName(name: String): StrategyType? =
            values().find { it.name == name }
    }
}

@Schema(title = "个人云桌面锁屏策略")
@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonalDesktopLockScreenStrategy(
    @get:Schema(title = "闲置分钟后锁屏，为空就是关闭功能", required = false)
    val lockScreenMin: Long?,
    @get:Schema(title = "闲置分钟后退出，为空就是关闭功能", required = false)
    val exitMin: Long?,
    @get:Schema(title = "全局锁屏时间，分钟。仅查询返回，未配置时为空", required = false)
    val globalLockScreenMin: Long? = null
) {
    companion object {
        fun default() = PersonalDesktopLockScreenStrategy(null, null)
    }
}

@Schema(title = "公共云桌面锁屏策略")
@JsonIgnoreProperties(ignoreUnknown = true)
data class PublicDesktopLockScreenStrategy(
    @get:Schema(title = "闲置分钟后锁屏，为空就是关闭功能", required = false)
    val lockScreenMin: Long?,
    @get:Schema(title = "闲置分钟后退出，为空就是关闭功能", required = false)
    val exitMin: Long?,
    @get:Schema(title = "全局锁屏时间，分钟。仅查询返回，未配置时为空", required = false)
    val globalLockScreenMin: Long? = null
) {
    companion object {
        fun default() = PublicDesktopLockScreenStrategy(null, null)
    }
}

@Schema(title = "静默升级策略")
@JsonIgnoreProperties(ignoreUnknown = true)
data class SilentUpgradeStrategy(
    @get:Schema(title = "是否开启静默升级", required = true)
    val enabled: Boolean
) {
    companion object {
        fun default() = SilentUpgradeStrategy(false)
    }
}

@Schema(title = "退出云桌面时退出登录策略")
@JsonIgnoreProperties(ignoreUnknown = true)
data class QuitOnExitStrategy(
    @get:Schema(title = "退出时是否退出登录", required = true)
    val enabled: Boolean
) {
    companion object {
        fun default() = QuitOnExitStrategy(false)
    }
}

@Schema(title = "锁屏自定义屏保策略")
@JsonIgnoreProperties(ignoreUnknown = true)
data class LockScreenSaverStrategy(
    @get:Schema(title = "是否开启自定义屏保", required = true)
    val enabled: Boolean,
    @get:Schema(title = "屏保图片COS路径，如 public/csig/csig-blue.png", required = false)
    val path: String? = null,
    @get:Schema(title = "签名后的屏保图片下载地址，仅查询时返回", required = false)
    val url: String? = null
) {
    companion object {
        fun default() = LockScreenSaverStrategy(false)
    }
}

@Schema(title = "项目策略修改信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectStrategyInfo(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "区域信息", required = true)
    val zoneType: WindowsResourceZoneConfigType,
    @get:Schema(title = "需要修改的策略类型列表")
    val typeList: List<StrategyType>,
    @get:Schema(title = "个人云桌面锁屏策略")
    val personalDesktopLockScreenStrategy: PersonalDesktopLockScreenStrategy?,
    @get:Schema(title = "公共云桌面锁屏策略")
    val publicDesktopLockScreenStrategy: PublicDesktopLockScreenStrategy?,
    @get:Schema(title = "静默升级策略")
    val silentUpgradeStrategy: SilentUpgradeStrategy? = null,
    @get:Schema(title = "退出云桌面时退出登录策略")
    val quitOnExitStrategy: QuitOnExitStrategy? = null,
    @get:Schema(title = "锁屏自定义屏保策略")
    val lockScreenSaverStrategy: LockScreenSaverStrategy? = null
)

@Schema(title = "项目策略查询信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectStrategyFetchInfo(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "区域信息", required = true)
    val zoneType: WindowsResourceZoneConfigType,
    @get:Schema(title = "需要修改的策略类型列表")
    val typeList: List<StrategyType>
)

@Schema(title = "项目策略信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectStrategyResp(
    @get:Schema(title = "个人云桌面锁屏策略")
    var personalDesktopLockScreenStrategy: PersonalDesktopLockScreenStrategy?,
    @get:Schema(title = "公共云桌面锁屏策略")
    var publicDesktopLockScreenStrategy: PublicDesktopLockScreenStrategy?,
    @get:Schema(title = "静默升级策略")
    var silentUpgradeStrategy: SilentUpgradeStrategy? = null,
    @get:Schema(title = "退出云桌面时退出登录策略")
    var quitOnExitStrategy: QuitOnExitStrategy? = null,
    @get:Schema(title = "锁屏自定义屏保策略")
    var lockScreenSaverStrategy: LockScreenSaverStrategy? = null
)
