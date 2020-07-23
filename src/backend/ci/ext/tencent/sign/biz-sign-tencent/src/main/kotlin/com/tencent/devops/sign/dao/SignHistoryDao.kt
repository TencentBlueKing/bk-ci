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

package com.tencent.devops.sign.dao

import com.tencent.devops.model.sign.tables.TSignHistory
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SignHistoryDao {

    fun initHistory(
        dslContext: DSLContext,
        resignId: String,
        userId: String,
        projectId: String?,
        pipelineId: String?,
        buildId: String?,
        archiveType: String?,
        archivePath: String?,
        md5: String?
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.insertInto(this,
                RESIGN_ID,
                USER_ID,
                UPLOAD_FINISH_TIME,
                UNZIP_FINISH_TIME,
                RESIGN_FINISH_TIME,
                ZIP_FINISH_TIME,
                ARCHIVE_FINISH_TIME,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                ARCHIVE_TYPE,
                ARCHIVE_PATH,
                FILE_MD5,
                RESULT_FILE_MD5,
                DOWNLOAD_URL
            ).values(
                resignId,
                userId,
                null,
                null,
                null,
                null,
                null,
                projectId,
                pipelineId,
                buildId,
                archiveType,
                archivePath,
                md5,
                null,
                null
            ).execute()
        }
    }

    fun finishUpload(
        dslContext: DSLContext,
        resignId: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)
                .set(UPLOAD_FINISH_TIME, LocalDateTime.now())
                .where(RESIGN_ID.eq(resignId))
        }
    }

    fun finishUnzip(
        dslContext: DSLContext,
        resignId: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)
                .set(UNZIP_FINISH_TIME, LocalDateTime.now())
                .where(RESIGN_ID.eq(resignId))
        }
    }

    fun finishResign(
        dslContext: DSLContext,
        resignId: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)
                .set(RESIGN_FINISH_TIME, LocalDateTime.now())
                .where(RESIGN_ID.eq(resignId))
        }
    }

    fun finishZip(
        dslContext: DSLContext,
        resignId: String,
        resultFileMd5: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)
                .set(ZIP_FINISH_TIME, LocalDateTime.now())
                .set(RESULT_FILE_MD5, resultFileMd5)
                .where(RESIGN_ID.eq(resignId))
        }
    }

    fun finishArchive(
        dslContext: DSLContext,
        resignId: String,
        downloadUrl: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)

                .set(DOWNLOAD_URL, downloadUrl)
                .set(ARCHIVE_FINISH_TIME, LocalDateTime.now())
                .where(RESIGN_ID.eq(resignId))
        }
    }
}
