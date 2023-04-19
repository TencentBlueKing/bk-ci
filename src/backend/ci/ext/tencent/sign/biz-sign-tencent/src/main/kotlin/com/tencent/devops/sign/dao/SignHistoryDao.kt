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

package com.tencent.devops.sign.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.sign.tables.TSignHistory
import com.tencent.devops.model.sign.tables.records.TSignHistoryRecord
import com.tencent.devops.sign.api.enums.EnumResignStatus
import com.tencent.devops.sign.api.pojo.SignHistory
import java.sql.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.jooq.Result

@Suppress("LongParameterList", "TooManyFunctions")
@Repository
class SignHistoryDao {

    fun initHistory(
        dslContext: DSLContext,
        resignId: String,
        userId: String,
        projectId: String?,
        pipelineId: String?,
        buildId: String?,
        taskId: String?,
        archiveType: String?,
        archivePath: String?,
        md5: String?
    ): Int {
        with(TSignHistory.T_SIGN_HISTORY) {
            var executeCount = 1
            val record = dslContext.selectFrom(this)
                .where(TASK_ID.eq(taskId))
                .and(BUILD_ID.eq(buildId))
                .orderBy(TASK_EXECUTE_COUNT.desc())
                .fetchOne()
            if (record != null) {
                executeCount = record.taskExecuteCount + 1
            }

            dslContext.insertInto(this,
                RESIGN_ID,
                USER_ID,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                TASK_ID,
                TASK_EXECUTE_COUNT,
                ARCHIVE_TYPE,
                ARCHIVE_PATH,
                FILE_MD5,
                STATUS,
                CREATE_TIME,
                RESULT_FILE_MD5,
                UPLOAD_FINISH_TIME,
                UNZIP_FINISH_TIME,
                RESIGN_FINISH_TIME,
                ZIP_FINISH_TIME,
                ARCHIVE_FINISH_TIME
            ).values(
                resignId,
                userId,
                projectId,
                pipelineId,
                buildId,
                taskId,
                executeCount,
                archiveType,
                archivePath,
                md5,
                EnumResignStatus.RUNNING.getValue(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null
            ).execute()
            return executeCount
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
                .execute()
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
                .execute()
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
                .execute()
        }
    }

    fun finishZip(
        dslContext: DSLContext,
        resignId: String,
        resultFileName: String,
        resultFileMd5: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)
                .set(ZIP_FINISH_TIME, LocalDateTime.now())
                .set(RESULT_FILE_NAME, resultFileName)
                .set(RESULT_FILE_MD5, resultFileMd5)
                .where(RESIGN_ID.eq(resignId))
                .execute()
        }
    }

    fun finishArchive(
        dslContext: DSLContext,
        resignId: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)
                .set(ARCHIVE_FINISH_TIME, LocalDateTime.now())
                .where(RESIGN_ID.eq(resignId))
                .execute()
        }
    }

    fun successResign(
        dslContext: DSLContext,
        resignId: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)
                    .set(END_TIME, LocalDateTime.now())
                    .set(STATUS, EnumResignStatus.SUCCESS.getValue())
                    .where(RESIGN_ID.eq(resignId))
                    .execute()
        }
    }

    fun failResign(
        dslContext: DSLContext,
        resignId: String,
        message: String
    ) {
        with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.update(this)
                .set(END_TIME, LocalDateTime.now())
                .set(STATUS, EnumResignStatus.FAIL.getValue())
                .set(ERROR_MESSAGE, message)
                .where(RESIGN_ID.eq(resignId))
                .execute()
        }
    }

    fun getSignHistory(dslContext: DSLContext, resignId: String): TSignHistoryRecord? {
        return with(TSignHistory.T_SIGN_HISTORY) {
            dslContext.selectFrom(this)
                .where(
                    RESIGN_ID.eq(resignId)
                ).fetchAny()
        }
    }

    fun count(
        dslContext: DSLContext,
        startTime: Long,
        endTime: Long
    ): Long {
        with(TSignHistory.T_SIGN_HISTORY) {
            return dslContext.selectCount()
                .from(this)
                .where(CREATE_TIME.ge(Timestamp(startTime).toLocalDateTime()))
                .and(CREATE_TIME.le(Timestamp(endTime).toLocalDateTime()))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun list(
        dslContext: DSLContext,
        startTime: Long,
        endTime: Long,
        offset: Int,
        limit: Int
    ): Result<TSignHistoryRecord> {
        with(TSignHistory.T_SIGN_HISTORY) {
            return dslContext.selectFrom(this)
                .where(CREATE_TIME.ge(Timestamp(startTime).toLocalDateTime()))
                .and(CREATE_TIME.le(Timestamp(endTime).toLocalDateTime()))
                .orderBy(CREATE_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun convert(tSignHistoryRecord: TSignHistoryRecord): SignHistory {
        return with(tSignHistoryRecord) {
            SignHistory(
                resignId = resignId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId,
                userId = userId,
                md5 = fileMd5,
                resultFileName = resultFileName,
                resultFileMd5 = resultFileMd5,
                archiveType = archiveType,
                archivePath = archivePath,
                status = status,
                createTime = createTime?.timestampmilli(),
                endTime = endTime?.timestampmilli(),
                uploadFinishTime = uploadFinishTime?.timestampmilli(),
                unzipFinishTime = unzipFinishTime?.timestampmilli(),
                resignFinishTime = resignFinishTime?.timestampmilli(),
                zipFinishTime = zipFinishTime?.timestampmilli(),
                archiveFinishTime = archiveFinishTime?.timestampmilli(),
                errorMessage = errorMessage,
                ipaSignInfoStr = null
            )
        }
    }
}
