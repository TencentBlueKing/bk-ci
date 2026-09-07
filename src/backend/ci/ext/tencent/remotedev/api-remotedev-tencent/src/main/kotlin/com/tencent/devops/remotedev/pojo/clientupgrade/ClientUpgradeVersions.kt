package com.tencent.devops.remotedev.pojo.clientupgrade

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "客户升级的所有版本信息")
data class ClientUpgradeVersions(
    @get:Schema(title = "每一次升级批次的数量")
    val parallelUpgradeCount: Int,
    @get:Schema(title = "MACOS系统,蓝盾客户端升级的版本信息")
    val macosClientVersion: UpgradeVersionsData,
    @get:Schema(title = "WINDOWS系统,蓝盾客户端升级的版本信息")
    val winClientVersion: UpgradeVersionsData,
    @get:Schema(title = "MACOS系统,START客户端升级的版本信息")
    val macosStartVersion: UpgradeVersionsData,
    @get:Schema(title = "WINDOWS系统,START客户端升级的版本信息")
    val winStartVersion: UpgradeVersionsData,
    @get:Schema(title = "ANDR系统，客户端升级版本信息")
    val andrClientVersion: UpgradeVersionsData,
    @get:Schema(title = "IOS系统,BKW手机端应用升级的版本信息")
    val iosAppVersion: UpgradeVersionsData,
    @get:Schema(title = "ANDROID系统,BKW手机端应用升级的版本信息")
    val androidAppVersion: UpgradeVersionsData,
    @get:Schema(title = "OHOS系统,BKW手机端应用升级的版本信息")
    val ohosAppVersion: UpgradeVersionsData
)

data class UpgradeVersionsData(
    @get:Schema(title = "普通升级，设定的需要升级的版本")
    val currentVersion: String,
    @get:Schema(title = "普通升级，设定的最大可以升级的个数")
    val maxCanUpgradeNumber: Int?,
    @get:Schema(title = "特殊升级，设定升级用户和其指定的版本")
    val userVersion: Map<String, String>,
    @get:Schema(title = "特殊升级，设定升级工作空间名称和其指定的版本")
    val workspaceNameVersion: Map<String, String>,
    @get:Schema(title = "特殊升级，设定升级项目ID和其指定的版本")
    val projectVersion: Map<String, String>
)