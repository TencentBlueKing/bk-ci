/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝盾持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝盾持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.support.model.imate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "imate 机器人类型（个人/共享）")
enum class IMateRobotScopeType {
    @Schema(title = "个人 imate")
    PERSONAL,

    @Schema(title = "共享 imate")
    SHARED,

    @Schema(title = "未知类型")
    UNKNOWN
}

@Schema(title = "imate 机器人归属类型")
enum class IMateRobotOwnerType {
    @Schema(title = "查询人自己创建")
    SELF_CREATED,

    @Schema(title = "他人分享给查询人")
    SHARED_TO_USER
}

@Schema(title = "imate 可见目标类型")
enum class IMateVisibleTargetType(private val value: String) {
    @Schema(title = "个人用户")
    USER("user"),

    @Schema(title = "组织架构")
    ORG("org"),

    @Schema(title = "未知类型")
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String?): IMateVisibleTargetType {
            return IMateVisibleTargetType.entries.firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}

@Schema(title = "imate 客户端类型")
enum class IMateClientType(
    val value: String,
    val robotScopeType: IMateRobotScopeType
) {
    @Schema(title = "个人 openclaw")
    DEVCLOUD("devcloud", IMateRobotScopeType.PERSONAL),

    @Schema(title = "共享 openclaw")
    TEAM_DEVCLOUD("team_devcloud", IMateRobotScopeType.SHARED),

    @Schema(title = "个人 hermes")
    HERMES_DEVCLOUD("hermes_devcloud", IMateRobotScopeType.PERSONAL),

    @Schema(title = "共享 hermes")
    TEAM_HERMES_DEVCLOUD("team_hermes_devcloud", IMateRobotScopeType.SHARED),

    @Schema(title = "未知类型")
    UNKNOWN("unknown", IMateRobotScopeType.UNKNOWN);

    companion object {
        fun fromValue(value: String?): IMateClientType {
            return IMateClientType.entries.firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}

@Schema(title = "imate 工作空间信息")
data class IMateRobotWorkspaceInfo(
    @get:Schema(title = "工作空间 ID")
    val id: String? = null,
    @get:Schema(title = "工作空间名称")
    val name: String? = null,
    @get:Schema(title = "用户 ID")
    val userId: String? = null,
    @get:Schema(title = "用户名")
    val username: String? = null,
    @get:Schema(title = "工作空间类型")
    val type: String? = null,
    @get:Schema(title = "环境 ID")
    val environmentId: String? = null,
    @get:Schema(title = "状态")
    val status: String? = null,
    @get:Schema(title = "删除时间")
    val deletedAt: String? = null,
    @get:Schema(title = "创建时间")
    val createdAt: String? = null,
    @get:Schema(title = "更新时间")
    val updatedAt: String? = null,
    @get:Schema(title = "扩展属性")
    val attributes: Map<String, Any>? = null
)

@Schema(title = "imate 灰度控制")
data class IMateRobotGrayControlInfo(
    @get:Schema(title = "是否开启安全模型控制")
    val safeModelControl: Boolean? = null,
    @get:Schema(title = "是否开启安全环境控制")
    val safeEnvironmentControl: Boolean? = null
)

@Schema(title = "imate 机器人信息")
data class IMateRobotInfo(
    @get:Schema(title = "机器人 ID")
    val id: Long?,
    @get:Schema(title = "机器人名称")
    val botName: String,
    @get:Schema(title = "机器人所属用户名（创建人或分享来源）")
    val username: String,
    @get:Schema(title = "imate clientUuid")
    val clientUuid: String,
    @get:Schema(title = "客户端别名")
    val clientAliasName: String? = null,
    @get:Schema(title = "客户端描述")
    val clientDescription: String? = null,
    @get:Schema(title = "客户端 IP")
    val clientIp: String? = null,
    @get:Schema(title = "工作空间 ID")
    val workspaceId: String? = null,
    @get:Schema(title = "模型名称")
    val model: String? = null,
    @get:Schema(title = "关联 Git 仓库列表")
    val gitRepos: List<Any>? = null,
    @get:Schema(title = "网关地址")
    val gatewayUrl: String? = null,
    @get:Schema(title = "运行状态，如 RUNNING")
    val status: String? = null,
    @get:Schema(title = "Webhook 地址")
    val webhookUrl: String? = null,
    @get:Schema(title = "Token")
    val token: String? = null,
    @get:Schema(title = "消息加解密密钥")
    val encodingAesKey: String? = null,
    @get:Schema(title = "创建时间")
    val createdAt: String? = null,
    @get:Schema(title = "更新时间")
    val updatedAt: String? = null,
    @get:Schema(title = "机器人 webhook 地址")
    val url: String? = null,
    @get:Schema(title = "来源")
    val source: String? = null,
    @get:Schema(title = "IDE 地址")
    val ideUrl: String? = null,
    @get:Schema(title = "是否主机器人，0/1")
    val master: Int? = null,
    @get:Schema(title = "客户端类型原始值，如 devcloud、team_devcloud")
    val clientType: String? = null,
    @get:Schema(title = "环境 ID")
    val envId: String? = null,
    @get:Schema(title = "开放环境地址")
    val openEnvUrl: String? = null,
    @get:Schema(title = "沙箱 ID")
    val sandboxId: String? = null,
    @get:Schema(title = "CVD 环境 ID")
    val cvdEnvId: String? = null,
    @get:Schema(title = "ITFS 空间 ID")
    val itfsSpaceId: String? = null,
    @get:Schema(title = "工作空间详情")
    val workspace: IMateRobotWorkspaceInfo? = null,
    @get:Schema(title = "状态变更时间")
    val statusModifyAt: String? = null,
    @get:Schema(title = "WebSocket 机器人 ID")
    val websocketBotId: String? = null,
    @get:Schema(title = "WebSocket 机器人密钥")
    val websocketBotSecret: String? = null,
    @get:Schema(title = "连接类型")
    val connectType: String? = null,
    @get:Schema(title = "OpenClaw 版本")
    val openclawVersion: String? = null,
    @get:Schema(title = "设备 ID")
    val deviceId: String? = null,
    @get:Schema(title = "设备名称")
    val deviceName: String? = null,
    @get:Schema(title = "微信机器人 Token")
    val wxBotToken: String? = null,
    @get:Schema(title = "微信 Link 机器人 ID")
    val wxLinkBotId: String? = null,
    @get:Schema(title = "微信 Link 用户 ID")
    val wxLinkUserId: String? = null,
    @get:Schema(title = "微信 Base URL")
    val wxBaseUrl: String? = null,
    @get:Schema(title = "微信机器人状态")
    val wxbotStatus: String? = null,
    @get:Schema(title = "WebSocket 机器人状态")
    val websocketBotStatus: String? = null,
    @get:Schema(title = "灰度控制")
    val grayControl: IMateRobotGrayControlInfo? = null,
    @get:Schema(title = "封面 Key")
    val coverKey: String? = null,
    @get:Schema(title = "是否强制更新")
    val forceUpdate: Boolean? = null,
    @get:Schema(title = "元宝 App Key")
    val yuanbaoAppKey: String? = null,
    @get:Schema(title = "元宝 App Secret")
    val yuanbaoAppSecret: String? = null,
    @get:Schema(title = "元宝机器人状态")
    val yuanbaoBotStatus: String? = null,
    @get:Schema(title = "太湖 Client ID")
    val taihuClientId: String? = null,
    @get:Schema(title = "机器人类型：个人或共享")
    val robotScopeType: IMateRobotScopeType,
    @get:Schema(title = "归属类型：自己创建或他人分享")
    val ownerType: IMateRobotOwnerType,
    @get:Schema(title = "OAuth 授权页地址")
    val authorizationUrl: String
)

@Schema(title = "imate 可见目标")
data class IMateVisibleTargetInfo(
    @get:Schema(title = "目标类型：用户或组织")
    val targetType: IMateVisibleTargetType,
    @get:Schema(title = "目标 ID")
    val targetId: String,
    @get:Schema(title = "目标名称")
    val targetName: String
)

@Schema(title = "imate 对话授权信息")
data class IMateAuthorizationInfo(
    @get:Schema(title = "用户是否为 imate 成员")
    val authorized: Boolean,
    @get:Schema(title = "用户是否已完成对话授权")
    val tokenAuthorized: Boolean,
    @get:Schema(title = "是否可与 imate 对话（成员且已授权）")
    val canChat: Boolean,
    @get:Schema(title = "OAuth 授权页地址")
    val authorizationUrl: String
)

@Schema(title = "imate安装任务返回")
data class IMateTaskResp(
    @get:Schema(title = "任务ID")
    val taskId: String
)
