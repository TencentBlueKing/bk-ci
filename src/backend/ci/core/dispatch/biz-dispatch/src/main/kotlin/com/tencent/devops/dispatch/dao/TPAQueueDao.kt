package com.tencent.devops.dispatch.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchDataSqlJson
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentSqlQueueType
import com.tencent.devops.model.dispatch.tables.TDispatchThirdpartyAgentQueue
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentQueueRecord
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TPAQueueDao {
    // TODO: 记得要配置索引
    fun add(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        data: String,
        dataType: ThirdPartyAgentSqlQueueType,
        info: ThirdPartyAgentDispatchDataSqlJson,
        retryTime: Int
    ) {
        with(TDispatchThirdpartyAgentQueue.T_DISPATCH_THIRDPARTY_AGENT_QUEUE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                DATA,
                DATA_TYPE,
                INFO,
                RETRY_TIME
            ).values(
                projectId,
                pipelineId,
                buildId,
                vmSeqId,
                data,
                dataType.name,
                JSON.json(JsonUtil.toJson(info)),
                retryTime
            ).execute()
        }
    }

    fun fetchProjectData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        data: String,
        dataType: ThirdPartyAgentSqlQueueType
    ): List<ThirdPartyAgentQueueSqlData> {
        with(TDispatchThirdpartyAgentQueue.T_DISPATCH_THIRDPARTY_AGENT_QUEUE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(DATA.eq(data))
                .and(DATA_TYPE.eq(dataType.name))
                .orderBy(CREATED_TIME.asc())
                .fetch(queueDataMapper)
        }
    }

    fun fetchProjectDataCount(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        data: String,
        dataType: ThirdPartyAgentSqlQueueType
    ): Long {
        with(TDispatchThirdpartyAgentQueue.T_DISPATCH_THIRDPARTY_AGENT_QUEUE) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(DATA.eq(data))
                .and(DATA_TYPE.eq(dataType.name))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun addRetryTimeByIds(
        dslContext: DSLContext,
        recordIds: Set<Long>
    ) {
        with(TDispatchThirdpartyAgentQueue.T_DISPATCH_THIRDPARTY_AGENT_QUEUE) {
            dslContext.update(this)
                .set(RETRY_TIME, RETRY_TIME.plus(1))
                .where(ID.`in`(recordIds))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ) {
        with(TDispatchThirdpartyAgentQueue.T_DISPATCH_THIRDPARTY_AGENT_QUEUE) {
            dslContext.deleteFrom(this).where(ID.eq(id)).execute()
        }
    }

    fun deleteByIds(
        dslContext: DSLContext,
        recordIds: Set<Long>
    ) {
        with(TDispatchThirdpartyAgentQueue.T_DISPATCH_THIRDPARTY_AGENT_QUEUE) {
            dslContext.deleteFrom(this).where(ID.`in`(recordIds)).execute()
        }
    }

    fun deleteByBuild(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?
    ) {
        with(TDispatchThirdpartyAgentQueue.T_DISPATCH_THIRDPARTY_AGENT_QUEUE) {
            val dsl = dslContext.deleteFrom(this).where(BUILD_ID.eq(buildId))
            if (!vmSeqId.isNullOrBlank()) {
                dsl.and(VM_SEQ_ID.eq(vmSeqId))
            }
            dsl.execute()
        }
    }

    companion object {
        val queueDataMapper = ThirdPartyAgentDispatchDataMapper()
    }
}

class ThirdPartyAgentDispatchDataMapper :
    RecordMapper<TDispatchThirdpartyAgentQueueRecord, ThirdPartyAgentQueueSqlData> {
    override fun map(record: TDispatchThirdpartyAgentQueueRecord?): ThirdPartyAgentQueueSqlData? {
        return record?.let {
            ThirdPartyAgentQueueSqlData(
                recordId = it.id,
                data = ThirdPartyAgentDispatchData(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    buildId = it.buildId,
                    vmSeqId = it.vmSeqId,
                    infoData = JsonUtil.to(
                        it.info.data(),
                        object : TypeReference<ThirdPartyAgentDispatchDataSqlJson>() {}
                    )
                ),
                createTime = it.createdTime,
                retryTime = it.retryTime
            )
        }
    }
}

data class ThirdPartyAgentQueueSqlData(
    val recordId: Long,
    val data: ThirdPartyAgentDispatchData,
    val createTime: LocalDateTime,
    val retryTime: Int
)
