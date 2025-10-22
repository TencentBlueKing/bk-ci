/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TGrayTest
import com.tencent.devops.project.pojo.service.GrayTestInfo
import org.jooq.DSLContext
import org.jooq.Record1
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class GrayTestDao {

    fun getStatus(dslContext: DSLContext, userId: String, serviceId: Long?): Record1<String>? {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.select(STATUS).from(this)
                .where(SERVICE_ID.eq(serviceId))
                .and(USERNAME.eq(userId))
                .fetchOne()
        }
    }

    fun create(dslContext: DSLContext, userId: String, serviceId: Long, status: String): GrayTestInfo {

        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.insertInto(
                this, SERVICE_ID, USERNAME, STATUS
            ).values(serviceId, userId, status).returning()
                .fetchOne().let {
                    GrayTestInfo(
                        it!!.id, it.serviceId, it.username, it.status
                    )
                }
        }
    }

    fun update(dslContext: DSLContext, userId: String, serviceId: Long, status: String, id: Long) {
        with(TGrayTest.T_GRAY_TEST) {
            dslContext.update(this)
                .set(SERVICE_ID, serviceId)
                .set(USERNAME, userId)
                .set(STATUS, status)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: Long) {

        with(TGrayTest.T_GRAY_TEST) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, id: Long): GrayTestInfo {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne {
                    GrayTestInfo(it.id, it.serviceId, it.username, it.status)
                }!!
        }
    }

    fun listByUser(dslContext: DSLContext, userId: String): List<GrayTestInfo> {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.selectFrom(this)
                .where(USERNAME.eq(userId))
                .fetch {
                    GrayTestInfo(it.id, it.serviceId, it.username, it.status)
                }
        }
    }

    fun listAllUsers(dslContext: DSLContext): List<String> {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.selectDistinct(USERNAME).from(this)
                    .orderBy(USERNAME.asc())
                    .fetch().map { it.value1() }
        }
    }

    fun listAllService(dslContext: DSLContext): List<Long> {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.selectDistinct(SERVICE_ID).from(this)
                    .orderBy(SERVICE_ID.asc())
                    .fetch().map { it.value1() }
        }
    }

    fun getSum(dslContext: DSLContext): Long {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.selectCount().from(this).fetchOne(0, Long::class.java)!!
        }
    }

    fun listByCondition(
        dslContext: DSLContext,
        userNameList: List<String>,
        serviceIdList: List<String>,
        statusList: List<String>,
        pageSize: Int,
        pageNum: Int
    ): List<GrayTestInfo> {
        with(TGrayTest.T_GRAY_TEST) {
            val condition = dslContext.selectFrom(this).where(USERNAME.isNotNull)
            if (userNameList.isNotEmpty()) condition.and(USERNAME.`in`(userNameList))
            if (serviceIdList.isNotEmpty()) condition.and(SERVICE_ID.`in`(serviceIdList))
            if (statusList.isNotEmpty()) condition.and(STATUS.`in`(statusList))
            condition.limit(pageNum * pageSize, pageSize)

            return condition.fetch {
                GrayTestInfo(it.id, it.serviceId, it.username, it.status)
            }
        }
    }
}
