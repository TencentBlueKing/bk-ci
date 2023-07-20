package com.tencent.devops.dispatch.codecc.dao

import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.model.dispatch.codecc.tables.TDispatchPipelineDockerBuild
import com.tencent.devops.model.dispatch.codecc.tables.records.TDispatchPipelineDockerBuildRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerBuildDao {

    fun startBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        secretKey: String,
        zone: String?,
        dockerIp: String,
        poolNo: Int
    ): Long {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            val now = LocalDateTime.now()
            val preRecord =
                dslContext.selectFrom(this).where(BUILD_ID.eq(buildId)).and(VM_SEQ_ID.eq(vmSeqId)).fetchAny()
            if (preRecord != null) { // 支持更新，让用户进行步骤重试时继续能使用
                dslContext.update(this).set(SECRET_KEY, SecurityUtil.encrypt(secretKey))
                    .set(CREATED_TIME, now)
                    .set(UPDATED_TIME, now)
                    .set(ZONE, zone)
                    .set(DOCKER_IP, dockerIp)
                    .set(POOL_NO, poolNo)
                    .where(ID.eq(preRecord.id)).execute()
                return preRecord.id
            }
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                SECRET_KEY,
                CREATED_TIME,
                UPDATED_TIME,
                ZONE,
                DOCKER_IP,
                POOL_NO
            )
                .values(
                    projectId,
                    pipelineId,
                    buildId,
                    vmSeqId,
                    SecurityUtil.encrypt(secretKey),
                    now,
                    now,
                    zone,
                    dockerIp,
                    poolNo
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun getBuild(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int
    ): TDispatchPipelineDockerBuildRecord? {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .fetchOne()
        }
    }
}
