package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TNodeTagInternalKey
import com.tencent.devops.model.environment.tables.TNodeTagKey
import com.tencent.devops.model.environment.tables.records.TNodeTagInternalKeyRecord
import com.tencent.devops.model.environment.tables.records.TNodeTagKeyRecord
import org.jooq.DSLContext
import org.jooq.UpdateSetMoreStep
import org.springframework.stereotype.Repository

@Repository
class NodeTagKeyDao {
    fun updateNodeTagKey(
        dslContext: DSLContext,
        projectId: String,
        tagKeyId: Long,
        tagName: String?,
        allowMulValue: Boolean?
    ) {
        if (tagName.isNullOrBlank() && allowMulValue == null) {
            return
        }
        with(TNodeTagKey.T_NODE_TAG_KEY) {
            val dsl = dslContext.update(this)
            if (!tagName.isNullOrBlank()) {
                dsl.set(KEY_NAME, tagName)
            }
            if (allowMulValue != null) {
                dsl.set(ALLOW_MUL_VALUES, allowMulValue)
            }
            (dsl as UpdateSetMoreStep<*>).where(ID.eq(tagKeyId)).and(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun fetchNodeKey(dslContext: DSLContext, projectId: String, keyName: String): TNodeTagKeyRecord? {
        with(TNodeTagKey.T_NODE_TAG_KEY) {
            return dslContext.selectFrom(this).where(KEY_NAME.eq(keyName)).and(PROJECT_ID.eq(projectId)).fetchAny()
        }
    }

    fun fetchNodeKeyByIds(dslContext: DSLContext, projectId: String, keyIds: Set<Long>): List<TNodeTagKeyRecord> {
        with(TNodeTagKey.T_NODE_TAG_KEY) {
            return dslContext.selectFrom(this).where(ID.`in`(keyIds)).and(PROJECT_ID.eq(projectId)).fetch()
        }
    }

    fun fetchAllInternalKeys(dslContext: DSLContext): List<TNodeTagInternalKeyRecord> {
        with(TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY) {
            return dslContext.selectFrom(this).fetch()
        }
    }
}