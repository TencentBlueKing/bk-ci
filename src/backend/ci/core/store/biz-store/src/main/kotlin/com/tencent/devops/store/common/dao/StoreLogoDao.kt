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

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.store.tables.TLogo
import com.tencent.devops.model.store.tables.records.TLogoRecord
import com.tencent.devops.store.pojo.common.logo.Logo
import com.tencent.devops.store.pojo.common.logo.StoreLogoReq
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreLogoDao {

    fun add(dslContext: DSLContext, id: String, userId: String, storeLogoReq: StoreLogoReq, type: String) {
        with(TLogo.T_LOGO) {
            dslContext.insertInto(
                this,
                ID,
                LOGO_URL,
                TYPE,
                CREATOR,
                MODIFIER,
                ORDER,
                LINK
            )
                .values(
                    id,
                    storeLogoReq.logoUrl,
                    type,
                    userId,
                    userId,
                    storeLogoReq.order,
                    storeLogoReq.link
                ).execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TLogo.T_LOGO) {
            dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun update(dslContext: DSLContext, id: String, userId: String, storeLogoReq: StoreLogoReq) {
        with(TLogo.T_LOGO) {
            dslContext.update(this)
                    .set(LOGO_URL, storeLogoReq.logoUrl)
                    .set(ORDER, storeLogoReq.order)
                    .set(LINK, storeLogoReq.link)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(MODIFIER, userId)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun getLogo(dslContext: DSLContext, id: String): TLogoRecord? {
        with(TLogo.T_LOGO) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }

    fun getAllLogo(dslContext: DSLContext, type: String): Result<TLogoRecord>? {
        with(TLogo.T_LOGO) {
            return dslContext
                    .selectFrom(this)
                    .where(TYPE.eq(type))
                    .orderBy(ORDER.asc(), CREATE_TIME.asc())
                    .fetch()
        }
    }

    fun convert(record: TLogoRecord): Logo {
        with(record) {
            return Logo(
                id = id,
                logoUrl = logoUrl?.let {
                    StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
                } ?: "",
                logoType = type,
                order = order,
                link = link,
                createTime = DateTimeUtil.toDateTime(createTime),
                updateTime = DateTimeUtil.toDateTime(updateTime),
                creator = creator,
                modifier = modifier
            )
        }
    }
}
