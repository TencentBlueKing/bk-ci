package com.tencent.devops.support.dao

import com.tencent.devops.model.support.tables.TWechatWorkMessage
import com.tencent.devops.model.support.tables.records.TWechatWorkMessageRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * freyzheng
 * 2018/11/13
 */
@Repository
class WechatWorkMessageDAO {
    fun getByMessageId(dslContext: DSLContext, messageId: String): TWechatWorkMessageRecord? {
        with(TWechatWorkMessage.T_WECHAT_WORK_MESSAGE) {
            return dslContext.selectFrom(this)
                    .where(MESSAGE_ID.eq(messageId))
                    .fetchOne()
        }
    }

    fun insertMassageId(dslContext: DSLContext, messageId: String) {
        with(TWechatWorkMessage.T_WECHAT_WORK_MESSAGE) {
            val result = if (exist(dslContext, messageId)) {
                "message id 已存在。"
            } else {
                // 插入新的
                dslContext
                        .insertInto(this, MESSAGE_ID)
                        .values(messageId)
                        .execute()
                "message id 插入成功。"
            }
            System.out.println(result)
        }
    }

    fun exist(dslContext: DSLContext, messageId: String) =
            getByMessageId(dslContext, messageId) != null
}