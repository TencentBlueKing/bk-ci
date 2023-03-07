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

import com.tencent.devops.model.remotedev.tables.TRemoteDevFile
import com.tencent.devops.remotedev.pojo.RemoteDevFile
import com.tencent.devops.remotedev.utils.GzipUtil
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class RemoteDevFileDao {

    fun createFile(
        dslContext: DSLContext,
        path: String,
        content: String,
        userId: String,
        md5: String
    ): Long {
        return with(TRemoteDevFile.T_REMOTE_DEV_FILE) {
            dslContext.insertInto(
                this,
                PATH,
                CONTENT,
                MD5,
                USER
            ).values(
                path,
                GzipUtil.gzipBytes(content.toByteArray()),
                md5,
                userId
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun fetchAnyFile(
        dslContext: DSLContext,
        id: Long
    ): RemoteDevFile? {
        return with(TRemoteDevFile.T_REMOTE_DEV_FILE) {
            dslContext.selectFrom(this).where(ID.eq(id)).fetchAny()?.let {
                RemoteDevFile(
                    id = it.id,
                    md5 = it.md5,
                    path = it.path,
                    content = String(GzipUtil.unzipBytes(it.content))
                )
            }
        }
    }

    fun fetchFile(
        dslContext: DSLContext,
        userId: String
    ): List<RemoteDevFile> {
        return with(TRemoteDevFile.T_REMOTE_DEV_FILE) {
            dslContext.selectFrom(this).where(USER.eq(userId)).orderBy(CREATED_TIME.desc())
                .limit(20).fetch().map {
                RemoteDevFile(
                    id = it.id,
                    md5 = it.md5,
                    path = it.path,
                    content = String(GzipUtil.unzipBytes(it.content))
                )
            }
        }
    }

    fun updateFile(
        dslContext: DSLContext,
        file: RemoteDevFile,
        md5: String,
        userId: String
    ): Boolean {
        return with(TRemoteDevFile.T_REMOTE_DEV_FILE) {
            dslContext.update(this)
                .set(PATH, file.path)
                .set(CONTENT, GzipUtil.gzipBytes(file.content.toByteArray()))
                .set(MD5, md5)
                .where(ID.eq(file.id)).and(USER.eq(userId)).execute() == 1
        }
    }

    /**
     * 注意：是 not in 操作删除
     */
    fun batchDeleteFile(
        dslContext: DSLContext,
        ids: Set<Long>,
        userId: String
    ): Int {
        return with(TRemoteDevFile.T_REMOTE_DEV_FILE) {
            dslContext.deleteFrom(this).where(USER.eq(userId)).and(ID.notIn(ids)).execute()
        }
    }
}
