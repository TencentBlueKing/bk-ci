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
package com.tencent.devops.notify.dao

import com.tencent.devops.model.notify.tables.TMoaNotifyMessageTemplate
import com.tencent.devops.model.notify.tables.records.TMoaNotifyMessageTemplateRecord
import com.tencent.devops.notify.pojo.NotifyTemplateMessage
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 消息通知数据库操作类
 * @author: carlyin
 * @since: 2019-08-13
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
@Repository
class TNotifyMessageTemplateDao {

    /**
     * 获取WOA消息模板
     * @param dslContext 数据库操作对象
     * @param commonTemplateId
     */
    fun getMoaNotifyMessageTemplate(
        dslContext: DSLContext,
        commonTemplateId: String
    ): TMoaNotifyMessageTemplateRecord? {
        with(TMoaNotifyMessageTemplate.T_MOA_NOTIFY_MESSAGE_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(COMMON_TEMPLATE_ID.contains(commonTemplateId))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
        }
    }

    fun saveOrUpdateMoaNotifyMessageTemplate(
        dslContext: DSLContext,
        id: String,
        newId: String,
        userId: String,
        addNotifyTemplateMessage: NotifyTemplateMessage
    ) {
        with(TMoaNotifyMessageTemplate.T_MOA_NOTIFY_MESSAGE_TEMPLATE) {
            dslContext.insertInto(
                this,
                ID,
                COMMON_TEMPLATE_ID,
                CREATOR,
                MODIFIOR,
                TITLE,
                CALLBACK_URL,
                PROCESS_NAME,
                BODY,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                newId,
                id,
                userId,
                userId,
                addNotifyTemplateMessage.title,
                addNotifyTemplateMessage.callBackUrl,
                addNotifyTemplateMessage.processName,
                addNotifyTemplateMessage.body,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(MODIFIOR, userId)
                .set(TITLE, addNotifyTemplateMessage.title)
                .set(CALLBACK_URL, addNotifyTemplateMessage.callBackUrl)
                .set(PROCESS_NAME, addNotifyTemplateMessage.processName)
                .set(BODY, addNotifyTemplateMessage.body)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }
}
