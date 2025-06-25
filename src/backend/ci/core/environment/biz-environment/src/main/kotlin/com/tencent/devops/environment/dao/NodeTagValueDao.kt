package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TNodeTagValues
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class NodeTagValueDao {
    fun deleteTagValue(dslContext: DSLContext, projectId: String, tagValueId: Long) {
        with(TNodeTagValues.T_NODE_TAG_VALUES) {
            dslContext.deleteFrom(this).where(ID.eq(tagValueId)).and(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun updateNodeTagValue(dslContext: DSLContext, projectId: String, tagValueId: Long, valueName: String) {
        with(TNodeTagValues.T_NODE_TAG_VALUES) {
            dslContext.update(this).set(VALUE_NAME, valueName).where(ID.eq(tagValueId)).and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }
}