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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.notify.tables.TCommonNotifyMessageTemplate
import com.tencent.devops.model.notify.tables.TEmailsNotifyMessageTemplate
import com.tencent.devops.model.notify.tables.TRtxNotifyMessageTemplate
import com.tencent.devops.model.notify.tables.TWechatNotifyMessageTemplate
import com.tencent.devops.model.notify.tables.TWeworkNotifyMessageTemplate
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TEmailsNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TRtxNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TWechatNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TWeworkNotifyMessageTemplateRecord
import com.tencent.devops.notify.pojo.NotifyTemplateMessage
import com.tencent.devops.notify.pojo.NotifyTemplateMessageRequest
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 消息通知数据库操作类
 */
@Repository
@Suppress("ALL")
class NotifyMessageTemplateDao {
    /**
     * 根据模板代码和模板名称搜索公共消息模板
     */
    fun searchCommonNotifyMessageTemplates(
        dslContext: DSLContext,
        templateCode: String?,
        templateName: String?,
        page: Int?,
        pageSize: Int?,
        new: Boolean
    ): Result<TCommonNotifyMessageTemplateRecord>? {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            if (!templateCode.isNullOrEmpty()) {
                conditions.add(TEMPLATE_CODE.contains(templateCode))
            }
            if (!templateName.isNullOrEmpty()) {
                conditions.add(TEMPLATE_NAME.contains(templateName))
            }
            val baseStep = dslContext.selectFrom(this)
                .where(conditions)
            return if (null != page && page > 0 && null != pageSize && pageSize > 0) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun getCommonNotifyMessageTemplatesNotifyType(dslContext: DSLContext, templateId: String): String? {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            val baseStep = dslContext.select(NOTIFY_TYPE_SCOPE)
                .from(this)
                .where(ID.eq(templateId))

            return baseStep.fetchOne(NOTIFY_TYPE_SCOPE)
        }
    }

    /**
     * 获取微信消息模板
     * @param dslContext 数据库操作对象
     * @param commonTemplateId
     */
    fun getWechatNotifyMessageTemplate(
        dslContext: DSLContext,
        commonTemplateId: String
    ): TWechatNotifyMessageTemplateRecord? {
        with(TWechatNotifyMessageTemplate.T_WECHAT_NOTIFY_MESSAGE_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(COMMON_TEMPLATE_ID.contains(commonTemplateId))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
        }
    }

    /**
     * 获取企业微信消息模板
     * @param dslContext 数据库操作对象
     * @param commonTemplateId
     */
    fun getRtxNotifyMessageTemplate(
        dslContext: DSLContext,
        commonTemplateId: String
    ): TRtxNotifyMessageTemplateRecord? {
        with(TRtxNotifyMessageTemplate.T_RTX_NOTIFY_MESSAGE_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(COMMON_TEMPLATE_ID.contains(commonTemplateId))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
        }
    }

    /**
     * 获取邮件消息模板
     */
    fun getEmailNotifyMessageTemplate(
        dslContext: DSLContext,
        commonTemplateId: String
    ): TEmailsNotifyMessageTemplateRecord? {
        with(TEmailsNotifyMessageTemplate.T_EMAILS_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(this.COMMON_TEMPLATE_ID.eq(commonTemplateId))
                .fetchOne()
        }
    }

    /**
     * 获取企业微信消息模板 新版
     * @param dslContext 数据库操作对象
     * @param commonTemplateId
     */
    fun getWeworkNotifyMessageTemplate(
        dslContext: DSLContext,
        commonTemplateId: String
    ): TWeworkNotifyMessageTemplateRecord? {
        with(TWeworkNotifyMessageTemplate.T_WEWORK_NOTIFY_MESSAGE_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(COMMON_TEMPLATE_ID.contains(commonTemplateId))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
        }
    }

