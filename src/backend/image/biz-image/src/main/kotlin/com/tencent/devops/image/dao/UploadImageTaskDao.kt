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