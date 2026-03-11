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

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.stream.tables.TGitProjectConf
import com.tencent.devops.model.stream.tables.records.TGitProjectConfRecord
import com.tencent.devops.stream.pojo.GitProjectConf
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime

@Repository
class GitProjectConfDao {

    fun create(
        dslContext: DSLContext,
        gitProjectId: Long,
        name: String,
        url: String,
        enable: Boolean
    ) {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            dslContext.insertInto(
                this,
                ID,
                NAME,
                URL,
                ENABLE,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                gitProjectId,
                name,
                url,
                enable,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        gitProjectId: Long,
        name: String?,
        url: String?,
        enable: Boolean?
    ) {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {

            val steps = dslContext.update(this).set(UPDATE_TIME, LocalDateTime.now())
            if (!name.isNullOrBlank()) {
                steps.set(NAME, name)
            }
            if (!url.isNullOrBlank()) {
                steps.set(URL, url)
            }
            if (enable != null) {
                steps.set(ENABLE, enable)
            }
            steps.where(ID.eq(gitProjectId)).execute()
        }
    }

    fun get(dslContext: DSLContext, gitProjectId: Long): GitProjectConf? {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            val record = dslContext.selectFrom(this)
                .where(ID.eq(gitProjectId))
                .fetchOne()
            return if (record == null) {
                null
            } else {
                GitProjectConf(
                    record.id,
                    record.name,
                    record.url,
                    record.enable,
                    record.createTime.timestampmilli(),
                    record.updateTime.timestampmilli()
                )
            }
        }
    }

    fun delete(
        dslContext: DSLContext,
        gitProjectId: Long
    ) {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            dslContext.deleteFrom(this)
                .where(ID.eq(gitProjectId))
                .execute()
        }
    }

    fun count(
        dslContext: DSLContext,
        gitProjectId: Long?,
        name: String?,
        url: String?
    ): Int {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            val conditions = mutableListOf<Condition>()
            if (gitProjectId != null) {
                conditions.add(ID.eq(gitProjectId))
            }
            if (!name.isNullOrBlank()) {
                conditions.add(
                    NAME.like(
                        "%" + URLDecoder.decode(
                            name,
                            "UTF-8"
                        ) + "%"
                    )
                )
            }
            if (!url.isNullOrBlank()) {
                conditions.add(
                    URL.like(
                        "%" + URLDecoder.decode(
                            url,
                            "UTF-8"
                        ) + "%"
                    )
                )
            }

            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getList(
        dslContext: DSLContext,
        gitProjectId: Long?,
        name: String?,
        url: String?,
        page: Int,
        pageSize: Int
    ): Result<TGitProjectConfRecord> {
        with(TGitProjectConf.T_GIT_PROJECT_CONF) {
            val conditions = mutableListOf<Condition>()
            if (gitProjectId != null) {
                conditions.add(ID.eq(gitProjectId))
            }
            if (!name.isNullOrBlank()) {
                conditions.add(
                    NAME.like(
                        "%" + URLDecoder.decode(
                            name,
                            "UTF-8"
                        ) + "%"
                    )
                )
            }
            if (!url.isNullOrBlank()) {
                conditions.add(
                    URL.like(
                        "%" + URLDecoder.decode(
                            url,
                            "UTF-8"
                        ) + "%"
                    )
                )
            }
            return dslContext.selectFrom(this).where(conditions)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }
}
