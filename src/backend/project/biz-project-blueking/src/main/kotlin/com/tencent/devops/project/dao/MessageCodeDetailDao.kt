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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.dao

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.MessageCodeDetail
import com.tencent.devops.model.project.tables.TMessageCodeDetail
import com.tencent.devops.model.project.tables.records.TMessageCodeDetailRecord
import com.tencent.devops.project.pojo.code.AddMessageCodeRequest
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

/**
 * 返回码信息数据库操作类
 *
 * @since: 2018-11-09
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
@Repository
class MessageCodeDetailDao {

    fun getMessageCodeDetails(
        dslContext: DSLContext,
        messageCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<TMessageCodeDetailRecord>? {
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
            dslContext.insertInto(
                this,
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

    fun updateMessageCodeDetail(
        dslContext: DSLContext,
        messageCode: String,
        messageDetailZhCn: String,
        messageDetailZhTw: String,
        messageDetailEn: String
    ) {
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
            return MessageCodeDetail(
                id,
                messageCode,
                SystemModuleEnum.getSystemModule(moduleCode),
                messageDetailZhCn,
                messageDetailZhTw,
                messageDetailEn
            )
        }
    }
}