package com.tencent.devops.environment.service

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TAG_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeTagDao
import com.tencent.devops.environment.dao.NodeTagKeyDao
import com.tencent.devops.environment.dao.NodeTagValueDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.UpdateNodeTag
import com.tencent.devops.environment.pojo.enums.NodeType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NodeTagService @Autowired constructor(
    private val dslContext: DSLContext,
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
        ActionAuditContext.current()
            .addInstanceInfo(tagKey, tagValues.joinToString(separator = ","), null, null)
        nodeTagDao.createTag(
            dslContext = dslContext,
            projectId = projectId,
            tagName = tagKey,
            tagValue = tagValues,
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
            tagValueId = tagValueId
        )
        if (!records.isNullOrEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_HAS_NODE)
        }
        ActionAuditContext.current()
            .addInstanceInfo(tagKey.toString(), tagValueId?.toString() ?: "", null, null)
        if (tagValueId != null) {
            nodeTagValueDao.deleteTagValue(dslContext = dslContext, projectId = projectId, tagValueId = tagValueId)
        } else {
            nodeTagDao.deleteTag(dslContext = dslContext, projectId = projectId, tagKeyId = tagKey)
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
    fun updateTag(userId: String, projectId: String, tagKey: Long, tagValueId: Long?, name: String) {
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
            tagValueId = tagValueId
        )
        if (!records.isNullOrEmpty()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_TAG_HAS_NODE)
        }
        ActionAuditContext.current()
            .addInstanceInfo("$tagKey|$tagValueId", name, null, null)
        if (tagValueId != null) {
            nodeTagValueDao.updateNodeTagValue(
                dslContext = dslContext,
                projectId = projectId,
                tagValueId = tagValueId,
                valueName = name
            )
        } else {
            nodeTagKeyDao.updateNodeTagKey(
                dslContext = dslContext,
                projectId = projectId,
                tagKeyId = tagKey,
                tagName = name,
                // 暂时先写死，等二期
                allowMulValue = null
            )
        }
    }

    // 查询这个节点有的标签
    fun fetchNodeTags(projectId: String, nodeId: Long): List<NodeTag>? {
        return nodeTagDao.fetchNodesTags(dslContext, projectId, setOf(nodeId)).values.firstOrNull()
    }
}