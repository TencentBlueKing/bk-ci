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

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.action.ActionInfo
import com.tencent.devops.auth.pojo.action.CreateActionDTO
import com.tencent.devops.auth.pojo.action.UpdateActionDTO
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.auth.tables.TAuthAction
import com.tencent.devops.model.auth.tables.records.TAuthActionRecord
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.swing.Action

@Repository
class ActionDao {

    fun createAction(dslContext: DSLContext, actionInfo: CreateActionDTO, userId: String) {
        with(TAuthAction.T_AUTH_ACTION) {
            dslContext.insertInto(
                this,
                ACTIONID,
                ACTIONNAME,
                RESOURCEID,
                RELATIONACTION,
                CREATOR,
                CREATETIME,
                DELETE
            ).values(
                actionInfo.actionId,
                actionInfo.actionName,
                actionInfo.resourceId,
                actionInfo.relationAction.joinToString { "," },
                userId,
                LocalDateTime.now(),
                false
            ).execute()
        }
    }

    fun updateAction(
        dslContext: DSLContext,
        actionInfo: UpdateActionDTO,
        actionId: String,
        userId: String
    ) {
        with(TAuthAction.T_AUTH_ACTION) {
            dslContext.update(this).set(ACTIONNAME, actionInfo.actionName)
                .set(RELATIONACTION, actionInfo.relationAction.joinToString { "," })
                .where(ACTIONID.eq(actionId)).execute()
        }
    }

    fun deleteAction(
        dslContext: DSLContext,
        actionId: String
    ) {
        with(TAuthAction.T_AUTH_ACTION) {
            dslContext.deleteFrom(this).where(ACTIONID.eq(actionId)).execute()
        }
    }

    fun getAction(
        dslContext: DSLContext,
        actionId: String,
        field: String
    ): ActionInfo? {
        with(TAuthAction.T_AUTH_ACTION) {
            val record = dslContext.selectFrom(field)
                .where(ACTIONID.eq(actionId).and(DELETE.eq(false)))
                .fetchAny() as TAuthActionRecord? ?: return null
            return convert(record)
        }
    }

    fun getAllAction(
        dslContext: DSLContext,
        field: String
    ): List<ActionInfo>? {
        with(TAuthAction.T_AUTH_ACTION) {
            val records = dslContext.selectFrom(field).where(DELETE.eq(false)).fetch()
            val actionInfos = mutableListOf<ActionInfo>()
            records.map {
                actionInfos.add(convert(it as TAuthActionRecord))
            }
            return actionInfos
        }
    }

    fun getActionByResource(
        dslContext: DSLContext,
        resourceId: String,
        field: String
    ): List<ActionInfo>? {
        with(TAuthAction.T_AUTH_ACTION) {
            val records = dslContext.selectFrom(field).where(RESOURCEID.eq(resourceId).and(DELETE.eq(false))).fetch()
            val actionInfos = mutableListOf<ActionInfo>()
            records.map {
                actionInfos.add(convert(it as TAuthActionRecord))
            }
            return actionInfos
        }
    }

    private fun convert(record: TAuthActionRecord): ActionInfo {
        return ActionInfo(
            actionId = record.actionid,
            actionName = record.actionname,
            actionEnglishName = record.actionname,
            relationAction = record.relationaction.split(","),
            resourceId = record.resourceid,
            creator = record.creator,
            creatorTime = DateTimeUtil.convertLocalDateTimeToTimestamp(record.createtime)
        )
    }
}