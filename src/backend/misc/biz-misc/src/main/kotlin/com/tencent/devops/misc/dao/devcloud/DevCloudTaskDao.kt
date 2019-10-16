package com.tencent.devops.misc.dao.devcloud

import com.tencent.devops.environment.pojo.devcloud.TaskStatus
import com.tencent.devops.model.environment.tables.TDevCloudTask
import com.tencent.devops.model.environment.tables.records.TDevCloudTaskRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class DevCloudTaskDao {

    fun updateTaskStatus(dslContext: DSLContext, taskId: Long, status: TaskStatus): Int {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.update(this)
                    .set(STATUS, status.name)
                    .where(TASK_ID.eq(taskId))
                    .execute()
        }
    }

    fun updateTaskStatus(dslContext: DSLContext, taskId: Long, status: TaskStatus, msg: String): Int {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.update(this)
                    .set(STATUS, status.name)
                    .set(DESCRIPTION, msg)
                    .where(TASK_ID.eq(taskId))
                    .execute()
        }
    }

    fun updateDevCloudTaskId(dslContext: DSLContext, taskId: Long, devCloudTaskId: String): Int {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.update(this)
                    .set(DEV_CLOUD_TASK_ID, devCloudTaskId)
                    .where(TASK_ID.eq(taskId))
                    .execute()
        }
    }

    fun getWaitingTask(dslContext: DSLContext): Result<TDevCloudTaskRecord>? {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.selectFrom(this)
                    .where(STATUS.eq(TaskStatus.WAITING.name))
                    .fetch()
        }
    }
}