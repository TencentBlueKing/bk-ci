package com.tencent.devops.process.dao.third

import com.tencent.devops.model.process.tables.TThirdJob
import com.tencent.devops.process.pojo.third.enum.JobType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ThirdJobDao {

    fun saveResponse(
        dslContext: DSLContext,
        taskId: String,
        responseStr: String,
        type: JobType,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        with(TThirdJob.T_THIRD_JOB) {
            dslContext.insertInto(this, TASK_ID, PROJECT_ID, PIPELINE_ID, BUILD_ID, RESPONSE, TYPE)
                    .values(taskId.toInt(), projectId, pipelineId, buildId, responseStr, type.name)
                    .execute()
        }
    }

    fun saveCallback(dslContext: DSLContext, taskId: String, type: JobType, callbackStr: String) {
        with(TThirdJob.T_THIRD_JOB) {
            dslContext.update(this)
                    .set(CALLBACK_RESPONSE, callbackStr)
                    .where(TASK_ID.eq(taskId.toInt()).and(TYPE.eq(type.name)))
                    .execute()
        }
    }
}