package com.tencent.devops.environment.dao

import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.NodeTagAddOrDeleteTagItem
import com.tencent.devops.environment.pojo.NodeTagValue
import com.tencent.devops.model.environment.tables.TEnvTag
import com.tencent.devops.model.environment.tables.TNodeTagKey
import com.tencent.devops.model.environment.tables.TNodeTagValues
import com.tencent.devops.model.environment.tables.TNodeTags
import com.tencent.devops.model.environment.tables.records.TEnvTagRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class EnvTagDao {
    fun fetchEnvTags(dslContext: DSLContext, envId: Long): List<TEnvTagRecord> {
        with(TEnvTag.T_ENV_TAG) {
            return dslContext.selectFrom(this).where(ENV_ID.eq(envId)).fetch()
        }
    }

    fun batchStoreEnvTag(
        dslContext: DSLContext,
        tags: List<NodeTagAddOrDeleteTagItem>,
        envId: Long,
        projectId: String
    ) {
        if (tags.isEmpty()) {
            return
        }
        dslContext.batch(
            tags.map {
                with(TEnvTag.T_ENV_TAG) {
                    dslContext.insertInto(
                        this,
                        ENV_ID,
                        TAG_KEY_ID,
                        TAG_VALUE_ID,
                        PROJECT_ID
                    ).values(
                        envId,
                        it.tagKeyId,
                        it.tagValueId,
                        projectId
                    )
                }
            }
        ).execute()
    }

    fun batchEnvTagNodeCount(
        dslContext: DSLContext,
        envIds: Set<Long>,
        projectId: String
    ): Map<Long, Int> {
        val resultMap = mutableMapOf<Long, Int>()
        if (envIds.isEmpty()) {
            return resultMap
        }
        with(TEnvTag.T_ENV_TAG) {
            dslContext.select(
                ENV_ID,
                DSL.count(TNodeTags.T_NODE_TAGS.NODE_ID).`as`("node_count")
            ).from(this)
                .leftJoin(TNodeTags.T_NODE_TAGS)
                .on(TAG_VALUE_ID.eq(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID))
                .where(ENV_ID.`in`(envIds))
                .and(PROJECT_ID.eq(projectId))
                .groupBy(TAG_VALUE_ID)
                .fetch()
                .forEach {
                    val envId = it[ENV_ID]
                    val nodeCount = it["node_count"] as Int
                    if (resultMap.containsKey(it[ENV_ID])) {
                        resultMap[envId] = (resultMap[envId] ?: 0) + nodeCount
                    } else {
                        resultMap[envId] = nodeCount
                    }
                }
        }
        return resultMap
    }

    fun batchEnvTagNode(
        dslContext: DSLContext,
        envIds: Set<Long>,
        projectId: String
    ): Map<Long, MutableList<Long>> {
        val resultMap = mutableMapOf<Long, MutableList<Long>>()
        if (envIds.isEmpty()) {
            return resultMap
        }
        with(TEnvTag.T_ENV_TAG) {
            dslContext.select(
                ENV_ID,
                TNodeTags.T_NODE_TAGS.NODE_ID
            ).from(this)
                .innerJoin(TNodeTags.T_NODE_TAGS)
                .on(TAG_VALUE_ID.eq(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID))
                .where(ENV_ID.`in`(envIds))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
                .forEach {
                    val envId = it[ENV_ID]
                    val nodeId = it[TNodeTags.T_NODE_TAGS.NODE_ID]
                    if (resultMap.containsKey(it[ENV_ID])) {
                        resultMap[envId]?.add(nodeId)
                    } else {
                        resultMap[envId] = mutableListOf(nodeId)
                    }
                }
        }
        return resultMap
    }

    fun deleteByEnvId(dslContext: DSLContext, envId: Long) {
        with(TEnvTag.T_ENV_TAG) {
            dslContext.deleteFrom(this).where(ENV_ID.eq(envId)).execute()
        }
    }

    fun deleteEnvTags(dslContext: DSLContext, projectId: String, envIds: Set<Long>) {
        with(TEnvTag.T_ENV_TAG) {
            dslContext.deleteFrom(this).where(ENV_ID.`in`(envIds)).and(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun batchAddEnvTags(
        dslContext: DSLContext,
        projectId: String,
        envAndValueAndKeyIds: Map<Long, Map<Long, Long>>
    ) {
        with(TEnvTag.T_ENV_TAG) {
            val records = envAndValueAndKeyIds.map { (envId, valueAndKeyIds) ->
                valueAndKeyIds.map { (valueId, keyId) ->
                    dslContext.newRecord(this).apply {
                        this.projectId = projectId
                        this.envId = envId
                        this.tagValueId = valueId
                        this.tagKeyId = keyId
                    }
                }
            }.flatten()
            dslContext.batchInsert(records).execute()
        }
    }

    fun fetchEnvTag(dslContext: DSLContext, projectId: String, envId: Long): List<NodeTag> {
        val resM = mutableMapOf<Long, NodeTag>()
        with(TEnvTag.T_ENV_TAG) {
            dslContext.select(
                TNodeTagKey.T_NODE_TAG_KEY.ID.`as`("KEY_ID"),
                TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME,
                TNodeTagKey.T_NODE_TAG_KEY.ALLOW_MUL_VALUES,
                TNodeTagValues.T_NODE_TAG_VALUES.ID.`as`("VALUE_ID"),
                TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME
            ).from(this)
                .leftJoin(TNodeTagKey.T_NODE_TAG_KEY)
                .on(TAG_KEY_ID.eq(TNodeTagKey.T_NODE_TAG_KEY.ID))
                .leftJoin(TNodeTagValues.T_NODE_TAG_VALUES)
                .on(TAG_VALUE_ID.eq(TNodeTagValues.T_NODE_TAG_VALUES.ID))
                .where(ENV_ID.eq(envId))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
                .forEach { tag ->
                    val keyId = (tag["KEY_ID"] as Long?) ?: return@forEach
                    val valueId = (tag["VALUE_ID"] as Long?) ?: return@forEach
                    val keyName = tag[TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME]
                    val allowMulVal = tag[TNodeTagKey.T_NODE_TAG_KEY.ALLOW_MUL_VALUES]
                    val valueName = tag[TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME]
                    val tagValue = NodeTagValue(
                        tagValueId = valueId,
                        tagValueName = valueName,
                        nodeCount = null,
                        canUpdate = null
                    )
                    if (resM.containsKey(keyId)) {
                        resM[keyId]?.tagValues?.add(tagValue)
                    } else {
                        resM[keyId] = NodeTag(
                            tagKeyId = keyId,
                            tagKeyName = keyName,
                            tagAllowMulValue = allowMulVal,
                            canUpdate = null,
                            tagValues = mutableListOf(tagValue)
                        )
                    }
                }
        }
        return resM.values.toList()
    }

    fun fetchTagEnvByNodeId(dslContext: DSLContext, projectId: String, nodeId: Long): List<Long> {
        with(TEnvTag.T_ENV_TAG) {
            return dslContext.select(ENV_ID).from(this)
                .leftJoin(TNodeTags.T_NODE_TAGS)
                .on(TAG_VALUE_ID.eq(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID))
                .where(TNodeTags.T_NODE_TAGS.NODE_ID.eq(nodeId))
                .and(TNodeTags.T_NODE_TAGS.PROJECT_ID.eq(projectId))
                .and(PROJECT_ID.eq(projectId))
                .fetch().map { it.value1() }
        }
    }
}