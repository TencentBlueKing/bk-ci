package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TNodeTagInternalKey
import com.tencent.devops.model.environment.tables.TNodeTagInternalValues
import com.tencent.devops.model.environment.tables.TNodeTagKey
import com.tencent.devops.model.environment.tables.TNodeTagValues
import com.tencent.devops.model.environment.tables.records.TNodeTagInternalKeyRecord
import com.tencent.devops.model.environment.tables.records.TNodeTagInternalValuesRecord
import com.tencent.devops.model.environment.tables.records.TNodeTagKeyRecord
import com.tencent.devops.model.environment.tables.records.TNodeTagValuesRecord
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

    fun fetchNodeKeyValueByIds(
        dslContext: DSLContext,
        projectId: String,
        valueIds: Set<Long>
    ): List<TNodeTagValuesRecord> {
        with(TNodeTagValues.T_NODE_TAG_VALUES) {
            return dslContext.selectFrom(this).where(ID.`in`(valueIds)).and(PROJECT_ID.eq(projectId)).fetch()
        }
    }

    fun fetchAllInternalValues(dslContext: DSLContext): List<TNodeTagInternalValuesRecord> {
        with(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES) {
            return dslContext.selectFrom(this).fetch()
        }
    }
}