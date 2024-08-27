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

package com.tencent.devops.environment.dao.job

import com.tencent.devops.model.environment.tables.TNetworkArea
import com.tencent.devops.model.environment.tables.records.TNetworkAreaRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL

import org.springframework.stereotype.Repository

@Repository
class NetworkAreaDao {
    fun getOssAndDevnetNetAreaList(dslContext: DSLContext): Result<TNetworkAreaRecord> {
        with(TNetworkArea.T_NETWORK_AREA) {
            return dslContext.selectFrom(this)
                .where(NET_AREA.`in`("OSS", "DEVNET"))
                .fetch()
        }
    }

    fun getAllNetworkArea(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int,
        keyword: String? = null
    ): Result<TNetworkAreaRecord> {
        with(TNetworkArea.T_NETWORK_AREA) {
            val queryNetworkArea = dslContext.selectFrom(this)
            if (!keyword.isNullOrEmpty())
                queryNetworkArea.where(NET_AREA.like("%$keyword%"))
            queryNetworkArea.orderBy(NET_AREA_ID.desc()).limit(pageSize).offset((page - 1) * pageSize)
            return queryNetworkArea.fetch()
        }
    }

    fun insertNetworkArea(dslContext: DSLContext, netArea: String, netSegment: String): Int {
        with(TNetworkArea.T_NETWORK_AREA) {
            return dslContext.insertInto(this)
                .set(NET_AREA, netArea)
                .set(NET_SEGMENT, netSegment)
                .execute()
        }
    }

    fun addNetWorkSegment(dslContext: DSLContext, netArea: String, netSegment: String): Int {
        with(TNetworkArea.T_NETWORK_AREA) {
            return dslContext.update(this)
                .set(NET_SEGMENT, DSL.concat(NET_SEGMENT, netSegment))
                .where(NET_AREA.eq(netArea))
                .execute()
        }
    }

    fun replaceNetworkAreaSegment(dslContext: DSLContext, netArea: String, netSegment: String): Int {
        with(TNetworkArea.T_NETWORK_AREA) {
            return dslContext.update(this)
                .set(NET_SEGMENT, netSegment)
                .where(NET_AREA.eq(netArea))
                .execute()
        }
    }

    fun deleteNetworkArea(dslContext: DSLContext, netArea: String): Int {
        with(TNetworkArea.T_NETWORK_AREA) {
            return dslContext.deleteFrom(this)
                .where(NET_AREA.eq(netArea))
                .execute()
        }
    }
}
