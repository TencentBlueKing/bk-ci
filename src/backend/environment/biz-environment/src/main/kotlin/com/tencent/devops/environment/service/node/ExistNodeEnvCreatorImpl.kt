package com.tencent.devops.environment.service.node

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.enums.NodeSource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ExistNodeEnvCreatorImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val environmentPermissionService: EnvironmentPermissionService
) : EnvCreator {

    override fun id(): String {
        return NodeSource.EXISTING.name
    }

    override fun createEnv(projectId: String, userId: String, envCreateInfo: EnvCreateInfo): EnvironmentId {

        if (envCreateInfo.source.name != id()) {
            throw IllegalArgumentException("wrong nodeSourceType [${envCreateInfo.source}] in [${id()}]")
        }

        val nodeLongIds = envCreateInfo.nodeHashIds!!.map { HashUtil.decodeIdToLong(it) }

        // 检查 node 权限
        val canUseNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.USE)
        val unauthorizedNodeIds = nodeLongIds.filterNot { canUseNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw OperationException(
                "节点权限不足 [${unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }}]"
            )
        }

        // 检查 node 是否存在
        val existNodes = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
        val existNodeIds = existNodes.map { it.nodeId }.toSet()
        val notExistNodeIds = nodeLongIds.filterNot { existNodeIds.contains(it) }
        if (notExistNodeIds.isNotEmpty()) {
            throw OperationException("节点 [${notExistNodeIds.joinToString(",") { HashUtil.encodeLongId(it) }}] 不存在")
        }

        var envId = 0L
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            envId = envDao.create(
                context, userId, projectId, envCreateInfo.name, envCreateInfo.desc,
                envCreateInfo.envType.name, ObjectMapper().writeValueAsString(envCreateInfo.envVars)
            )
            envNodeDao.batchStoreEnvNode(context, nodeLongIds, envId, projectId)
            environmentPermissionService.createEnv(userId, projectId, envId, envCreateInfo.name)
        }
        return EnvironmentId(HashUtil.encodeLongId(envId))
    }
}