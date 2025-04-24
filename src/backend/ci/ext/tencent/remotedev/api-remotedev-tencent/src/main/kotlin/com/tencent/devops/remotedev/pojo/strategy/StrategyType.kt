package com.tencent.devops.remotedev.pojo.strategy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import io.swagger.v3.oas.annotations.media.Schema

enum class StrategyType {
    PERSONAL_DESKTOP_LOCK_SCREEN,
    PUBLIC_DESKTOP_LOCK_SCREEN;

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
    val exitMin: Long?
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
    val exitMin: Long?
) {
    companion object {
        fun default() = PublicDesktopLockScreenStrategy(null, null)
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
    val publicDesktopLockScreenStrategy: PublicDesktopLockScreenStrategy?
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
    var publicDesktopLockScreenStrategy: PublicDesktopLockScreenStrategy?
)
