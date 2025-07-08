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
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TAG_NOW_UPDATING
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TAG_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeTagDao
import com.tencent.devops.environment.dao.NodeTagKeyDao
import com.tencent.devops.environment.dao.NodeTagValueDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.NodeTagUpdateReq
import com.tencent.devops.environment.pojo.UpdateNodeTag
import com.tencent.devops.environment.pojo.enums.NodeType
import org.jooq.DSLContext
import org.jooq.impl.DSL
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
        if (nodeTagKeyDao.fetchNodeKey(dslContext, projectId, tagKey) != null) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_EXIST)
        }
        if (tagValues.isEmpty()) {
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
        return nodeTagDao.fetchTagAndNodeCount(dslContext, projectId)
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
            nodeTagDao.deleteNodesTags(ctx, projectId, data.nodeId)
            nodeTagDao.batchAddNodeTags(
                dslContext = ctx,
                projectId = projectId,
                nodeId = data.nodeId,
                valueAndKeyIds = data.tags.associate { it.tagValueId to it.tagKeyId }
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
    fun fetchNodeTags(projectId: String, nodeId: Long): List<NodeTag>? {
        return nodeTagDao.fetchNodesTags(dslContext, projectId, setOf(nodeId)).values.firstOrNull()
    }

    companion object {
        private fun genUpdateNodeTagLockKey(projectId: String, tagKeyId: Long) =
            "environment.nodetag.update:$projectId:$tagKeyId"
    }
}