package com.tencent.devops.auth.dao

import com.tencent.devops.auth.entity.MangerOrganizationInfo
import com.tencent.devops.model.auth.tables.TAuthManager
import com.tencent.devops.model.auth.tables.records.TAuthManagerRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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

@Repository
class ManagerOrganizationDao {

    fun create(dslContext: DSLContext, userId: String, managerOrganization: MangerOrganizationInfo): Int {
        with(TAuthManager.T_AUTH_MANAGER) {
            return dslContext.insertInto(this,
                NAME,
                ORGANIZATION_ID,
                LEVEL,
                STRATEGYID,
                IS_DELETE,
                CREATE_USER,
                CREATE_TIME,
                UPDATE_USER,
                UPDATE_TIME
            ).values(
                managerOrganization.name,
                managerOrganization.organizationId,
                managerOrganization.organizationLevel,
                managerOrganization.strategyId,
                0,
                userId,
                LocalDateTime.now(),
                "",
                null
            ).execute()
        }
    }

    fun update(dslContext: DSLContext, id: Int, managerOrganization: MangerOrganizationInfo, userId: String) {
        with(TAuthManager.T_AUTH_MANAGER) {
            dslContext.update(this)
                .set(NAME, managerOrganization.name)
                .set(STRATEGYID, managerOrganization.strategyId)
                .set(ORGANIZATION_ID, managerOrganization.organizationId)
                .set(LEVEL, managerOrganization.organizationLevel)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(UPDATE_USER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: Int, userId: String) {
        with(TAuthManager.T_AUTH_MANAGER) {
            dslContext.update(this)
                .set(IS_DELETE, 1)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(UPDATE_USER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, id: Int) : TAuthManagerRecord? {
        with(TAuthManager.T_AUTH_MANAGER) {
            return dslContext.selectFrom(this).where(ID.eq(id)).fetchOne()
        }
    }
}
