package com.tencent.devops.dispatch.dao

import com.tencent.devops.dispatch.pojo.DockerDevCluster
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerDevCluster
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerDevClusterRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerDevClusterDao {

    fun create(
        dslContext: DSLContext,
        dockerDevCluster: DockerDevCluster,
        userId: String
    ) {
        val now = LocalDateTime.now()
        with(
            TDispatchPipelineDockerDevCluster.T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER
        ) {
            dslContext.insertInto(
                this,
                CLUSTER_ID,
                CLUSTER_NAME,
                ENABLE,
                CREATE_USER,
                CREATE_TIME,
                UPDATE_USER,
                UPDATE_TIME
            ).values(
                dockerDevCluster.clusterId,
                dockerDevCluster.clusterName,
                dockerDevCluster.enable,
                userId,
                now,
                userId,
                now
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        dockerDevCluster: DockerDevCluster,
        userId: String
    ) {
        with(
            TDispatchPipelineDockerDevCluster.T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER
        ) {
            dslContext.update(this)
                .set(CLUSTER_NAME, dockerDevCluster.clusterName)
                .set(ENABLE, dockerDevCluster.enable).set(UPDATE_USER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(CLUSTER_ID.eq(dockerDevCluster.clusterId)).execute()
        }
    }

    fun getClusterById(
        dslContext: DSLContext,
        clusterId: String
    ): TDispatchPipelineDockerDevClusterRecord? {
        with(
            TDispatchPipelineDockerDevCluster.T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER
        ) {
            return dslContext.selectFrom(this).where(CLUSTER_ID.eq(clusterId))
                .fetchOne()
        }
    }

    fun list(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int,
        enable: Boolean? = null
    ): Result<TDispatchPipelineDockerDevClusterRecord> {
        with(
            TDispatchPipelineDockerDevCluster.T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER
        ) {
            val selectFrom = dslContext.selectFrom(this)
            if (enable != null) selectFrom.where(ENABLE.eq(enable))
            return selectFrom.orderBy(CREATE_TIME).limit(pageSize)
                .offset((page - 1) * pageSize).fetch()
        }
    }


    fun list(
        dslContext: DSLContext,
        enable: Boolean? = null
    ): Result<TDispatchPipelineDockerDevClusterRecord> {
        with(
            TDispatchPipelineDockerDevCluster.T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER
        ) {
            val selectFrom = dslContext.selectFrom(this)
            if (enable != null) selectFrom.where(ENABLE.eq(enable))
            return selectFrom.orderBy(CREATE_TIME).fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        enable: Boolean? = null
    ): Long {
        with(
            TDispatchPipelineDockerDevCluster.T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER
        ) {
            val selectFrom = dslContext.selectCount().from(this)
            if (enable != null) selectFrom.where(ENABLE.eq(enable))
            return selectFrom.fetchOne(0, Long::class.java)
        }
    }

    fun delete(dslContext: DSLContext, clusterId: String) {
        with(
            TDispatchPipelineDockerDevCluster.T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER
        ) {
            dslContext.deleteFrom(this).where(CLUSTER_ID.eq(clusterId))
                .execute()
        }
    }

    fun convert(
        record: TDispatchPipelineDockerDevClusterRecord
    ): DockerDevCluster {
        return DockerDevCluster(
            record.clusterId,
            record.clusterName,
            record.enable
        )
    }
}