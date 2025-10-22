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

package com.tencent.devops.image.dao

import com.tencent.devops.model.image.tables.TUploadImageTask
import com.tencent.devops.model.image.tables.records.TUploadImageTaskRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UploadImageTaskDao {
    fun get(dslContext: DSLContext, taskId: String, projectId: String): TUploadImageTaskRecord? {
        with(TUploadImageTask.T_UPLOAD_IMAGE_TASK) {
            return dslContext.selectFrom(this)
                    .where(TASK_ID.eq(taskId))
                    .and(PROJECT_ID.eq(projectId))
                    .fetchOne()
        }
    }

    fun add(dslContext: DSLContext, importImage: TUploadImageTaskRecord) {
        with(TUploadImageTask.T_UPLOAD_IMAGE_TASK) {
            dslContext.insertInto(this,
                    TASK_ID,
                    PROJECT_ID,
                    OPERATOR,
                    CREATED_TIME,
                    UPDATED_TIME,
                    TASK_STATUS,
                    TASK_MESSAGE,
                    IMAGE_DATA
            ).values(
                    importImage.taskId,
                    importImage.projectId,
                    importImage.operator,
                    importImage.createdTime,
                    importImage.updatedTime,
                    importImage.taskStatus,
                    importImage.taskMessage,
                    importImage.imageData
            ).execute()
        }
    }

    fun update(dslContext: DSLContext, taskId: String, taskStatus: String, taskMessage: String, imageData: String) {
        with(TUploadImageTask.T_UPLOAD_IMAGE_TASK) {
            dslContext.update(this)
                    .set(UPDATED_TIME, LocalDateTime.now())
                    .set(TASK_STATUS, taskStatus)
                    .set(TASK_MESSAGE, taskMessage)
                    .set(IMAGE_DATA, imageData)
                    .set(UPDATED_TIME, LocalDateTime.now())
                    .where(TASK_ID.eq(taskId))
                    .execute()
        }
    }

    fun delete(dslContext: DSLContext, taskId: String) {
        with(TUploadImageTask.T_UPLOAD_IMAGE_TASK) {
            dslContext.deleteFrom(this)
                    .where(TASK_ID.eq(taskId))
                    .execute()
        }
    }
}
