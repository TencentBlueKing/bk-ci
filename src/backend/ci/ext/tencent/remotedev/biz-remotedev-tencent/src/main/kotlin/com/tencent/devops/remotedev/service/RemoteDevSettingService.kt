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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.client.TaiUserInfoRequest
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.transfer.GitTransferService
import com.tencent.devops.remotedev.service.transfer.GithubTransferService
import com.tencent.devops.remotedev.service.transfer.TGitTransferService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevSettingService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val gitTransferService: GitTransferService,
    private val tGitTransferService: TGitTransferService,
    private val githubTransferService: GithubTransferService,
    private val redisCacheService: RedisCacheService,
    private val whiteListService: WhiteListService,
    private val taiClient: TaiClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevSettingService::class.java)
    }

    fun getRemoteDevSettings(userId: String): RemoteDevSettings {
        logger.info("$userId get remote dev setting")
        val setting = remoteDevSettingDao.fetchOneSetting(dslContext, userId)
        return setting.copy(
            envsForFile = emptyList(),
            gitAttached = false,
            tGitAttached = false,
            githubAttached = false
        )
    }

    fun userWinTimeLeft(userId: String): Int {
        val time = remoteDevSettingDao.fetchSingleUserWinTimeLeft(dslContext, userId)
        logger.info("get user Win time left $time")
        return time ?: (startCloudExperienceDuration(userId) * 60 * 60)
    }

    fun updateRemoteDevSettings(userId: String, setting: RemoteDevSettings): Boolean {
        logger.info("$userId get remote dev setting")
        remoteDevSettingDao.createOrUpdateSetting(dslContext, setting, userId)
        return true
    }

    fun renewalExperienceDuration(userId: String, time: Int): Boolean {
        logger.info("$userId renewalExperienceDuration")
        val setting = remoteDevSettingDao.fetchAnyUserSetting(dslContext, userId)
        val data = OPUserSetting(
            userIds = listOf(userId),
            maxRunningCount = setting.maxRunningCount,
            maxHavingCount = setting.maxHavingCount,
            onlyCloudIDE = setting.onlyCloudIDE,
            allowedDownload = setting.allowedDownload,
            needWatermark = setting.needWatermark,
            autoDeletedDays = setting.autoDeletedDays,
            mountType = setting.mountType,
            startCloudExperienceDuration = setting.startCloudExperienceDuration?.plus(time),
            allowedCopy = setting.allowedCopy,
            clientWhiteList = setting.clientWhiteList,
            grayFlag = false,
            startWhiteList = setting.startWhiteList
        )

        remoteDevSettingDao.createOrUpdateSetting4OP(dslContext, userId, data)

        return true
    }

    fun updateSetting4Op(data: OPUserSetting) {
        logger.info("updateSettingByOp $data")
        data.userIds.forEach { userId ->
            remoteDevSettingDao.createOrUpdateSetting4OP(dslContext, userId, data)
            // 根据OPUserSetting中设置是否开启客户端白名单 + START白名单，分别做处理
            data.clientWhiteList?.let { isEnabled ->
                if (isEnabled) {
                    whiteListService.addWhiteListUser(userId = ADMIN_NAME, whiteListUser = userId)
                } else {
                    whiteListService.removeWhiteListUser(userId = ADMIN_NAME, whiteListUser = userId)
                }
            }

            data.startWhiteList?.let { isEnabled ->
                if (isEnabled) {
                    whiteListService.addGPUWhiteListUser(userId = ADMIN_NAME, whiteListUser = userId, override = true)
                } else {
                    whiteListService.removeGPUWhiteListUser(userId = ADMIN_NAME, whiteListUser = userId)
                }
            }
        }
    }

    fun getUserSetting(userId: String): RemoteDevUserSettings {
        logger.info("$userId get user setting")
        return remoteDevSettingDao.fetchAnyUserSetting(dslContext, userId)
    }

    fun startCloudExperienceDuration(userId: String): Int {
        return remoteDevSettingDao.fetchAnyUserSetting(dslContext, userId).startCloudExperienceDuration
            ?: redisCacheService.get(RedisKeys.REDIS_DEFAULT_AVAILABLE_TIME)?.toInt() ?: 24
    }

    fun getAllUserSetting4Op(queryUser: String?, page: Int?, pageSize: Int?): Page<RemoteDevUserSettings> {
        logger.info("Start to getAllUserSetting4Op")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val count = remoteDevSettingDao.countAllUserSettings(
            dslContext = dslContext,
            queryUser = queryUser
        )
        val settings = remoteDevSettingDao.fetchAllUserSettings(
            dslContext = dslContext,
            queryUser = queryUser,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        )
            .mapNotNull {
                JsonUtil.toOrNull(it.userSetting, RemoteDevUserSettings::class.java)?.apply {
                    userId = it.userId
                    remainExperienceDuration = it.winUsageRemainingTime
                    clientWhiteList = whiteListService.checkInWhiteList(it.userId)
                    startWhiteList = whiteListService.checkInGPUWhiteList(it.userId)
                }
            }
        logger.info("getAllUserSetting4Op|result|$settings")
        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = settings
        )
    }

    /**
     * op接口使用，批量更新太湖账号信息
     */
    fun updateAllTaiUserInfo(userIds: List<String> = emptyList()) {
        val update = userIds.isNotEmpty()
        var page = 1
        val pageSize = 50
        while (true) {
            val taiUsers = remoteDevSettingDao.fetchTaiUserInfo(
                dslContext = dslContext,
                limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize),
                userIds = userIds.filter { UserUtil.isTaiUser(it) }.toSet().ifEmpty { null }
            )
            val notInit = taiUsers.filter {
                (it.value["EMAIL"] as String).isBlank() ||
                    (it.value["PHONE"] as String).isBlank() ||
                    (it.value["PHONE_COUNTRY_CODE"] as String).isBlank() || update
            }
            val taiInfos = taiClient.taiUserInfo(TaiUserInfoRequest(usernames = notInit.keys))
                .associateBy({ it.username }, { it })
            remoteDevSettingDao.updateTaiUserInfo(dslContext, taiInfos)
            if (taiUsers.size < pageSize) return
            page++
        }
    }
}
