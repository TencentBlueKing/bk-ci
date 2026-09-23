/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.service

import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_ENV_NO_VIEW_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TAG_NO_ALLOW_VALUES
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_TAG_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.EnvTagDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.NodeTagKeyDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeTagAddOrDeleteTagItem
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.EnvNodeType
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.envOperate.EnvOperateContent
import com.tencent.devops.environment.pojo.envOperate.EnvOperateName
import com.tencent.devops.environment.pojo.envOperate.EnvOperateOrigin
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 动态环境（EnvNodeType.TAG）标签相关的业务逻辑：
 * 1. 环境标签规则的维护（关联/清空标签）
 * 2. 标签规则 -> 节点 的匹配（动态环境列节点、统计节点数、创建前预览）
 *
 * 匹配规则统一为：同一 tagKey 内的多个值为 OR，不同 tagKey 之间为 AND。
 * 所有标签匹配都必须经过 [batchMatchNodesByTags]，保证规则一致。
 */
@Service
class EnvTagService @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val envTagDao: EnvTagDao,
    private val nodeTagKeyDao: NodeTagKeyDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val nodeService: NodeService,
    private val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val envOperateLogService: EnvOperateLogService
) {

    // ----------------------------------------------------------------------------------------------------------------
    // 环境标签规则维护
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * 更新环境关联的标签规则。tags 为空时清空规则。
     * 会将环境的节点类型切换为 TAG，并清理静态节点关联。
     * 调用方需已完成环境 EDIT 权限校验。
     */
    fun updateEnvTags(
        userId: String,
        projectId: String,
        envHashId: String,
        envRecord: TEnvRecord,
        tags: List<NodeTagAddOrDeleteTagItem>,
        envOperateOrigin: EnvOperateOrigin
    ) {
        val envId = envRecord.envId
        if (!authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_TAG_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        ActionAuditContext.current()
            .addInstanceInfo(envHashId, JsonUtil.toJson(tags), null, null)
        // 清空
        if (tags.isEmpty()) {
            dslContext.transaction { config ->
                val ctx = DSL.using(config)
                switchToTagEnv(ctx, envRecord)
                envTagDao.deleteByEnvId(ctx, envId)
            }
            return
        }

        checkTagAllowMultiValues(projectId, tags)

        dslContext.transaction { config ->
            val ctx = DSL.using(config)
            switchToTagEnv(ctx, envRecord)
            envTagDao.deleteByEnvId(ctx, envId)
            envTagDao.batchAddEnvTags(
                dslContext = ctx,
                projectId = projectId,
                envAndValueAndKeyIds = mapOf(envId to tags.associate { it.tagValueId to it.tagKeyId })
            )
        }

        envOperateLogService.addOperateLog(
            projectId = projectId,
            envId = envId,
            operateOrigin = envOperateOrigin,
            operateName = EnvOperateName.UPDATE_ENV_LINK_TAG,
            operateContent = EnvOperateContent(
                content = null,
                resourceCount = tags.size
            ),
            operator = userId
        )
    }

    /**
     * 类型转换需要清空之前类型的记录：静态环境切换为动态环境时删除静态节点关联
     */
    private fun switchToTagEnv(ctx: DSLContext, envRecord: TEnvRecord) {
        if (envRecord.envNodeType == EnvNodeType.NODE.name) {
            envNodeDao.deleteByEnvId(ctx, envRecord.envId)
            envDao.updateEnvNodeType(ctx, envRecord.envId, EnvNodeType.TAG)
        }
    }

    /**
     * 校验不允许多值的 tagKey 是否被选择了多个值
     */
    private fun checkTagAllowMultiValues(projectId: String, tags: List<NodeTagAddOrDeleteTagItem>) {
        val tagKeys = nodeTagKeyDao.fetchNodeKeyByIds(
            dslContext = dslContext,
            projectId = projectId,
            keyIds = tags.map { it.tagKeyId }.toSet()
        ).associate { it.id to Pair((it.allowMulValues ?: false), it.keyName) }
        val tagKeyValues = groupTagKeyValues(tags)
        tags.forEach { tag ->
            if (tagKeys[tag.tagKeyId]?.first == false && (tagKeyValues[tag.tagKeyId]?.size ?: 0) > 1) {
                throw ErrorCodeException(
                    errorCode = ERROR_NODE_TAG_NO_ALLOW_VALUES,
                    params = arrayOf(tagKeys[tag.tagKeyId]?.second ?: "")
                )
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // 标签规则 -> 节点 匹配
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * 批量查询 TAG 类型环境命中的节点，匹配规则见 [batchMatchNodesByTags]。
     * @return <envId, Set<nodeId>>，仅包含命中了节点的环境
     */
    fun batchEnvTagNode(
        projectId: String,
        envIds: Set<Long>
    ): Map<Long, Set<Long>> {
        if (envIds.isEmpty()) {
            return emptyMap()
        }
        val envTagKeyValues = envTagDao.fetchEnvTagKeyValues(dslContext, projectId, envIds)
        return batchMatchNodesByTags(projectId, envTagKeyValues).filterValues { it.isNotEmpty() }
    }

    /**
     * 批量统计 TAG 类型环境命中的节点数量，匹配规则见 [batchMatchNodesByTags]，并按节点类型过滤。
     * @return <envId, nodeCount>，仅包含 nodeCount > 0 的环境
     */
    fun batchEnvTagNodeCount(
        projectId: String,
        envIds: Set<Long>,
        nodeType: Set<String>
    ): Map<Long, Int> {
        if (envIds.isEmpty()) {
            return emptyMap()
        }
        val envTagKeyValues = envTagDao.fetchEnvTagKeyValues(dslContext, projectId, envIds)
        val envMatchedNodes = batchMatchNodesByTags(projectId, envTagKeyValues)
        val allMatchedNodeIds = envMatchedNodes.values.flatMapTo(mutableSetOf()) { it }
        if (allMatchedNodeIds.isEmpty()) {
            return emptyMap()
        }
        val validNodeIds = nodeDao.listNodeIdsByType(dslContext, projectId, allMatchedNodeIds, nodeType)
        if (validNodeIds.isEmpty()) {
            return emptyMap()
        }
        return envMatchedNodes
            .mapValues { (_, nodeIds) -> nodeIds.count { it in validNodeIds } }
            .filterValues { it > 0 }
    }

    /**
     * 统计 TAG 类型环境命中的节点数量（非创作流），按每个 env 自身的环境类型对应的节点类型过滤。
     * @return <envId, nodeCount>
     */
    fun getTagNodeCount(
        projectId: String,
        tagEnvs: List<TEnvRecord>
    ): Map<Long, Int> {
        if (tagEnvs.isEmpty()) {
            return emptyMap()
        }
        val envNodeTypeMap = tagEnvs.associate { it.envId to EnvType.toNodeType(it.envType) }
        val envTagKeyValues = envTagDao.fetchEnvTagKeyValues(dslContext, projectId, envNodeTypeMap.keys)
        val envMatchedNodes = batchMatchNodesByTags(projectId, envTagKeyValues)
        val allMatchedNodeIds = envMatchedNodes.values.flatMapTo(mutableSetOf()) { it }
        if (allMatchedNodeIds.isEmpty()) {
            return emptyMap()
        }
        val nodeTypeMap = nodeDao.fetchNodeWithType(dslContext, projectId, allMatchedNodeIds)
        return envMatchedNodes.mapValues { (envId, nodeIds) ->
            val envNodeType = envNodeTypeMap[envId]
            nodeIds.count { nodeId ->
                val nodeType = nodeTypeMap[nodeId] ?: return@count false
                NodeType.get(nodeType) == envNodeType
            }
        }
    }

    /**
     * 统计 TAG 类型创作流环境命中的节点数量，按创作流节点权限过滤、按环境系统(OS)匹配。
     * 调用方需保证 tagEnvs 中创作流环境的 os 已修复（非空）。
     * @return <envId, nodeCount>
     */
    fun getCreateTagNodeCount(
        projectId: String,
        tagEnvs: List<TEnvRecord>,
        permissionNodes: Set<Long>
    ): Map<Long, Int> {
        if (tagEnvs.isEmpty()) {
            return emptyMap()
        }
        val envOsMap = tagEnvs.associate { it.envId to it.os }
        val envTagKeyValues = envTagDao.fetchEnvTagKeyValues(dslContext, projectId, envOsMap.keys)
        val envMatchedNodes = batchMatchNodesByTags(projectId, envTagKeyValues)
        val candidateNodeIds = envMatchedNodes.values
            .flatMapTo(mutableSetOf()) { it }
            .filterTo(mutableSetOf()) { it in permissionNodes }
        if (candidateNodeIds.isEmpty()) {
            return emptyMap()
        }
        val nodeOsMap = thirdPartyAgentDao.getCreateAgentsByNodeIdsWithOs(dslContext, projectId, candidateNodeIds)
        return envMatchedNodes.mapValues { (envId, nodeIds) ->
            val envOs = envOsMap[envId]
            nodeIds.count { nodeId ->
                if (nodeId !in permissionNodes) return@count false
                val nodeOs = nodeOsMap[nodeId] ?: return@count false
                nodeOs == envOs
            }
        }
    }

    /**
     * 预览动态环境按标签会匹配到的节点，匹配规则与动态环境节点关联完全一致，见 [batchMatchNodesByTags]。
     * 返回结构与节点列表 fetchNodes 一致。
     */
    fun previewTagEnvNodes(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        tags: List<NodeTagAddOrDeleteTagItem>
    ): Page<NodeWithPermission> {
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, AuthPermission.VIEW)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_ENV_NO_VIEW_PERMISSSION)
            )
        }
        val curPage = page ?: 1
        val curPageSize = pageSize ?: 20
        val matchedNodeIds = matchNodesByTags(projectId, groupTagKeyValues(tags))
        if (matchedNodeIds.isEmpty()) {
            return Page(page = curPage, pageSize = curPageSize, count = 0, records = emptyList())
        }
        val nodeRecordList = nodeDao.listAllByIds(dslContext, projectId, matchedNodeIds)
        val nodes = nodeService.formatNodeWithPermissions(
            userId = userId,
            projectId = projectId,
            nodeRecordList = nodeRecordList,
            envId = null
        )
        // 内存分页，page = -1 表示不分页
        val records = if (curPage == -1) {
            nodes
        } else {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(curPage, curPageSize)
            nodes.drop(sqlLimit.offset).take(sqlLimit.limit)
        }
        return Page(
            page = curPage,
            pageSize = curPageSize,
            count = nodes.size.toLong(),
            records = records
        )
    }

    /**
     * 动态环境标签匹配节点的唯一入口（批量版）。
     * 动态环境的节点关联、节点数统计、以及创建环境前的预览都必须走这里，保证规则一致。
     *
     * 匹配规则：同一 tagKey 内的多个值为 OR，不同 tagKey 之间为 AND，
     * 即节点对规则中的每一个 tagKey，都至少命中该 key 下的一个标签值。
     *
     * @param rules <规则标识(通常为 envId), <tagKeyId, Set<tagValueId>>>
     * @return <规则标识, Set<nodeId>>，未命中任何节点的规则对应空集合
     */
    private fun <K> batchMatchNodesByTags(
        projectId: String,
        rules: Map<K, Map<Long, Set<Long>>>
    ): Map<K, Set<Long>> {
        if (rules.isEmpty()) {
            return emptyMap()
        }
        // 1. 一次性查询所有规则涉及的标签值下，每个节点拥有的标签值集合
        val allTagValueIds = rules.values
            .flatMapTo(mutableSetOf()) { keyValues -> keyValues.values.flatten() }
        val nodeTagValues = envTagDao.fetchNodeTagValues(dslContext, projectId, allTagValueIds)
        // 2. 内存匹配
        return rules.mapValues { (_, keyValues) ->
            if (keyValues.isEmpty()) {
                emptySet()
            } else {
                nodeTagValues.filterValues { nodeValues -> nodeMatchEnvTags(nodeValues, keyValues) }.keys
            }
        }
    }

    /**
     * 动态环境标签匹配节点（单规则版），规则见 [batchMatchNodesByTags]。
     * @param tagKeyValues <tagKeyId, Set<tagValueId>>
     */
    private fun matchNodesByTags(projectId: String, tagKeyValues: Map<Long, Set<Long>>): Set<Long> {
        return batchMatchNodesByTags(projectId, mapOf(Unit to tagKeyValues))[Unit] ?: emptySet()
    }

    /**
     * 判断节点是否匹配 env 的标签条件：同一 tagKey 内的值为 OR，不同 tagKey 之间为 AND。
     */
    private fun nodeMatchEnvTags(nodeValues: Set<Long>?, envKeyValues: Map<Long, Set<Long>>): Boolean {
        if (nodeValues.isNullOrEmpty()) {
            return false
        }
        return envKeyValues.values.all { valuesOfKey -> valuesOfKey.any { it in nodeValues } }
    }

    /**
     * 将标签列表整理成与 T_ENV_TAG 相同的规则结构：<tagKeyId, Set<tagValueId>>
     */
    private fun groupTagKeyValues(tags: List<NodeTagAddOrDeleteTagItem>): Map<Long, Set<Long>> {
        return tags.groupBy(
            keySelector = { it.tagKeyId },
            valueTransform = { it.tagValueId }
        ).mapValues { (_, values) -> values.toSet() }
    }
}
