package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.wechatwork.WechatWorkRobotService
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest

import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class WeworkGroupNotifier @Autowired constructor(
    private val wechatWorkService: WechatWorkService,
    private val wechatWorkRobotService: WechatWorkRobotService,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val dslContext: DSLContext
) : INotifier {
    @Value("\${wework.domain}")
    private val userUseDomain: Boolean = true

    override fun type(): NotifyType = NotifyType.WEWORK_GROUP
    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        logger.info("send WEWORK_GROUP msg: ${commonNotifyMessageTemplateRecord.id}")
        val groups = request.bodyParams?.get(NotifyUtils.WEWORK_GROUP_KEY)?.split("[,;]".toRegex())
        if (groups.isNullOrEmpty()) {
            logger.info("wework group is empty, so return.")
            return
        }
        // 有RTX就从RTX表查，没有才从WEWORK_GROUP表查
        val rtxTplRecord = if (
            request.notifyType?.contains(NotifyType.RTX.name) != false
        ) {
            notifyMessageTemplateDao.getRtxNotifyMessageTemplate(
                dslContext = dslContext,
                commonTemplateId = commonNotifyMessageTemplateRecord.id
            )
        } else {
            null
        }
        val weworkGroupTplRecord = if (
            rtxTplRecord == null && request.notifyType?.contains(NotifyType.WEWORK_GROUP.name) == true
        ) {
            notifyMessageTemplateDao.getWeworkGroupNotifyMessageTemplate(
                dslContext = dslContext,
                commonTemplateId = commonNotifyMessageTemplateRecord.id
            )
        } else {
            null
        }
        if (rtxTplRecord == null && weworkGroupTplRecord == null) {
            logger.warn("no rtx or wework group template found for ${commonNotifyMessageTemplateRecord.id}")
            return
        }
        val title = NotifierUtils.replaceContentParams(
            request.titleParams,
            rtxTplRecord?.title ?: weworkGroupTplRecord?.title ?: ""
        )
        // 替换内容里的动态参数
        val body = NotifierUtils.replaceContentParams(
            request.bodyParams,
            if (request.markdownContent == true) {
                rtxTplRecord?.bodyMd ?: rtxTplRecord?.body
                    ?: weworkGroupTplRecord?.body ?: ""
            } else {
                rtxTplRecord?.body ?: weworkGroupTplRecord!!.body
            }
        )

        val content = title + "\n\n" + body
        val mentionUsers = if (request.mentionReceivers == true) {
            request.receivers.toList()
        } else {
            emptyList()
        }
        groups.forEach {
            if (it.startsWith("ww")) { // 应用号逻辑
                wechatWorkService.sendByApp(
                    chatId = it,
                    content = content,
                    markerDownFlag = request.markdownContent ?: false,
                    mentionUsers = mentionUsers
                )
            } else if (CHAT_PATTERN.matches(it)) { // 机器人逻辑
                wechatWorkRobotService.sendByRobot(
                    chatId = it,
                    content = content,
                    markerDownFlag = request.markdownContent ?: false,
                    mentionUsers = mentionUsers
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WeworkGroupNotifier::class.java)
        private val CHAT_PATTERN = Regex("^[A-Za-z0-9_-]+$")
    }
}
