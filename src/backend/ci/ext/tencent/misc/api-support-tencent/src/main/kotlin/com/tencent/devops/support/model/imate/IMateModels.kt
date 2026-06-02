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
            return values().firstOrNull { it.value == value } ?: UNKNOWN
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
    @get:Schema(title = "客户端类型原始值，如 devcloud、team_devcloud")
    val clientType: String?,
    @get:Schema(title = "机器人类型：个人或共享")
    val robotScopeType: IMateRobotScopeType,
    @get:Schema(title = "归属类型：自己创建或他人分享")
    val ownerType: IMateRobotOwnerType,
    @get:Schema(title = "运行状态，如 RUNNING")
    val status: String?,
    @get:Schema(title = "机器人 webhook 地址")
    val url: String?,
    @get:Schema(title = "OAuth 授权页地址")
    val authorizationUrl: String,
    @get:Schema(title = "创建时间")
    val createdAt: String?,
    @get:Schema(title = "更新时间")
    val updatedAt: String?
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
