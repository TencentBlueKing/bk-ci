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
import java.util.regex.Pattern

@Component
class WeworkGroupNotifier @Autowired constructor(
    private val wechatWorkService: WechatWorkService,
    private val wechatWorkRobotService: WechatWorkRobotService,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val dslContext: DSLContext
) : INotifier {
    @Value("\${wework.domain}")
    private val userUseDomain: Boolean = true

    private val chatPatten = "^[A-Za-z0-9_-]+\$" // 数字和字母组成的群chatId正则表达式
    override fun type(): NotifyType = NotifyType.WEWORK_GROUP
    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        logger.info("send WEWORK_GROUP msg: $commonNotifyMessageTemplateRecord.id")
        val groups = request.bodyParams?.get(NotifyUtils.WEWORK_GROUP_KEY)?.split("[,;]".toRegex())
        if (groups.isNullOrEmpty()) {
            logger.info("wework group is empty, so return.")
            return
        }
        val weworkTplRecord =
            notifyMessageTemplateDao.getRtxNotifyMessageTemplate(
                dslContext = dslContext,
                commonTemplateId = commonNotifyMessageTemplateRecord.id
            )!!
        val title = NotifierUtils.replaceContentParams(request.titleParams, weworkTplRecord.title)
        // 替换内容里的动态参数
        val body = NotifierUtils.replaceContentParams(
            request.bodyParams,
            if (request.markdownContent == true) {
                weworkTplRecord.bodyMd ?: weworkTplRecord.body
            } else {
                weworkTplRecord.body
            }
        )

        val content = title + "\n\n" + body

        groups.forEach {
            if (it.startsWith("ww")) { // 应用号逻辑
                wechatWorkService.sendByApp(
                    chatId = it,
                    content = content,
                    markerDownFlag = request.markdownContent ?: false
                )
            } else if (Pattern.matches(chatPatten, it)) { // 机器人逻辑
                wechatWorkRobotService.sendByRobot(
                    chatId = it,
                    content = content,
                    markerDownFlag = request.markdownContent ?: false
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WeworkGroupNotifier::class.java)
    }
}
