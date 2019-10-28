package com.tencent.devops.misc.dao

import com.tencent.devops.model.environment.tables.TEnvNode
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class EnvNodeDao {
    fun deleteByNodeIds(dslContext: DSLContext, nodeIds: List<Long>) {
        if (nodeIds.isEmpty()) {
            return
        }

        with(TEnvNode.T_ENV_NODE) {
            dslContext.deleteFrom(this)
                    .where(NODE_ID.`in`(nodeIds))
                    .execute()
        }
    }
}