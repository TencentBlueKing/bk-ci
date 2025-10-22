package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TNodeTagValues
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class NodeTagValueDao {
    fun deleteTagValue(dslContext: DSLContext, projectId: String, tagValueIds: Set<Long>) {
        with(TNodeTagValues.T_NODE_TAG_VALUES) {
            dslContext.deleteFrom(this).where(ID.`in`(tagValueIds)).and(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun updateNodeTagValue(dslContext: DSLContext, projectId: String, tagValueId: Long, valueName: String) {
        with(TNodeTagValues.T_NODE_TAG_VALUES) {
            dslContext.update(this).set(VALUE_NAME, valueName).where(ID.eq(tagValueId)).and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun batchCreateTagValue(dslContext: DSLContext, projectId: String, tagKeyId: Long, tagValues: Set<String>) {
        if (tagValues.isEmpty()) {
            return
        }
        with(TNodeTagValues.T_NODE_TAG_VALUES) {
            val records = tagValues.map { v ->
                dslContext.newRecord(this).apply {
                    this.projectId = projectId
                    this.tagKeyId = tagKeyId
                    this.valueName = v
                }
            }
            dslContext.batchInsert(records).execute()
        }
    }
}