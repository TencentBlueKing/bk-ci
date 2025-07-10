package com.tencent.devops.environment.dao

import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.NodeTagValue
import com.tencent.devops.model.environment.tables.TNodeTagInternalKey
import com.tencent.devops.model.environment.tables.TNodeTagInternalValues
import com.tencent.devops.model.environment.tables.TNodeTagKey
import com.tencent.devops.model.environment.tables.TNodeTagValues
import com.tencent.devops.model.environment.tables.TNodeTags
import com.tencent.devops.model.environment.tables.records.TNodeTagsRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

// 标签名和值联合查询相关
@Repository
class NodeTagDao {
    fun createTag(
        dslContext: DSLContext,
        projectId: String,
        tagName: String,
        tagValue: Set<String>,
        allowMulVal: Boolean
    ) {
        dslContext.transaction { config ->
            val ctx = DSL.using(config)
            // 新增标签名
            val tagKeyId = ctx.insertInto(
                TNodeTagKey.T_NODE_TAG_KEY,
                TNodeTagKey.T_NODE_TAG_KEY.PROJECT_ID,
                TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME,
                TNodeTagKey.T_NODE_TAG_KEY.ALLOW_MUL_VALUES
            ).values(
                projectId,
                tagName,
                allowMulVal
            ).returning().fetchOne()!!.id
            // 新增标签值
            val valueInserts = tagValue.map { v ->
                ctx.insertInto(
                    TNodeTagValues.T_NODE_TAG_VALUES,
                    TNodeTagValues.T_NODE_TAG_VALUES.PROJECT_ID,
                    TNodeTagValues.T_NODE_TAG_VALUES.TAG_KEY_ID,
                    TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME
                ).values(
                    projectId,
                    tagKeyId,
                    v
                )
            }
            ctx.batch(valueInserts).execute()
        }
    }

