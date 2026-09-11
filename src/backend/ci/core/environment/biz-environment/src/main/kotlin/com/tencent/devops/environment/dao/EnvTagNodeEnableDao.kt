package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TEnvTagNodeEnable
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class EnvTagNodeEnableDao {

    fun disableOrEnableNode(
        dslContext: DSLContext,
        projectId: String,
        envId: Long,
        nodeId: Long,
        enable: Boolean
    ) = with(TEnvTagNodeEnable.T_ENV_TAG_NODE_ENABLE) {
        dslContext.insertInto(
            this,
            ENABLE_NODE,
            ENV_ID,
            NODE_ID,
            PROJECT_ID
        ).values(
            enable,
            envId,
            nodeId,
            projectId
        ).onDuplicateKeyUpdate()
            .set(ENABLE_NODE, enable)
            .execute() == 1
    }

    fun listEnvNodeEnable(
        dslContext: DSLContext,
        projectId: String,
        envIds: Set<Long>,
    ) = with(TEnvTagNodeEnable.T_ENV_TAG_NODE_ENABLE) {
        dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).and(ENV_ID.`in`(envIds)).fetch()
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        envId: Long
    ) {
        with(TEnvTagNodeEnable.T_ENV_TAG_NODE_ENABLE) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(ENV_ID.eq(envId)).execute()
        }
    }
}