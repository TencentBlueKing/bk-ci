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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.enums.NotifyUser
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.environment.dao.thirdPartyAgent.AgentDisconnectNotifyDao
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.ws.rs.NotFoundException

@Service
class AgentDisconnectNotifyService @Autowired constructor(
    private val client: Client,
    private val agentDisconnectNotifyDao: AgentDisconnectNotifyDao
) : IAgentDisconnectNotifyService {

    private val executor = Executors.newSingleThreadExecutor()
    private val notifyUserCache = ArrayList<NotifyUser>()
    private var lastUpdate: Long = 0

    override fun online(projectId: String, ip: String, hostname: String, createUser: String, os: String) {
        executor.execute(
            OnOfflineThread(
                service = this,
                projectId = projectId,
                ip = ip,
                hostname = hostname,
                createUser = createUser,
                os = os,
                type = Type.ONLINE
            )
        )
    }

    override fun offline(projectId: String, ip: String, hostname: String, createUser: String, os: String) {
        executor.execute(
            OnOfflineThread(
                service = this,
                projectId = projectId,
                ip = ip,
                hostname = hostname,
                createUser = createUser,
                os = os,
                type = Type.OFFLINE
            )
        )
    }

    private class OnOfflineThread(
        private val service: AgentDisconnectNotifyService,
        private val projectId: String,
        private val ip: String,
        private val hostname: String,
        private val createUser: String,
        private val os: String,
        private val type: Type
    ) : Runnable {
        override fun run() {
            logger.info("The agent $ip $type")
            val notifyUsers = service.getNotifyUser()
            if (notifyUsers.isEmpty()) {
                logger.info("The agent notify user is empty")
                return
            }
            service.sendNotify(notifyUsers, projectId, ip, hostname, createUser, os, type)
        }
    }

    private fun sendNotify(
        users: List<NotifyUser>,
        projectId: String,
        ip: String,
        hostname: String,
        createUser: String,
        os: String,
        type: Type
    ) {
        val url = "${HomeHostUtil.innerServerHost()}/console/environment/$projectId/nodeList"
        val mapData = mapOf(
            "projectName" to (getProjectName(projectId) ?: projectId),
            "ip" to ip,
            "hostname" to hostname,
            "url" to url,
            "username" to createUser,
            "os" to os
        )

        val emailUserIds = users.filter { it.notifyTypes.contains(NotifyType.EMAIL) }.map {
            it.userId
        }.toSet()

        val rtxUserIds = users.filter { it.notifyTypes.contains(NotifyType.RTX) }.map {
            it.userId
        }.toSet()

        val wechatUserIds = users.filter { it.notifyTypes.contains(NotifyType.WECHAT) }.map {
            it.userId
        }.toSet()

        sendEmail(emailUserIds, mapData, type)
        sendRTX(rtxUserIds, mapData, type)
        sendWechat(wechatUserIds, mapData, type)
    }

    private fun sendEmail(users: Set<String>, mapData: Map<String, String>, type: Type) {
        if (users.isEmpty()) {
            return
        }
        val message = EmailNotifyMessage().apply {
            addAllReceivers(users)
            format = EnumEmailFormat.HTML
            body = parseMessageTemplate(getEmailBody(type), mapData)
            title = parseMessageTemplate(getEmailTitle(type), mapData)
            sender = "DevOps"
        }
        logger.info("Send the email for agent $type notify")
        val result = client.get(ServiceNotifyResource::class).sendEmailNotify(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to send the email message($message) because of ${result.message}")
        }
    }

    private fun sendRTX(users: Set<String>, mapData: Map<String, String>, type: Type) {
        if (users.isEmpty()) {
            return
        }
        val rtxBody = if (type == Type.ONLINE) RTX_ONLINE_BODY else RTX_OFFLINE_BODY
        val rtxTitle = if (type == Type.ONLINE) RTX_ONLINE_TITLE else RTX_OFFLINE_TITLE
        val message = RtxNotifyMessage().apply {
            addAllReceivers(users)
            body = parseMessageTemplate(rtxBody, mapData)
            title = parseMessageTemplate(rtxTitle, mapData)
            sender = "蓝鲸助手"
        }

        logger.info("Send the rtx for agent $type notify")
        val result = client.get(ServiceNotifyResource::class).sendRtxNotify(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to send the rtx message($message) because of ${result.message}")
        }
    }

    private fun sendWechat(users: Set<String>, mapData: Map<String, String>, type: Type) {
        if (users.isEmpty()) {
            return
        }
        val wechatBody = if (type == Type.ONLINE) WECHAT_ONLINE_BODY else WECHAT_OFFLINE_BODY
        val message = WechatNotifyMessage().apply {
            addAllReceivers(users)
            body = parseMessageTemplate(wechatBody, mapData)
        }
        logger.info("Send the wechat for agent $type notify")
        val result = client.get(ServiceNotifyResource::class).sendWechatNotify(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("Fail to send the email message($message) because of ${result.message}")
        }
    }

    private fun getNotifyUser(): List<NotifyUser> {
        if (need2Refresh()) {
            synchronized(this) {
                if (need2Refresh()) {
                    try {
                        logger.info("Refresh the agent notify user")
                        notifyUserCache.clear()
                        val records = agentDisconnectNotifyDao.list()
                        if (records.isNotEmpty()) {
                            records.filter { !it.userId.isNullOrBlank() }.forEach { record ->
                                val types = ArrayList<NotifyType>()
                                record.notifyTypes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    .forEach { type ->
                                        try {
                                            types.add(NotifyType.valueOf(type))
                                        } catch (t: Throwable) {
                                            logger.warn("Fail to convert the notify type $type", t)
                                        }
                                    }
                                if (types.isNotEmpty()) {
                                    notifyUserCache.add(NotifyUser(record.userId, types))
                                }
                            }
                        }
                        logger.info("Get the agent notify users $notifyUserCache")
                    } finally {
                        lastUpdate = System.currentTimeMillis()
                    }
                }
            }
        }
        return notifyUserCache
    }

    /**
     * ProjectID -> ProjectName cache
     */
    private val cache = CacheBuilder.newBuilder().maximumSize(1000)
        .build(object : CacheLoader<String, String>() {
            override fun load(projectId: String) = getProjectNameInner(projectId)
        })

    private fun parseMessageTemplate(content: String, data: Map<String, String>): String {
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

    private fun getEmailTitle(type: Type) =
        if (type == Type.ONLINE) "✔️【蓝盾第三方构建机上线告警通知 - #{projectName}】" else "❌【蓝盾第三方构建机下线告警通知 - #{projectName}】"

    private fun getEmailBody(type: Type): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val title = if (type == Type.ONLINE) "✔️【蓝盾第三方构建机上线告警通知】" else "❌【蓝盾第三方构建机下线告警通知】"
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
            "\t\t\t\t\t\t\t\t\t\t<td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">$title</td>\n" +
            "\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t<td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">来自BKDevOps/蓝盾DevOps平台的通知推送</td>\n" +
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
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"20%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">所属项目</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"20%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">IP</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"20%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">主机名</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"20%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">操作系统</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"20%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\">导入人</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</thead>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tbody style=\"color: #707070;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">#{projectName}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\"><a href=\"#{url}\" style=\"color: #3c96ff\">#{ip}</a></td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">#{hostname}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">#{os}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\">#{username}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tbody>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<!-- 空数据 -->\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<!-- <tr class=\"no-data\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 40px; color: #707070;\">敬请期待！</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr> -->\n" +
            "\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"prompt-tips\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">如有任何问题，可随时联系蓝盾助手。</td>\n" +
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
            "\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为你关注了 #{projectName} 项目，或其他人@了你</td>\n" +
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

    private fun getProjectName(projectId: String): String? {
        try {
            return cache.get(projectId)
        } catch (t: Throwable) {
            logger.warn("Fail to get the project name project code($projectId)", t)
        }
        return null
    }

    private fun getProjectNameInner(projectId: String): String {
        val bkAuthProject = client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw NotFoundException("Fail to find the project info of project($projectId)")
        return bkAuthProject.projectName
    }

    private fun need2Refresh() = System.currentTimeMillis() - lastUpdate >= TimeUnit.MINUTES.toMillis(2)

    private enum class Type {
        ONLINE,
        OFFLINE
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentDisconnectNotifyService::class.java)
        private const val RTX_ONLINE_TITLE = "✔️【#{projectName}】- 主机【#{ip}】#{hostname} #{os} 导入人【#{username}】上线"
        private const val RTX_ONLINE_BODY = "<a href=\"#{url}\">查看详情</a>"

        private const val RTX_OFFLINE_TITLE = "❌【#{projectName}】- 主机【#{ip}】#{hostname} #{os} 导入人【#{username}】断线"
        private const val RTX_OFFLINE_BODY = "<a href=\"#{url}\">查看详情</a>"

        private const val WECHAT_ONLINE_BODY = "✔️【#{projectName}】- 主机【#{ip}】#{hostname} #{os} 导入人【#{username}】上线"
        private const val WECHAT_OFFLINE_BODY = "❌【#{projectName}】- 主机【#{ip}】#{hostname} #{os} 导入人【#{username}】断线"
    }
}