    // 根据标签名删除标签
    fun deleteTag(dslContext: DSLContext, projectId: String, tagKeyId: Long) {
        dslContext.transaction { config ->
            val ctx = DSL.using(config)
            ctx.deleteFrom(TNodeTagValues.T_NODE_TAG_VALUES)
                .where(TNodeTagValues.T_NODE_TAG_VALUES.TAG_KEY_ID.eq(tagKeyId))
                .and(TNodeTagValues.T_NODE_TAG_VALUES.PROJECT_ID.eq(projectId))
                .execute()
            ctx.deleteFrom(TNodeTagKey.T_NODE_TAG_KEY)
                .where(TNodeTagKey.T_NODE_TAG_KEY.ID.eq(tagKeyId))
                .and(TNodeTagKey.T_NODE_TAG_KEY.PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    // 获取所有标签和所有的节点数量
    fun fetchTagAndNodeCount(dslContext: DSLContext, projectId: String): List<NodeTag> {
        val resM = mutableMapOf<Long, NodeTag>()
        dslContext.select(
            TNodeTagKey.T_NODE_TAG_KEY.ID.`as`("KEY_ID"),
            TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME,
            TNodeTagKey.T_NODE_TAG_KEY.ALLOW_MUL_VALUES,
            TNodeTagValues.T_NODE_TAG_VALUES.ID.`as`("VALUE_ID"),
            TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME,
            DSL.count(TNodeTags.T_NODE_TAGS.NODE_ID).`as`("COUNT")
        ).from(TNodeTagKey.T_NODE_TAG_KEY)
            .leftJoin(TNodeTagValues.T_NODE_TAG_VALUES)
            .on(TNodeTagKey.T_NODE_TAG_KEY.ID.eq(TNodeTagValues.T_NODE_TAG_VALUES.TAG_KEY_ID))
            .leftJoin(TNodeTags.T_NODE_TAGS)
            .on(TNodeTagValues.T_NODE_TAG_VALUES.ID.eq(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID))
            .where(TNodeTagKey.T_NODE_TAG_KEY.PROJECT_ID.eq(projectId))
            .groupBy(
                TNodeTagKey.T_NODE_TAG_KEY.ID,
                TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME,
                TNodeTagKey.T_NODE_TAG_KEY.ALLOW_MUL_VALUES,
                TNodeTagValues.T_NODE_TAG_VALUES.ID,
                TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME
            )
            .fetch().forEach { tag ->
                genNodeTag(
                    resM = resM,
                    keyId = (tag["KEY_ID"] as Long?) ?: return@forEach,
                    keyName = tag[TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME],
                    allowMulVal = tag[TNodeTagKey.T_NODE_TAG_KEY.ALLOW_MUL_VALUES],
                    valueId = (tag["VALUE_ID"] as Long?) ?: return@forEach,
                    valueName = tag[TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME],
                    nodeCount = tag["COUNT"] as Int?,
                    internal = false
                )
            }
        return resM.values.toList()
    }

    // 获取所有内部标签和所有的节点数量
    fun fetchInternalTagAndNodeCount(dslContext: DSLContext, projectId: String): List<NodeTag> {
        val resM = mutableMapOf<Long, NodeTag>()
        dslContext.select(
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ID.`as`("KEY_ID"),
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.KEY_NAME,
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ALLOW_MUL_VALUES,
            TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.ID.`as`("VALUE_ID"),
            TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.VALUE_NAME,
            DSL.count(TNodeTags.T_NODE_TAGS.NODE_ID).`as`("COUNT")
        ).from(TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY)
            .leftJoin(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES)
            .on(TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ID.eq(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.TAG_KEY_ID))
            .leftJoin(TNodeTags.T_NODE_TAGS)
            .on(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.ID.eq(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID))
            .where(TNodeTags.T_NODE_TAGS.PROJECT_ID.eq(projectId))
            .groupBy(
                TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ID,
                TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.KEY_NAME,
                TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ALLOW_MUL_VALUES,
                TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.ID,
                TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.VALUE_NAME
            )
            .fetch().forEach { tag ->
                genNodeTag(
                    resM = resM,
                    keyId = (tag["KEY_ID"] as Long?) ?: return@forEach,
                    keyName = tag[TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.KEY_NAME],
                    allowMulVal = tag[TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ALLOW_MUL_VALUES],
                    valueId = (tag["VALUE_ID"] as Long?) ?: return@forEach,
                    valueName = tag[TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.VALUE_NAME],
                    nodeCount = tag["COUNT"] as Int?,
                    internal = true
                )
            }
        return resM.values.toList()
    }

    private fun genNodeTag(
        resM: MutableMap<Long, NodeTag>,
        keyId: Long,
        keyName: String,
        allowMulVal: Boolean,
        valueId: Long,
        valueName: String,
        nodeCount: Int?,
        internal: Boolean
    ) {
        val id = keyId
        resM.putIfAbsent(
            id, NodeTag(
                tagKeyId = id,
                tagKeyName = keyName,
                tagAllowMulValue = allowMulVal,
                canUpdate = !internal && (nodeCount ?: 0) == 0,
                tagValues = mutableListOf(
                    NodeTagValue(
                        tagValueId = valueId,
                        tagValueName = valueName,
                        nodeCount = nodeCount,
                        canUpdate = !internal && (nodeCount ?: 0) == 0
                    )
                )
            )
        )?.tagValues?.add(
            NodeTagValue(
                tagValueId = valueId,
                tagValueName = valueName,
                nodeCount = nodeCount,
                canUpdate = !internal && (nodeCount ?: 0) == 0
            )
        )
    }

    // 查询节点有哪些标签
    fun fetchNodesTags(dslContext: DSLContext, projectId: String, nodeIds: Set<Long>): Map<Long, List<NodeTag>> {
        val resM = mutableMapOf<Long, MutableMap<Long, NodeTag>>()
        dslContext.select(
            TNodeTags.T_NODE_TAGS.NODE_ID,
            TNodeTagKey.T_NODE_TAG_KEY.ID.`as`("KEY_ID"),
            TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME,
            TNodeTagKey.T_NODE_TAG_KEY.ALLOW_MUL_VALUES,
            TNodeTagValues.T_NODE_TAG_VALUES.ID.`as`("VALUE_ID"),
            TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME
        ).from(TNodeTags.T_NODE_TAGS)
            .leftJoin(TNodeTagValues.T_NODE_TAG_VALUES)
            .on(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID.eq(TNodeTagValues.T_NODE_TAG_VALUES.ID))
            .leftJoin(TNodeTagKey.T_NODE_TAG_KEY)
            .on(TNodeTags.T_NODE_TAGS.TAG_KEY_ID.eq(TNodeTagKey.T_NODE_TAG_KEY.ID))
            .where(TNodeTags.T_NODE_TAGS.PROJECT_ID.eq(projectId))
            .and(TNodeTags.T_NODE_TAGS.NODE_ID.`in`(nodeIds))
            .fetch()
            .forEach { nodeTag ->
                val nodeId = nodeTag[TNodeTags.T_NODE_TAGS.NODE_ID]
                if (!resM.containsKey(nodeId)) {
                    resM[nodeId] = mutableMapOf()
                }
                genNodeTag(
                    resM = resM[nodeId] ?: return@forEach,
                    keyId = (nodeTag["KEY_ID"] as Long?) ?: return@forEach,
                    keyName = nodeTag[TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME],
                    allowMulVal = nodeTag[TNodeTagKey.T_NODE_TAG_KEY.ALLOW_MUL_VALUES],
                    valueId = nodeTag["VALUE_ID"] as Long,
                    valueName = nodeTag[TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME],
                    nodeCount = null,
                    internal = false
                )
            }
        return resM.map { it.key to it.value.values.toList() }.toMap()
    }

    // 查询节点有哪些内部标签
    fun fetchNodesInternalTags(
        dslContext: DSLContext,
        projectId: String,
        nodeIds: Set<Long>
    ): Map<Long, List<NodeTag>> {
        val resM = mutableMapOf<Long, MutableMap<Long, NodeTag>>()
        dslContext.select(
            TNodeTags.T_NODE_TAGS.NODE_ID,
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ID.`as`("KEY_ID"),
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.KEY_NAME,
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ALLOW_MUL_VALUES,
            TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.ID.`as`("VALUE_ID"),
            TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.VALUE_NAME
        ).from(TNodeTags.T_NODE_TAGS)
            .leftJoin(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES)
            .on(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID.eq(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.ID))
            .leftJoin(TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY)
            .on(TNodeTags.T_NODE_TAGS.TAG_KEY_ID.eq(TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ID))
            .where(TNodeTags.T_NODE_TAGS.PROJECT_ID.eq(projectId))
            .and(TNodeTags.T_NODE_TAGS.NODE_ID.`in`(nodeIds))
            .fetch()
            .forEach { nodeTag ->
                val nodeId = nodeTag[TNodeTags.T_NODE_TAGS.NODE_ID]
                if (!resM.containsKey(nodeId)) {
                    resM[nodeId] = mutableMapOf()
                }
                genNodeTag(
                    resM = resM[nodeId] ?: return@forEach,
                    keyId = (nodeTag["KEY_ID"] as Long?) ?: return@forEach,
                    keyName = nodeTag[TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.KEY_NAME],
                    allowMulVal = nodeTag[TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ALLOW_MUL_VALUES],
                    valueId = nodeTag["VALUE_ID"] as Long,
                    valueName = nodeTag[TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.VALUE_NAME],
                    nodeCount = null,
                    internal = true
                )
            }
        return resM.map { it.key to it.value.values.toList() }.toMap()
    }

    fun deleteNodesTags(dslContext: DSLContext, projectId: String, nodeId: Long) {
        with(TNodeTags.T_NODE_TAGS) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(NODE_ID.eq(nodeId)).execute()
        }
    }

    fun batchAddNodeTags(
        dslContext: DSLContext,
        projectId: String,
        nodeId: Long,
        valueAndKeyIds: Map<Long, Long>
    ) {
        with(TNodeTags.T_NODE_TAGS) {
            val dsls = valueAndKeyIds.map { (v, k) ->
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    NODE_ID,
                    TAG_VALUE_ID,
                    TAG_KEY_ID
                ).values(
                    projectId,
                    nodeId,
                    v,
                    k
                )
            }
            dslContext.batch(dsls).execute()
        }
    }

    fun fetchNodeTagByKeyOrValue(
        dslContext: DSLContext,
        projectId: String,
        tagKeyId: Long?,
        tagValueIds: Set<Long>?
    ): List<TNodeTagsRecord>? {
        with(TNodeTags.T_NODE_TAGS) {
            if (tagKeyId == null && tagValueIds.isNullOrEmpty()) {
                return null
            }
            val dsl = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (tagValueIds != null) {
                dsl.and(TAG_VALUE_ID.`in`(tagValueIds))
            }
            if (tagKeyId != null) {
                dsl.and(TAG_KEY_ID.eq(tagKeyId))
            }
            return dsl.fetch()
        }
    }

    fun fetchTag(
        dslContext: DSLContext,
        projectId: String,
        tagKeyId: Long
    ): Pair<String, Map<Long, String>>? {
        val result = dslContext.select(
            TNodeTagKey.T_NODE_TAG_KEY.ID,
            TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME,
            TNodeTagValues.T_NODE_TAG_VALUES.ID,
            TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME
        ).from(TNodeTagKey.T_NODE_TAG_KEY)
            .leftJoin(TNodeTagValues.T_NODE_TAG_VALUES)
            .on(TNodeTagKey.T_NODE_TAG_KEY.ID.eq(TNodeTagValues.T_NODE_TAG_VALUES.TAG_KEY_ID))
            .where(TNodeTagKey.T_NODE_TAG_KEY.ID.eq(tagKeyId))
            .and(TNodeTagKey.T_NODE_TAG_KEY.PROJECT_ID.eq(projectId))
            .fetch()
        if (result.isEmpty()) {
            return null
        }
        val keyName = result.first()[TNodeTagKey.T_NODE_TAG_KEY.KEY_NAME]
        val valueMap =
            result.map { it[TNodeTagValues.T_NODE_TAG_VALUES.ID] to it[TNodeTagValues.T_NODE_TAG_VALUES.VALUE_NAME] }
                .toMap()
        return Pair(keyName, valueMap)
    }

    fun fetchInternalTag(dslContext: DSLContext): Map<String, NodeTag> {
        val resM = mutableMapOf<Long, NodeTag>()
        dslContext.select(
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ID.`as`("KEY_ID"),
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.KEY_NAME,
            TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ALLOW_MUL_VALUES,
            TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.ID.`as`("VALUE_ID"),
            TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.VALUE_NAME
        ).from(TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY)
            .leftJoin(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES)
            .on(TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ID.eq(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.TAG_KEY_ID))
            .leftJoin(TNodeTags.T_NODE_TAGS)
            .on(TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.ID.eq(TNodeTags.T_NODE_TAGS.TAG_VALUE_ID))
            .groupBy(
                TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ID,
                TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.KEY_NAME,
                TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ALLOW_MUL_VALUES,
                TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.ID,
                TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.VALUE_NAME
            )
            .fetch().forEach { tag ->
                genNodeTag(
                    resM = resM,
                    keyId = (tag["KEY_ID"] as Long?) ?: return@forEach,
                    keyName = tag[TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.KEY_NAME],
                    allowMulVal = tag[TNodeTagInternalKey.T_NODE_TAG_INTERNAL_KEY.ALLOW_MUL_VALUES],
                    valueId = (tag["VALUE_ID"] as Long?) ?: return@forEach,
                    valueName = tag[TNodeTagInternalValues.T_NODE_TAG_INTERNAL_VALUES.VALUE_NAME],
                    nodeCount = null,
                    internal = true
                )
            }
        return resM.values.associateBy { it.tagKeyName }
    }
}