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

package com.tencent.devops.misc.dao.project

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class ProjectMiscDao {
    fun getMinId(
        dslContext: DSLContext,
        projectIdList: List<String>? = null
    ): Long {
        with(TProject.T_PROJECT) {
            val baseStep = dslContext.select(DSL.min(ID)).from(this)
            if (!projectIdList.isNullOrEmpty()) {
                baseStep.where(ENGLISH_NAME.`in`(projectIdList))
            }
            return baseStep.skipCheck().fetchOne(0, Long::class.java)!!
        }
    }

    fun getMaxId(
        dslContext: DSLContext,
        projectIdList: List<String>? = null
    ): Long {
        with(TProject.T_PROJECT) {
            val baseStep = dslContext.select(DSL.max(ID)).from(this)
            if (!projectIdList.isNullOrEmpty()) {
                baseStep.where(ENGLISH_NAME.`in`(projectIdList))
            }
            return baseStep.skipCheck().fetchOne(0, Long::class.java)!!
        }
    }

    fun getProjectInfoList(
        dslContext: DSLContext,
        projectIdList: List<String>? = null,
        minId: Long? = null,
        maxId: Long? = null,
        channelCodeList: List<String>? = null
    ): Result<out Record>? {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (!projectIdList.isNullOrEmpty()) {
                conditions.add(ENGLISH_NAME.`in`(projectIdList))
            }
            if (minId != null) {
                conditions.add(ID.ge(minId))
            }
            if (maxId != null) {
                conditions.add(ID.lt(maxId))
            }
            if (!channelCodeList.isNullOrEmpty()) {
                conditions.add(CHANNEL.`in`(channelCodeList))
            }
            return dslContext.select(
                ID.`as`("ID"),
                ENGLISH_NAME.`as`("ENGLISH_NAME"),
                CHANNEL.`as`("CHANNEL")
            ).from(this).where(conditions).fetch()
        }
    }

    fun getDisableProject(
        dslContext: DSLContext,
        channelCodes: List<String>,
        /*获取小于该更新时间的项目*/
        ltUpdateTime: String,
        limit: Int,
        offset: Int
    ): Result<TProjectRecord> {
        val date = DateTimeUtil.stringToLocalDate(ltUpdateTime)!!.atStartOfDay()
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this)
                .where(ENABLED.eq(false))
                .and(CHANNEL.`in`(channelCodes))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .and(UPDATED_AT.lt(date))
                .limit(limit).offset(offset).fetch()
        }
    }

    companion object {
        private val UNSUCCESSFUL_CREATE_STATUS = listOf(
            ProjectApproveStatus.CREATE_PENDING.status,
            ProjectApproveStatus.CREATE_REJECT.status
        )
    }
}