    /**
     * 根据模板代码获取模板公共信息
     */
    fun getCommonNotifyMessageTemplateByCode(
        dslContext: DSLContext,
        templateCode: String?
    ): TCommonNotifyMessageTemplateRecord? {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.selectFrom(this).where(TEMPLATE_CODE.eq(templateCode)).fetchOne()
        }
    }

    /**
     * 根据模板名称获取模板公共信息
     */
    fun getCommonNotifyMessageTemplateByName(
        dslContext: DSLContext,
        templateName: String?
    ): TCommonNotifyMessageTemplateRecord? {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.selectFrom(this).where(TEMPLATE_CODE.eq(templateName)).fetchOne()
        }
    }

    /**
     * 更新消息通知模板的类型
     * @param typeScope 要修改的消息类型集合
     */
    fun modifyNotifyTypeScope(dslContext: DSLContext, typeScope: List<String>, id: String) {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            dslContext.update(this)
                .set(NOTIFY_TYPE_SCOPE, JsonUtil.toJson(typeScope, formatted = false))
                .where(ID.eq(id))
                .execute()
        }
    }

    /**
     * 添加消息通知模板的公共信息
     * @param id 本次添加记录的ID
     * @param addNotifyTemplateMessageRequest 添加消息模板信息对象
     */
    fun addCommonNotifyMessageTemplate(
        dslContext: DSLContext,
        id: String,
        addNotifyTemplateMessageRequest: NotifyTemplateMessageRequest,
        notifyTypeScope: Set<String>
    ) {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            dslContext.insertInto(
                this,
                ID,
                TEMPLATE_CODE,
                TEMPLATE_NAME,
                NOTIFY_TYPE_SCOPE,
                PRIORITY,
                SOURCE
            )
                .values(
                    id,
                    addNotifyTemplateMessageRequest.templateCode,
                    addNotifyTemplateMessageRequest.templateName,
                    JsonUtil.toJson(notifyTypeScope, formatted = false),
                    addNotifyTemplateMessageRequest.priority.getValue().toByte(),
                    addNotifyTemplateMessageRequest.source.getValue().toByte()
                )
                .execute()
        }
    }

    /**
     * 添加消息通知模板的邮件信息
     * @param id 本此添加邮件信息对应与Common表的ID字段，作为邮件信息表的外键
     * @param newId 邮件信息表的ID
     * @param addNotifyTemplateMessage 添加的邮件信息对象
     */
    fun addEmailsNotifyMessageTemplate(
        dslContext: DSLContext,
        id: String,
        newId: String,
        userId: String,
        addNotifyTemplateMessage: NotifyTemplateMessage
    ) {
        with(TEmailsNotifyMessageTemplate.T_EMAILS_NOTIFY_MESSAGE_TEMPLATE) {
            dslContext.insertInto(
                this,
                ID,
                COMMON_TEMPLATE_ID,
                TITLE,
                BODY,
                CREATOR,
                MODIFIOR,
                BODY_FORMAT,
                EMAIL_TYPE,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    newId,
                    id,
                    addNotifyTemplateMessage.title,
                    addNotifyTemplateMessage.body,
                    userId,
                    userId,
                    addNotifyTemplateMessage.bodyFormat!!.getValue().toByte(),
                    addNotifyTemplateMessage.emailType!!.getValue().toByte(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun addRTXNotifyMessageTemplate(
        dslContext: DSLContext,
        id: String,
        newId: String,
        userId: String,
        notifyTemplateMessage: NotifyTemplateMessage
    ) {
        with(TRtxNotifyMessageTemplate.T_RTX_NOTIFY_MESSAGE_TEMPLATE) {
            dslContext.insertInto(
                this,
                ID,
                COMMON_TEMPLATE_ID,
                TITLE,
                BODY,
                BODY_MD,
                CREATOR,
                MODIFIOR,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    newId,
                    id,
                    notifyTemplateMessage.title,
                    notifyTemplateMessage.body,
                    notifyTemplateMessage.bodyMD,
                    userId,
                    userId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun addWECHATNotifyMessageTemplate(
        dslContext: DSLContext,
        id: String,
        newId: String,
        userId: String,
        notifyTemplateMessage: NotifyTemplateMessage
    ) {
        with(TWechatNotifyMessageTemplate.T_WECHAT_NOTIFY_MESSAGE_TEMPLATE) {
            dslContext.insertInto(
                this,
                ID,
                COMMON_TEMPLATE_ID,
                TITLE,
                BODY,
                CREATOR,
                MODIFIOR,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    newId,
                    id,
                    notifyTemplateMessage.title,
                    notifyTemplateMessage.body,
                    userId,
                    userId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    /**
     * 根据Common表的ID字段删除消息模板主体信息
     */
    fun deleteCommonNotifyMessageTemplate(dslContext: DSLContext, id: String): Int {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun updateTXSESTemplateId(dslContext: DSLContext, templateId: String, sesTemplateId: Int?): Boolean {
        with(TEmailsNotifyMessageTemplate.T_EMAILS_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.update(this).set(TENCENT_CLOUD_TEMPLATE_ID, sesTemplateId).where(
                COMMON_TEMPLATE_ID.eq(templateId)
            ).execute() == 1
        }
    }

    /**
     * 删除邮件类型的消息通知模板信息
     */
    fun deleteEmailsNotifyMessageTemplate(dslContext: DSLContext, id: String): Int {
        with(TEmailsNotifyMessageTemplate.T_EMAILS_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(COMMON_TEMPLATE_ID.eq(id))
                .execute()
        }
    }

    /**
     * 删除企业微信类型的消息通知模板信息
     */
    fun deleteRtxNotifyMessageTemplate(dslContext: DSLContext, id: String): Int {
        with(TRtxNotifyMessageTemplate.T_RTX_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(COMMON_TEMPLATE_ID.eq(id))
                .execute()
        }
    }

    /**
     * 删除微信类型的消息通知模板信息
     */
    fun deleteWechatNotifyMessageTemplate(dslContext: DSLContext, id: String): Int {
        with(TWechatNotifyMessageTemplate.T_WECHAT_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(COMMON_TEMPLATE_ID.eq(id))
                .execute()
        }
    }

    /**
     * 根据模板ID，更新微信消息模板信息
     * @param userId 更新用户ID
     * @param templateId 要修改的模板ID
     * @param notifyMessageTemplate 修改信息对象
     */
    fun updateWechatNotifyMessageTemplate(
        dslContext: DSLContext,
        userId: String,
        templateId: String,
        notifyMessageTemplate: NotifyTemplateMessage
    ): Int {
        with(TWechatNotifyMessageTemplate.T_WECHAT_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.update(this)
                .set(this.MODIFIOR, userId)
                .set(this.TITLE, notifyMessageTemplate.title)
                .set(this.BODY, notifyMessageTemplate.body)
                .set(this.UPDATE_TIME, LocalDateTime.now())
                .where(this.COMMON_TEMPLATE_ID.eq(templateId))
                .execute()
        }
    }

    /**
     * 根据模板ID，更新企业微信消息模板信息
     * @param userId 更新用户ID
     * @param templateId 要修改的模板ID
     * @param notifyMessageTemplate 修改信息对象
     */
    fun updateRtxNotifyMessageTemplate(
        dslContext: DSLContext,
        userId: String,
        templateId: String,
        notifyMessageTemplate: NotifyTemplateMessage
    ): Int {
        with(TRtxNotifyMessageTemplate.T_RTX_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.update(this)
                .set(this.MODIFIOR, userId)
                .set(this.TITLE, notifyMessageTemplate.title)
                .set(this.BODY, notifyMessageTemplate.body)
                .set(this.BODY_MD, notifyMessageTemplate.bodyMD)
                .set(this.UPDATE_TIME, LocalDateTime.now())
                .where(this.COMMON_TEMPLATE_ID.eq(templateId))
                .execute()
        }
    }

    /**
     * 根据模板ID，更新邮件消息模板信息
     * @param userId 更新用户ID
     * @param templateId 要修改的模板ID
     * @param notifyTemplateMessage 修改信息对象
     */
    fun updateEmailsNotifyMessageTemplate(
        dslContext: DSLContext,
        userId: String,
        templateId: String,
        notifyTemplateMessage: NotifyTemplateMessage
    ): Int {
        with(TEmailsNotifyMessageTemplate.T_EMAILS_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.update(this)
                .set(this.MODIFIOR, userId)
                .set(this.TITLE, notifyTemplateMessage.title)
                .set(this.BODY, notifyTemplateMessage.body)
                .set(this.UPDATE_TIME, LocalDateTime.now())
                .set(this.EMAIL_TYPE, notifyTemplateMessage.emailType!!.getValue().toByte())
                .set(this.BODY_FORMAT, notifyTemplateMessage.bodyFormat!!.getValue().toByte())
                .where(this.COMMON_TEMPLATE_ID.eq(templateId))
                .execute()
        }
    }

    /**
     * 根据模板ID，更新公共消息模板信息
     * @param templateId 要修改的模板ID
     * @param notifyMessageTemplateRequest 修改信息对象
     */
    fun updateCommonNotifyMessageTemplate(
        dslContext: DSLContext,
        templateId: String,
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest,
        notifyTypeScopeSet: Set<String>
    ): Int {
        with(TCommonNotifyMessageTemplate.T_COMMON_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.update(this)
                .set(this.TEMPLATE_NAME, notifyMessageTemplateRequest.templateName)
                .set(this.NOTIFY_TYPE_SCOPE, JsonUtil.toJson(notifyTypeScopeSet, formatted = false))
                .set(this.PRIORITY, notifyMessageTemplateRequest.priority.getValue().toByte())
                .set(this.SOURCE, notifyMessageTemplateRequest.source.getValue().toByte())
                .where(this.ID.eq(templateId))
                .execute()
        }
    }

    /**
     * 根据模板ID判断此模板是否含有邮件消息类型
     */
    fun countEmailMessageTemplate(dslContext: DSLContext, templateId: String): Int {
        with(TEmailsNotifyMessageTemplate.T_EMAILS_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.selectCount()
                .from(this)
                .where(COMMON_TEMPLATE_ID.eq(templateId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 根据模板ID判断此模板是否含有微信消息类型
     */
    fun countWechatMessageTemplate(dslContext: DSLContext, templateId: String): Int {
        with(TWechatNotifyMessageTemplate.T_WECHAT_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.selectCount()
                .from(this)
                .where(COMMON_TEMPLATE_ID.eq(templateId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 根据模板ID判断此模板是否含有企业微信消息类型
     */
    fun countRtxMessageTemplate(dslContext: DSLContext, templateId: String): Int {
        with(TRtxNotifyMessageTemplate.T_RTX_NOTIFY_MESSAGE_TEMPLATE) {
            return dslContext.selectCount()
                .from(this)
                .where(COMMON_TEMPLATE_ID.eq(templateId))
                .fetchOne(0, Int::class.java)!!
        }
    }
}
