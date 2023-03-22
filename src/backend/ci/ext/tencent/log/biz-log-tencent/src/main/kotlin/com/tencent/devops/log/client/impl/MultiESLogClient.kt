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

package com.tencent.devops.log.client.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.LogCode.BK_CLUSTER_NAME
import com.tencent.devops.log.LogCode.BK_CONTACT_BLUE_SHIELD_ASSISTANT
import com.tencent.devops.log.LogCode.BK_EMPTY_DATA
import com.tencent.devops.log.LogCode.BK_ES_CLUSTER_RECOVERY
import com.tencent.devops.log.LogCode.BK_ES_CLUSTER_STATUS_ALARM_NOTIFICATION
import com.tencent.devops.log.LogCode.BK_FAILED_INSERT_DATA
import com.tencent.devops.log.LogCode.BK_FAILURE
import com.tencent.devops.log.LogCode.BK_HEAD_OF_BLUE_SHIELD_LOG_MANAGEMENT
import com.tencent.devops.log.LogCode.BK_LOOK_FORWARD_IT
import com.tencent.devops.log.LogCode.BK_NOTIFICATION_PUSH_FROM_BKDEVOP
import com.tencent.devops.log.LogCode.BK_RECOVERY
import com.tencent.devops.log.LogCode.BK_STATUS
import com.tencent.devops.log.client.LogClient
import com.tencent.devops.log.dao.IndexDao
import com.tencent.devops.log.dao.TencentIndexDao
import com.tencent.devops.log.es.ESClient
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.abs

