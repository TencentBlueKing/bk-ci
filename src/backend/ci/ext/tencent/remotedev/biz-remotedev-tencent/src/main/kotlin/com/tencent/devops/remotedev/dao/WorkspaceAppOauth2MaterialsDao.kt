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

package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceAppOauth2Materials
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceAppOauth2MaterialsRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WorkspaceAppOauth2MaterialsDao {

    fun create(
        dslContext: DSLContext,
        appId: String,
        workspaceName: String,
        clientId: String,
        clientSecret: String
    ) {
        with(TWorkspaceAppOauth2Materials.T_WORKSPACE_APP_OAUTH2_MATERIALS) {
            dslContext.insertInto(
                this,
                APP_ID,
                WORKSPACE_NAME,
                CLIENT_ID,
                CLIENT_SECRET
            )
                .values(
                    appId,
                    workspaceName,
                    clientId,
                    clientSecret
                ).onDuplicateKeyIgnore().execute()
        }
    }

    fun fetchAny(
        dslContext: DSLContext,
        appId: String,
        workspaceName: String
    ): TWorkspaceAppOauth2MaterialsRecord? {
        with(TWorkspaceAppOauth2Materials.T_WORKSPACE_APP_OAUTH2_MATERIALS) {
            return dslContext.selectFrom(this)
                .where(APP_ID.eq(appId))
                .and(WORKSPACE_NAME.eq(workspaceName))
                .fetchAny()
        }
    }
}
