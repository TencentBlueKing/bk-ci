package com.tencent.devops.notify.dao

import com.tencent.devops.model.notify.tables.TCommonNotifyMessageTemplate
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class CommonNotifyMessageTemplateDao {
    /**
     * 根据模板代码和模板名称获取公共消息模板
     */
    fun getCommonNotifyMessageTemplateByCode(
        dslContext: DSLContext,
        templateCode: String
    ): TCommonNotifyMessageTemplateRecord? {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(TEMPLATE_CODE.eq(templateCode))
            val baseStep = dslContext.selectFrom(this)
                .where(conditions)
            return baseStep.fetchOne()
        }
    }
}