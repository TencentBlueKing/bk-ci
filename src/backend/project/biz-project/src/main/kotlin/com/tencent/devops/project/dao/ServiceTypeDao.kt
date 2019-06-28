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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.project.tables.TServiceType
import com.tencent.devops.model.project.tables.records.TServiceTypeRecord
import com.tencent.devops.project.pojo.service.ServiceType
import com.tencent.devops.project.pojo.service.ServiceTypeModify
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ServiceTypeDao {
    fun getAllIdAndTitle(dslContext: DSLContext): Result<TServiceTypeRecord> {
        with(TServiceType.T_SERVICE_TYPE) {
            return dslContext.selectFrom(this)
                .where(DELETED.eq(false))
                .fetch()
        }
    }

    fun list(dslContext: DSLContext): List<ServiceType> {
        with(TServiceType.T_SERVICE_TYPE) {
            return dslContext.selectFrom(this)
                .where(DELETED.eq(false))
                .orderBy(WEIGHT.asc())
                .fetch {
                    ServiceType(
                        it.id, it.title, it.weight ?: 0, it.createdUser,
                        DateTimeUtil.toDateTime(it.createdTime), it.updatedUser,
                        DateTimeUtil.toDateTime(it.updatedTime)
                    )
                }
        }
    }

    fun create(dslContext: DSLContext, userId: String, title: String, weight: Int): ServiceType {
        val now = LocalDateTime.now()
        with(TServiceType.T_SERVICE_TYPE) {
            return dslContext.insertInto(
                this, TITLE, WEIGHT, CREATED_USER, CREATED_TIME, UPDATED_USER, UPDATED_TIME, DELETED
            ).values(title, weight, userId, now, userId, now, false).returning()
                .fetchOne().let {
                    ServiceType(
                        it.id, it.title, weight, it.createdUser,
                        DateTimeUtil.toDateTime(it.createdTime), it.updatedUser, DateTimeUtil.toDateTime(it.updatedTime)
                    )
                }
        }
    }

    fun update(dslContext: DSLContext, userId: String, serviceTypeId: Long, serviceTypeModify: ServiceTypeModify) {
        val now = LocalDateTime.now()
        with(TServiceType.T_SERVICE_TYPE) {
            dslContext.update(this)
                .set(TITLE, serviceTypeModify.title)
                .set(WEIGHT, serviceTypeModify.weight)
                .set(UPDATED_USER, userId)
                .set(UPDATED_TIME, now)
                .where(ID.eq(serviceTypeId))
                .and(DELETED.eq(false))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, serviceTypeId: Long): Boolean {
        with(TServiceType.T_SERVICE_TYPE) {
            return dslContext.update(this)
                .set(DELETED, true)
                .where(ID.eq(serviceTypeId))
                .execute() > 0
        }
    }

    fun get(dslContext: DSLContext, id: Long): ServiceType {
        with(TServiceType.T_SERVICE_TYPE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .and(DELETED.eq(false))
                .fetchOne {
                    ServiceType(
                        it.id, it.title, it.weight ?: 0, it.createdUser,
                        DateTimeUtil.toDateTime(it.createdTime), it.updatedUser, DateTimeUtil.toDateTime(it.updatedTime)
                    )
                }
        }
    }
}