class MultiESLogClient constructor(
    private val clients: List<ESClient>,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val tencentIndexDao: TencentIndexDao,
    private val indexDao: IndexDao
) : LogClient {

    init {
        val names = clients.map { it.clusterName }.toSet()
        if (names.size != clients.size) {
            logger.warn("There are same es names between es cluster")
            throw RuntimeException("There are same es names between es cluster")
        }
    }

    private val cache = Caffeine.newBuilder()
        .maximumSize(300000)
        .expireAfterWrite(2, TimeUnit.DAYS)
        .build<String/*buildId*/, String/*ES NAME*/>()

    // The cache store the bad ES
    private val inactiveESCache = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String/*ES Name*/, Boolean> { esName ->
            getInactiveESFromRedis().contains(esName)
        }

    private val notifyExecutor = Executors.newSingleThreadExecutor()

    private val notifyUsers = HashSet<String>()
    @Volatile
    private var notifyUserLastUpdate = 0L

    @Synchronized
    fun markESInactive(esName: String) {
        logger.warn("[$esName] Mark as inactive es cluster")
        inactiveESCache.put(esName, true)
        setInactiveES(esName)
        notifyExecutor.submit {
            sendNotify(esName, true)
        }
    }

    @Synchronized
    fun markESActive(esName: String) {
        logger.info("[$esName] Mark as active es cluster")
        inactiveESCache.put(esName, false)
        removeInactiveES(esName)
        notifyExecutor.submit {
            sendNotify(esName, false)
        }
    }

    @Synchronized
    fun addNotifyUser(userId: String) {
        logger.info("Add the notify user: $userId")
        addNotifyUserToRedis(userId)
        notifyUserLastUpdate = 0
    }

    @Synchronized
    fun removeNotifyUser(userId: String) {
        logger.info("Remove the notify user: $userId")
        removeNotifyUserFromRedis(userId)
        notifyUserLastUpdate = 0
    }

    fun getInactiveClients(): List<ESClient> {
        return clients.filter { inactiveESCache.get(it.clusterName) ?: false }
    }

    override fun getActiveClients(): List<ESClient> {
        return clients.filter { inactiveESCache.get(it.clusterName) != true }
    }

    /**
     * 1. Get es name from local cache
     * 2. If local cache is not exist, then try to get from DB
     * 3. If DB is not exist, then hash the build id to the ESClients
     * 4.
     */
    override fun hashClient(buildId: String): ESClient {
        val activeClients = getActiveClients()
        if (activeClients.isEmpty()) {
            logger.warn("All client is inactive, try to use the first one")
            if (clients.isEmpty()) {
                throw RuntimeException("Empty es clients")
            }
            return mainCluster()
        }

        var esName = cache.getIfPresent(buildId)
        if (esName.isNullOrBlank()) {
            val redisLock = RedisLock(redisOperation, "$MULTI_LOG_CLIENT_LOCK_KEY:$buildId", 10)
            try {
                redisLock.lock()
                esName = cache.getIfPresent(buildId)
                if (esName.isNullOrBlank()) {
                    // 兼容老的日志， 如果这个日志之前已经被写入了， 那么默认返回mainCluster对应的集群的数据， 要不然就会导致前端查询不到数据
                    val buildIndex = indexDao.getBuild(dslContext, buildId)
                    if (buildIndex == null || (!buildIndex.useCluster)) {
                        val c = mainCluster()
                        cache.put(buildId, c.clusterName)
                        return c
                    }
                    esName = buildIndex.logClusterName
                    if (esName.isNullOrBlank()) {
                        // hash from build
                        logger.info("[$buildId|$esName] Rehash the build id")
                        val c = getWritableClient(activeClients, buildId)
                        esName = c.clusterName
                        logger.info("[$buildId] Set the build id to es log cluster: $esName")
                        tencentIndexDao.updateClusterName(dslContext, buildId, esName!!)
                    } else {
                        // set to cache
                        logger.info("[$buildId] The build ID already bind to the ES: ($esName)")
                    }
                    cache.put(buildId, esName!!)
                }
            } finally {
                redisLock.unlock()
            }
        }
        activeClients.forEach {
            if (it.clusterName == esName) {
                return it
            }
        }
        logger.warn("[$buildId|$esName] Fail to get the es name for the build, return the first one")
        return mainCluster()
    }

    private fun getWritableClient(activeClients: List<ESClient>, buildId: String): ESClient {
        val writableClients = activeClients.filter { it.writable == true }
        return writableClients[hashBuildId(buildId, writableClients.size)]
    }

    private fun hashBuildId(buildId: String, size: Int): Int {
        if (size == 1) {
            return 0
        }
        return abs(buildId.hashCode()) % size
    }

    private fun getInactiveESFromRedis(): Set<String> {
        val tmp = redisOperation.getSetMembers(MULTI_LOG_CLIENT_BAD_ES_KEY)
        logger.info("Get the inactive es: $tmp")
        return tmp ?: emptySet()
    }

    private fun setInactiveES(esName: String) {
        redisOperation.addSetValue(MULTI_LOG_CLIENT_BAD_ES_KEY, esName)
    }

    private fun removeInactiveES(esName: String) {
        redisOperation.removeSetMember(MULTI_LOG_CLIENT_BAD_ES_KEY, esName)
    }

    fun getNotifyUserFromRedis(): Set<String> {
        return redisOperation.getSetMembers(MULTI_LOG_STATUS_CHANGE_NOTIFY_USER_KEY) ?: emptySet()
    }

    private fun addNotifyUserToRedis(userId: String) {
        redisOperation.addSetValue(MULTI_LOG_STATUS_CHANGE_NOTIFY_USER_KEY, userId)
    }

    private fun removeNotifyUserFromRedis(userId: String) {
        redisOperation.removeSetMember(MULTI_LOG_STATUS_CHANGE_NOTIFY_USER_KEY, userId)
    }

    private fun mainCluster(): ESClient {
        clients.forEach {
            if (it.mainCluster == true) {
                return it
            }
        }
        return clients.first()
    }

    private fun writableClient(): ESClient {
        clients.forEach {
            if (it.writable == true) {
                return it
            }
        }
        return mainCluster()
    }

    private fun getNotifyUser(): Set<String> {
        if (!isNotifyUserValid()) {
            synchronized(this) {
                if (!isNotifyUserValid()) {
                    val redisUser = getNotifyUserFromRedis()
                    logger.info("Get the notify user: $redisUser")
                    notifyUsers.clear()
                    notifyUsers.addAll(redisUser)
                    notifyUserLastUpdate = System.currentTimeMillis()
                }
            }
        }
        return notifyUsers
    }

    private fun sendNotify(esName: String, inactive: Boolean) {
        try {
            val users = getNotifyUser()
            if (users.isEmpty()) {
                logger.info("Notify user is empty ignore")
                return
            }
            val t = if (inactive) {
                MessageUtil.getMessageByLocale(
                    messageCode = BK_FAILED_INSERT_DATA,
                    language = I18nUtil.getLanguage()
                )
            } else {
                MessageUtil.getMessageByLocale(
                    messageCode = BK_ES_CLUSTER_RECOVERY,
                    language = I18nUtil.getLanguage()
                )
            }
            val map = mapOf(
                "esName" to esName,
                "status" to if (inactive) MessageUtil.getMessageByLocale(
                    messageCode = BK_FAILURE,
                    language = I18nUtil.getLanguage()
                ) else MessageUtil.getMessageByLocale(
                    messageCode = BK_RECOVERY,
                    language = I18nUtil.getLanguage()
                )
            )
            val message = parseMessageTemplate(getEmailBody(), map)
            val emailMessage = EmailNotifyMessage().apply {
                addAllReceivers(users)
                format = EnumEmailFormat.HTML
                title = t
                sender = "DevOps"
                body = message
            }
            SpringContextUtil.getBean(Client::class.java).get(ServiceNotifyResource::class).sendEmailNotify(emailMessage)
        } catch (t: Throwable) {
            logger.warn("[$esName|$inactive] Fail to send the notify message", t)
        }
    }

    private fun getEmailBody(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = simpleDateFormat.format(Date())
        return "<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\n" +
            "\t<tbody>\n" +
            "\t\t<tr>\n" +
            "\t\t\t<td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\n" +
            "\t\t\t   <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t<tbody>\n" +
            "\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t<td valign=\"top\" align=\"center\">\n" +
            "\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f9f8f6\" class=\"layout layout-table root-table\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t\t\t\t\t<tbody>\n" +
            "\t\t\t\t\t\t\t\t\t\t<tr style=\"height: 64px; background: #555;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-left: 24px;\" width=\"60\" align=\"center\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<img src=\"http://file.tapd.oa.com//tfl/pictures/201807/tapd_20363462_1531467552_72.png\" width=\"52\" style=\"display: block\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-left: 6px;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<img src=\"http://file.tapd.oa.com//tfl/pictures/201807/tapd_20363462_1531467605_41.png\" width=\"176\" style=\"display: block\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t</tbody>\n" +
            "\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t<td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\n" +
            "\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\n" +
            "\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t<td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">" + MessageUtil.getMessageByLocale(
            messageCode = BK_ES_CLUSTER_STATUS_ALARM_NOTIFICATION,
            language = I18nUtil.getLanguage()
        ) + "</td>\n" +
            "\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t<td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">" + MessageUtil.getMessageByLocale(
            messageCode = BK_NOTIFICATION_PUSH_FROM_BKDEVOP,
            language = I18nUtil.getLanguage()
        ) + "</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"email-information\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td class=\"table-info\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"table-title\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 36px; padding-bottom: 14px; color: #707070;\"></td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<thead style=\"background: #f6f8f8;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr style=\"color: #333C48;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"50%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">" + MessageUtil.getMessageByLocale(
            messageCode = BK_CLUSTER_NAME,
            language = I18nUtil.getLanguage()
        ) + "</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"50%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">" + MessageUtil.getMessageByLocale(
            messageCode = BK_STATUS,
            language = I18nUtil.getLanguage()
        ) + "</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</thead>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tbody style=\"color: #707070;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">#{esName}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">#{status}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tbody>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<!-- " + MessageUtil.getMessageByLocale(
            messageCode = BK_EMPTY_DATA,
            language = I18nUtil.getLanguage()
        ) + " -->\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<!-- <tr class=\"no-data\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 40px; color: #707070;\">" + MessageUtil.getMessageByLocale(
            messageCode = BK_LOOK_FORWARD_IT,
            language = I18nUtil.getLanguage()
        ) + "</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr> -->\n" +
            "\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"prompt-tips\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">" + MessageUtil.getMessageByLocale(
            messageCode = BK_CONTACT_BLUE_SHIELD_ASSISTANT,
            language = I18nUtil.getLanguage()
        ) + "</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"info-remark\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding: 20px 0; text-align: right; line-height: 24px; color: #707070;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t<div>$date</div>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t<tr class=\"email-footer\">\n" +
            "\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">" + MessageUtil.getMessageByLocale(
            messageCode = BK_HEAD_OF_BLUE_SHIELD_LOG_MANAGEMENT,
            language = I18nUtil.getLanguage()
        ) + "</td>\n" +
            "\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t</tbody>\n" +
            "\t\t\t   </table>\n" +
            "\t\t\t</td>\n" +
            "\t\t</tr>\n" +
            "\t</tbody>\n" +
            "</table>\n"
    }

    fun parseMessageTemplate(content: String, data: Map<String, String>): String {
        if (content.isBlank()) {
            return content
        }
        val pattern = Pattern.compile("#\\{([^}]+)}")
        val newValue = StringBuffer(content.length)
        val matcher = pattern.matcher(content)
        while (matcher.find()) {
            val key = matcher.group(1)
            val variable = data[key] ?: ""
            matcher.appendReplacement(newValue, variable)
        }
        matcher.appendTail(newValue)
        return newValue.toString()
    }
    // 1 minute
    private fun isNotifyUserValid() = (System.currentTimeMillis() - notifyUserLastUpdate) <= 60 * 1000

    companion object {
        private val logger = LoggerFactory.getLogger(MultiESLogClient::class.java)
        private const val MULTI_LOG_CLIENT_LOCK_KEY = "log:multi:log:client:lock:key"
        private const val MULTI_LOG_CLIENT_BAD_ES_KEY = "log::multi::log:client:bad:es:key"
        private const val MULTI_LOG_STATUS_CHANGE_NOTIFY_USER_KEY = "log:multi:log:status:change:notify:user:key"
    }
}
