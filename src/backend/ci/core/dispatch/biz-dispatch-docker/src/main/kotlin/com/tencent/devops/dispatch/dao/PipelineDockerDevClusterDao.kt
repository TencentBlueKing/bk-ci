package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerDevCluster
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerDevClusterRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineDockerDevClusterDao {

    fun getClusterById(dslContext: DSLContext, clusterId: String): TDispatchPipelineDockerDevClusterRecord? {
        with(TDispatchPipelineDockerDevCluster.T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER) {
            return dslContext.selectFrom(this).where(CLUSTER_ID.eq(clusterId)).fetchOne()
        }
    }
}