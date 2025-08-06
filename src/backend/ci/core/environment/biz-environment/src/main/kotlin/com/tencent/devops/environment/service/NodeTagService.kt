package com.tencent.devops.environment.service

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODES_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TAG_NOW_UPDATING
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TAG_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeTagDao
import com.tencent.devops.environment.dao.NodeTagKeyDao
import com.tencent.devops.environment.dao.NodeTagValueDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.NodeTagCanUpdateType
import com.tencent.devops.environment.pojo.NodeTagUpdateReq
import com.tencent.devops.environment.pojo.UpdateNodeTag
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NodeTagService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val nodeTagDao: NodeTagDao,
    private val nodeTagKeyDao: NodeTagKeyDao,
    private val nodeTagValueDao: NodeTagValueDao,
    private val nodeDao: NodeDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) {
    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_TAG_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE_TAG
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_TAG_CREATE_CONTENT
    )
    fun createTag(
        userId: String,
        projectId: String,
        tagKey: String,
        tagValues: List<String>,
        allowMulValue: Boolean?
    ) {
        if (!authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_TAG_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        val allInternalKeys = nodeTagKeyDao.fetchAllInternalKeys(dslContext).map { it.keyName }.toSet()
        if (nodeTagKeyDao.fetchNodeKey(dslContext, projectId, tagKey) != null || allInternalKeys.contains(tagKey)) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_EXIST, params = arrayOf(tagKey))
        }
        if (tagValues.isEmpty() || tagValues.any { it.isBlank() }) {
            throw ParamBlankException("tag value is empty")
        }
        val tagValuesSet = tagValues.toSet()
        ActionAuditContext.current()
            .addInstanceInfo(tagKey, tagValuesSet.joinToString(separator = ","), null, null)
        nodeTagDao.createTag(
            dslContext = dslContext,
            projectId = projectId,
            tagName = tagKey,
            tagValue = tagValuesSet,
            allowMulVal = allowMulValue ?: false
        )
    }

    fun fetchTagAndNodeCount(
        projectId: String
    ): List<NodeTag> {
        val tags = mutableListOf<NodeTag>().apply {
            addAll(nodeTagDao.fetchTagAndNode(dslContext, projectId))
            addAll(nodeTagDao.fetchInternalTag(dslContext).values)
        }
        val nodeTags = nodeTagDao.fetchNodeTag(dslContext, projectId)
        val nodeTagsCountMap = mutableMapOf<Long, MutableMap<Long, Int>>()
        nodeTags.forEach {
            val m = nodeTagsCountMap.putIfAbsent(it.tagKeyId, mutableMapOf(it.tagValueId to 1)) ?: return@forEach
            if (m.contains(it.tagValueId)) {
                m[it.tagValueId] = m[it.tagValueId]!! + 1
            } else {
                m[it.tagValueId] = 1
            }
        }
        tags.forEach { k ->
            var countFlag = true
            k.tagValues.forEach value@{ v ->
                v.nodeCount = nodeTagsCountMap[k.tagKeyId]?.get(v.tagValueId) ?: 0
                if (v.canUpdate != null) {
                    return@value
                }
                v.canUpdate = if ((v.nodeCount ?: 0) == 0) {
                    NodeTagCanUpdateType.TRUE
                } else {
                    countFlag = false
                    NodeTagCanUpdateType.FALSE
                }
            }
            if (k.canUpdate != null) {
                return@forEach
            }
            k.canUpdate = if (countFlag) {
                NodeTagCanUpdateType.TRUE
            } else {
                NodeTagCanUpdateType.FALSE
            }
        }
        return tags
    }

    // 更新节点标签，先删后添加
    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_TAG_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE_TAG
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_TAG_EDIT_CONTENT
    )
    fun addNodeTag(userId: String, projectId: String, data: UpdateNodeTag) {
        if (!environmentPermissionService.checkNodePermission(userId, projectId, data.nodeId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        // 只有第三方机器支持节点
        if (nodeDao.get(dslContext, projectId, data.nodeId)?.nodeType != NodeType.THIRDPARTY.name) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_ONLY_SUP_THIRD)
        }
        // 检查标签是否支持多个值同时添加
        val tagKeys = nodeTagKeyDao.fetchNodeKeyByIds(
            dslContext = dslContext,
            projectId = projectId,
            keyIds = data.tags.map { it.tagKeyId }.toSet()
        ).associate { it.id to Pair((it.allowMulValues ?: false), it.keyName) }
        val tagsMap = mutableMapOf<Long, MutableSet<Long>>()
        data.tags.forEach { tag ->
            tagsMap.putIfAbsent(tag.tagKeyId, mutableSetOf(tag.tagValueId))?.add(tag.tagValueId)
        }
        data.tags.forEach { tag ->
            if (tagKeys[tag.tagKeyId]?.first == false && (tagsMap[tag.tagKeyId]?.size ?: 0) > 1) {
                throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_NO_ALLOW_VALUES,
                    params = arrayOf(tagKeys[tag.tagKeyId]?.second ?: "")
                )
            }
        }
        ActionAuditContext.current()
            .addInstanceInfo(data.nodeId.toString(), JsonUtil.toJson(data.tags), null, null)
        dslContext.transaction { config ->
            val ctx = DSL.using(config)
            nodeTagDao.deleteNodesTags(ctx, projectId, setOf(data.nodeId))
            nodeTagDao.batchAddNodeTags(
                dslContext = ctx,
                projectId = projectId,
                nodeAndValueAndKeyIds = mapOf(data.nodeId to data.tags.associate { it.tagValueId to it.tagKeyId })
            )
        }
    }

    // 批量更新节点标签，先删后添加
    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_TAG_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE_TAG
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_TAG_EDIT_CONTENT
    )
    fun batchAddNodeTag(userId: String, projectId: String, data: List<UpdateNodeTag>) {
        val nodeIds = data.map { it.nodeId }.toSet()
        val nodes = nodeDao.listByIds(dslContext, projectId, nodeIds).associateBy { it.nodeId }
        // 只有第三方机器支持节点
        if (nodes.values.any { it.nodeType != NodeType.THIRDPARTY.name }) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_ONLY_SUP_THIRD)
        }
        // 校验权限
        val hasRbacPermissionNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.EDIT)
        val noPermissionNodes = nodeIds.filter { !hasRbacPermissionNodeIds.contains(it) }.toSet()
        if (noPermissionNodes.isNotEmpty()) {
            val names =
                nodes.filter { it.key in noPermissionNodes }.values.joinToString(separator = ",") { it.displayName }
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODES_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(names)
                )
            )
        }
        // 检查标签是否支持多个值同时添加
        val tagKeyIds = data.map { it.tags.map { tag -> tag.tagKeyId } }.flatten().toSet()
        val tagKeys = nodeTagKeyDao.fetchNodeKeyByIds(
            dslContext = dslContext,
            projectId = projectId,
            keyIds = tagKeyIds
        ).associate { it.id to Pair((it.allowMulValues ?: false), it.keyName) }
        data.forEach { nodeTag ->
            val tagsMap = mutableMapOf<Long, MutableSet<Long>>()
            nodeTag.tags.forEach { tag ->
                tagsMap.putIfAbsent(tag.tagKeyId, mutableSetOf(tag.tagValueId))?.add(tag.tagValueId)
            }
            nodeTag.tags.forEach { tag ->
                if (tagKeys[tag.tagKeyId]?.first == false && (tagsMap[tag.tagKeyId]?.size ?: 0) > 1) {
                    throw ErrorCodeException(
                        errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_NO_ALLOW_VALUES,
                        params = arrayOf(tagKeys[tag.tagKeyId]?.second ?: "")
                    )
                }
            }
        }
        ActionAuditContext.current()
            .addInstanceInfo(
                data.map { it.nodeId }.joinToString(separator = ","),
                JsonUtil.toJson(data),
                null,
                null
            )
        dslContext.transaction { config ->
            val ctx = DSL.using(config)
            nodeTagDao.deleteNodesTags(ctx, projectId, nodeIds)
            nodeTagDao.batchAddNodeTags(
                dslContext = ctx,
                projectId = projectId,
                nodeAndValueAndKeyIds = data.associate {
                    it.nodeId to it.tags.associate { tag -> tag.tagValueId to tag.tagKeyId }
                }
            )
        }
    }

    // 删除标签
    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_TAG_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE_TAG
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_TAG_DELETE_CONTENT
    )
    fun deleteTag(userId: String, projectId: String, tagKey: Long, tagValueId: Long?) {
        if (!authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_TAG_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        // 内部标签不能删除
        if (tagKey < 0 || ((tagValueId ?: 0) < 0)) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_INTERNAL_NOT_EDIT)
        }
        val records = nodeTagDao.fetchNodeTagByKeyOrValue(
            dslContext = dslContext,
            projectId = projectId,
            // key 传错误的可能导致未校验成功
            tagKeyId = if (tagValueId != null) {
                null
            } else {
                tagKey
            },
            tagValueIds = if (tagValueId == null) {
                null
            } else {
                setOf(tagValueId)
            }
        )
        if (!records.isNullOrEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_HAS_NODE)
        }
        val lock = RedisLock(redisOperation, genUpdateNodeTagLockKey(projectId, tagKey), 5)
        if (!lock.tryLock()) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_TAG_NOW_UPDATING,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        try {
            ActionAuditContext.current()
                .addInstanceInfo(tagKey.toString(), tagValueId?.toString() ?: "", null, null)
            if (tagValueId != null) {
                nodeTagValueDao.deleteTagValue(
                    dslContext = dslContext,
                    projectId = projectId,
                    tagValueIds = setOf(tagValueId)
                )
            } else {
                nodeTagDao.deleteTag(dslContext = dslContext, projectId = projectId, tagKeyId = tagKey)
            }
        } finally {
            lock.unlock()
        }
    }

    // 修改标签
    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_TAG_UPDATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE_TAG
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_TAG_UPDATE_CONTENT
    )
    fun updateTag(userId: String, projectId: String, data: NodeTagUpdateReq) {
        if (!authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_TAG_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        // 内部标签不能修改
        if (data.tagKeyId < 0 || data.tagValues?.any { (it.tagValueId ?: 0) < 0 } == true) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_INTERNAL_NOT_EDIT)
        }
        val lock = RedisLock(redisOperation, genUpdateNodeTagLockKey(projectId, data.tagKeyId), 5)

        // 获取老的节点标签
        val tags = nodeTagDao.fetchTag(dslContext, projectId, data.tagKeyId) ?: run {
            // 为空说明之前的标签已经被删除了，直接修改即可，理论上不会出现，先返回
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_TAG_NOW_UPDATING,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        val oldTagKeyName = tags.first
        val oldTagValues = tags.second
        if (!lock.tryLock()) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_TAG_NOW_UPDATING,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        try {
            ActionAuditContext.current()
                .addInstanceInfo("${data.tagKeyId}", data.tagValues?.joinToString(separator = ","), null, null)
            dslContext.transaction { config ->
                val dslCtx = DSL.using(config)
                // 检验是否修改了 key
                if (data.tagKeyName != oldTagKeyName) {
                    checkUsedTagNode(projectId, data.tagKeyId, null)
                    nodeTagKeyDao.updateNodeTagKey(
                        dslContext = dslCtx,
                        projectId = projectId,
                        tagKeyId = data.tagKeyId,
                        tagName = data.tagKeyName,
                        // 暂时先写死，等二期
                        allowMulValue = null
                    )
                }
                // 检验是否修改了tag值
                val newTagValueIds =
                    data.tagValues?.filter { it.tagValueId != null }?.map { it.tagValueId!! }?.toSet() ?: emptySet()
                val deletedTagValueIds = oldTagValues.keys.subtract(newTagValueIds)
                val updateValues = data.tagValues!!.filter {
                    it.tagValueId != null && oldTagValues[it.tagValueId] != it.tagValueName
                }.associate { it.tagValueId!! to it.tagValueName }
                val deletedAndUpdateTagValueIds = mutableSetOf<Long>().apply {
                    addAll(deletedTagValueIds)
                    addAll(updateValues.keys)
                }
                // 校验被删除的和被修改的是否有机器使用
                if (deletedAndUpdateTagValueIds.isNotEmpty()) {
                    checkUsedTagNode(projectId, null, deletedAndUpdateTagValueIds)
                }
                // 删除被删除的values
                nodeTagValueDao.deleteTagValue(dslCtx, projectId, deletedTagValueIds)
                // 修改和新增values
                val createValue = data.tagValues!!.filter { it.tagValueId == null }.map { it.tagValueName }.toSet()
                nodeTagValueDao.batchCreateTagValue(dslCtx, projectId, data.tagKeyId, createValue)
                updateValues.forEach {
                    nodeTagValueDao.updateNodeTagValue(dslCtx, projectId, it.key, it.value)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    private fun checkUsedTagNode(projectId: String, tagKey: Long?, tagValueIds: Set<Long>?) {
        if (tagKey == null && tagValueIds.isNullOrEmpty()) {
            return
        }
        val records = nodeTagDao.fetchNodeTagByKeyOrValue(
            dslContext = dslContext,
            projectId = projectId,
            tagKeyId = tagKey,
            tagValueIds = tagValueIds
        )
        if (!records.isNullOrEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_HAS_NODE)
        }
    }

    // 查询这个节点有的标签
    fun fetchNodeTags(projectId: String, nodeIds: Set<Long>): Map<Long, List<NodeTag>> {
        val res = mutableMapOf<Long, MutableList<NodeTag>>().apply {
            putAll(nodeTagDao.fetchNodesTags(dslContext, projectId, nodeIds))
        }
        nodeTagDao.fetchNodesInternalTags(dslContext, projectId, nodeIds).forEach { (k, v) ->
            res.putIfAbsent(k, v)?.addAll(v)
        }
        return res
    }

    // 为节点添加内置标签
    fun editInternalTags(projectId: String, agentId: Long) {
        logger.info("editInternalTags|add project $projectId|$agentId")
        val tags = nodeTagDao.fetchInternalTag(dslContext)
        val tpa = thirdPartyAgentDao.getAgentByProject(dslContext, agentId, projectId) ?: return
        editNodeInternalTags(tags, tpa, projectId)
    }

    // OP使用，刷新内置标签数据
    fun refreshInternalNodeTags(projectId: String?) {
        val tags = nodeTagDao.fetchInternalTag(dslContext)
        val projects = if (projectId != null) {
            setOf(projectId)
        } else {
            nodeDao.fetchNodeProject(dslContext)
        }
        projects.forEach { projectId ->
            logger.info("refreshInternalNodeTags|add project $projectId")
            thirdPartyAgentDao.fetchByProjectId(dslContext, projectId).forEach { tpa ->
                editNodeInternalTags(tags, tpa, projectId)
            }
        }
    }

    private fun editNodeInternalTags(
        tags: Map<String, NodeTag>,
        tpa: TEnvironmentThirdpartyAgentRecord,
        projectId: String
    ) {
        val osKeyId = tags["os"]?.tagKeyId
        val osValueId = tags["os"]?.tagValues?.firstOrNull { it.tagValueName == tpa.os.lowercase() }?.tagValueId
        val archKeyId = tags["arch"]?.tagKeyId
        val archValueId =
            tags["arch"]?.tagValues?.firstOrNull { it.tagValueName == getAgentProperties(tpa)?.arch }?.tagValueId
        val tagMap = mutableMapOf<Long, Long>()
        if (osKeyId != null && osValueId != null) {
            tagMap[osValueId] = osKeyId
        }
        if (archKeyId != null && archValueId != null) {
            tagMap[archValueId] = archKeyId
        }
        logger.info("editNodeInternalTags|batchAddNodeTags $projectId|${tpa.nodeId}|$tagMap")
        try {
            nodeTagDao.batchAddNodeTags(dslContext, projectId, mapOf(tpa.nodeId to tagMap))
        } catch (e: Exception) {
            logger.warn("editNodeInternalTags|batchAddNodeTags $projectId|${tpa.nodeId}|$tagMap error", e)
        }
    }

    private fun getAgentProperties(agentRecord: TEnvironmentThirdpartyAgentRecord): AgentProps? {
        if (agentRecord.agentProps.isNullOrBlank()) {
            return null
        }

        return try {
            JsonUtil.to(agentRecord.agentProps, AgentProps::class.java)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private fun genUpdateNodeTagLockKey(projectId: String, tagKeyId: Long) =
            "environment.nodetag.update:$projectId:$tagKeyId"

        private val logger = LoggerFactory.getLogger(NodeTagService::class.java)
    }
}