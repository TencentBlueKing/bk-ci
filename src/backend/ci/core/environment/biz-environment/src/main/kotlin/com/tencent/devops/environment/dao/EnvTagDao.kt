package com.tencent.devops.environment.dao

import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.NodeTagAddOrDeleteTagItem
import com.tencent.devops.environment.pojo.NodeTagValue
import com.tencent.devops.model.environment.tables.TEnvTag
import com.tencent.devops.model.environment.tables.TNodeTagKey
import com.tencent.devops.model.environment.tables.TNodeTagValues
import com.tencent.devops.model.environment.tables.TNodeTags
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class EnvTagDao {
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

    /**
     * 查询多个 env 各自拥有的标签，按 tagKey 分组
     * @return <envId, <tagKeyId, Set<tagValueId>>>
     */
    fun fetchEnvTagKeyValues(
        dslContext: DSLContext,
        projectId: String,
        envIds: Set<Long>
    ): Map<Long, Map<Long, MutableSet<Long>>> {
        val envTagKeyValues = mutableMapOf<Long, MutableMap<Long, MutableSet<Long>>>()
        if (envIds.isEmpty()) {
            return envTagKeyValues
        }
        with(TEnvTag.T_ENV_TAG) {
            dslContext.select(ENV_ID, TAG_KEY_ID, TAG_VALUE_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ENV_ID.`in`(envIds))
                .fetch()
                .forEach {
                    envTagKeyValues.getOrPut(it[ENV_ID]) { mutableMapOf() }
                        .getOrPut(it[TAG_KEY_ID]) { mutableSetOf() }
                        .add(it[TAG_VALUE_ID])
                }
        }
        return envTagKeyValues
    }

    /**
     * 查询指定标签值下，每个节点拥有的标签值集合（仅保留这些标签值范围内的）
     * @return <nodeId, Set<tagValueId>>
     */
    fun fetchNodeTagValues(
        dslContext: DSLContext,
        projectId: String,
        tagValueIds: Set<Long>
    ): Map<Long, MutableSet<Long>> {
        val nodeTagValues = mutableMapOf<Long, MutableSet<Long>>()
        if (tagValueIds.isEmpty()) {
            return nodeTagValues
        }
        with(TNodeTags.T_NODE_TAGS) {
            dslContext.select(NODE_ID, TAG_VALUE_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TAG_VALUE_ID.`in`(tagValueIds))
                .fetch()
                .forEach {
                    nodeTagValues.getOrPut(it[NODE_ID]) { mutableSetOf() }.add(it[TAG_VALUE_ID])
                }
        }
        return nodeTagValues
    }

    fun deleteByEnvId(dslContext: DSLContext, envId: Long) {
        with(TEnvTag.T_ENV_TAG) {
            dslContext.deleteFrom(this).where(ENV_ID.eq(envId)).execute()
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
                .where(PROJECT_ID.eq(projectId))
                .and(ENV_ID.eq(envId))
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
        val tagValueIds = with(TNodeTags.T_NODE_TAGS) {
            dslContext.select(TAG_VALUE_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.eq(nodeId))
                .fetchSet(TAG_VALUE_ID)
        }
        return fetchTagEnvByTagValueIds(dslContext, projectId, tagValueIds.toList())
    }

    fun fetchTagEnvByTagValueIds(dslContext: DSLContext, projectId: String, tagValueIds: List<Long>): List<Long> {
        if (tagValueIds.isEmpty()) {
            return emptyList()
        }
        val nodeTagValues = tagValueIds.toSet()
        val candidateEnvIds = with(TEnvTag.T_ENV_TAG) {
            dslContext.selectDistinct(ENV_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TAG_VALUE_ID.`in`(nodeTagValues))
                .fetchSet(ENV_ID)
        }
        if (candidateEnvIds.isEmpty()) {
            return emptyList()
        }
        return fetchEnvTagKeyValues(dslContext, projectId, candidateEnvIds)
            .filterValues { keyValues ->
                keyValues.values.all { valuesOfKey -> valuesOfKey.any { it in nodeTagValues } }
            }
            .keys
            .toList()
    }
}
