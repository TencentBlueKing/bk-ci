package com.tencent.devops.dispatch.dao

import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.TDispatchThirdpartyAgentDockerDebug
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentDockerDebugRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ThirdPartyAgentDockerDebugDao {

    fun fetchOneQueueBuild(
        dslContext: DSLContext,
        agentId: String
    ): TDispatchThirdpartyAgentDockerDebugRecord? {
        with(TDispatchThirdpartyAgentDockerDebug.T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) {
            val select = dslContext.selectFrom(this.forceIndex("IDX_AGENTID_STATUS_UPDATE"))
                .where(AGENT_ID.eq(agentId))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
            return select
                .orderBy(UPDATED_TIME.asc())
                .limit(1)
                .fetchAny()
        }
    }
}
