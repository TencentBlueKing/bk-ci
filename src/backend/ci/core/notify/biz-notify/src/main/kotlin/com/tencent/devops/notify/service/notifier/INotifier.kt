package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest

/**
 * 通知器
 */
interface INotifier {
    /**
     * 支持类型
     */
    fun type(): NotifyType

    /**
     * 发送
     */
    fun send(
        request: SendNotifyMessageTemplateRequest, // 请求
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord // 模板
    )
}
