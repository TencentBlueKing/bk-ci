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

package com.tencent.devops.stream.dao

import com.tencent.devops.model.stream.tables.TGitCiServicesConf
import com.tencent.devops.model.stream.tables.records.TGitCiServicesConfRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitCIServicesConfDao {

    fun create(
        dslContext: DSLContext,
        imageName: String,
        imageTag: String,
        repoUrl: String,
        repoUsername: String?,
        repoPwd: String?,
        enable: Boolean,
        env: String?,
        createUser: String?,
        updateUser: String?
    ) {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {
            dslContext.insertInto(
                this,
                IMAGE_NAME,
                IMAGE_TAG,
                REPO_URL,
                REPO_USERNAME,
                REPO_PWD,
                ENABLE,
                ENV,
                CREATE_USER,
                UPDATE_USER,
                GMT_MODIFIED,
                GMT_CREATE
            ).values(
                imageName,
                imageTag,
                repoUrl,
                repoUsername,
                repoPwd,
                enable,
                env,
                createUser,
                updateUser,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Long,
        user: String,
        enable: Boolean?
    ) {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {

            val steps = dslContext.update(this)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .set(UPDATE_USER, user)

            if (enable != null) {
                steps.set(ENABLE, enable)
            }
            steps.where(ID.eq(id)).execute()
        }
    }

    fun get(dslContext: DSLContext, imageName: String, imageTag: String): TGitCiServicesConfRecord? {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {
            return dslContext.selectFrom(this)
                .where(IMAGE_NAME.eq(imageName))
                .and(IMAGE_TAG.eq(imageTag))
                .fetchOne()
        }
    }

    fun list(dslContext: DSLContext): Result<TGitCiServicesConfRecord> {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ) {
        with(TGitCiServicesConf.T_GIT_CI_SERVICES_CONF) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }
}
