package com.tencent.devops.support.dao

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.MessageCodeDetail
import com.tencent.devops.model.support.tables.TMessageCodeDetail
import com.tencent.devops.model.support.tables.records.TMessageCodeDetailRecord
import com.tencent.devops.support.model.code.AddMessageCodeRequest
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

/**
 * code数据库操作类
 * @author: carlyin
 * @since: 2018-11-09
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
@Repository
class MessageCodeDetailDao {

    fun getMessageCodeDetails(dslContext: DSLContext, messageCode: String?, page: Int?, pageSize: Int?): Result<TMessageCodeDetailRecord>? {
        with(TMessageCodeDetail.T_MESSAGE_CODE_DETAIL) {
            val conditions = mutableListOf<Condition>()
            if (null != messageCode && messageCode.isNotBlank()) {
                conditions.add(MESSAGE_CODE.contains(messageCode))
            }
            val baseStep = dslContext.selectFrom(this).where(conditions)
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun getMessageCodeDetailCount(
        dslContext: DSLContext,
        messageCode: String?
    ): Long {
        with(TMessageCodeDetail.T_MESSAGE_CODE_DETAIL) {
            val conditions = mutableListOf<Condition>()
            if (null != messageCode && messageCode.isNotBlank()) {
                conditions.add(MESSAGE_CODE.contains(messageCode))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Long::class.java)
        }
    }

    fun getMessageCodeDetail(dslContext: DSLContext, messageCode: String): TMessageCodeDetailRecord? {
        return with(TMessageCodeDetail.T_MESSAGE_CODE_DETAIL) {
            dslContext.selectFrom(this)
                .where(MESSAGE_CODE.eq(messageCode))
                .fetchOne()
        }
    }

    fun addMessageCodeDetail(dslContext: DSLContext, id: String, addMessageCodeRequest: AddMessageCodeRequest) {
        with(TMessageCodeDetail.T_MESSAGE_CODE_DETAIL) {
            dslContext.insertInto(this,
                ID,
                MESSAGE_CODE,
                MODULE_CODE,
                MESSAGE_DETAIL_ZH_CN,
                MESSAGE_DETAIL_ZH_TW,
                MESSAGE_DETAIL_EN
            )
                .values(
                    id,
                    addMessageCodeRequest.messageCode,
                    addMessageCodeRequest.moduleCode.code,
                    addMessageCodeRequest.messageDetailZhCn,
                    addMessageCodeRequest.messageDetailZhTw,
                    addMessageCodeRequest.messageDetailEn
                )
                .execute()
        }
    }

    fun updateMessageCodeDetail(dslContext: DSLContext, messageCode: String, messageDetailZhCn: String, messageDetailZhTw: String, messageDetailEn: String) {
        with(TMessageCodeDetail.T_MESSAGE_CODE_DETAIL) {
            dslContext.update(this)
                .set(MESSAGE_DETAIL_ZH_CN, messageDetailZhCn)
                .set(MESSAGE_DETAIL_ZH_TW, messageDetailZhTw)
                .set(MESSAGE_DETAIL_EN, messageDetailEn)
                .where(MESSAGE_CODE.eq(messageCode))
                .execute()
        }
    }

    fun convert(record: TMessageCodeDetailRecord): MessageCodeDetail {
        with(record) {
            return MessageCodeDetail(id, messageCode, SystemModuleEnum.getSystemModule(moduleCode), messageDetailZhCn, messageDetailZhTw, messageDetailEn)
        }
    }
}