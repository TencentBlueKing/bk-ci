/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserRemoteDevResource
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeData
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeResp
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import com.tencent.devops.remotedev.pojo.Watermark
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.service.clientupgrade.ClientUpgradeService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.WatermarkService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.expert.ExpertSupportService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.tuxiaochao.TxcService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserRemoteDevResourceImpl @Autowired constructor(
    private val remoteDevSettingService: RemoteDevSettingService,
    private val workspaceService: WorkspaceService,
    private val watermarkService: WatermarkService,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val permissionService: PermissionService,
    private val expertSupportService: ExpertSupportService,
    private val txcService: TxcService,
    private val redisCache: RedisCacheService,
    private val clientUpgradeService: ClientUpgradeService
) : UserRemoteDevResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserRemoteDevResourceImpl::class.java)
    }

    override fun getRemoteDevSettings(userId: String): Result<RemoteDevSettings> {
        return Result(remoteDevSettingService.getRemoteDevSettings(userId))
    }

    override fun updateRemoteDevSettings(userId: String, remoteDevSettings: RemoteDevSettings): Result<Boolean> {
        return Result(remoteDevSettingService.updateRemoteDevSettings(userId, remoteDevSettings))
    }

    override fun getWatermark(userId: String, watermark: Watermark): Result<Any> {
        return Result(watermarkService.getWatermark(userId, watermark))
    }

    override fun getUser(userId: String): Result<String> {
        return Result(userId)
    }

    override fun getAllWindowsResourceConfig(
        userId: String,
        withUnavailable: Boolean?
    ): Result<List<WindowsResourceTypeConfig>> {
        logger.info("getAllWindowsResourceConfig|$userId|withUnavailable|$withUnavailable")
        return Result(windowsResourceConfigService.getAllType(withUnavailable, null))
    }

    override fun getAllWindowsResourceZone(userId: String): Result<List<WindowsResourceZoneConfig>> {
        logger.info("getAllWindowsResourceZone|$userId")
        return Result(windowsResourceConfigService.getAllZone())
    }

    override fun allWindowsQuota(
        projectId: String,
        userId: String,
        searchCustom: Boolean?
    ): Result<Map<String, Map<String, Int>>> {
        return Result(
            windowsResourceConfigService.allWindowsQuota(
                userId = userId,
                searchCustom = searchCustom,
                quotaType = QuotaType.OFFSHORE,
                withProjectLimit = projectId
            )
        )
    }

    override fun onePassword(userId: String, workspaceName: String): Result<String> {
        return Result(
            permissionService.init1Password(
                userId = userId,
                workspaceName = workspaceName,
                projectId = null,
                expiredInSecond = redisCache.get(RedisKeys.REDIS_1PASSWORD_EXPIRED_SECOND)?.toLongOrNull()
            )
        )
    }

    override fun addExpSup(userId: String, id: Long, workspaceName: String): Result<Boolean> {
        val (res, message) = expertSupportService.assignExpSup(userId, id, workspaceName)
        return if (message.isNullOrBlank()) {
            Result(res)
        } else {
            Result(message, res)
        }
    }

    override fun queryCgsPwd(userId: String, cgsId: String): Result<Boolean> {
        val (res, message) = expertSupportService.queryCgsPwd(userId, cgsId)
        return if (message.isNullOrBlank()) {
            Result(res)
        } else {
            Result(message, res)
        }
    }

    override fun clientUpgrade(userId: String, data: ClientUpgradeData): Result<ClientUpgradeResp> {
        return Result(clientUpgradeService.checkUpgrade(userId, data))
    }

    override fun getTxcToken(userId: String, openId: String, nickName: String, avatar: String): Result<String> {
        return Result(
            txcService.getTxcToken(
                openId = openId,
                nickName = nickName,
                avatar = avatar
            )
        )
    }
}
