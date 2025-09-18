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
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.remotedev.dao.ConfigDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.pojo.MonitorConfig
import com.tencent.devops.remotedev.pojo.MonitorType
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.client.TaiUserInfoRequest
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.utils.TokenEncryptUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevSettingService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val whiteListService: WhiteListService,
    private val taiClient: TaiClient,
    private val configCacheService: ConfigCacheService,
    private val configDao: ConfigDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevSettingService::class.java)
        private const val BKREPO_HOST_KEY = "remotedev:bkrepoHost"
        private const val MONITOR_URL_KEY = "monitor:url"
        private const val MONITOR_TOKEN_KEY = "monitor:token"
    }

    /**
     * 获取监控配置信息
     *
     * @param type 监控类型，默认为DEFAULT
     * @return 监控配置对象，包含URL和加密Token
     */
    fun getMonitorConfig(type: String = "DEFAULT"): MonitorConfig {
        logger.debug("Getting monitor configuration for type: $type")

        try {
            val monitorType = MonitorType.parseType(type)
            val typePrefix = monitorType.name.lowercase()
            
            // 根据类型构建配置键
            val urlKey = if (monitorType == MonitorType.DEFAULT) {
                MONITOR_URL_KEY
            } else {
                "monitor:${typePrefix}:url"
            }
            
            val tokenKey = if (monitorType == MonitorType.DEFAULT) {
                MONITOR_TOKEN_KEY
            } else {
                "monitor:${typePrefix}:token"
            }

            // 从缓存或数据库获取监控URL
            val monitorUrl = configCacheService.get(urlKey)
                ?: configDao.fetchConfig(dslContext, urlKey)

            // 从缓存或数据库获取原始Token
            val originalToken = configCacheService.get(tokenKey)
                ?: configDao.fetchConfig(dslContext, tokenKey)

            // 对Token进行加密
            val encryptedToken = originalToken?.let { TokenEncryptUtil.encryptToken(it) }

            val enabled = !monitorUrl.isNullOrBlank() && !encryptedToken.isNullOrBlank()

            return MonitorConfig(
                monitorUrl = monitorUrl,
                monitorToken = encryptedToken,
                type = monitorType.name,
                enabled = enabled
            )
        } catch (e: Exception) {
            logger.error("Failed to get monitor configuration for type: $type", e)
            return MonitorConfig(type = type, enabled = false)
        }
    }

    /**
     * 获取所有类型的监控配置列表
     *
     * @return 监控配置列表
     */
    fun getAllMonitorConfigs(): List<MonitorConfig> {
        logger.debug("Getting all monitor configurations")
        
        return try {
            MonitorType.values().mapNotNull { monitorType ->
                val config = getMonitorConfig(monitorType.name)
                // 只返回已启用的配置，或者至少有URL配置的
                if (config.enabled || !config.monitorUrl.isNullOrBlank()) {
                    config
                } else {
                    null
                }
            }.ifEmpty {
                // 如果没有任何配置，返回一个默认的空配置
                listOf(MonitorConfig(type = MonitorType.DEFAULT.name, enabled = false))
            }
        } catch (e: Exception) {
            logger.error("Failed to get all monitor configurations", e)
            listOf(MonitorConfig(type = MonitorType.DEFAULT.name, enabled = false))
        }
    }

    fun getRemoteDevSettings(userId: String): RemoteDevSettings {
        logger.info("$userId get remote dev setting")
        val setting = remoteDevSettingDao.fetchOneSetting(dslContext, userId)
        // TODO 待删除，等新版本客户端不依赖这个项目 id 后再去掉。
        if (setting.projectId.isBlank()) {
            kotlin.runCatching {
                client.get(ServiceTxProjectResource::class).getRemoteDevUserProject(userId)
            }.onFailure { logger.warn("create user project fail ${it.message}", it) }.getOrNull().let {
                if (it?.data == null) {
                    logger.warn("create user project fail ${it?.message}")
                }
                remoteDevSettingDao.updateProjectId(dslContext, userId, it?.data?.englishName ?: "")
                setting.projectId = it?.data?.englishName ?: ""
            }
        }

        // 获取所有监控配置信息
        val monitorConfigs = getAllMonitorConfigs()

        // 在现有设置基础上添加监控配置信息
        return setting.copy(
            monitorConfigs = monitorConfigs
        )
    }

    fun getFileGateway(): Map<String, String> {
        // 配置示例  zone1=https://zone1.bkrepo.com,zone2=https://zone2.bkrepo.com
        return configCacheService.get(BKREPO_HOST_KEY)?.split(",")?.mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size != 2) {
                logger.warn("Invalid file gateway configuration item: $it")
                return@mapNotNull null
            }
            val key = parts[0].trim()
            val value = parts[1].trim()
            if (key.isEmpty() || value.isEmpty()) {
                logger.warn("Invalid file gateway configuration item: $it")
                return@mapNotNull null
            }
            key to value
        }?.toMap() ?: emptyMap()
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

    fun updateSetting4Op(operator: String, data: OPUserSetting) {
        logger.info("updateSettingByOp $data")
        data.userIds.forEach { userId ->
            remoteDevSettingDao.createOrUpdateSetting4OP(dslContext, userId, data)
            // 根据OPUserSetting中设置是否开启客户端白名单 + START白名单，分别做处理
            data.clientWhiteList?.let { isEnabled ->
                if (isEnabled) {
                    whiteListService.addWhiteListUser(operator = operator, whiteListUser = userId)
                } else {
                    whiteListService.removeWhiteListUser(operator = operator, whiteListUser = userId)
                }
            }

            data.startWhiteList?.let { isEnabled ->
                if (isEnabled) {
                    whiteListService.addGPUWhiteListUser(operator = operator, whiteListUser = userId, override = true)
                } else {
                    whiteListService.removeGPUWhiteListUser(userId = operator, whiteListUser = userId)
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
            ?: 1
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
